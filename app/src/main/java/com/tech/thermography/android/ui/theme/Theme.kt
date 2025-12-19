package com.tech.thermography.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
        darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
        lightColorScheme(
                primary = Purple40,
                secondary = PurpleGrey40,
                tertiary = Pink40,
                
                // Define a cor de fundo e superfície globais para o tom Cinza/Azul claro (Ghost White)
                background = AppBackground,
                surface = AppBackground,
                
                // Opcional: Ajustar onBackground e onSurface para contraste adequado se necessário
                // onBackground = Color(0xFF1C1B1F),
                // onSurface = Color(0xFF1C1B1F),

                /* Other default colors to override
                background = Color(0xFFFFFBFE),
                surface = Color(0xFFFFFBFE),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onTertiary = Color.White,
                onBackground = Color(0xFF1C1B1F),
                onSurface = Color(0xFF1C1B1F),
                */
        )

@Composable
fun ThermographyAndroidTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        // Dynamic color is available on Android 12+
        dynamicColor: Boolean = true,
        content: @Composable () -> Unit
) {
        val colorScheme =
                when {
                        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                                val context = LocalContext.current
                                // Se o sistema suportar cores dinâmicas, o background será o padrão do sistema.
                                // Se você quiser FORÇAR o AppBackground mesmo com dynamicColor,
                                // você teria que criar um copy do scheme retornado.
                                // Para garantir que o background seja a nossa cor, aplicamos um .copy:
                                if (darkTheme) dynamicDarkColorScheme(context)
                                else dynamicLightColorScheme(context).copy(background = AppBackground, surface = AppBackground)
                        }
                        darkTheme -> DarkColorScheme
                        else -> LightColorScheme
                }

        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
