package com.miruna.habittracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val NavyLightScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = NavyOnPrimary,
    primaryContainer = NavyContainer,
    onPrimaryContainer = NavyOnContainer,
    secondary = SkySecondary,
    secondaryContainer = SkySecondaryContainer,
    onSecondaryContainer = SkyOnSecondaryContainer,
    background = IceBackground,
    onBackground = IceOnBackground,
    surface = IceSurface,
    onSurface = IceOnSurface,
    surfaceVariant = IceSurfaceVariant,
    onSurfaceVariant = IceOnSurfaceVariant
)

private val NavyDarkScheme = darkColorScheme(
    primary = NavyDarkPrimary,
    onPrimary = NavyDarkOnPrimary,
    primaryContainer = NavyDarkContainer,
    onPrimaryContainer = NavyDarkOnContainer,
    secondary = NavyDarkPrimary,
    background = NavyDarkBackground,
    onBackground = NavyDarkOnBackground,
    surface = NavyDarkSurface,
    onSurface = NavyDarkOnSurface,
    surfaceVariant = NavyDarkSurfaceVariant,
    onSurfaceVariant = NavyDarkOnSurfaceVariant
)

@Composable
fun HabitTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> NavyDarkScheme
        else -> NavyLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
