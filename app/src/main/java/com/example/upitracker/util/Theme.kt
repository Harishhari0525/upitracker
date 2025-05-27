package com.example.upitracker.util

import android.app.Activity // For window operations
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color // For Color.Transparent
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Assuming your AppLightColorScheme and AppDarkColorScheme are defined here as before
// For brevity, I'll skip re-pasting them, but ensure they are present in your file.

private val AppLightColorScheme = lightColorScheme(
    primary = Color(0xFF006A6A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF70F7F7),
    onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFFB95636),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCF),
    onSecondaryContainer = Color(0xFF4A1A0B),
    tertiary = Color(0xFF7C5A0A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEA4),
    onTertiaryContainer = Color(0xFF271A00),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFAFDFB),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFF7FCFA),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDAE5E3),
    onSurfaceVariant = Color(0xFF3F4948),
    outline = Color(0xFF6F7978),
    outlineVariant = Color(0xFFBEC9C7),
    inverseSurface = Color(0xFF2E3130),
    inverseOnSurface = Color(0xFFF0F1EF),
    inversePrimary = Color(0xFF4CDADA),
    surfaceTint = Color(0xFF006A6A),
    scrim = Color(0xFF000000)
)

private val AppDarkColorScheme = darkColorScheme(
    primary = Color(0xFF50D7D7),
    onPrimary = Color(0xFF003737),
    primaryContainer = Color(0xFF004F4F),
    onPrimaryContainer = Color(0xFF70F7F7),
    secondary = Color(0xFFFFB59C),
    onSecondary = Color(0xFF652916),
    secondaryContainer = Color(0xFF86402A),
    onSecondaryContainer = Color(0xFFFFDBCF),
    tertiary = Color(0xFFF9C63A),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFDEA4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF191C1B),
    onBackground = Color(0xFFE1E3E1),
    surface = Color(0xFF191C1B),
    onSurface = Color(0xFFE1E3E1),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBEC9C7),
    outline = Color(0xFF899391),
    outlineVariant = Color(0xFF3F4948),
    inverseSurface = Color(0xFFE1E3E1),
    inverseOnSurface = Color(0xFF191C1B),
    inversePrimary = Color(0xFF006A6A),
    surfaceTint = Color(0xFF50D7D7),
    scrim = Color(0xFF000000)
)


@Composable
fun Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AppDarkColorScheme
        else -> AppLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // ✨ Make status bar transparent for edge-to-edge display ✨
                window.statusBarColor = Color.Transparent.toArgb()
                // If you also want the navigation bar to be transparent for edge-to-edge:
                // window.navigationBarColor = Color.Transparent.toArgb()
                // WindowCompat.setNavigationBarContrastEnforced(window, false) // If using translucent nav bar

                // This tells the system to use dark icons on a light status bar background and vice-versa
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                // Optional: For navigation bar icons if you make it transparent/translucent
                // WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(), // Using default Material 3 Typography
        content = content
    )
}