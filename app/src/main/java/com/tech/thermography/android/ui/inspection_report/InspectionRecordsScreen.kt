package com.tech.thermography.android.ui.inspection_report

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.graphics.PathParser
import androidx.hilt.navigation.compose.hiltViewModel
import com.tech.thermography.android.data.local.entity.InspectionRecordEntity
import com.tech.thermography.android.data.local.entity.PlantEntity
import com.tech.thermography.android.ui.components.CompactUiWrapper
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID


@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun InspectionRecordsScreen(
    viewModel: InspectionRecordsViewModel = hiltViewModel(),
    onViewRouteClick: (java.util.UUID) -> Unit = {}
) {
    val plants by viewModel.plants.collectAsState()
    val filteredPlants by viewModel.filteredPlants.collectAsState()
    val filteredInspectionRecords by viewModel.filteredInspectionRecords.collectAsState()
    val selectedPlantId by viewModel.selectedPlantId.collectAsState()

    // Envolvemos tudo em BoxWithConstraints para saber a altura total disponível
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Usamos 'constraints' diretamente para garantir o uso do scope e evitar erros de resolução
        val totalHeightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        // Altura inicial do mapa (50% da tela conforme solicitado)
        var topHeightPx by remember {
            mutableFloatStateOf(totalHeightPx * 0.45f)
        }

        // Converte pixels para Dp para usar no Modifier.height
        val topHeightDp = with(density) { topHeightPx.toDp() }

        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Upper half: Map (Altura Variável)
            Box(
                modifier = Modifier
                    .height(topHeightDp)
                    .fillMaxWidth()
                    .zIndex(0f)
                    .clipToBounds() // CORREÇÃO: Força o corte do conteúdo (mapa) para não vazar para fora da Box
            ) {
                MapComponent(
                    plants = plants,
                    selectedPlantId = selectedPlantId,
                    onPlantSelected = { plantId -> viewModel.selectPlant(plantId) }
                )
            }

            // Draggable Divider (Alça de redimensionamento)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .zIndex(2f)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            // Atualiza a altura em pixels
                            val newHeight = topHeightPx + dragAmount
                            
                            // Limites de segurança (mínimo 100dp para mapa, mínimo 100dp para lista)
                            val minH = with(density) { 100.dp.toPx() }
                            // Calcula limite máximo subtraindo 100dp do total em pixels
                            val maxH = totalHeightPx - with(density) { 100.dp.toPx() }
                            
                            topHeightPx = newHeight.coerceIn(minH, maxH)
                        }
                    }
            ) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = "Resize",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Lower half: Select and List (Ocupa o restante do espaço)
            // Adicionado zIndex(1f) para garantir que a lista fique sobre o mapa caso haja sobreposição visual (ex: dropdowns ou rendering issue do AndroidView)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Importante: Ocupa todo o espaço vertical restante
                    .zIndex(1f)
            ) {
                PlantSelect(
                    plants = plants,
                    selectedPlantId = selectedPlantId,
                    onPlantSelected = { plantId -> viewModel.selectPlant(plantId) }
                )
                
//                Spacer(modifier = Modifier.height(8.dp))
                
                // Passamos Modifier.weight(1f) para a lista ocupar o espaço restante da coluna inferior
                Box(modifier = Modifier.weight(1f)) {
                    InspectionRecordsList(
                        inspectionRecords = filteredInspectionRecords,
                        onViewRouteClick = { record -> onViewRouteClick(record.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MapComponent(
    plants: List<PlantEntity>,
    selectedPlantId: UUID?,
    onPlantSelected: (UUID?) -> Unit // Alterado para aceitar Null (desseleção)
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    // Configuração inicial do mapa (executa apenas uma vez)
    LaunchedEffect(Unit) {
        // Define um User Agent para evitar bloqueios de CDN
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().load(context, context.getSharedPreferences("osm", 0))
        
        // CORREÇÃO DEFINITIVA DO MAPA EMBARALHADO:
        // O padrão OSM é Z/X/Y. O servidor Esri usa Z/Y/X (Row/Col).
        // 1. Criamos um OnlineTileSourceBase customizado para inverter X e Y na URL.
        // 2. Renomeamos a fonte para "EsriImagery" para invalidar o cache anterior (que estava embaralhado).
        val esriSource = object : OnlineTileSourceBase(
            "EsriImagery", // Nome alterado para evitar cache corrompido
            0,
            19,
            256,
            "",
            arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                // Monta a URL no formato: .../tile/{z}/{y}/{x}
                return (baseUrl
                        + MapTileIndex.getZoom(pMapTileIndex)
                        + "/" + MapTileIndex.getY(pMapTileIndex) // Y (Row) vem antes no Esri REST
                        + "/" + MapTileIndex.getX(pMapTileIndex)) // X (Col) vem depois
            }
        }
        
        mapView.setTileSource(esriSource)
        mapView.setMultiTouchControls(true)
        
        // Configuração inicial: Centraliza no Brasil
        val startPoint = GeoPoint(-14.2350, -51.9253) // Centro aproximado do Brasil
        mapView.controller.setCenter(startPoint)
        mapView.controller.setZoom(4.0) // Zoom que mostra a maior parte do país
    }

    // Atualização dos marcadores sempre que a lista de 'plants' ou 'selectedPlantId' mudar
    LaunchedEffect(plants, selectedPlantId) {
        mapView.overlays.clear()
        
        val geoPoints = mutableListOf<GeoPoint>()

        plants.forEach { plant ->
            plant.latitude?.let { lat ->
                plant.longitude?.let { lon ->
                    val point = GeoPoint(lat, lon)
                    geoPoints.add(point)
                    
                    val marker = Marker(mapView)
                    marker.position = point
                    
                    // Lógica do Tooltip: Se selecionado, mostra o CODE, senão mostra o NAME
                    val isSelected = plant.id == selectedPlantId
                    marker.title = if (isSelected) (plant.code ?: plant.name) else (plant.name ?: "Plant")
                    
                    // Define o nível de risco/cor baseando-se no hash do nome (simulação)
                    // 1=Grey, 2=Green, 3=Yellow, 4=Orange, 5=Red
                    val level = (plant.name?.length?.rem(5) ?: 0) + 1
                    
                    // Gera o ícone customizado com gradiente e nome da planta
                    val customIcon = createCustomMarker(context, level, plant.name)
                    marker.icon = customIcon
                    
                    // Ajusta a âncora dinamicamente
                    val anchorV = 65f / customIcon.intrinsicHeight.toFloat()
                    marker.setAnchor(Marker.ANCHOR_CENTER, anchorV)

                    // Se for o selecionado, já abre o info window
                    if (isSelected) {
                        marker.showInfoWindow()
                    }

                    marker.setOnMarkerClickListener { _, _ ->
                        if (plant.id == selectedPlantId) {
                            // Se clicou no que já está selecionado -> Zoom Out para original (8.0) e limpa seleção
                            mapView.controller.setZoom(8.0) // Usar setZoom é mais confiável que zoomTo
                            onPlantSelected(null) // Limpa o dropdown/seleção
                        } else {
                            // Novo clique -> Seleciona, Centraliza e Zoom In para o MÁXIMO DISPONÍVEL
                            onPlantSelected(plant.id)
                            mapView.controller.animateTo(marker.position)
                            
                            // Obtém o zoom máximo REAL da fonte de tiles (Esri vai até 19)
                            val maxSourceZoom = mapView.tileProvider.tileSource.maximumZoomLevel
                            val targetZoom = if (maxSourceZoom > 17) 17.0 else maxSourceZoom.toDouble()
                            
                            // Usar setZoom garante a mudança imediata e evita conflitos de animação
                            mapView.controller.setZoom(targetZoom)
                            
                            marker.showInfoWindow()
                        }
                        true
                    }
                    
                    mapView.overlays.add(marker)
                }
            }
        }
        
        // Ajuste automático de zoom e posição (Fit Bounds) se nenhum item estiver selecionado e houver pontos
        if (selectedPlantId == null && geoPoints.isNotEmpty()) {
            val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
            // Padding de 100px para garantir que os marcadores não fiquem colados na borda
            val padding = 100 
            // Post para garantir que o MapView já tenha dimensões calculadas
            mapView.post {
                mapView.zoomToBoundingBox(boundingBox, true, padding, 16.0, null)
            }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Cria um Drawable de marcador customizado estilo "Leaflet divIcon"
 * Formato: Gota com gradiente linear e círculo branco no centro.
 * Tamanho base: 50x65px (metade do original 100x130px).
 */
fun createCustomMarker(context: Context, level: Int, text: String? = null): Drawable {
    // 1. Mapeamento de cores baseado no nível
    val (darkColorHex, lightColorHex) = when (level) {
        1 -> "#7a7a7a" to "#d4d4d4" // Grey
        2 -> "#006400" to "#00FF7F" // Green
        3 -> "#b8860b" to "#FFFF66" // Yellow
        4 -> "#cc5500" to "#ff9900" // Orange
        5 -> "#8B0000" to "#FF6347" // Red
        else -> "#006400" to "#00FF7F" // Default Green
    }
    
    val darkColor = android.graphics.Color.parseColor(darkColorHex)
    val lightColor = android.graphics.Color.parseColor(lightColorHex)

    // Fator de escala para reduzir o tamanho pela metade
    val scale = 0.6f

    // 2. Configuração de Texto
    val baseTextSizePx = 60f
    val baseTextPadding = 10f
    
    val textPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
        textSize = baseTextSizePx // Será escalado pelo Canvas
        textAlign = Paint.Align.CENTER
        // Adiciona sombra preta para contraste
        setShadowLayer(5f, 0f, 0f, android.graphics.Color.BLACK)
    }

    // Calcula dimensões na base original (100x130)
    val baseMarkerWidth = 100
    val baseMarkerHeight = 130
    
    // Se houver texto, calcula largura necessária (base)
    val textWidth = if (text != null) textPaint.measureText(text) else 0f
    val baseTotalWidth = maxOf(baseMarkerWidth, textWidth.toInt() + 20)
    val baseExtraHeight = if (text != null) (baseTextSizePx + baseTextPadding * 2).toInt() else 0
    val baseTotalHeight = baseMarkerHeight + baseExtraHeight

    // Cria Bitmap com dimensões escaladas (Metade)
    val scaledWidth = (baseTotalWidth * scale).toInt()
    val scaledHeight = (baseTotalHeight * scale).toInt()
    
    val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Aplica a escala ao Canvas
    canvas.scale(scale, scale)

    // Centraliza o desenho do marcador horizontalmente (usando coordenadas base)
    val markerOffsetX = (baseTotalWidth - baseMarkerWidth) / 2f

    // 3. Paint para o corpo com gradiente
    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        shader = LinearGradient(
            0f, 0f, baseMarkerWidth.toFloat(), 0f,
            darkColor, lightColor,
            Shader.TileMode.CLAMP
        )
    }

    canvas.save()
    canvas.translate(markerOffsetX, 0f)

    // 4. Caminho da Gota (SVG Path - Coordenadas originais 100x130)
    val pathData = "M50 0 C22 0, 0 22, 0 50 C0 78, 50 130, 50 130 C50 130, 100 78, 100 50 C100 22, 78 0, 50 0 Z"
    val path = try {
        PathParser.createPathFromPathData(pathData)
    } catch (e: Exception) {
        android.graphics.Path().apply { addCircle(50f, 50f, 50f, android.graphics.Path.Direction.CW) }
    }
    canvas.drawPath(path, paint)

    // 5. Círculo Branco Interno
    val circlePaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(50f, 50f, 20f, circlePaint)
    
    canvas.restore()

    // 6. Desenha o Texto abaixo do marcador (Coordenadas base)
    if (text != null) {
        val textX = baseTotalWidth / 2f
        val textY = baseMarkerHeight.toFloat() + baseTextSizePx
        canvas.drawText(text, textX, textY, textPaint)
    }

    return BitmapDrawable(context.resources, bitmap)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantSelect(
    plants: List<PlantEntity>,
    selectedPlantId: UUID?,
    onPlantSelected: (UUID?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    CompactUiWrapper {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = plants.find { it.id == selectedPlantId }?.name ?: "Selecionar Instalação",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Todas as Instalações") },
                    onClick = {
                        onPlantSelected(null)
                        expanded = false
                    }
                )
                plants.forEach { plant ->
                    DropdownMenuItem(
                        text = { Text(plant.name ?: "Unnamed Plant") },
                        onClick = {
                            onPlantSelected(plant.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InspectionRecordsList(
    inspectionRecords: List<InspectionRecordEntity>,
    onViewRouteClick: (InspectionRecordEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(inspectionRecords) { record ->
            InspectionRecordCard(
                record = record,
                onViewRouteClick = { onViewRouteClick(record) }
            )
        }
    }
}

@Composable
fun InspectionRecordCard(
    record: InspectionRecordEntity,
    onViewRouteClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    
    // Verifica status (Trata null como false)
    val isFinished = record.finished == true
    // Atraso só importa se não estiver finalizado
    val isExpired = !isFinished && record.expectedEndDate.isBefore(LocalDate.now())
    
    // Definições visuais
    val statusText = when {
        isFinished -> "Inspecionado"
        isExpired -> "Atrasado"
        else -> "Não inspecionado"
    }

    val statusColor = when {
        isFinished -> Color(0xFF2E7D32) // Verde Escuro
        isExpired -> Color.Red
        else -> MaterialTheme.colorScheme.primary
    }

    // Fundo do texto (apenas para "Inspecionado" conforme solicitado)
    val statusModifier = if (isFinished) {
        Modifier
            .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) // Verde bem clarinho
            .padding(horizontal = 8.dp, vertical = 2.dp)
    } else {
        Modifier
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = record.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Início: " + record.expectedStartDate.format(dateFormatter),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Button(
                        onClick = onViewRouteClick,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Ver Rota")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Subheadline: Start and End Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "Fim:    " + record.expectedEndDate.format(dateFormatter),
                        style = MaterialTheme.typography.labelLarge,
                        // Se expirado (e não finalizado), cor Vermelha, senão cor padrão
                        color = if (isExpired) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = statusModifier
                    )
                }
            }
        }
    }
}
