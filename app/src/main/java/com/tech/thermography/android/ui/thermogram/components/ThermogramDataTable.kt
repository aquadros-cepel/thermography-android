package com.tech.thermography.android.ui.thermogram.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tech.thermography.android.data.local.entity.ROIEntity
import com.tech.thermography.android.data.local.entity.ThermogramEntity
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun ThermogramDataTable(
    thermogram: ThermogramEntity,
    selectedRoi: ROIEntity?,
    selectedRefRoi: ROIEntity?,
    temperatureDifference: Double?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        var rowIndex = 0

        // Áudio (mantém topo sem alternância)
        if (thermogram.audioPath != null) {
            AudioPlayerRow(audioPath = thermogram.audioPath, index = rowIndex)
            rowIndex++
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        }

        // Temperatura do Objeto
        DataRow(
            index = rowIndex++,
            label = "Temperatura do Objeto",
            value = (thermogram.maxTempRoi ?: selectedRoi?.maxTemp ?: thermogram.maxTemp)
                ?.let { "%.1f °C".format(it) } ?: "--"
        )

        // Temperatura de Referência
        DataRow(
            index = rowIndex++,
            label = "Temperatura de Referência",
            value = (selectedRefRoi?.maxTemp ?: thermogram.reflectedTemp)
                ?.let { "%.1f °C".format(it) } ?: "--"
        )

        // Diferença de Temperatura (destacado)
        DataRowHighlighted(
            index = rowIndex++,
            label = "Diferença de Temperatura",
            value = temperatureDifference?.let { "%.1f °C".format(it) } ?: "--"
        )

        // Emissividade
        DataRow(
            index = rowIndex++,
            label = "Emissividade",
            value = thermogram.emissivity?.toString() ?: "--"
        )

        // Distância
        DataRow(
            index = rowIndex++,
            label = "Distância",
            value = thermogram.subjectDistance?.let { "%.0f m".format(it) } ?: "--"
        )

        // Temperatura Refletida
        DataRow(
            index = rowIndex++,
            label = "Temperatura Refletida",
            value = thermogram.reflectedTemp?.let { "%.0f °C".format(it) } ?: "--"
        )

        // Temperatura Ambiente
        DataRow(
            index = rowIndex++,
            label = "Temperatura Ambiente",
            value = thermogram.atmosphericTemp?.let { "%.1f °C".format(it) } ?: "--"
        )

        // Umidade Relativa do Ar
        DataRow(
            index = rowIndex++,
            label = "Umidade Relativa do Ar",
            value = thermogram.relativeHumidity?.let { "%.0f %%".format(it) } ?: "--"
        )

        // Data do Registro (renomeado)
        DataRow(
            index = rowIndex++,
            label = "Data do Registro",
            value = thermogram.createdAt?.atZone(TimeZone.getDefault().toZoneId())
                ?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) ?: "N/A"
        )

        // Ocultados por enquanto:
        // Velocidade do Vento
        // DataRow(index = rowIndex++, label = "Velocidade do Vento", value = "0.0 m/s")

        // Carga
        // DataRow(index = rowIndex++, label = "Carga", value = "175.0 kA")

        // Câmera
        DataRow(
            index = rowIndex++,
            label = "Câmera",
            value = thermogram.cameraModel ?: "--"
        )

        // Resolução
        DataRow(
            index = rowIndex++,
            label = "Resolução",
            value = thermogram.imageResolution ?: "--"
        )

        // Lente
        DataRow(
            index = rowIndex++,
            label = "Lente",
            value = thermogram.cameraLens?.let { "${it}°" } ?: "--"
        )
    }
}

private fun rowBackgroundColor(index: Int): Color {
    // Alternância: branco e cinza claro
    return if (index % 2 == 0) Color.White else Color(0xFFF0F2F5) // rgb(240,242,245) leve
}

@Composable
private fun DataRow(
    index: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBackgroundColor(index))
            .padding(horizontal = 12.dp, vertical = 8.dp), // linhas mais estreitas
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        // Valor alinhado à direita e mais próximo do label
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

@Composable
private fun DataRowHighlighted(
    index: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBackgroundColor(index))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

@Composable
private fun AudioPlayerRow(
    audioPath: String,
    index: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBackgroundColor(index))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Áudio",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* TODO: Implementar player de áudio */ }) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Reproduzir áudio",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "0:00 / 0:00",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
