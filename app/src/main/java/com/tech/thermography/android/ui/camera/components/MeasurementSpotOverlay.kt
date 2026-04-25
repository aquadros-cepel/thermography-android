package com.tech.thermography.android.ui.camera.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tech.thermography.android.ui.camera.ThermogramCameraViewModel
import kotlin.math.roundToInt



// ThermalSpot composable function
@Composable
fun ThermalSpot(
    modifier: Modifier = Modifier,
    radius: Dp = 16.dp,
    strokeWhite: Float = 3f,
    strokeBlack: Float = 1f
) {
    Canvas(modifier = modifier.size(radius * 4)) {
        val center = center
        val r = radius.toPx()
        // Círculo externo (preto)
        drawCircle(
            color = Color.Black,
            radius = r,
            center = center,
            style = Stroke(width = strokeBlack)
        )
        // Círculo interno (branco)
        drawCircle(
            color = Color.White,
            radius = r - strokeBlack,
            center = center,
            style = Stroke(width = strokeWhite)
        )

        // Círculo interno (preto)
        drawCircle(
            color = Color.Black,
            radius = r - 3*strokeBlack,
            center = center,
            style = Stroke(width = strokeBlack)
        )
        val tickLength = r * 0.6f
        val gap = r * 0.1f
        // Ticks (crosshair)
        // Top
        drawLine(
            color = Color.Black,
            start = Offset(center.x, center.y - r - gap),
            end = Offset(center.x, center.y - r - gap - tickLength),
            strokeWidth = strokeBlack
        )
        drawLine(
            color = Color.White,
            start = Offset(center.x, center.y - r - gap),
            end = Offset(center.x, center.y - r - gap - tickLength),
            strokeWidth = strokeWhite
        )
        // Bottom
        drawLine(
            color = Color.Black,
            start = Offset(center.x, center.y + r + gap),
            end = Offset(center.x, center.y + r + gap + tickLength),
            strokeWidth = strokeBlack
        )
        drawLine(
            color = Color.White,
            start = Offset(center.x, center.y + r + gap),
            end = Offset(center.x, center.y + r + gap + tickLength),
            strokeWidth = strokeWhite
        )
        // Left
        drawLine(
            color = Color.Black,
            start = Offset(center.x - r - gap, center.y),
            end = Offset(center.x - r - gap - tickLength, center.y),
            strokeWidth = strokeBlack
        )
        drawLine(
            color = Color.White,
            start = Offset(center.x - r - gap, center.y),
            end = Offset(center.x - r - gap - tickLength, center.y),
            strokeWidth = strokeWhite
        )
        // Right
        drawLine(
            color = Color.Black,
            start = Offset(center.x + r + gap, center.y),
            end = Offset(center.x + r + gap + tickLength, center.y),
            strokeWidth = strokeBlack
        )
        drawLine(
            color = Color.White,
            start = Offset(center.x + r + gap, center.y),
            end = Offset(center.x + r + gap + tickLength, center.y),
            strokeWidth = strokeWhite
        )
    }
}

// MeasurementSpot glyph composable usando ThermalSpot
@Composable
fun MeasurementSpotGlyph(
    modifier: Modifier = Modifier,
    pressed: Boolean = false
) {
    // Ajuste de tamanho responsivo ao pressionar
    val radius = if (pressed) 16.dp else 16.dp
    ThermalSpot(
        modifier = modifier,
        radius = radius,
        strokeWhite = if (pressed) 3.5f else 3f,
        strokeBlack = if (pressed) 2f else 1.5f
    )
}
@Composable
fun MeasurementSpotOverlay(
    state: ThermogramCameraViewModel.MeasurementSpotState,
    overlaySize: IntSize,
    onStateChange: (ThermogramCameraViewModel.MeasurementSpotState) -> Unit,
) {
    if (!state.enabled || overlaySize.width <= 0 || overlaySize.height <= 0) return

    val currentState = rememberUpdatedState(state)
    val currentOnStateChange = rememberUpdatedState(onStateChange)
    val density = LocalDensity.current

    // Tamanho fixo do glyph do spot
    val spotDiameterDp = 32.dp
    val spotRadiusPx = with(density) { (spotDiameterDp / 2).toPx() }

    // Calcula a posição central do spot
    val centerX = (state.centerXFraction * overlaySize.width).roundToInt()
    val centerY = (state.centerYFraction * overlaySize.height).roundToInt()

    // Garante que o spot não saia dos limites do overlay
    val minX = spotRadiusPx
    val maxX = overlaySize.width - spotRadiusPx
    val minY = spotRadiusPx
    val maxY = overlaySize.height - spotRadiusPx

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (centerX - spotRadiusPx).roundToInt(),
                    (centerY - spotRadiusPx).roundToInt()
                )
            }
            .size(spotDiameterDp)
            .pointerInput(overlaySize, state.enabled) {
                detectTapGestures(
                    onDoubleTap = {
                        // Reseta o ponto para o centro da tela
                        currentOnStateChange.value(
                            state.copy(centerXFraction = 0.5f, centerYFraction = 0.5f)
                        )
                    }
                )
            }
            .pointerInput(overlaySize, state.enabled) {
                detectTransformGestures(
                    onGesture = { _, pan, _, _ ->
                        val width = overlaySize.width.toFloat().coerceAtLeast(1f)
                        val height = overlaySize.height.toFloat().coerceAtLeast(1f)
                        val latestState = currentState.value
                        val currentX = latestState.centerXFraction * width
                        val currentY = latestState.centerYFraction * height
                        val newX = (currentX + pan.x).coerceIn(minX, maxX)
                        val newY = (currentY + pan.y).coerceIn(minY, maxY)
                        currentOnStateChange.value(
                            latestState.copy(
                                centerXFraction = newX / width,
                                centerYFraction = newY / height
                            )
                        )
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        MeasurementSpotGlyph()
//        Surface(
//            color = Color(0xCC000000),
//            shape = MaterialTheme.shapes.large,
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .border(1.dp, Color.White, MaterialTheme.shapes.large)
//        ) {
//            Text(
//                text = state.label,
//                color = Color.White,
//                style = MaterialTheme.typography.labelLarge,
//                modifier = Modifier
//                    .background(Color.Transparent)
//                    .padding(horizontal = 6.dp, vertical = 5.dp)
//            )
//        }
    }
}
