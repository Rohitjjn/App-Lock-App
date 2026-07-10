package com.example.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PinPad(
    pinHash: String?,
    isVibrationEnabled: Boolean,
    onUnlockSuccess: () -> Unit,
    onIncorrectAttempt: () -> Unit,
    modifier: Modifier = Modifier,
    pinLength: Int = 4
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var enteredPin by remember { mutableStateOf("") }
    var isWrong by remember { mutableStateOf(false) }

    // Shake animation setup
    val shakeOffset = remember { Animatable(0f) }

    suspend fun triggerShake() {
        shakeOffset.animateTo(
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

    fun playClickVibration() {
        if (isVibrationEnabled) {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(35)
                    }
                }
            } catch (e: Exception) {
                // Safe ignore if permission or context isn't available
            }
        }
    }

    val isDark = isSystemInDarkTheme()
    val accentColor = com.example.ui.theme.LocalAccentColor.current
    val textColor = if (isDark) Color.White else Color.Black
    val circleButtonBg = if (isDark) Color(0xFF191919) else Color(0xFFEDEDF0)

    val app = context.applicationContext as com.example.AppLockApplication
    val encryptedPrefs = app.encryptedPrefs
    val liquidGlassLook = remember { encryptedPrefs.getLockLiquidGlassLook() }

    val pinFontSize = remember { encryptedPrefs.getLockPinFontSize() }

    val pinFontColor = textColor
    val pinDotOutlineColor = textColor.copy(alpha = 0.3f)
    val pinDotFilledColor = accentColor
    val keypadButtonShape = androidx.compose.foundation.shape.CircleShape
    val keypadDotShape = androidx.compose.foundation.shape.CircleShape
    val keypadFontFamily = androidx.compose.ui.text.font.FontFamily.Default

    val isGlass = liquidGlassLook != "None"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Dot indicator
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(bottom = 32.dp)
                .graphicsLayer { translationX = shakeOffset.value }
                .testTag("pin_dots_container")
        ) {
            val totalDots = maxOf(pinLength, enteredPin.length)
            for (i in 0 until totalDots) {
                val isFilled = i < enteredPin.length
                val dotColor = if (isWrong || isFilled) {
                    pinDotFilledColor
                } else {
                    Color.Transparent
                }

                val dotScale by animateFloatAsState(
                    targetValue = if (isFilled) 1.25f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "dot_scale"
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(12.dp)
                        .graphicsLayer {
                            scaleX = dotScale
                            scaleY = dotScale
                        }
                        .clip(keypadDotShape)
                        .background(dotColor)
                        .border(
                            width = 1.5.dp,
                            color = if (isWrong) pinDotFilledColor else if (isFilled) Color.Transparent else pinDotOutlineColor,
                            shape = keypadDotShape
                        )
                )
            }
        }

        // Layout numeric grid (0-9 buttons, circular, 72dp size)
        val numKeys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "C")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            numKeys.forEach { rowKeys ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowKeys.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.size(74.dp))
                        } else {
                            PremiumKeyButton(
                                key = key,
                                isWrong = isWrong,
                                fontColor = pinFontColor,
                                fontSize = pinFontSize.toFloat(),
                                isDark = isDark,
                                accentColor = accentColor,
                                onTap = {
                                    if (isWrong) return@PremiumKeyButton
                                    playClickVibration()
                                    
                                    if (key == "C") {
                                        if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                        }
                                    } else {
                                        if (enteredPin.length < 16) {
                                            val nextPin = enteredPin + key
                                            enteredPin = nextPin
                                            if (nextPin.length == pinLength) {
                                                if (pinHash == null || com.example.util.HashUtil.verify(nextPin, pinHash)) {
                                                    onUnlockSuccess()
                                                } else {
                                                    isWrong = true
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(isWrong) {
        if (isWrong) {
            onIncorrectAttempt()
            coroutineScope.launch {
                triggerShake()
            }
            delay(1000)
            enteredPin = ""
            isWrong = false
        }
    }
}

data class SparkleParticle(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val alpha: Float,
    val size: Float,
    val color: Color
)

@Composable
fun PremiumKeyButton(
    key: String,
    isWrong: Boolean,
    fontColor: Color,
    fontSize: Float,
    isDark: Boolean,
    accentColor: Color,
    onTap: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_scale"
    )

    val buttonBg = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.05f)
    }

    val borderCol = if (isDark) {
        Color.White.copy(alpha = 0.15f)
    } else {
        Color.Black.copy(alpha = 0.1f)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(74.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isWrong
            ) {
                onTap()
            }
            .testTag("pin_button_$key")
    ) {
        // Inner actual glass button shape
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(buttonBg)
                .border(1.dp, borderCol, CircleShape)
        ) {
            if (key == "C") {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Backspace Button",
                    tint = fontColor,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Text(
                    text = key,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = fontColor
                )
            }
        }
    }
}
