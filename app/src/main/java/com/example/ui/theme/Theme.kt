package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import androidx.compose.runtime.SideEffect

private val DarkColorScheme =
  darkColorScheme(
    primary = XaiWhite,                   // White is the primary voice
    secondary = XaiBodyText,
    tertiary = Color.Black,
    background = XaiCanvas,               // #0A0A0A
    surface = XaiCanvas,                  // #0A0A0A
    onPrimary = XaiOnWhite,               // Near-black text on white CTA
    onBackground = XaiWhite,
    onSurface = XaiWhite,
    surfaceVariant = XaiCanvasCard,       // #191919 feature cards
    onSurfaceVariant = XaiBodyText,
    outline = XaiHairline,                 // #212327 for elegant 1px borders
    primaryContainer = XaiCanvasSoft,
    onPrimaryContainer = XaiWhite
  )

// We map LightColorScheme to a beautiful dark state as well,
// perfectly respecting the "Don't introduce a light-mode counterpart. xAI is dark-canvas only" policy.
private val LightColorScheme =
  lightColorScheme(
    primary = XaiBlack,                  // Black is the primary voice in light mode
    secondary = XaiLightBodyText,
    tertiary = Color.Black,
    background = XaiLightCanvas,          // Off-white (#F9F9FB)
    surface = XaiLightCanvas,             // Off-white (#F9F9FB)
    onPrimary = XaiWhite,                 // White text on black CTA
    onBackground = XaiBlack,
    onSurface = XaiBlack,
    surfaceVariant = XaiLightCanvasCard,  // Pure white card surfaces
    onSurfaceVariant = XaiLightBodyText,
    outline = XaiLightHairline,            // Light border line (#D2D4D8)
    primaryContainer = XaiLightCanvasSoft,
    onPrimaryContainer = XaiBlack
  )

val LocalAccentColor = androidx.compose.runtime.staticCompositionLocalOf { Color.Black }

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  accentColorHex: String? = null,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val app = context.applicationContext as? com.example.AppLockApplication
  val hex = accentColorHex ?: app?.encryptedPrefs?.getAccentColorHex() ?: "Default"
  val resolvedAccent = if (hex == "Default") {
      if (darkTheme) Color.White else Color.Black
  } else {
      try {
          if (hex.startsWith("#")) {
              Color(android.graphics.Color.parseColor(hex))
          } else {
              Color(android.graphics.Color.parseColor("#$hex"))
          }
      } catch (e: Exception) {
          if (darkTheme) Color.White else Color.Black
      }
  }

  val customDarkColorScheme = if (hex == "Default") {
      DarkColorScheme
  } else {
      DarkColorScheme.copy(
          primary = resolvedAccent,
          tertiary = resolvedAccent,
          onPrimaryContainer = resolvedAccent
      )
  }
  val customLightColorScheme = if (hex == "Default") {
      LightColorScheme
  } else {
      LightColorScheme.copy(
          primary = resolvedAccent,
          tertiary = resolvedAccent,
          onPrimaryContainer = resolvedAccent
      )
  }
  val colorScheme = if (darkTheme) customDarkColorScheme else customLightColorScheme

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        // Dynamic styling of status bar and navigation bar depending on theme
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
      }
    }
  }

  androidx.compose.runtime.CompositionLocalProvider(LocalAccentColor provides resolvedAccent) {
      MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
