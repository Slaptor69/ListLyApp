package ru.misterpotz.listly.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = BlueLight,
    primaryContainer = BlueContainerLight,
    secondaryContainer = BlueContainerLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = TextDark,
    onPrimaryContainer = TextLight,
    onSecondaryContainer = TextLight,
    onBackground = TextLight,
    onSurface = TextLight
)

private val DarkColors = darkColorScheme(
    primary = BlueDark,
    primaryContainer = BlueContainerDark,
    secondaryContainer = BlueContainerDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = TextDark,
    onPrimaryContainer = TextDark,
    onSecondaryContainer = TextDark,
    onBackground = TextDark,
    onSurface = TextDark
)

@Composable
fun ListlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
