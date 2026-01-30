package com.tech.thermography.android.ui.sync

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

@Composable
fun SyncScreen(
    viewModel: SyncViewModel = hiltViewModel(),
    onSyncComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val animatedProgress by animateFloatAsState(targetValue = uiState.overallProgress, label = "Overall Progress")
    var countdown by remember { mutableStateOf(5) }

    // Inicia a sincronização quando a tela é criada
    LaunchedEffect(Unit) {
//        viewModel.startSync()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (uiState.isSyncFinished) {
            LaunchedEffect(Unit) {
                for (i in 5 downTo 1) {
                    countdown = i
                    delay(1000)
                }
                onSyncComplete()
            }

            Text("Dados sincronizados do dispositivo", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Redirecionando em $countdown s", style = MaterialTheme.typography.bodyLarge)
        } else {
            Text("Sincronizando dados", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(32.dp))

            // Barra de progresso geral
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("${(uiState.overallProgress * 100).toInt()}% concluído")

            Spacer(modifier = Modifier.height(32.dp))

            // Lista de tarefas
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(uiState.tasks) { task ->
                    SyncTaskItem(task = task)
                }
            }
        }
    }
}

@Composable
private fun SyncTaskItem(task: SyncTask) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (task.status) {
            SyncStatus.PENDING -> Icon(Icons.Filled.Refresh, contentDescription = "Pendente", tint = Color.Gray)
            SyncStatus.IN_PROGRESS -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
            SyncStatus.COMPLETED -> Icon(Icons.Filled.CheckCircle, contentDescription = "Concluído", tint = Color(0xFF4CAF50))
            SyncStatus.FAILED -> Icon(Icons.Filled.Close, contentDescription = "Falhou", tint = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.padding(start = 16.dp))
        Text(task.name)
    }
}
