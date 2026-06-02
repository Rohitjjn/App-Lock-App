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
    primary = AppAccent,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = AppBgDark,
    surface = AppBgDark,
    onPrimary = AppBgDark,
    onBackground = AppTextDark,
    onSurface = AppTextDark,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = AppTextDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = AppAccent,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = AppBgLight,
    surface = AppBgLight,
    onPrimary = AppBgLight,
    onBackground = AppTextLight,
    onSurface = AppTextLight,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = AppTextLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamic colors by default to preserve the exact brand color identity requested by the user
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
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

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
