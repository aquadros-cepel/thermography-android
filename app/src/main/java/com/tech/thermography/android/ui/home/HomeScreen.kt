package com.tech.thermography.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tech.thermography.android.data.local.entity.enumeration.ConditionType
import com.tech.thermography.android.navigation.NavBarItems
import com.tech.thermography.android.navigation.NavRoutes
import java.io.File
import java.net.URLEncoder
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val navBarItems = NavBarItems.items
    var showLogoutDialog by remember { mutableStateOf(false) }
    val recentRecords by viewModel.recentRecords.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CenterAlignedTopAppBar(
            title = { Text("Thermal Energy") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Renderiza os botões em 2 linhas x 2 colunas
            for (row in 0 until 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0 until 2) {
                        val index = row * 2 + col
                        if (index < navBarItems.size) {
                            val (route, label, icon) = navBarItems[index]
                            Button(
                                onClick = {
                                    if (label == "Logout") {
                                        showLogoutDialog = true
                                    } else {
                                        navController?.navigate(route)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(2f)
                                    .padding(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(icon, contentDescription = label, tint = Color.Black)
                                    Text(
                                        text = label,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Título da seção
        Text(
            text = "Últimos Registros de Inspeção Termográfica",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (recentRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum registro encontrado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentRecords) { item ->
                    RecentRecordCard(
                        item = item,
                        onClick = {
                            val p = item.record.plantId.toString()
                            val e = item.record.equipmentId.toString()
                            val r = item.record.routeId?.toString() ?: "null"
                            val t = item.record.id.toString()
                            val rp = URLEncoder.encode(p, "UTF-8")
                            val re = URLEncoder.encode(e, "UTF-8")
                            val rr = URLEncoder.encode(r, "UTF-8")
                            val rt = URLEncoder.encode(t, "UTF-8")
                            val route = "${NavRoutes.THERMAL_ANOMALY}?plantId=$rp&equipmentId=$re&inspectionRecordId=$rr&thermographicId=$rt"
                            navController?.navigate(route)
                        }
                    )
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirmar Logout") },
            text = {
                Text(
                    "Após fazer o Logout você só conseguirá fazer Login estando on-line.\n\nSe estiver trabalhando em um local sem conexão de rede não será possível autenticar via Login e entrar na aplicação.\n\nTem certeza que deseja fazer Logout?",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout {
                            navController?.navigate(NavRoutes.LOGIN) {
                                popUpTo(NavRoutes.HOME) { inclusive = true }
                            }
                        }
                    }
                ) {
                    Text("Sim", color = MaterialTheme.colorScheme.error, fontSize = 18.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Não", fontSize = 18.sp)
                }
            }
        )
    }
}

@Composable
private fun RecentRecordCard(
    item: RecentRecordItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
    }
    val dateText = remember(item.record.createdAt) {
        item.record.createdAt
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }
    val conditionLabel = when (item.record.condition) {
        ConditionType.NORMAL -> "Normal"
        ConditionType.LOW_RISK -> "Baixo Risco"
        ConditionType.MEDIUM_RISK -> "Médio Risco"
        ConditionType.HIGH_RISK -> "Alto Risco"
        ConditionType.IMMINENT_HIGH_RISK -> "Risco Iminente"
    }
    val conditionColor = when (item.record.condition) {
        ConditionType.NORMAL -> Color(0xFF4CAF50)
        ConditionType.LOW_RISK -> Color(0xFF8BC34A)
        ConditionType.MEDIUM_RISK -> Color(0xFFFFC107)
        ConditionType.HIGH_RISK -> Color(0xFFFF5722)
        ConditionType.IMMINENT_HIGH_RISK -> Color(0xFFB71C1C)
    }

    val imageUri = remember(item.thermogram) {
        val t = item.thermogram ?: return@remember null
        when {
            t.localImagePath.isNotBlank() -> File(t.localImagePath).toUri()
            !t.imagePath.isNullOrBlank() -> t.imagePath.toUri()
            else -> null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagem do termograma
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Termograma",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "🌡",
                        fontSize = 32.sp
                    )
                }
            }

            // Informações
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.equipment?.let { eq ->
                        val code = eq.code?.takeIf { it.isNotBlank() }
                        if (code != null) "$code (${eq.name})" else eq.name
                    } ?: item.record.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(conditionColor, shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = conditionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


