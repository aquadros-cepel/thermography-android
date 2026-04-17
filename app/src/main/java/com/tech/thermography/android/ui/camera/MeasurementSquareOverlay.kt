package com.tech.thermography.android.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import kotlin.math.roundToInt

@Composable
fun MeasurementSquareOverlay(
    state: ThermogramAceViewModel.MeasurementSquareState,
    overlaySize: IntSize,
    onStateChange: (ThermogramAceViewModel.MeasurementSquareState) -> Unit,
) {
    if (!state.enabled || overlaySize.width <= 0 || overlaySize.height <= 0) return

    val currentState = rememberUpdatedState(state)
    val currentOnStateChange = rememberUpdatedState(onStateChange)

    val squareSizePx = (minOf(overlaySize.width, overlaySize.height) * state.sizeFraction)
        .roundToInt()
        .coerceAtLeast(24)
    val halfSquarePx = squareSizePx / 2
    val centerX = (state.centerXFraction * overlaySize.width).roundToInt()
    val centerY = (state.centerYFraction * overlaySize.height).roundToInt()
    val left = (centerX - halfSquarePx).coerceIn(0, (overlaySize.width - squareSizePx).coerceAtLeast(0))
    val top = (centerY - halfSquarePx).coerceIn(0, (overlaySize.height - squareSizePx).coerceAtLeast(0))
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .offset { IntOffset(left, top) }
            .wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(with(density) { squareSizePx.toDp() })
                .border(2.dp, Color.White)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0x22FFFFFF))
                .pointerInput(overlaySize, state.enabled, state.sizeFraction) {
                    detectDragGestures { _, dragAmount ->
                        val width = overlaySize.width.toFloat().coerceAtLeast(1f)
                        val height = overlaySize.height.toFloat().coerceAtLeast(1f)
                        val latestState = currentState.value
                        val newCenterX = ((latestState.centerXFraction * width) + dragAmount.x)
                            .coerceIn(halfSquarePx.toFloat(), width - halfSquarePx.toFloat())
                        val newCenterY = ((latestState.centerYFraction * height) + dragAmount.y)
                            .coerceIn(halfSquarePx.toFloat(), height - halfSquarePx.toFloat())

                        currentOnStateChange.value(
                            latestState.copy(
                                centerXFraction = newCenterX / width,
                                centerYFraction = newCenterY / height
                            )
                        )
                    }
                }
        )

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            color = Color(0xCC000000),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.border(1.dp, Color.White, MaterialTheme.shapes.large)
        ) {
            Text(
                text = state.label,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .background(Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}



