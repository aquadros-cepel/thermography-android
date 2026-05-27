package com.tech.thermography.android.flir.network

import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import com.flir.thermalsdk.ErrorCode
import com.flir.thermalsdk.androidsdk.helpers.ShowMessage
import com.flir.thermalsdk.live.AuthenticationResponse
import com.flir.thermalsdk.live.Identity
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener
import com.flir.thermalsdk.live.discovery.DiscoveredCamera
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener
import com.flir.thermalsdk.live.importing.FileInfo
import com.flir.thermalsdk.live.importing.FileReference
import com.flir.thermalsdk.log.ThermalLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper Kotlin (coroutine-friendly) para operações de câmera FLIR via rede WiFi.
 *
 * Encapsula [CameraHandler] expondo StateFlows reativos para uso em Jetpack Compose ViewModels.
 */
@Singleton
class FlirNetworkService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "FlirNetworkService"
        private const val MONITOR_INTERVAL_MS = 1_000L
        /** Número mínimo de ciclos consecutivos que um arquivo deve aparecer antes de ser importado.
         *  Garante que a câmera terminou de gravar (1 ciclo = 1 segundo). */
        private const val FILE_STABLE_CYCLES = 3
    }

    // Escopo próprio do serviço — sobrevive à destruição de qualquer ViewModel
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitoringJob: Job? = null

    private val fileHandler = FileHandler(context)
    private val cameraHandler = CameraHandler(
        ShowMessage { msg -> ThermalLog.w(TAG, msg) },
        fileHandler
    )

    // ---- MulticastLock: necessário para mDNS/Bonjour usado pelo FLIR SDK ----
    private val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val multicastLock: WifiManager.MulticastLock =
        wifiManager.createMulticastLock("FlirDiscovery").apply {
            setReferenceCounted(true)
        }

    // ---- Estado reativo ----

    private val _discoveryRunning = MutableStateFlow(false)
    val discoveryRunning: StateFlow<Boolean> = _discoveryRunning.asStateFlow()

    private val _foundCameras = MutableStateFlow<List<Identity>>(emptyList())
    val foundCameras: StateFlow<List<Identity>> = _foundCameras.asStateFlow()

    private val _connectionState = MutableStateFlow<CameraConnectionState>(CameraConnectionState.Idle)
    val connectionState: StateFlow<CameraConnectionState> = _connectionState.asStateFlow()

    private val _cameraFiles = MutableStateFlow<List<FileInfo>>(emptyList())
    val cameraFiles: StateFlow<List<FileInfo>> = _cameraFiles.asStateFlow()

    private val _importedFiles = MutableStateFlow<List<String>>(emptyList())
    val importedFiles: StateFlow<List<String>> = _importedFiles.asStateFlow()

    private val _monitoringActive = MutableStateFlow(false)
    val monitoringActive: StateFlow<Boolean> = _monitoringActive.asStateFlow()

    private val _lastImportedFile = MutableStateFlow<String?>(null)
    val lastImportedFile: StateFlow<String?> = _lastImportedFile.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Identidade da câmera conectada no momento
    private var connectedIdentity: Identity? = null

    // Controle do monitoramento
    private val knownFilePaths = mutableSetOf<String>()
    /**
     * Arquivos detectados mas ainda não estáveis.
     * Chave = path do arquivo na câmera
     * Valor = quantos ciclos consecutivos o arquivo foi visto
     * Quando atingir FILE_STABLE_CYCLES → câmera terminou de gravar → importar.
     */
    private val pendingFiles = mutableMapOf<String, Int>()

    // ---- Discovery ----

    fun startDiscovery() {
        // ── Log de diagnóstico de permissões ──────────────────────────────
        val permissionsToCheck = listOf(
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.CHANGE_WIFI_MULTICAST_STATE
        )
        permissionsToCheck.forEach { perm ->
            val granted = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            ThermalLog.d(TAG, "Permission [$perm] granted=$granted")
        }
        // ─────────────────────────────────────────────────────────────────

        cameraHandler.clear()
        _foundCameras.value = emptyList()
        _errorMessage.value = null

        // Adquire MulticastLock para permitir que o FLIR SDK receba pacotes mDNS
        if (!multicastLock.isHeld) {
            multicastLock.acquire()
            ThermalLog.d(TAG, "MulticastLock acquired")
        }

        cameraHandler.startDiscovery(
            object : DiscoveryEventListener {
                override fun onCameraFound(discoveredCamera: DiscoveredCamera) {
                    ThermalLog.d(TAG, "Camera found: ${discoveredCamera.identity}")
                    cameraHandler.add(discoveredCamera.identity)
                    _foundCameras.value = cameraHandler.getCameraList().toList()
                }

                override fun onDiscoveryError(communicationInterface: com.flir.thermalsdk.live.CommunicationInterface, errorCode: ErrorCode) {
                    ThermalLog.e(TAG, "Discovery error [$communicationInterface]: $errorCode")
                    // Só interrompe o discovery se o erro for na interface NETWORK
                    if (communicationInterface == com.flir.thermalsdk.live.CommunicationInterface.NETWORK) {
                        val msg = when (errorCode.code) {
                            4 -> "Permissão negada — verifique se 'Dispositivos próximos' foi concedida (code=4)"
                            else -> "Erro no discovery: $errorCode"
                        }
                        _errorMessage.value = msg
                        _discoveryRunning.value = false
                    }
                }
            },
            object : CameraHandler.DiscoveryStatus {
                override fun started() { _discoveryRunning.value = true }
                override fun stopped() { _discoveryRunning.value = false }
            }
        )
    }

    fun stopDiscovery() {
        cameraHandler.stopDiscovery(object : CameraHandler.DiscoveryStatus {
            override fun started() {}
            override fun stopped() {
                _discoveryRunning.value = false
                // Libera o MulticastLock ao parar o discovery
                if (multicastLock.isHeld) {
                    multicastLock.release()
                    ThermalLog.d(TAG, "MulticastLock released")
                }
            }
        })
    }

    // ---- Conexão ----

    /**
     * Autentica e conecta na câmera indicada (bloqueante — deve ser chamado em Dispatchers.IO).
     * Atualiza [connectionState] durante o processo.
     */
    suspend fun connectToCamera(identity: Identity) = withContext(Dispatchers.IO) {
        if (connectedIdentity != null) {
            ThermalLog.w(TAG, "Already connected to ${connectedIdentity?.deviceId}")
            _errorMessage.value = "Já conectado a ${connectedIdentity?.deviceId}"
            return@withContext
        }

        stopDiscovery()
        connectedIdentity = identity
        _errorMessage.value = null
        _connectionState.value = CameraConnectionState.Authenticating(identity.deviceId)

        val appName = CameraAuthName.getApplicationName(context)
        ThermalLog.d(TAG, "Authenticating with name: $appName")

        try {
            var response: AuthenticationResponse
            do {
                response = cameraHandler.authenticate(identity, appName)
                if (response.authenticationStatus != AuthenticationResponse.AuthenticationStatus.APPROVED) {
                    _connectionState.value = CameraConnectionState.WaitingApproval(identity.deviceId)
                    ThermalLog.d(TAG, "Waiting camera approval...")
                    delay(1_000)
                }
            } while (response.authenticationStatus != AuthenticationResponse.AuthenticationStatus.APPROVED)

            _connectionState.value = CameraConnectionState.Connecting(identity.deviceId)
            ThermalLog.d(TAG, "Connecting to ${identity.deviceId}")

            cameraHandler.connect(identity, object : ConnectionStatusListener {
                override fun onDisconnected(errorCode: ErrorCode?) {
                    ThermalLog.d(TAG, "Camera disconnected: $errorCode")
                    connectedIdentity = null
                    _connectionState.value = CameraConnectionState.Disconnected(identity.deviceId, errorCode?.toString())
                    stopMonitoring()
                }
            })

            _connectionState.value = CameraConnectionState.Connected(identity.deviceId)
            ThermalLog.d(TAG, "Connected to ${identity.deviceId}")

            // Listar arquivos disponíveis logo após conectar
            refreshFileList()

        } catch (e: IOException) {
            ThermalLog.e(TAG, "Connection failed: $e")
            connectedIdentity = null
            _connectionState.value = CameraConnectionState.Error(identity.deviceId, e.message ?: "Erro desconhecido")
            _errorMessage.value = "Falha na conexão: ${e.message}"
        } catch (e: InterruptedException) {
            ThermalLog.e(TAG, "Connection interrupted: $e")
            connectedIdentity = null
            _connectionState.value = CameraConnectionState.Error(identity.deviceId, "Operação interrompida")
        }
    }

    fun disconnect() {
        stopMonitoring()
        connectedIdentity?.let { id ->
            _connectionState.value = CameraConnectionState.Disconnected(id.deviceId, null)
        }
        connectedIdentity = null
        cameraHandler.disconnect()
        // Reseta o último arquivo importado para que o banner não apareça após desconectar
        _lastImportedFile.value = null
    }

    // ---- Listagem e importação de arquivos ----

    /**
     * Atualiza [cameraFiles] com a lista de imagens disponíveis na câmera.
     * Também sincroniza [importedFiles] com os arquivos que já existem no disco local,
     * garantindo que arquivos importados em sessões anteriores apareçam como importados (✅).
     * Deve ser chamado em Dispatchers.IO.
     */
    suspend fun refreshFileList() = withContext(Dispatchers.IO) {
        try {
            val files = cameraHandler.listAllImages()
            _cameraFiles.value = files

            // Pré-popula importedFiles com os arquivos que já existem no disco local.
            // Isso garante que arquivos importados em sessões anteriores sejam exibidos
            // como importados (✅) mesmo após reiniciar o app ou reconectar com a câmera.
            val storageDir = fileHandler.getImageStoragePath()
            val existingLocalFiles = storageDir.listFiles()?.map { it.absolutePath } ?: emptyList()

            val alreadyOnDisk = files
                .map { extractFileName(it.reference.path) }
                .filter { remoteName ->
                    existingLocalFiles.any { localPath -> localPath.endsWith(remoteName) }
                }
                .map { remoteName -> File(storageDir, remoteName).absolutePath }

            val current = _importedFiles.value.toMutableList()
            alreadyOnDisk.forEach { path -> if (!current.contains(path)) current.add(path) }
            _importedFiles.value = current

            ThermalLog.d(TAG, "Refreshed file list: ${files.size} files, ${current.size} already imported")
        } catch (e: IOException) {
            ThermalLog.e(TAG, "Failed to list files: $e")
            _errorMessage.value = "Erro ao listar arquivos: ${e.message}"
        }
    }

    /**
     * Importa todos os arquivos disponíveis na câmera para o armazenamento local.
     * Deve ser chamado em Dispatchers.IO.
     */
    suspend fun importAllFiles() = withContext(Dispatchers.IO) {
        try {
            val allFiles = cameraHandler.listAllImages()
            if (allFiles.isEmpty()) {
                ThermalLog.w(TAG, "importAllFiles: no files to import")
                return@withContext
            }

            val references = allFiles.map { it.reference }
            importFileReferences(references)
        } catch (e: IOException) {
            ThermalLog.e(TAG, "importAllFiles failed: $e")
            _errorMessage.value = "Erro ao importar arquivos: ${e.message}"
        }
    }

    private suspend fun importFileReferences(references: List<FileReference>) = withContext(Dispatchers.IO) {
        cameraHandler.importFiles(references, object : CameraHandler.ImportInformation {
            override fun importedFile(file: FileReference, savedInDir: File) {
                val localPath = File(savedInDir, extractFileName(file.path)).absolutePath
                ThermalLog.d(TAG, "File imported: $localPath")
                val current = _importedFiles.value.toMutableList()
                if (!current.contains(localPath)) current.add(localPath)
                _importedFiles.value = current
                _lastImportedFile.value = localPath
                knownFilePaths.add(file.path)
            }

            override fun importedFileError(file: FileReference, errorCode: ErrorCode) {
                ThermalLog.e(TAG, "Import error for ${file.path}: $errorCode")
                _errorMessage.value = "Erro ao importar ${extractFileName(file.path)}: $errorCode"
            }
        })
    }

    // ---- Monitoramento contínuo ----

    /**
     * Inicia monitoramento em background: verifica a cada 1 segundo por novos termogramas.
     * Roda no [serviceScope] — continua mesmo quando o usuário sai da tela de discovery.
     */
    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
        _monitoringActive.value = true
        knownFilePaths.clear()
        knownFilePaths.addAll(_cameraFiles.value.map { it.reference.path })
        pendingFiles.clear()
        ThermalLog.d(TAG, "Monitoring started (background)")

        monitoringJob = serviceScope.launch {
            while (true) {
                delay(MONITOR_INTERVAL_MS)
                if (!_monitoringActive.value) break
                checkForNewFiles()
            }
            _monitoringActive.value = false
            ThermalLog.d(TAG, "Monitoring stopped")
        }
    }

    fun stopMonitoring() {
        _monitoringActive.value = false
        monitoringJob?.cancel()
        monitoringJob = null
        pendingFiles.clear()
    }

    private suspend fun checkForNewFiles() = withContext(Dispatchers.IO) {
        try {
            val currentFiles = cameraHandler.listAllImages()
            val currentPaths = currentFiles.map { it.reference.path }.toSet()

            // 1. Arquivos novos → entram em pendingFiles com contagem 1
            currentFiles
                .filter { it.reference.path !in knownFilePaths && it.reference.path !in pendingFiles }
                .forEach { file ->
                    pendingFiles[file.reference.path] = 1
                    ThermalLog.d(TAG, "Novo arquivo (ciclo 1/${FILE_STABLE_CYCLES}): ${extractFileName(file.reference.path)}")
                }

            // 2. Arquivos pendentes ainda presentes na câmera → incrementa contagem
            pendingFiles.keys
                .filter { it in currentPaths && pendingFiles[it]!! < FILE_STABLE_CYCLES }
                .forEach { path ->
                    val newCount = pendingFiles[path]!! + 1
                    pendingFiles[path] = newCount
                    ThermalLog.d(TAG, "Arquivo estabilizando (ciclo ${newCount}/${FILE_STABLE_CYCLES}): ${extractFileName(path)}")
                }

            // 3. Arquivos que atingiram FILE_STABLE_CYCLES → prontos para importar
            val readyPaths = pendingFiles.filter { it.value >= FILE_STABLE_CYCLES }.keys
            val readyFiles = currentFiles.filter { it.reference.path in readyPaths }

            if (readyFiles.isNotEmpty()) {
                ThermalLog.d(TAG, "${readyFiles.size} arquivo(s) estável(is) após ${FILE_STABLE_CYCLES}s, importando...")
                _cameraFiles.value = currentFiles
                importFileReferences(readyFiles.map { it.reference })
                readyFiles.forEach {
                    knownFilePaths.add(it.reference.path)
                    pendingFiles.remove(it.reference.path)
                }
            }

        } catch (e: IOException) {
            ThermalLog.e(TAG, "checkForNewFiles error: $e")
        }
    }

    // ---- Utilitários ----

    fun getStoragePath(): File = fileHandler.getImageStoragePath()

    private fun extractFileName(remotePath: String): String =
        remotePath.substringAfterLast('/').substringAfterLast('\\').ifBlank { remotePath }
}

/**
 * Estados de conexão com a câmera FLIR de rede.
 */
sealed class CameraConnectionState {
    object Idle : CameraConnectionState()
    data class Authenticating(val deviceId: String) : CameraConnectionState()
    data class WaitingApproval(val deviceId: String) : CameraConnectionState()
    data class Connecting(val deviceId: String) : CameraConnectionState()
    data class Connected(val deviceId: String) : CameraConnectionState()
    data class Disconnected(val deviceId: String, val reason: String?) : CameraConnectionState()
    data class Error(val deviceId: String, val message: String) : CameraConnectionState()
}
