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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF1A1A1A)
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
        if (nextCount >= 3) {
            val endCooldown = System.currentTimeMillis() + 30000 // 30 sec cooldown
            encryptedPrefs.setCooldownEndTime(endCooldown)
            cooldownSecondsRemaining = 30
            
            // Capture background silent front camera selfie of intruder within 3 seconds
            coroutineScope.launch {
                delay(1500)
                com.example.util.IntruderCameraHelper.captureSelfie(context, lifecycleOwner)
            }
        }
        coroutineScope.launch {
            triggerIconShake()
        }
        onIncorrectAttempt()
    }

    // 1. Theme colors matching requirements: accent is #FF6B6B, Light bg #FFFFFF, Dark bg #1A1A1A
    val accentColor = Color(0xFFFF6B6B)
    val textColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1A1A1A)
    
    val bgGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1F1F1F),
                Color(0xFF141414),
                Color(0xFF0F0F0F)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF9F9FB),
                Color(0xFFEDEDF0)
            )
        )
    }

    // Time & Date automatic updates
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormatter = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        while (true) {
            val calendar = Calendar.getInstance().time
            currentTime = timeFormatter.format(calendar)
            currentDate = dateFormatter.format(calendar)
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
        } else if (configuredLockMethod == LockMethod.DEFAULT) {
            val list = mutableListOf<LockMethod>()
            if (pinHash != null) list.add(LockMethod.PIN)
            if (passwordHash != null) list.add(LockMethod.PASSWORD)
            if (patternHash != null) list.add(LockMethod.PATTERN)
            list.add(LockMethod.BIOMETRIC)
            list.distinct()
        } else {
            listOf(configuredLockMethod)
        }

        // Align BIOMETRIC to the right-most end to match user's UI layout requirements
        if (baseList.size > 1 && baseList.contains(LockMethod.BIOMETRIC)) {
            baseList.filter { it != LockMethod.BIOMETRIC } + LockMethod.BIOMETRIC
        } else {
            baseList
        }
    }

    // Step state for multi-lock sequential security verification (counting right-to-left config)
    var currentStepIndex by remember(availableMethods, isMultiLock) {
        mutableStateOf(if (isMultiLock) availableMethods.size - 1 else 0)
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
        if (isMultiLock) {
            if (currentStepIndex > 0) {
                currentStepIndex -= 1
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
        appIcon?.let { getSafeImageBitmap(it) }
    }

    // Re-synchronize active method if starting method conditions change
    LaunchedEffect(defaultStartingMethod) {
        activeMethod = defaultStartingMethod
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Real-time dynamic Clock & Date
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = currentTime,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    letterSpacing = (-1).sp,
                    modifier = Modifier.testTag("lock_time_display")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentDate,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.55f),
                    modifier = Modifier.testTag("lock_date_display")
                )
            }

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
                        .clip(RoundedCornerShape(18.dp))
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
                                .clip(RoundedCornerShape(14.dp))
                                .testTag("locked_app_icon")
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = appName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.testTag("locked_app_name")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SECURED PASSCODE DETECTED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        letterSpacing = 1.sp
                    )
                }

                // Smooth, gpu-friendly right-to-left step progress indicator
                if (isMultiLock && availableMethods.size > 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in (availableMethods.size - 1) downTo 0) {
                            val method = availableMethods[i]
                            val isCompleted = i > currentStepIndex
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
                                    Icon(
                                        imageVector = when (method) {
                                            LockMethod.PIN -> Icons.Default.Password
                                            LockMethod.PATTERN -> Icons.Default.Grid3x3
                                            LockMethod.BIOMETRIC -> Icons.Default.Fingerprint
                                            LockMethod.PASSWORD -> Icons.Default.Lock
                                            else -> Icons.Default.Lock
                                        },
                                        contentDescription = null,
                                        tint = if (isCompleted || isCurrent) textColor else textColor.copy(alpha = 0.4f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Step ${availableMethods.size - i}",
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCompleted || isCurrent) textColor else textColor.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
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
                                        textColor = textColor
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
                        availableMethods.forEach { method ->
                            RoundLockMethodButton(
                                method = method,
                                isSelected = activeMethod == method,
                                onClick = {
                                    if (method == LockMethod.BIOMETRIC) {
                                        activeMethod = LockMethod.BIOMETRIC
                                        onTriggerBiometric {
                                            handleMethodUnlockSuccess()
                                        }
                                    } else {
                                        activeMethod = method
                                    }
                                },
                                accentColor = accentColor,
                                textColor = textColor
                            )
                        }
                    }
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
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    // Scale pulse animation for scanning circle effect
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Alpha pulsing animation for scanning glow
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Gentle rotational vibration to indicate it's active & scanning
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onTriggerBiometric,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null // Custom visual rings are used instead of default square ripple
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(150.dp)
        ) {
            // First Outer concentric glowing pulse ring
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .background(accentColor.copy(alpha = 0.35f), CircleShape)
            )

            // Second Inner concentric glowing pulse ring
            Box(
                modifier = Modifier
                    .size(85.dp)
                    .graphicsLayer {
                        scaleX = 1f + (pulseScale - 1f) * 0.5f
                        scaleY = 1f + (pulseScale - 1f) * 0.5f
                        alpha = pulseAlpha + 0.15f
                    }
                    .background(accentColor.copy(alpha = 0.25f), CircleShape)
            )

            // Primary Touch target button surface
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(70.dp)
                    .graphicsLayer {
                        rotationZ = rotationAngle
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.95f),
                                accentColor.copy(alpha = 0.70f)
                            )
                        ),
                        CircleShape
                    )
                    .testTag("tap_biometric_indicator")
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Tap fingerprint scanner icon to authenticate",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "TAP TO SCAN FINGERPRINT",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Touch sensor to unlock the application",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor.copy(alpha = 0.45f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RoundLockMethodButton(
    method: LockMethod,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    textColor: Color
) {
    val icon = when (method) {
        LockMethod.PIN -> Icons.Default.Password
        LockMethod.PASSWORD -> Icons.Default.Lock
        LockMethod.PATTERN -> Icons.Default.Grid3x3
        LockMethod.BIOMETRIC -> Icons.Default.Fingerprint
        else -> Icons.Default.Lock
    }
    
    val contentDescription = "Switch to ${method.name}"
    val alphaValue = if (isSelected) 1.0f else 0.40f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .graphicsLayer { alpha = alphaValue }
            .background(if (isSelected) accentColor else textColor.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) Color.White else textColor.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun getSafeImageBitmap(drawable: Drawable): androidx.compose.ui.graphics.ImageBitmap? {
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
