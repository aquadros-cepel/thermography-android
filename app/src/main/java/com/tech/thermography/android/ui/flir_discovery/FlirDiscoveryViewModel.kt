package com.tech.thermography.android.ui.flir_discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flir.thermalsdk.live.Identity
import com.flir.thermalsdk.live.importing.FileInfo
import com.tech.thermography.android.flir.network.CameraConnectionState
import com.tech.thermography.android.flir.network.FlirNetworkService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel da tela de descoberta, conexão e monitoramento de câmeras FLIR de rede.
 */
@HiltViewModel
class FlirDiscoveryViewModel @Inject constructor(
    private val networkService: FlirNetworkService
) : ViewModel() {

    // ---- Repassa os StateFlows do serviço ----
    val discoveryRunning: StateFlow<Boolean> = networkService.discoveryRunning
    val foundCameras: StateFlow<List<Identity>> = networkService.foundCameras
    val connectionState: StateFlow<CameraConnectionState> = networkService.connectionState
    val cameraFiles: StateFlow<List<FileInfo>> = networkService.cameraFiles
    val importedFiles: StateFlow<List<String>> = networkService.importedFiles
    val monitoringActive: StateFlow<Boolean> = networkService.monitoringActive
    val lastImportedFile: StateFlow<String?> = networkService.lastImportedFile
    val errorMessage: StateFlow<String?> = networkService.errorMessage

    // Status legível para o usuário
    private val _statusMessage = MutableStateFlow("Pronto para descoberta")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    /** true após a primeira descoberta ser iniciada e concluída (com ou sem resultado) */
    private val _discoveryAttempted = MutableStateFlow(false)
    val discoveryAttempted: StateFlow<Boolean> = _discoveryAttempted.asStateFlow()

    init {
        // Observa o estado de conexão para atualizar a mensagem de status
        viewModelScope.launch {
            networkService.connectionState.collect { state ->
                _statusMessage.value = when (state) {
                    is CameraConnectionState.Idle -> "Pronto para descoberta"
                    is CameraConnectionState.Authenticating -> "Autenticando com ${state.deviceId}..."
                    is CameraConnectionState.WaitingApproval -> "⏳ Aguardando aprovação na câmera ${state.deviceId}..."
                    is CameraConnectionState.Connecting -> "Conectando em ${state.deviceId}..."
                    is CameraConnectionState.Connected -> "✅ Conectado em ${state.deviceId}"
                    is CameraConnectionState.Disconnected -> "Desconectado de ${state.deviceId}"
                    is CameraConnectionState.Error -> "❌ Erro: ${state.message}"
                }
            }
        }
    }

    // ---- Actions ----

    private var discoveryTimeoutJob: Job? = null

    fun startDiscovery() {
        networkService.startDiscovery()

        // Timeout de 15 segundos: se nenhuma câmera for encontrada, para o discovery
        discoveryTimeoutJob?.cancel()
        discoveryTimeoutJob = viewModelScope.launch {
            delay(15_000)
            if (networkService.foundCameras.value.isEmpty() && networkService.discoveryRunning.value) {
                networkService.stopDiscovery()
                _statusMessage.value = "Nenhuma câmera encontrada"
            }
            _discoveryAttempted.value = true
        }
    }

    fun stopDiscovery() {
        discoveryTimeoutJob?.cancel()
        _discoveryAttempted.value = true
        networkService.stopDiscovery()
    }

    fun connectToCamera(identity: Identity) {
        viewModelScope.launch {
            networkService.connectToCamera(identity)
            // Após conectar, inicia monitoramento automaticamente se conectou com sucesso
            if (networkService.connectionState.value is CameraConnectionState.Connected) {
                startMonitoring()
            }
        }
    }

    fun disconnectCamera() {
        stopMonitoring()
        networkService.disconnect()
    }

    fun importAllFiles() {
        viewModelScope.launch {
            networkService.importAllFiles()
        }
    }

    fun refreshFileList() {
        viewModelScope.launch {
            networkService.refreshFileList()
        }
    }

    fun startMonitoring() {
        networkService.startMonitoring()
    }

    fun stopMonitoring() {
        networkService.stopMonitoring()
    }

    fun clearError() {
        // Limpa a mensagem de erro (expose via networkService quando implementado)
    }

    override fun onCleared() {
        super.onCleared()
        // NÃO para o monitoramento nem desconecta aqui.
        // O FlirNetworkService é @Singleton e continua rodando em background
        // para importar termogramas enquanto o usuário preenche o formulário.
    }
}

