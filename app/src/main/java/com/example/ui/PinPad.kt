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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

    // Colors matching user requirements: Accent is #FF6B6B, text is #1A1A1A, bg is #FFFFFF (with system isSystemInDarkTheme validation)
    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background == Color(0xFF1A1A1A)
    val accentColor = Color(0xFFFF6B6B)
    val textColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1A1A1A)
    val circleButtonBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)

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
                    accentColor
                } else {
                    textColor.copy(alpha = 0.2f)
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
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(
                            width = if (!isFilled) 1.5.dp else 0.dp,
                            color = if (isWrong) accentColor else textColor.copy(alpha = 0.3f),
                            shape = CircleShape
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
                            Spacer(modifier = Modifier.size(72.dp))
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(circleButtonBg)
                                    .clickable {
                                        if (isWrong) return@clickable
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
                                    .testTag("pin_button_$key")
                            ) {
                                if (key == "C") {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace Button",
                                        tint = textColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                            }
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
