package com.tech.thermography.android.ui.thermogram.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ThermogramImage(
    imageUri: Uri?,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = imageUri != null) { onImageClick() },
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Imagem térmica",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "Nenhuma imagem",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Versão com overlay de ROIs (para expansão futura)
@Composable
fun ThermogramImageWithRois(
    imageUri: Uri?,
    onImageClick: () -> Unit,
    showRoiOverlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ThermogramImage(
            imageUri = imageUri,
            onImageClick = onImageClick,
            modifier = Modifier.fillMaxWidth()
        )

        // TODO: Adicionar overlay de ROIs se necessário
        if (showRoiOverlay) {
            // Canvas para desenhar os retângulos dos ROIs
        }
    }
}

