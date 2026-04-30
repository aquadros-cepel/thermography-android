package com.tech.thermography.android.ui.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tech.thermography.android.ui.camera.MeasurementSquareState
import kotlin.math.roundToInt

@Composable
fun MeasurementSquareOverlay(
    state: MeasurementSquareState,
    overlaySize: IntSize,
    onStateChange: (MeasurementSquareState) -> Unit,
) {
    if (!state.enabled || overlaySize.width <= 0 || overlaySize.height <= 0) return

    val currentState = rememberUpdatedState(state)
    val currentOnStateChange = rememberUpdatedState(onStateChange)
    val density = LocalDensity.current

    val squareSizePx = (minOf(overlaySize.width, overlaySize.height) * state.sizeFraction)
        .roundToInt()
        .coerceAtLeast(24)
    val halfSquarePx = squareSizePx / 2
    val interactionPaddingPx = with(density) { 12.dp.roundToPx() }
    val interactionSizePx = squareSizePx + (interactionPaddingPx * 2)
    val centerX = (state.centerXFraction * overlaySize.width).roundToInt()
    val centerY = (state.centerYFraction * overlaySize.height).roundToInt()
    // Permite que o quadrado vá até a borda da área útil, sem limitar pelo interactionPaddingPx
    val left = (centerX - halfSquarePx)
        .coerceIn(0, (overlaySize.width - squareSizePx).coerceAtLeast(0))
    val top = (centerY - halfSquarePx)
        .coerceIn(0, (overlaySize.height - squareSizePx).coerceAtLeast(0))

    Column(
        modifier = Modifier
            .offset { IntOffset(left, top) }
            .wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(with(density) { interactionSizePx.toDp() })
                .pointerInput(overlaySize, state.enabled) {
                    detectTapGestures(
                        onDoubleTap = {
                            val latestState = currentState.value
                            currentOnStateChange.value(
                                latestState.copy(sizeFraction = latestState.initialSizeFraction)
                            )
                        }
                    )
                }
                .pointerInput(overlaySize, state.enabled) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val width = overlaySize.width.toFloat().coerceAtLeast(1f)
                        val height = overlaySize.height.toFloat().coerceAtLeast(1f)
                        val latestState = currentState.value
                        val newSizeFraction = (latestState.sizeFraction * zoom).coerceIn(0.08f, 0.6f)
                        val newSquareSizePx = (minOf(overlaySize.width, overlaySize.height) * newSizeFraction)
                            .roundToInt()
                            .coerceAtLeast(24)
                        val halfNewSquarePx = newSquareSizePx / 2

                        val newCenterX = ((latestState.centerXFraction * width) + pan.x)
                            .coerceIn(halfNewSquarePx.toFloat(), width - halfNewSquarePx.toFloat())
                        val newCenterY = ((latestState.centerYFraction * height) + pan.y)
                            .coerceIn(halfNewSquarePx.toFloat(), height - halfNewSquarePx.toFloat())

                        currentOnStateChange.value(
                            latestState.copy(
                                centerXFraction = newCenterX / width,
                                centerYFraction = newCenterY / height,
                                sizeFraction = newSizeFraction
                            )
                        )
                    }
                }
                .clip(RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(with(density) { squareSizePx.toDp() })
                    .border(2.dp, Color.White)
                    .clip(RoundedCornerShape(2.dp))
            ) {
                // Label dentro da caixa branca, alinhado ao canto inferior esquerdo
                if (state.label.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .background(Color(0xFF000000).copy(alpha = 0.3f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = state.label,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
