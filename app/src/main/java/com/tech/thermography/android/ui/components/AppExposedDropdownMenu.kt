package com.tech.thermography.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AppExposedDropdownMenu(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit,
    optionLabelProvider: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true, // NOVO: permite desabilitar o campo
    isCritical: Boolean = false // NOVO: quando true, aplica estilo crítico (vermelho + bold)
) {
    var expanded by remember { mutableStateOf(false) }
    val currentDensity = LocalDensity.current

    // Reduz a densidade em 15% (fator 0.85) para reduzir a altura dos componentes.
    val compactDensity = Density(
        density = currentDensity.density * 0.85f,
        fontScale = currentDensity.fontScale * 1.0f
    )

    // Formas arredondadas ajustadas para 10.dp
    val roundedShapes = MaterialTheme.shapes.copy(
        extraSmall = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(10.dp),
        large = RoundedCornerShape(10.dp)
    )

    // Ajuste na tipografia
    val currentTypography = MaterialTheme.typography
    fun scaleTextStyle(style: TextStyle, factor: Float): TextStyle {
        return style.copy(fontSize = style.fontSize * factor)
    }
    val boostedTypography = currentTypography.copy(
        bodyLarge = scaleTextStyle(currentTypography.bodyLarge, 1.1f),
        bodyMedium = scaleTextStyle(currentTypography.bodyMedium, 1.1f),
        labelLarge = scaleTextStyle(currentTypography.labelLarge, 1.1f),
        labelMedium = scaleTextStyle(currentTypography.labelMedium, 1.1f)
    )

    // Cores do tema
    val labelColor = if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val textColor = if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val disabledBg = Color(0xFFF4F6F8)
    val disabledText = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val fieldBg = MaterialTheme.colorScheme.background

    CompositionLocalProvider(
        LocalDensity provides compactDensity
    ) {
        MaterialTheme(
            shapes = roundedShapes,
            colorScheme = MaterialTheme.colorScheme,
            typography = boostedTypography,
            content = {
                ExposedDropdownMenuBox(
                    expanded = expanded && enabled,
                    onExpandedChange = { if (enabled) expanded = !expanded },
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = selectedOption?.let { optionLabelProvider(it) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = enabled,
                        label = { Text(label, color = if (enabled) labelColor else disabledText, fontWeight = if (isCritical) FontWeight.Bold else FontWeight.Normal) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (enabled) textColor else disabledText, fontWeight = if (isCritical) FontWeight.Bold else FontWeight.Normal),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedBorderColor = if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                            unfocusedBorderColor = if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
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
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .background(if (enabled) fieldBg else disabledBg)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded && enabled,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(fieldBg)
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(optionLabelProvider(option), color = textColor) },
                                onClick = {
                                    onOptionSelected(option)
                                    expanded = false
                                },
                                enabled = enabled,
                                modifier = Modifier.background(fieldBg)
                            )
                        }
                    }
                }
            }
        )
    }
}
