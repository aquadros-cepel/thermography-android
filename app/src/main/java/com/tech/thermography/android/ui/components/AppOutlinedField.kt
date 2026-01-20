package com.tech.thermography.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    maxLines: Int = 1
) {
    val currentDensity = LocalDensity.current
    val compactDensity = Density(
        density = currentDensity.density * 0.85f,
        fontScale = currentDensity.fontScale * 1.0f
    )
    val roundedShapes = MaterialTheme.shapes.copy(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(10.dp),
        large = RoundedCornerShape(10.dp)
    )
    val currentTypography = MaterialTheme.typography
    fun scaleTextStyle(style: TextStyle): TextStyle {
        return style.copy(fontSize = style.fontSize * 1.1f)
    }
    val boostedTypography = currentTypography.copy(
        bodyLarge = scaleTextStyle(currentTypography.bodyLarge),
        bodyMedium = scaleTextStyle(currentTypography.bodyMedium),
        labelLarge = scaleTextStyle(currentTypography.labelLarge),
        labelMedium = scaleTextStyle(currentTypography.labelMedium)
    )
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val fieldBg = MaterialTheme.colorScheme.background
    val disabledBg = Color(0xFFF5F7FA)
    val disabledText = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    CompositionLocalProvider(
        LocalDensity provides compactDensity
    ) {
        MaterialTheme(
            shapes = roundedShapes,
            colorScheme = MaterialTheme.colorScheme,
            typography = boostedTypography,
            content = {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    label = { Text(label, color = if (enabled) labelColor else disabledText) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (enabled) textColor else disabledText),
                    maxLines = maxLines,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedLabelColor = labelColor,
                        unfocusedLabelColor = labelColor,
                        disabledLabelColor = disabledText,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        disabledTextColor = disabledText,
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg,
                        disabledContainerColor = disabledBg
                    ),
                    modifier = modifier
                        .fillMaxWidth()
                        .background(fieldBg)
                )
            }
        )
    }
}
