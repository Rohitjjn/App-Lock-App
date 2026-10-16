package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = SafeLockClay,
    onPrimary = Color.White,
    primaryContainer = SafeLockClayContainer,
    onPrimaryContainer = Color(0xFF47190D),
    secondary = SafeLockSage,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EBE2),
    onSecondaryContainer = Color(0xFF1B3526),
    tertiary = SafeLockGold,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF5E4CF),
    onTertiaryContainer = Color(0xFF3E2813),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = SafeLockCanvas,
    onBackground = SafeLockInk,
    surface = SafeLockSurface,
    onSurface = SafeLockInk,
    surfaceVariant = SafeLockSurfaceRaised,
    onSurfaceVariant = SafeLockInkMuted,
    outline = SafeLockOutline,
    outlineVariant = Color(0xFFECE4DC),
    inverseSurface = Color(0xFF322D28),
    inverseOnSurface = Color(0xFFF5F0EA),
    inversePrimary = Color(0xFFFFB59B),
    scrim = Color(0xFF000000),
    surfaceTint = SafeLockClay
)

private val DarkColorScheme = darkColorScheme(
    primary = SafeLockDarkClay,
    onPrimary = Color(0xFF37180E),
    primaryContainer = SafeLockDarkClayContainer,
    onPrimaryContainer = Color(0xFFFFD9CB),
    secondary = SafeLockDarkSage,
    onSecondary = Color(0xFF203328),
    secondaryContainer = Color(0xFF33483A),
    onSecondaryContainer = Color(0xFFD4E5D4),
    tertiary = SafeLockDarkGold,
    onTertiary = Color(0xFF38220D),
    tertiaryContainer = Color(0xFF523B23),
    onTertiaryContainer = Color(0xFFF8DDBB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = SafeLockDarkCanvas,
    onBackground = SafeLockDarkInk,
    surface = SafeLockDarkSurface,
    onSurface = SafeLockDarkInk,
    surfaceVariant = SafeLockDarkSurfaceRaised,
    onSurfaceVariant = SafeLockDarkInkMuted,
    outline = SafeLockDarkOutline,
    outlineVariant = Color(0xFF3B352F),
    inverseSurface = Color(0xFFEAE2D9),
    inverseOnSurface = Color(0xFF322D28),
    inversePrimary = SafeLockClay,
    scrim = Color(0xFF000000),
    surfaceTint = SafeLockDarkClay
)

private val SafeLockShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val LocalDarkTheme = staticCompositionLocalOf { false }
val LocalAccentColor = staticCompositionLocalOf { SafeLockClay }

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    accentColorHex: String? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as? com.example.AppLockApplication
    val storedAccent = accentColorHex ?: app?.encryptedPrefs?.getAccentColorHex() ?: "Default"
    val isDefaultAccent = storedAccent.equals("Default", ignoreCase = true)
    val defaultAccent = if (darkTheme) SafeLockDarkClay else SafeLockClay
    val accent = if (isDefaultAccent) {
        defaultAccent
    } else {
        runCatching {
            Color(android.graphics.Color.parseColor(storedAccent.let {
                if (it.startsWith("#")) it else "#$it"
            }))
        }.getOrDefault(defaultAccent)
    }

    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val surfaceForContainer = if (darkTheme) SafeLockDarkSurface else SafeLockSurface
    val accentContainer = accent.copy(alpha = if (darkTheme) 0.20f else 0.12f)
        .compositeOver(surfaceForContainer)
    val onAccentContainer = if (accentContainer.luminance() > 0.42f) SafeLockInk else Color.White
    val colorScheme = if (isDefaultAccent) {
        baseScheme
    } else {
        baseScheme.copy(
            primary = accent,
            onPrimary = if (accent.luminance() > 0.20f) SafeLockInk else Color.White,
            onTertiary = if (accent.luminance() > 0.20f) SafeLockInk else Color.White,
            primaryContainer = accentContainer,
            onPrimaryContainer = onAccentContainer,
            tertiary = accent,
            surfaceTint = accent
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    // `dynamicColor` stays opt-in. SafeLock's saved theme/accent preferences remain
    // the source of truth so this visual update cannot silently replace user choices.
    @Suppress("UNUSED_VARIABLE")
    val dynamicColorOptIn = dynamicColor

    CompositionLocalProvider(
        LocalAccentColor provides accent,
        LocalDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = SafeLockShapes,
            content = content
        )
    }
}
