package com.galaicatech.sshbuttons.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TerminalBackground = Color(0xFF0A1210)
val TerminalText = Color(0xFFC9F7E4)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B56),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8EF8D2),
    onPrimaryContainer = Color(0xFF002118),
    secondary = Color(0xFF4B635B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE8DD),
    onSecondaryContainer = Color(0xFF082019),
    tertiary = Color(0xFF3F6374),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC2E8FA),
    onTertiaryContainer = Color(0xFF001F2A),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6FBF7),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF6FBF7),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDCE5DF),
    onSurfaceVariant = Color(0xFF404944),
    outline = Color(0xFF707974),
    outlineVariant = Color(0xFFC0C9C3)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF72DBB7),
    onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF005140),
    onPrimaryContainer = Color(0xFF8EF8D2),
    secondary = Color(0xFFB1CCC1),
    onSecondary = Color(0xFF1D352D),
    secondaryContainer = Color(0xFF334B43),
    onSecondaryContainer = Color(0xFFCDE8DD),
    tertiary = Color(0xFFA7CCE0),
    onTertiary = Color(0xFF073544),
    tertiaryContainer = Color(0xFF254C5C),
    onTertiaryContainer = Color(0xFFC2E8FA),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFDEE4DF),
    surface = Color(0xFF0F1512),
    onSurface = Color(0xFFDEE4DF),
    surfaceVariant = Color(0xFF404944),
    onSurfaceVariant = Color(0xFFC0C9C3),
    outline = Color(0xFF8A938D),
    outlineVariant = Color(0xFF404944)
)

@Composable
fun SshButtonsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
