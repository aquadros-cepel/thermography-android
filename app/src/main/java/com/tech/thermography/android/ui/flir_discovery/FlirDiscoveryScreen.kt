package com.tech.thermography.android.ui.flir_discovery

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.flir.thermalsdk.live.Identity
import com.flir.thermalsdk.live.importing.FileInfo
import com.tech.thermography.android.flir.network.CameraConnectionState
import java.io.File

/**
 * Tela de descoberta, conexão e importação de termogramas de câmeras FLIR via WiFi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlirDiscoveryScreen(
    navController: NavController,
    viewModel: FlirDiscoveryViewModel = hiltViewModel()
) {
    val discoveryRunning by viewModel.discoveryRunning.collectAsState()
    val foundCameras by viewModel.foundCameras.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val cameraFiles by viewModel.cameraFiles.collectAsState()
    val importedFiles by viewModel.importedFiles.collectAsState()
    val monitoringActive by viewModel.monitoringActive.collectAsState()
    val lastImportedFile by viewModel.lastImportedFile.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val discoveryAttempted by viewModel.discoveryAttempted.collectAsState()

    val isConnected = connectionState is CameraConnectionState.Connected
    val isConnecting = connectionState is CameraConnectionState.Authenticating ||
            connectionState is CameraConnectionState.WaitingApproval ||
            connectionState is CameraConnectionState.Connecting

    // ── Permissões necessárias para discovery FLIR ─────────────────────────
    val context = LocalContext.current
    val requiredPermissions = arrayOf(
        Manifest.permission.NEARBY_WIFI_DEVICES,   // Android 13+: scan WiFi sem exigir localização
        Manifest.permission.ACCESS_FINE_LOCATION   // fallback / exigido por alguns SDKs
    )

    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            permissionDenied = false
            viewModel.startDiscovery()
        } else {
            permissionDenied = true
        }
    }

    fun checkAndStartDiscovery() {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PermissionChecker.PERMISSION_GRANTED
        }
        if (allGranted) {
            permissionDenied = false
            viewModel.startDiscovery()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }
    // ──────────────────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Câmeras FLIR") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // ── Aviso de permissão negada ──────────────────────────────
            if (permissionDenied) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "⚠️ Permissão necessária\n\n" +
                                        "Conceda 'Dispositivos próximos' e 'Localização' nas configurações do app para fazer discovery de câmeras WiFi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            TextButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("Abrir Configurações", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // ── Secção Discovery ──────────────────────────────────────────
            item {
                DiscoverySection(
                    discoveryRunning = discoveryRunning,
                    onStartDiscovery = { checkAndStartDiscovery() },
                    onStopDiscovery = { viewModel.stopDiscovery() }
                )
            }

            // ── Mensagem de status / erro (só mostra erro ou estado de conexão em progresso) ──
            if (errorMessage != null || isConnecting) {
                item {
                    StatusCard(
                        statusMessage = statusMessage,
                        errorMessage = errorMessage,
                        isConnecting = isConnecting
                    )
                }
            }

            // ── Lista de câmeras descobertas ──────────────────────────────
            if (foundCameras.isNotEmpty()) {
                item {
                    Text(
                        text = "Câmeras encontradas (${foundCameras.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(foundCameras) { identity ->
                    CameraItem(
                        identity = identity,
                        connectionState = connectionState,
                        onConnect = { viewModel.connectToCamera(identity) },
                        onDisconnect = { viewModel.disconnectCamera() }
                    )
                }
            } else if (!discoveryRunning && discoveryAttempted) {
                item {
                    EmptyCamerasPlaceholder()
                }
            }

            // ── Seção de arquivos (só visível quando conectado) ───────────
            if (isConnected) {
                item { HorizontalDivider() }

                item {
                    CameraFilesSection(
                        cameraFiles = cameraFiles,
                        importedFiles = importedFiles,
                        monitoringActive = monitoringActive,
                        lastImportedFile = lastImportedFile,
                        onImportAll = { viewModel.importAllFiles() },
                        onRefreshFiles = { viewModel.refreshFileList() },
                        onStartMonitor = { viewModel.startMonitoring() },
                        onStopMonitor = { viewModel.stopMonitoring() }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes internos
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DiscoverySection(
    discoveryRunning: Boolean,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Descoberta de Câmeras",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = if (discoveryRunning) "🔍 Procurando câmeras FLIR na rede WiFi..."
                else "Inicie a descoberta para encontrar câmeras FLIR disponíveis na rede.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStartDiscovery,
                    enabled = !discoveryRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Iniciar")
                }
                OutlinedButton(
                    onClick = onStopDiscovery,
                    enabled = discoveryRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Parar")
                }
            }

            if (discoveryRunning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    statusMessage: String,
    errorMessage: String?,
    isConnecting: Boolean
) {
    val isError = errorMessage != null
    val containerColor = when {
        isError -> MaterialTheme.colorScheme.errorContainer
        isConnecting -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        isError -> MaterialTheme.colorScheme.onErrorContainer
        isConnecting -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = errorMessage ?: statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun CameraItem(
    identity: Identity,
    connectionState: CameraConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val deviceId = identity.deviceId
    // Tenta obter o IP via toString do Identity; pode ser que a SDK não exponha diretamente em Kotlin
    val cameraDescription = runCatching { identity.toString() }.getOrDefault(deviceId)

    val isMineConnected = connectionState is CameraConnectionState.Connected &&
            connectionState.deviceId == deviceId
    val isMineConnecting = (connectionState is CameraConnectionState.Authenticating ||
            connectionState is CameraConnectionState.WaitingApproval ||
            connectionState is CameraConnectionState.Connecting) &&
            when (connectionState) {
                is CameraConnectionState.Authenticating -> connectionState.deviceId == deviceId
                is CameraConnectionState.WaitingApproval -> connectionState.deviceId == deviceId
                is CameraConnectionState.Connecting -> connectionState.deviceId == deviceId
                else -> false
            }

    val borderColor = when {
        isMineConnected -> Color(0xFF4CAF50)
        isMineConnecting -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isMineConnected) Color(0xFF4CAF50).copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = if (isMineConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceId,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (cameraDescription != deviceId) {
                    Text(
                        text = cameraDescription,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val stateText = when {
                    isMineConnected -> "Conectado ✅"
                    isMineConnecting -> "Conectando..."
                    else -> "Disponível"
                }
                Text(
                    text = stateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMineConnected) Color(0xFF4CAF50)
                    else if (isMineConnecting) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Botão de ação
            when {
                isMineConnected -> {
                    OutlinedButton(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Desconectar", fontSize = 12.sp)
                    }
                }
                isMineConnecting -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
                else -> {
                    Button(onClick = onConnect) {
                        Text("Conectar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCamerasPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = "Nenhuma câmera encontrada",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Verifique se a câmera FLIR está ligada e conectada à mesma rede WiFi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun CameraFilesSection(
    cameraFiles: List<FileInfo>,
    importedFiles: List<String>,
    monitoringActive: Boolean,
    lastImportedFile: String?,
    onImportAll: () -> Unit,
    onRefreshFiles: () -> Unit,
    onStartMonitor: () -> Unit,
    onStopMonitor: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Cabeçalho
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Termogramas na Câmera",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRefreshFiles) {
                Icon(Icons.Default.Refresh, contentDescription = "Atualizar lista")
            }
        }

        // Contadores
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip(label = "Na câmera", value = "${cameraFiles.size}")
            InfoChip(label = "Importados", value = "${importedFiles.size}")
        }

        // Botões de ação
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onImportAll,
                enabled = cameraFiles.isNotEmpty(),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Importar Todos", maxLines = 1, softWrap = false)
            }

            if (monitoringActive) {
                OutlinedButton(
                    onClick = onStopMonitor,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Parar Monitor")
                }
            } else {
                OutlinedButton(
                    onClick = onStartMonitor,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Monitorar")
                }
            }
        }

        // Status do monitoramento
        if (monitoringActive) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF00BCD4).copy(alpha = 0.10f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF00BCD4)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Monitorando novos termogramas...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF00BCD4)
                    )
                }
            }
        }

        // Último arquivo importado
        lastImportedFile?.let { path ->
            AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Último importado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = File(path).name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Lista de arquivos da câmera
        if (cameraFiles.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                cameraFiles.take(20).forEach { fileInfo ->
                    val remoteName = fileInfo.reference.path
                        .substringAfterLast('/').substringAfterLast('\\').ifBlank { fileInfo.reference.path }
                    val isImported = importedFiles.any { it.endsWith(remoteName) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isImported) "✅" else "📷",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = remoteName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isImported) Color(0xFF4CAF50)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (cameraFiles.size > 20) {
                    Text(
                        text = "... e mais ${cameraFiles.size - 20} arquivos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}






