package com.tech.thermography.android.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Aplica uma densidade reduzida (0.85x) aos componentes filhos,
 * tornando-os visualmente mais compactos (menor altura).
 * 
 * Também aplica bordas mais arredondadas e ajusta a escala da fonte.
 */
@Composable
fun CompactUiWrapper(
    content: @Composable () -> Unit
) {
    val currentDensity = LocalDensity.current
    
    // Reduz a densidade em 15% (fator 0.85) para reduzir a altura dos componentes.
    // FontScale ajustado para 1.2f conforme solicitado.
    val compactDensity = Density(
        density = currentDensity.density * 0.85f, 
        fontScale = currentDensity.fontScale * 1.0f
    )

    // Formas arredondadas ajustadas para 15.dp conforme solicitado.
    val roundedShapes = MaterialTheme.shapes.copy(
        extraSmall = RoundedCornerShape(15.dp), 
        small = RoundedCornerShape(15.dp),
        medium = RoundedCornerShape(15.dp),
        large = RoundedCornerShape(15.dp)
    )

    // Ajuste agressivo na tipografia para os itens do Select Box (DropdownMenu).
    // O DropdownMenu geralmente usa styles como bodyLarge ou labelLarge.
    // Vamos aumentar esses estilos especificamente para compensar qualquer redução e garantir legibilidade.
    // Multiplicamos por 1.25x EXTRA além do fontScale já definido.
    val currentTypography = MaterialTheme.typography
    
    fun scaleTextStyle(style: TextStyle, factor: Float): TextStyle {
        return style.copy(fontSize = style.fontSize * factor)
    }

    val boostedTypography = currentTypography.copy(
        bodyLarge = scaleTextStyle(currentTypography.bodyLarge, 1.25f),
        bodyMedium = scaleTextStyle(currentTypography.bodyMedium, 1.25f),
        labelLarge = scaleTextStyle(currentTypography.labelLarge, 1.25f),
        labelMedium = scaleTextStyle(currentTypography.labelMedium, 1.25f)
    )

    CompositionLocalProvider(
        LocalDensity provides compactDensity
    ) {
        MaterialTheme(
            shapes = roundedShapes,
            colorScheme = MaterialTheme.colorScheme,
            typography = boostedTypography, // Aplica a tipografia aumentada
            content = content
        )
    }
}
