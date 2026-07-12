package com.example.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.Toast
import com.example.AppLockApplication
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.model.LockMethod
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LockScreen(
    appName: String,
    appIcon: Drawable?,
    configuredLockMethod: LockMethod,
    pinHash: String?,
    patternHash: String?,
    isVibrationEnabled: Boolean,
    onUnlockSuccess: () -> Unit,
    onIncorrectAttempt: () -> Unit,
    onTriggerBiometric: (onSuccess: () -> Unit) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    actionKeyword: String? = null,
    previewPrefs: Map<String, Any>? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as AppLockApplication
    val encryptedPrefs = app.encryptedPrefs

    // Cooldown verification state
    var wrongAttemptsCount by remember { mutableStateOf(encryptedPrefs.getWrongAttempts()) }
    var cooldownSecondsRemaining by remember { mutableStateOf(0L) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Security Question state loaded asynchronously from DB
    var securityQuestion by remember { mutableStateOf<com.example.data.model.SecurityQuestion?>(null) }
    LaunchedEffect(Unit) {
        app.repository.getSecurityQuestion().collect {
            securityQuestion = it
        }
    }

    // Cooldown live countdown ticks
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            val cooldownEnd = encryptedPrefs.getCooldownEndTime()
            if (cooldownEnd > now) {
                cooldownSecondsRemaining = (cooldownEnd - now) / 1000
            } else {
                cooldownSecondsRemaining = 0L
                if (encryptedPrefs.getWrongAttempts() >= 3) {
                    encryptedPrefs.setWrongAttempts(0)
                    wrongAttemptsCount = 0
                }
            }
            delay(1000)
        }
    }

    val iconShakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    suspend fun triggerIconShake() {
        iconShakeOffset.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 400
                0f at 0
                -25f at 60
                24f at 120
                -17f at 180
                15f at 240
                -8f at 300
                6f at 350
                0f at 400
            }
        )
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    val onFailedAttempt = {
        val nextCount = wrongAttemptsCount + 1
        wrongAttemptsCount = nextCount
        encryptedPrefs.setWrongAttempts(nextCount)
        if (nextCount == 2) {
            com.example.util.IntruderCameraHelper.prepareCamera(context, lifecycleOwner)
        }
        if (nextCount >= 3) {
            val endCooldown = System.currentTimeMillis() + 30000 // 30 sec cooldown
            encryptedPrefs.setCooldownEndTime(endCooldown)
            cooldownSecondsRemaining = 30
            
            // Capture background silent front camera selfie of intruder within 3 seconds
            coroutineScope.launch {
                com.example.util.IntruderCameraHelper.takeSelfieAndClose(context)
            }
        }
        coroutineScope.launch {
            triggerIconShake()
        }
        onIncorrectAttempt()
    }

    // 1. Theme colors matching requirements: default black accent for light mode and white accent for dark mode
    val isDark = isSystemInDarkTheme()

    // Helper function to read value
    fun getPrefString(key: String, defaultValue: String): String {
        if (previewPrefs != null && previewPrefs.containsKey(key)) {
            return previewPrefs[key] as? String ?: defaultValue
        }
        return when (key) {
            "lock_clock_style" -> encryptedPrefs.getLockClockStyle()
            "lock_clock_font" -> encryptedPrefs.getLockClockFont()
            "lock_clock_weight" -> encryptedPrefs.getLockClockWeight()
            "lock_clock_position" -> encryptedPrefs.getLockClockPosition()
            "lock_bg_theme" -> encryptedPrefs.getLockBackgroundTheme()
            "lock_liquid_glass" -> encryptedPrefs.getLockLiquidGlassLook()
            "lock_btn_shape" -> encryptedPrefs.getLockButtonShape()
            "lock_clock_color" -> encryptedPrefs.getLockClockColor()
            "lock_app_icon_shape" -> encryptedPrefs.getLockAppIconShape()
            "lock_all_locks_shape" -> encryptedPrefs.getLockAllLocksShape()
            "lock_all_locks_color" -> encryptedPrefs.getLockAllLocksColor()
            "lock_pin_font" -> encryptedPrefs.getLockPinFont()
            "lock_pin_color" -> encryptedPrefs.getLockPinFontColor()
            "lock_pin_btn_shape" -> encryptedPrefs.getLockPinBtnShape()
            "lock_pin_dot_shape" -> encryptedPrefs.getLockPinDotShape()
            "lock_pin_dot_outline_color" -> encryptedPrefs.getLockPinDotOutlineColor()
            "lock_pin_dot_filled_color" -> encryptedPrefs.getLockPinDotFilledColor()
            "lock_password_icon_color" -> encryptedPrefs.getLockPasswordIconColor()
            "lock_password_border_color" -> encryptedPrefs.getLockPasswordBorderColor()
            "lock_password_eye_color" -> encryptedPrefs.getLockPasswordEyeColor()
            "lock_password_unlock_btn_color" -> encryptedPrefs.getLockPasswordUnlockButtonColor()
            "lock_pattern_dots_color" -> encryptedPrefs.getLockPatternDotsColor()
            "lock_pattern_dots_shape" -> encryptedPrefs.getLockPatternDotsShape()
            "lock_pattern_line_color" -> encryptedPrefs.getLockPatternLineColor()
            "lock_pattern_animation" -> encryptedPrefs.getLockPatternAnimation()
            "lock_biometric_animation" -> encryptedPrefs.getLockBiometricAnimation()
            "lock_biometric_color" -> encryptedPrefs.getLockBiometricColor()
            "lock_secured_text_color" -> encryptedPrefs.getLockSecuredTextColor()
            "lock_bottom_icon_color" -> encryptedPrefs.getLockBottomIconColor()
            "settings_lock_type_color" -> encryptedPrefs.getSettingsLockTypeColor()
            "lock_biometric_icon_color" -> encryptedPrefs.getLockBiometricIconColor()
            "lock_type_color_bg_pin" -> encryptedPrefs.getLockTypeColorBgPin()
            "lock_type_color_ic_pin" -> encryptedPrefs.getLockTypeColorIcPin()
            "lock_type_color_bg_password" -> encryptedPrefs.getLockTypeColorBgPassword()
            "lock_type_color_ic_password" -> encryptedPrefs.getLockTypeColorIcPassword()
            "lock_type_color_bg_pattern" -> encryptedPrefs.getLockTypeColorBgPattern()
            "lock_type_color_ic_pattern" -> encryptedPrefs.getLockTypeColorIcPattern()
            "lock_type_color_bg_biometric" -> encryptedPrefs.getLockTypeColorBgBiometric()
            "lock_type_color_ic_biometric" -> encryptedPrefs.getLockTypeColorIcBiometric()
            "lock_text_font_secured" -> encryptedPrefs.getLockTextFontSecured()
            "lock_text_color_secured" -> encryptedPrefs.getLockTextColorSecured()
            "lock_text_font_forgot" -> encryptedPrefs.getLockTextFontForgot()
            "lock_text_color_forgot" -> encryptedPrefs.getLockTextColorForgot()
            "lock_text_font_tap_scan" -> encryptedPrefs.getLockTextFontTapScan()
            "lock_text_color_tap_scan" -> encryptedPrefs.getLockTextColorTapScan()
            "lock_text_font_touch_sensor" -> encryptedPrefs.getLockTextFontTouchSensor()
            "lock_text_color_touch_sensor" -> encryptedPrefs.getLockTextColorTouchSensor()
            "lock_text_font_enter_password" -> encryptedPrefs.getLockTextFontEnterPassword()
            "lock_text_color_enter_password" -> encryptedPrefs.getLockTextColorEnterPassword()
            "lock_bg_slider_color" -> encryptedPrefs.getLockBgSliderColor()
            "lock_bg_mix_type" -> encryptedPrefs.getLockBgMixType()
            "lock_bg_custom_wallpaper" -> encryptedPrefs.getLockBgCustomWallpaperPath()
            "lock_clock_animation" -> encryptedPrefs.prefs.getString("lock_clock_animation", "None") ?: "None"
            "lock_bg_animation" -> encryptedPrefs.prefs.getString("lock_bg_animation", "None") ?: "None"
            "lock_app_name_font" -> encryptedPrefs.prefs.getString("lock_app_name_font", "System") ?: "System"
            "lock_app_name_color" -> encryptedPrefs.prefs.getString("lock_app_name_color", "Default") ?: "Default"
            "lock_pin_pad_font" -> encryptedPrefs.prefs.getString("lock_pin_pad_font", "System") ?: "System"
            "lock_pin_pad_font_color" -> encryptedPrefs.prefs.getString("lock_pin_pad_font_color", "Default") ?: "Default"
            "lock_pin_key_press_anim" -> encryptedPrefs.prefs.getString("lock_pin_key_press_anim", "None") ?: "None"
            "lock_padd_unlock_btn_txt_color" -> encryptedPrefs.prefs.getString("lock_padd_unlock_btn_txt_color", "Default") ?: "Default"
            "settings_accent_color" -> encryptedPrefs.getAccentColorHex()
            else -> defaultValue
        }
    }

    fun getPrefFloat(key: String, defaultValue: Float): Float {
        if (previewPrefs != null && previewPrefs.containsKey(key)) {
            val v = previewPrefs[key]
            if (v is Float) return v
            if (v is Double) return v.toFloat()
            if (v is Int) return v.toFloat()
        }
        return when (key) {
            "lock_clock_font_size" -> encryptedPrefs.getLockClockFontSize()
            "lock_all_locks_size" -> encryptedPrefs.getLockAllLocksSize()
            "lock_pin_size" -> encryptedPrefs.getLockPinFontSize()
            "lock_pattern_dots_size" -> encryptedPrefs.getLockPatternDotsSize()
            "lock_biometric_size" -> encryptedPrefs.getLockBiometricSize()
            "lock_app_icon_size" -> encryptedPrefs.prefs.getFloat("lock_app_icon_size", 64f)
            else -> defaultValue
        }
    }

    fun resolveCustomColor(prefHex: String, defaultColor: Color): Color {
        val resolved = if (prefHex == "Default" || prefHex.isBlank()) {
            defaultColor
        } else {
            try {
                if (prefHex.startsWith("#")) {
                    Color(android.graphics.Color.parseColor(prefHex))
                } else {
                    Color(android.graphics.Color.parseColor("#$prefHex"))
                }
            } catch (e: Exception) {
                defaultColor
            }
        }

        val r = resolved.red
        val g = resolved.green
        val b = resolved.blue
        
        val isAlmostBlack = r < 0.15f && g < 0.15f && b < 0.15f
        val isAlmostWhite = r > 0.85f && g > 0.85f && b > 0.85f
        
        if (isDark && isAlmostBlack) {
            return Color.White
        }
        if (!isDark && isAlmostWhite) {
            return Color.Black
        }
        return resolved
    }

    val defaultAccentColor = com.example.ui.theme.LocalAccentColor.current
    val accentColor = remember(defaultAccentColor) {
        resolveCustomColor("Default", defaultAccentColor)
    }
    val textColor = if (isDark) Color.White else Color.Black

    val clockFont = getPrefString("lock_clock_font", "System")
    val clockWeight = getPrefString("lock_clock_weight", "Bold")
    val clockPosition = getPrefString("lock_clock_position", "Top")
    val bgTheme = getPrefString("lock_bg_theme", "Default Theme")
    val liquidGlassLook = getPrefString("lock_liquid_glass", "None")
    val buttonShapePref = getPrefString("lock_btn_shape", "Circle")

    val clockFontSize = getPrefFloat("lock_clock_font_size", 64f)
    val clockColorHex = getPrefString("lock_clock_color", "Default")
    val appIconShape = getPrefString("lock_app_icon_shape", "Rounded Square")
    val allLocksShape = getPrefString("lock_all_locks_shape", "Circle")
    val allLocksColorHex = getPrefString("lock_all_locks_color", "Default")
    val allLocksSize = getPrefFloat("lock_all_locks_size", 64f)
    val bgSliderColorHex = getPrefString("lock_bg_slider_color", "Default")
    val bgMixType = getPrefString("lock_bg_mix_type", "Default Theme")
    val bgCustomWallpaperPath = getPrefString("lock_bg_custom_wallpaper", "")

    val biometricAnimation = getPrefString("lock_biometric_animation", "Area Scan")
    val biometricSize = getPrefFloat("lock_biometric_size", 64f)
    val biometricColorHex = getPrefString("lock_biometric_color", "Default")
    val biometricIconColorHex = getPrefString("lock_biometric_icon_color", "Default")
    val lockSecuredTextColorHex = getPrefString("lock_secured_text_color", "Default")
    val lockBottomIconColorHex = getPrefString("lock_bottom_icon_color", "Default")

    val resolvedBiometricColor = resolveCustomColor(biometricColorHex, accentColor)
    val resolvedBiometricIconColor = resolveCustomColor(biometricIconColorHex, accentColor)
    val resolvedSecuredTextColor = resolveCustomColor(lockSecuredTextColorHex, accentColor)
    val resolvedBottomIconColor = resolveCustomColor(lockBottomIconColorHex, accentColor)

    val dynamicClockFontFamily = getFontFamilyFromPref(clockFont)

    val dynamicClockFontWeight = when (clockWeight) {
        "Normal", "Faded", "Italic", "Dot Dot", "System", "Light" -> FontWeight.Normal
        "Medium" -> FontWeight.Medium
        "Black" -> FontWeight.Black
        "Bold" -> FontWeight.Bold
        else -> FontWeight.Bold
    }
    val dynamicClockFontStyle = if (clockWeight == "Italic") androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
    val dynamicClockAlpha = if (clockWeight == "Faded") 0.55f else 1.0f

    fun formatClockText(text: String, weightPref: String): String {
        return text
    }

    val isGlass = liquidGlassLook != "None"

    val bgGradient = remember(bgMixType, bgSliderColorHex, isDark) {
        if (bgMixType == "Custom Color" && bgSliderColorHex.isNotEmpty() && bgSliderColorHex != "Default") {
            val customCol = try {
                Color(android.graphics.Color.parseColor(bgSliderColorHex))
            } catch (e: Exception) {
                if (isDark) Color(0xFF0A0A0A) else Color(0xFFFFFFFF)
            }
            Brush.verticalGradient(listOf(customCol, customCol))
        } else {
            if (isDark) {
                Brush.verticalGradient(listOf(Color(0xFF0A0A0A), Color(0xFF0A0A0A)))
            } else {
                Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF)))
            }
        }
    }

    val clockStyle = getPrefString("lock_clock_style", "Standard AM/PM")
    val clockColor = resolveCustomColor(clockColorHex, textColor)
    val allLocksColor = resolveCustomColor(allLocksColorHex, accentColor)

    // Time & Date automatic updates
    var currentTime by remember { mutableStateOf("") }
    var currentMinutes by remember { mutableStateOf("") }
    var currentSeconds by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(clockStyle) {
        while (true) {
            val calendar = Calendar.getInstance().time
            currentDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(calendar)
            
            when (clockStyle) {
                "24-Hour Digital" -> {
                    currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar)
                }
                "Retro Minimalist hh.mm" -> {
                    currentTime = SimpleDateFormat("hh.mm", Locale.getDefault()).format(calendar)
                }
                "Dotted Center hh•mm" -> {
                    currentTime = SimpleDateFormat("hh • mm", Locale.getDefault()).format(calendar)
                }
                "Military Modern" -> {
                    currentTime = SimpleDateFormat("HHmm", Locale.getDefault()).format(calendar)
                }
                "Seconds Accent" -> {
                    currentTime = SimpleDateFormat("hh:mm", Locale.getDefault()).format(calendar)
                    currentSeconds = SimpleDateFormat("ss", Locale.getDefault()).format(calendar)
                }
                "Aesthetic Dash" -> {
                    currentTime = SimpleDateFormat("hh — mm", Locale.getDefault()).format(calendar)
                }
                "Double Bracket" -> {
                    currentTime = "[ " + SimpleDateFormat("hh:mm", Locale.getDefault()).format(calendar) + " ]"
                }
                "Lowercase Modern" -> {
                    currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar).lowercase()
                }
                else -> { // "Standard AM/PM"
                    currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar)
                }
            }
            delay(1000)
        }
    }

    val passwordHash = remember(configuredLockMethod) { encryptedPrefs.getPasswordHash() }
    val isMultiLock = remember { encryptedPrefs.isMultiLockEnabled() }
    val multiSequence = remember { encryptedPrefs.getMultiLockSequence() }

    val availableMethods = remember(pinHash, patternHash, passwordHash, configuredLockMethod, isMultiLock, multiSequence) {
        val baseList = if (isMultiLock) {
            val filtered = multiSequence.filter { method ->
                when (method) {
                    LockMethod.PIN -> pinHash != null
                    LockMethod.PASSWORD -> passwordHash != null
                    LockMethod.PATTERN -> patternHash != null
                    LockMethod.BIOMETRIC -> true
                    else -> false
                }
            }
            if (filtered.isNotEmpty()) {
                filtered
            } else {
                val list = mutableListOf<LockMethod>()
                if (pinHash != null) list.add(LockMethod.PIN)
                if (patternHash != null) list.add(LockMethod.PATTERN)
                if (passwordHash != null) list.add(LockMethod.PASSWORD)
                list.add(LockMethod.BIOMETRIC)
                list
            }
        } else {
            val list = mutableListOf<LockMethod>()
            val isSingleBiometric = encryptedPrefs.isSingleBiometricEnabled()
            if (configuredLockMethod != LockMethod.DEFAULT) {
                // If a specific lock type is selected, only show that
                list.add(configuredLockMethod)
                if (isSingleBiometric) {
                    list.add(LockMethod.BIOMETRIC)
                }
            } else {
                // Default: show all standard ones that are set up
                if (pinHash != null) list.add(LockMethod.PIN)
                if (passwordHash != null) list.add(LockMethod.PASSWORD)
                if (patternHash != null) list.add(LockMethod.PATTERN)
                list.add(LockMethod.BIOMETRIC)
            }
            list.distinct()
        }

        baseList
    }

    // Step state for multi-lock sequential security verification
    var currentStepIndex by remember(availableMethods, isMultiLock) {
        mutableStateOf(0)
    }

    // Default starting method prioritizing the rightmost (e.g. BIOMETRIC) if multiLock is enabled
    val defaultStartingMethod = remember(availableMethods, configuredLockMethod, isMultiLock, currentStepIndex) {
        if (isMultiLock && currentStepIndex in availableMethods.indices) {
            availableMethods[currentStepIndex]
        } else if (availableMethods.contains(LockMethod.BIOMETRIC)) {
            LockMethod.BIOMETRIC
        } else if (isMultiLock) {
            availableMethods.firstOrNull() ?: LockMethod.PIN
        } else if (availableMethods.contains(configuredLockMethod)) {
            configuredLockMethod
        } else {
            availableMethods.firstOrNull() ?: LockMethod.PIN
        }
    }

    // Secondary UI active checking method with dynamic key updates
    var activeMethod by remember(defaultStartingMethod) { mutableStateOf(defaultStartingMethod) }

    val handleMethodUnlockSuccess = {
        com.example.util.IntruderCameraHelper.releaseCamera()
        wrongAttemptsCount = 0
        encryptedPrefs.setWrongAttempts(0)

        if (isMultiLock) {
            if (currentStepIndex < availableMethods.size - 1) {
                currentStepIndex += 1
                val nextMethod = availableMethods.getOrNull(currentStepIndex)
                if (nextMethod != null) {
                    activeMethod = nextMethod
                    Toast.makeText(context, "Step verified! Please complete remaining security.", Toast.LENGTH_SHORT).show()
                } else {
                    onUnlockSuccess()
                }
            } else {
                onUnlockSuccess()
            }
        } else {
            onUnlockSuccess()
        }
    }

    LaunchedEffect(activeMethod, lifecycleOwner) {
        if (activeMethod == LockMethod.BIOMETRIC && encryptedPrefs.isBiometricAutoPromptEnabled()) {
            // Safe 500ms delay to ensure the parent layout transitions settle and the device window gains focus
            delay(500)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                onTriggerBiometric {
                    handleMethodUnlockSuccess()
                }
            }
        }
    }

    val safeAppIconBitmap = remember(appIcon) {
        appIcon?.let { getLockScreenSafeImageBitmap(it) }
    }

    val loadedWallpaperBitmap = remember(bgCustomWallpaperPath, bgMixType) {
        if (bgMixType == "Wallpaper Image" && bgCustomWallpaperPath.isNotEmpty()) {
            loadUriToImageBitmap(context, bgCustomWallpaperPath)
        } else {
            null
        }
    }

    // Re-synchronize active method if starting method conditions change
    LaunchedEffect(defaultStartingMethod) {
        activeMethod = defaultStartingMethod
    }

    androidx.compose.runtime.CompositionLocalProvider(com.example.ui.theme.LocalAccentColor provides accentColor) {
        ProvideTextStyle(value = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(bgGradient)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
        if (bgMixType == "Wallpaper Image" && loadedWallpaperBitmap != null) {
            Image(
                bitmap = loadedWallpaperBitmap,
                contentDescription = "Background Wallpaper image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isGlass || bgMixType == "Glassmorphism") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.15f }
                    .background(Color.White)
                    .blur(20.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val clockBlock = @Composable {
                Column(
                     horizontalAlignment = Alignment.CenterHorizontally,
                     modifier = Modifier.padding(top = 16.dp)
                 ) {
                     val baseClockColor = clockColor.copy(alpha = dynamicClockAlpha)
                     if (clockStyle == "Seconds Accent") {
                          Row(
                              verticalAlignment = Alignment.Bottom,
                              horizontalArrangement = Arrangement.Center
                          ) {
                              Text(
                                  text = formatClockText(currentTime, clockWeight),
                                  fontSize = clockFontSize.sp,
                                  fontFamily = dynamicClockFontFamily,
                                  fontWeight = dynamicClockFontWeight,
                                  color = baseClockColor,
                                  fontStyle = dynamicClockFontStyle,
                                  letterSpacing = (-1).sp,
                                  modifier = Modifier.testTag("lock_time_display")
                              )
                              Spacer(modifier = Modifier.width(4.dp))
                              Text(
                                  text = ":" + currentSeconds,
                                  fontSize = (clockFontSize * 0.5f).sp,
                                  fontFamily = dynamicClockFontFamily,
                                  fontWeight = FontWeight.Normal,
                                  color = baseClockColor.copy(alpha = 0.8f * dynamicClockAlpha),
                                  modifier = Modifier.padding(bottom = 6.dp)
                              )
                          }
                     } else {
                          Text(
                              text = formatClockText(currentTime, clockWeight),
                              fontSize = clockFontSize.sp,
                              fontFamily = dynamicClockFontFamily,
                              fontWeight = dynamicClockFontWeight,
                              color = baseClockColor,
                              fontStyle = dynamicClockFontStyle,
                              letterSpacing = (-1).sp,
                              modifier = Modifier.testTag("lock_time_display")
                          )
                     }
                     Spacer(modifier = Modifier.height(4.dp))
                     Text(
                          text = currentDate,
                          fontSize = 15.sp,
                          fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                          fontWeight = FontWeight.Medium,
                          color = baseClockColor.copy(alpha = 0.7f * dynamicClockAlpha),
                          modifier = Modifier.testTag("lock_date_display")
                     )
                 }
            }

            val brandBlock = @Composable {
                // Middle app brand info header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .graphicsLayer { translationX = iconShakeOffset.value }
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(textColor.copy(alpha = 0.05f))
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (safeAppIconBitmap != null) {
                            Image(
                                bitmap = safeAppIconBitmap,
                                contentDescription = "Target Locked App Icon AppIcon",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .testTag("locked_app_icon")
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = allLocksColor,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!actionKeyword.isNullOrEmpty()) {
                        Text(
                            text = "Enter password to $actionKeyword $appName",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor.copy(alpha = 0.9f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp).testTag("action_keyword_name")
                        )
                    } else {
                        Text(
                            text = appName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.testTag("locked_app_name")
                        )
                    }



                    // Smooth, gpu-friendly right-to-left step progress indicator
                    if (isMultiLock && availableMethods.size > 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until availableMethods.size) {
                                val method = availableMethods[i]
                                val isCompleted = i < currentStepIndex
                                val isCurrent = i == currentStepIndex

                                val containerColor = when {
                                    isCompleted -> accentColor
                                    isCurrent -> accentColor.copy(alpha = 0.25f)
                                    else -> textColor.copy(alpha = 0.08f)
                                }

                                val borderStroke = if (isCurrent) {
                                    androidx.compose.foundation.BorderStroke(1.5.dp, accentColor)
                                } else {
                                    null
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(8.dp)) else Modifier)
                                        .background(containerColor)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val contentTint = if (isCompleted) {
                                            if (accentColor.luminance() > 0.5f) Color.Black else Color.White
                                        } else if (isCurrent) {
                                            textColor
                                        } else {
                                            textColor.copy(alpha = 0.4f)
                                        }

                                        Icon(
                                            imageVector = when (method) {
                                                LockMethod.PIN -> Icons.Default.Password
                                                LockMethod.PATTERN -> Icons.Default.Grid3x3
                                                LockMethod.BIOMETRIC -> Icons.Default.Fingerprint
                                                LockMethod.PASSWORD -> Icons.Default.Lock
                                                else -> Icons.Default.Lock
                                            },
                                            contentDescription = null,
                                            tint = contentTint,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Step ${i + 1}",
                                            fontSize = 10.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            color = contentTint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (clockPosition == "Top" || clockPosition == "Default" || clockPosition == "") {
                clockBlock()
            }
            brandBlock()
            if (clockPosition == "Center") {
                clockBlock()
            }

            // Entry validation body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (cooldownSecondsRemaining > 0) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(24.dp)
                            .testTag("cooldown_panel")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Securing cooldown active",
                            tint = accentColor,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Security Cooldown Active",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Too many wrong attempts. For your safety, the keypad is locked.",
                            fontSize = 14.sp,
                            color = textColor.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Try again in ${cooldownSecondsRemaining}s",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Crossfade(targetState = activeMethod, label = "method_transition") { method ->
                        when (method) {
                            LockMethod.PIN -> {
                                PinPad(
                                    pinHash = pinHash,
                                    isVibrationEnabled = isVibrationEnabled,
                                    onUnlockSuccess = handleMethodUnlockSuccess,
                                    onIncorrectAttempt = onFailedAttempt,
                                    pinLength = encryptedPrefs.getPinLength()
                                )
                            }
                            LockMethod.PASSWORD -> {
                                PasswordEntryPad(
                                    passwordHash = passwordHash,
                                    isVibrationEnabled = isVibrationEnabled,
                                    onUnlockSuccess = handleMethodUnlockSuccess,
                                    onIncorrectAttempt = onFailedAttempt
                                )
                            }
                            LockMethod.PATTERN -> {
                                PatternView(
                                    patternHash = patternHash,
                                    isVibrationEnabled = isVibrationEnabled,
                                    onUnlockSuccess = handleMethodUnlockSuccess,
                                    onIncorrectAttempt = onFailedAttempt
                                )
                            }
                            LockMethod.BIOMETRIC -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AnimatedFingerprintScanner(
                                        onTriggerBiometric = {
                                            onTriggerBiometric {
                                                handleMethodUnlockSuccess()
                                            }
                                        },
                                        accentColor = accentColor,
                                        textColor = textColor,
                                        biometricColor = resolvedBiometricColor,
                                        biometricIconColor = resolvedBiometricIconColor,
                                        biometricSize = biometricSize,
                                        biometricAnimation = biometricAnimation
                                    )
                                }
                            }
                            else -> {
                                Spacer(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }

            // Bottom controls: Forgot passcode reset link & Rounded Lock Switch Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Forgot passcode trigger recovery answer (Only display under PIN/Pattern keyboard entry modes)
                if (securityQuestion != null && cooldownSecondsRemaining == 0L && activeMethod != LockMethod.BIOMETRIC) {
                    TextButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("forgot_passcode_button")
                    ) {
                        Text(
                            text = "Forgot Passcode?",
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                if (cooldownSecondsRemaining == 0L && availableMethods.size > 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("lock_method_switch_row")
                    ) {
                        for (method in availableMethods) {
                            val isCurrentlyActive = activeMethod == method
                            val isInteractive = !isMultiLock
                            
                            RoundLockMethodButton(
                                method = method,
                                isSelected = isCurrentlyActive,
                                isEnabled = isInteractive || isCurrentlyActive,
                                onClick = {
                                    if (isInteractive) {
                                        activeMethod = method
                                    }
                                },
                                accentColor = accentColor,
                                encryptedPrefs = encryptedPrefs,
                                textColor = textColor
                            )
                        }
                    }
                }
            }
            
            if (clockPosition == "Bottom") {
                clockBlock()
            }
        }
        }
    }

    // Alarm question verification popup
    if (showResetDialog && securityQuestion != null) {
        val q = securityQuestion!!
        var answerInput by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset Passcode",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            },
            text = {
                Column {
                    Text(
                        text = "Security Question:",
                        fontSize = 13.sp,
                        color = textColor.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = q.question,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = answerInput,
                        onValueChange = {
                            answerInput = it
                            errorMsg = null
                        },
                        label = { Text("Your Answer") },
                        isError = errorMsg != null,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = textColor.copy(alpha = 0.3f),
                            focusedLabelColor = accentColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMsg != null) {
                         Spacer(modifier = Modifier.height(4.dp))
                         Text(
                             text = errorMsg!!,
                             color = Color.Red,
                             fontSize = 12.sp,
                             fontWeight = FontWeight.Bold
                         )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanedVal = answerInput.trim().lowercase()
                        if (cleanedVal.isEmpty()) {
                            errorMsg = "Please enter an answer"
                            return@Button
                        }
                        val matched = com.example.util.HashUtil.verify(cleanedVal, q.answerHash)
                        if (matched) {
                            encryptedPrefs.clearPinHash()
                            encryptedPrefs.clearPatternHash()
                            Toast.makeText(context, "Lock Reset Successful! Security turned off.", Toast.LENGTH_LONG).show()
                            showResetDialog = false
                            onUnlockSuccess()
                        } else {
                            errorMsg = "Incorrect answer. Please try again."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier.testTag("reset_verify_button")
                ) {
                    Text("Verify Answer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = textColor.copy(alpha = 0.7f))
                }
            }
        )
    }
}
}

@Composable
fun SelectorIcon(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    textColor: Color
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) accentColor else textColor.copy(alpha = 0.45f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun AnimatedFingerprintScanner(
    onTriggerBiometric: () -> Unit,
    accentColor: Color,
    textColor: Color,
    biometricColor: Color = accentColor,
    biometricIconColor: Color = accentColor,
    biometricSize: Float = 64f,
    biometricAnimation: String = "Ripple Pulse",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fingerprint_fill")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fill_progress"
    )

    // Stage 1: progress 0.0f -> 0.75f is filling
    // Stage 2: progress 0.75f -> 1.0f is erasing/dissolving (alpha goes 1.0f -> 0.0f)
    val fillProgress = (animationProgress / 0.75f).coerceAtMost(1f)
    val fadeAlpha = if (animationProgress > 0.75f) {
        (1f - (animationProgress - 0.75f) / 0.25f).coerceIn(0f, 1f)
    } else {
        1f
    }

    val scannerSize = remember(biometricSize) { biometricSize.dp }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onTriggerBiometric,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(scannerSize * 1.5f)
                .testTag("tap_biometric_indicator")
        ) {
            // Base Grey-White Fingerprint Icon for high contrast
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Fingerprint,
                contentDescription = "Tap fingerprint scanner icon to authenticate",
                tint = Color(0xFFE2E8F0), // High contrast Grey-White
                modifier = Modifier.size(scannerSize)
            )

            // Overlay themed Fingerprint Icon, clipped to expanding circle, and faded on dissolve stage
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Fingerprint,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .size(scannerSize)
                    .graphicsLayer {
                        alpha = fadeAlpha
                        clip = true
                        shape = object : Shape {
                            override fun createOutline(
                                size: androidx.compose.ui.geometry.Size,
                                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                                density: androidx.compose.ui.unit.Density
                            ): Outline {
                                val maxRadius = Math.sqrt((size.width * size.width + size.height * size.height).toDouble()).toFloat() / 1.7f
                                val radius = maxRadius * fillProgress
                                val path = Path().apply {
                                    addOval(
                                        androidx.compose.ui.geometry.Rect(
                                            center = Offset(size.width / 2f, size.height / 2f),
                                            radius = radius
                                        )
                                    )
                                }
                                return Outline.Generic(path)
                            }
                        }
                    }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "TAP TO SCAN FINGERPRINT",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Touch sensor to unlock the application",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor.copy(alpha = 0.65f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RoundLockMethodButton(
    method: LockMethod,
    isSelected: Boolean,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
    accentColor: Color,
    textColor: Color,
    encryptedPrefs: com.example.data.local.EncryptedPrefs
) {
    val bg = if (isSelected) accentColor else accentColor.copy(alpha = 0.12f)
    val ic = if (isSelected) {
        if (accentColor.luminance() > 0.5f) Color.Black else Color.White
    } else {
        accentColor.copy(alpha = 0.65f)
    }

    val icon = when (method) {
        LockMethod.PIN -> Icons.Default.Password
        LockMethod.PASSWORD -> Icons.Default.Lock
        LockMethod.PATTERN -> Icons.Default.Grid3x3
        LockMethod.BIOMETRIC -> Icons.Default.Fingerprint
        else -> Icons.Default.Lock
    }
    
    val contentDescription = "Switch to ${method.name}"
    val alphaValue = if (!isEnabled) 0.35f else if (isSelected) 1.0f else 0.85f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .then(if (!isEnabled && !isSelected) Modifier.blur(1.5.dp) else Modifier)
            .graphicsLayer { alpha = alphaValue }
            .background(bg)
            .then(if (isEnabled) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = ic,
            modifier = Modifier.size(24.dp)
        )
    }
}

fun getLockScreenSafeImageBitmap(drawable: Drawable): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        val width = 96
        val height = 96
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

fun getShapeFromPref(shapeName: String): androidx.compose.ui.graphics.Shape {
    return when (shapeName) {
        "Circle" -> androidx.compose.foundation.shape.CircleShape
        "Rounded Square" -> androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        "Rounded Small" -> androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        "Cut Corner 8" -> androidx.compose.foundation.shape.CutCornerShape(8.dp)
        "Cut Corner 16" -> androidx.compose.foundation.shape.CutCornerShape(16.dp)
        "Capsule" -> androidx.compose.foundation.shape.RoundedCornerShape(50)
        "Leaf Shape" -> androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp)
        "Diamond" -> androidx.compose.foundation.shape.CutCornerShape(24.dp)
        "Squircle" -> androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        "Wavy Arch" -> androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        else -> androidx.compose.foundation.shape.CircleShape
    }
}

fun getFontFamilyFromPref(fontName: String): FontFamily {
    return when (fontName) {
        "Default", "System" -> FontFamily.Default
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
        "Sans-Serif" -> FontFamily.SansSerif
        "Roboto" -> FontFamily.Default
        "Elegant Serif" -> FontFamily.Serif
        "Modern Typewriter" -> FontFamily.Monospace
        "Grotesque Bold" -> FontFamily.SansSerif
        "Handwritten" -> FontFamily.Cursive
        else -> FontFamily.Default
    }
}

fun hexToColor(hex: String, defaultColor: Color): Color {
    if (hex == "Default" || hex.isBlank()) return defaultColor
    return try {
        if (hex.startsWith("#")) {
            Color(android.graphics.Color.parseColor(hex))
        } else {
            Color(android.graphics.Color.parseColor("#$hex"))
        }
    } catch (e: Exception) {
        defaultColor
    }
}

fun loadUriToImageBitmap(context: Context, uriString: String): androidx.compose.ui.graphics.ImageBitmap? {
    if (uriString.isBlank()) return null
    return try {
        if (uriString == "wallpaper") {
            val file = java.io.File(context.filesDir, "custom_wallpaper.png")
            if (file.exists()) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                return bitmap?.asImageBitmap()
            }
        }
        val uri = android.net.Uri.parse(uriString)
        context.contentResolver.openInputStream(uri).use { stream ->
            val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
            bitmap?.asImageBitmap()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun PremiumBiometricIcon(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)
            
            // Tech-corners tracking brackets surrounding scanner
            val bracketLength = w * 0.15f
            val strokeW = 1.8.dp.toPx()
            
            // Top-Left corner bracket
            drawPath(
                path = Path().apply {
                    moveTo(strokeW, strokeW + bracketLength)
                    lineTo(strokeW, strokeW)
                    lineTo(strokeW + bracketLength, strokeW)
                },
                color = color.copy(alpha = 0.85f),
                style = Stroke(width = strokeW)
            )
            // Top-Right corner bracket
            drawPath(
                path = Path().apply {
                    moveTo(w - strokeW, strokeW + bracketLength)
                    lineTo(w - strokeW, strokeW)
                    lineTo(w - strokeW - bracketLength, strokeW)
                },
                color = color.copy(alpha = 0.85f),
                style = Stroke(width = strokeW)
            )
            // Bottom-Left corner bracket
            drawPath(
                path = Path().apply {
                    moveTo(strokeW, h - strokeW - bracketLength)
                    lineTo(strokeW, h - strokeW)
                    lineTo(strokeW + bracketLength, h - strokeW)
                },
                color = color.copy(alpha = 0.85f),
                style = Stroke(width = strokeW)
            )
            // Bottom-Right corner bracket
            drawPath(
                path = Path().apply {
                    moveTo(w - strokeW, h - strokeW - bracketLength)
                    lineTo(w - strokeW, h - strokeW)
                    lineTo(w - strokeW - bracketLength, h - strokeW)
                },
                color = color.copy(alpha = 0.85f),
                style = Stroke(width = strokeW)
            )
            
            // Tech-notches/ticks around the circumference in circular ring shape
            val notchesCount = 12
            for (i in 0 until notchesCount) {
                val angleDeg = (i * 360f / notchesCount)
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val r1 = w * 0.44f
                val r2 = w * 0.49f
                val p1 = Offset(
                    (center.x + r1 * Math.cos(angleRad)).toFloat(),
                    (center.y + r1 * Math.sin(angleRad)).toFloat()
                )
                val p2 = Offset(
                    (center.x + r2 * Math.cos(angleRad)).toFloat(),
                    (center.y + r2 * Math.sin(angleRad)).toFloat()
                )
                drawLine(
                    color = color.copy(alpha = 0.35f),
                    start = p1,
                    end = p2,
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
        
        // Inner actual fingerprint scanner icon
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Fingerprint,
            contentDescription = null,
            tint = color,
            modifier = Modifier.fillMaxSize(0.55f)
        )
    }
}
