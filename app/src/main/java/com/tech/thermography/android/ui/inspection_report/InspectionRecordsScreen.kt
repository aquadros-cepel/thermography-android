package com.tech.thermography.android.ui.inspection_report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.tech.thermography.android.data.local.entity.InspectionRecordEntity
import com.tech.thermography.android.data.local.entity.PlantEntity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.time.format.DateTimeFormatter
import java.util.UUID


@Composable
fun InspectionRecordsScreen(
    viewModel: InspectionRecordsViewModel = hiltViewModel()
) {
    val plants by viewModel.plants.collectAsState()
    val filteredPlants by viewModel.filteredPlants.collectAsState()
    val filteredInspectionRecords by viewModel.filteredInspectionRecords.collectAsState()
    val selectedPlantId by viewModel.selectedPlantId.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Upper half: Map
        Box(modifier = Modifier.weight(0.5f)) {
            MapComponent(
                plants = plants,
                onPlantSelected = { plantId -> viewModel.selectPlantFromMap(plantId) }
            )
        }

        // Lower half: Select and Table
        Column(modifier = Modifier.weight(0.5f)) {
            PlantSelect(
                plants = plants,
                selectedPlantId = selectedPlantId,
                onPlantSelected = { plantId -> viewModel.selectPlant(plantId) }
            )
            InspectionRecordsTable(inspectionRecords = filteredInspectionRecords)
        }
    }
}

@Composable
fun MapComponent(
    plants: List<PlantEntity>,
    onPlantSelected: (UUID) -> Unit
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osm", 0))
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(10.0)

        // Add markers for plants
        plants.forEach { plant ->
            plant.latitude?.let { lat ->
                plant.longitude?.let { lon ->
                    val marker = Marker(mapView)
                    marker.position = GeoPoint(lat, lon)
                    marker.title = plant.name ?: "Plant"
                    marker.setOnMarkerClickListener { _, _ ->
                        onPlantSelected(plant.id)
                        true
                    }
                    mapView.overlays.add(marker)
                }
            }
        }

        // Center map on first plant if available
        plants.firstOrNull()?.let { firstPlant ->
            firstPlant.latitude?.let { lat ->
                firstPlant.longitude?.let { lon ->
                    mapView.controller.setCenter(GeoPoint(lat, lon))
                }
            }
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantSelect(
    plants: List<PlantEntity>,
    selectedPlantId: UUID?,
    onPlantSelected: (UUID?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = plants.find { it.id == selectedPlantId }?.name ?: "Select Plant",
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
                text = { Text("All Plants") },
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

@Composable
fun InspectionRecordsTable(inspectionRecords: List<InspectionRecordEntity>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Code", modifier = Modifier.weight(1f))
                Text("Name", modifier = Modifier.weight(2f))
                Text("Start Date", modifier = Modifier.weight(1f))
                Text("End Date", modifier = Modifier.weight(1f))
            }
        }
        items(inspectionRecords) { record ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(record.code ?: "", modifier = Modifier.weight(1f))
                Text(record.name, modifier = Modifier.weight(2f))
                Text(
                    record.expectedStartDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    record.expectedEndDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    modifier = Modifier.weight(1f)
                )
            }
            Divider()
        }
    }
}
