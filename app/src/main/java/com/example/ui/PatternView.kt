package com.example.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PatternView(
    patternHash: String?,
    isVibrationEnabled: Boolean,
    onUnlockSuccess: () -> Unit,
    onIncorrectAttempt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = com.example.ui.theme.LocalDarkTheme.current
    val textColor = MaterialTheme.colorScheme.onBackground
    val accentColor = com.example.ui.theme.LocalAccentColor.current
    var isWrong by remember { mutableStateOf(false) }
    var currentMsg by remember { mutableStateOf("Draw pattern to unlock") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = currentMsg,
            color = accentColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(bottom = 24.dp)
                .testTag("pattern_status_label")
        )

        PatternCanvas(
            isVibrationEnabled = isVibrationEnabled,
            isWrong = isWrong,
            onPatternComplete = { selectedDots ->
                if (selectedDots.size < 4) {
                    currentMsg = "Connect at least 4 dots"
                    isWrong = true
                    onIncorrectAttempt()
                    coroutineScope.launch {
                        delay(1500)
                        isWrong = false
                        currentMsg = "Draw pattern to unlock"
                    }
                } else {
                    val patternString = selectedDots.joinToString(",")
                    if (com.example.util.HashUtil.verify(patternString, patternHash)) {
                        onUnlockSuccess()
                    } else {
                        currentMsg = "Incorrect pattern"
                        isWrong = true
                        onIncorrectAttempt()
                        coroutineScope.launch {
                            delay(1500)
                            isWrong = false
                            currentMsg = "Draw pattern to unlock"
                        }
                    }
                }
            },
            onResetWrongState = {
                isWrong = false
                currentMsg = "Draw pattern to unlock"
            },
            modifier = Modifier.size(320.dp)
        )
    }
}

@Composable
fun PatternCanvas(
    isVibrationEnabled: Boolean,
    isWrong: Boolean,
    onPatternComplete: (List<Int>) -> Unit,
    onResetWrongState: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedDots by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentTouchPoint by remember { mutableStateOf<Offset?>(null) }
    var autoClearJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val app = context.applicationContext as com.example.AppLockApplication
    val encryptedPrefs = app.encryptedPrefs

    val patternDotsColorHex = remember { encryptedPrefs.getLockPatternDotsColor() }
    val patternDotsSize = remember { encryptedPrefs.getLockPatternDotsSize() }
    val patternDotsShape = remember { encryptedPrefs.getLockPatternDotsShape() }
    val patternLineColorHex = remember { encryptedPrefs.getLockAllLocksColor() } // matching lock shape color or line color
    val realLineColorHex = remember { encryptedPrefs.getLockPatternLineColor() }
    val patternAnimation = remember { encryptedPrefs.getLockPatternAnimation() }
    
    val isDark = com.example.ui.theme.LocalDarkTheme.current
    val accentColor = com.example.ui.theme.LocalAccentColor.current
    val textColor = MaterialTheme.colorScheme.onBackground

    val dotOutlineColor = textColor.copy(alpha = 0.65f)
    val traceLineColor = accentColor

    val dotRadius = patternDotsSize.dp
    val hitRadius = 38.dp

    fun playDotConnectVibration() {
        if (isVibrationEnabled) {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(15)
                    }
                }
            } catch (e: Exception) {
                // Safeguard
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .testTag("pattern_canvas_container"),
        contentAlignment = Alignment.Center
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val sideLength = minOf(width, height)
        val offsetX = (width - sideLength) / 2f
        val offsetY = (height - sideLength) / 2f
        val padding = sideLength / 6
        val step = (sideLength - 2 * padding) / 2

        val dotCenters = remember(sideLength, offsetX, offsetY) {
            val centers = mutableListOf<Offset>()
            for (row in 0..2) {
                for (col in 0..2) {
                    val x = offsetX + padding + col * step
                    val y = offsetY + padding + row * step
                    centers.add(Offset(x, y))
                }
            }
            centers
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isWrong) {
                    if (isWrong) return@pointerInput
                    
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            autoClearJob?.cancel()
                            onResetWrongState()
                            selectedDots = emptyList()
                            currentTouchPoint = startOffset

                            val index = dotCenters.indexOfFirst { center ->
                                (center - startOffset).getDistance() < hitRadius.toPx()
                            }
                            if (index != -1) {
                                selectedDots = listOf(index)
                                playDotConnectVibration()
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val point = change.position
                            currentTouchPoint = point
                            
                            val index = dotCenters.indexOfFirst { center ->
                                (center - point).getDistance() < hitRadius.toPx()
                            }
                            if (index != -1 && index !in selectedDots) {
                                val newlyConnected = mutableListOf<Int>()
                                addPassedDotsInLine(selectedDots, index)?.let { crossedDots ->
                                    selectedDots = selectedDots + crossedDots
                                    newlyConnected.addAll(crossedDots)
                                } ?: run {
                                    selectedDots = selectedDots + index
                                    newlyConnected.add(index)
                                }
                                playDotConnectVibration()
                            }
                        },
                        onDragEnd = {
                            if (selectedDots.isNotEmpty()) {
                                onPatternComplete(selectedDots)
                                autoClearJob?.cancel()
                                autoClearJob = coroutineScope.launch {
                                    delay(2000)
                                    selectedDots = emptyList()
                                }
                            }
                            currentTouchPoint = null
                        },
                        onDragCancel = {
                            autoClearJob?.cancel()
                            selectedDots = emptyList()
                            currentTouchPoint = null
                        }
                    )
                }
        ) {
            val lineColor = if (isWrong) Color.Red else traceLineColor

            if (selectedDots.isNotEmpty()) {
                val connectorPath = Path().apply {
                    val first = dotCenters[selectedDots.first()]
                    moveTo(first.x, first.y)
                    for (i in 1 until selectedDots.size) {
                        val next = dotCenters[selectedDots[i]]
                        lineTo(next.x, next.y)
                    }
                    currentTouchPoint?.let { lineTo(it.x, it.y) }
                }
                
                // Broad translucent glow line
                drawPath(
                    path = connectorPath,
                    color = lineColor.copy(alpha = 0.15f),
                    style = Stroke(
                        width = 24f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Sharp inner connector line
                drawPath(
                    path = connectorPath,
                    color = lineColor,
                    style = Stroke(
                        width = 8f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            dotCenters.forEachIndexed { index, center ->
                val isSelected = index in selectedDots
                val dotColor = when {
                    isSelected -> if (isWrong) Color.Red else traceLineColor
                    else -> dotOutlineColor
                }

                if (isSelected) {
                    if (patternDotsShape == "Square") {
                        drawRect(
                            color = lineColor.copy(alpha = 0.15f),
                            topLeft = Offset(center.x - dotRadius.toPx() * 2.2f, center.y - dotRadius.toPx() * 2.2f),
                            size = androidx.compose.ui.geometry.Size(dotRadius.toPx() * 4.4f, dotRadius.toPx() * 4.4f)
                        )
                        drawRect(
                            color = lineColor.copy(alpha = 0.3f),
                            topLeft = Offset(center.x - dotRadius.toPx() * 1.5f, center.y - dotRadius.toPx() * 1.5f),
                            size = androidx.compose.ui.geometry.Size(dotRadius.toPx() * 3f, dotRadius.toPx() * 3f)
                        )
                        drawRect(
                            color = dotColor,
                            topLeft = Offset(center.x - dotRadius.toPx() * 0.7f, center.y - dotRadius.toPx() * 0.7f),
                            size = androidx.compose.ui.geometry.Size(dotRadius.toPx() * 1.4f, dotRadius.toPx() * 1.4f)
                        )
                    } else if (patternDotsShape == "Star") {
                        val path = Path().apply {
                            moveTo(center.x, center.y - dotRadius.toPx() * 2.2f)
                            lineTo(center.x + dotRadius.toPx() * 2.2f, center.y)
                            lineTo(center.x, center.y + dotRadius.toPx() * 2.2f)
                            lineTo(center.x - dotRadius.toPx() * 2.2f, center.y)
                            close()
                        }
                        drawPath(path = path, color = lineColor.copy(alpha = 0.3f))
                        
                        val innerPath = Path().apply {
                            moveTo(center.x, center.y - dotRadius.toPx() * 0.9f)
                            lineTo(center.x + dotRadius.toPx() * 0.9f, center.y)
                            lineTo(center.x, center.y + dotRadius.toPx() * 0.9f)
                            lineTo(center.x - dotRadius.toPx() * 0.9f, center.y)
                            close()
                        }
                        drawPath(path = innerPath, color = dotColor)
                    } else {
                        drawCircle(
                            color = lineColor.copy(alpha = 0.15f),
                            radius = dotRadius.toPx() * 2.2f,
                            center = center
                        )
                        drawCircle(
                            color = lineColor.copy(alpha = 0.3f),
                            radius = dotRadius.toPx() * 1.5f,
                            center = center
                        )
                        drawCircle(
                            color = dotColor,
                            radius = dotRadius.toPx() * 0.7f,
                            center = center
                        )
                    }
                } else {
                    if (patternDotsShape == "Square") {
                        drawRect(
                            color = dotOutlineColor,
                            topLeft = Offset(center.x - dotRadius.toPx() * 0.5f, center.y - dotRadius.toPx() * 0.5f),
                            size = androidx.compose.ui.geometry.Size(dotRadius.toPx(), dotRadius.toPx())
                        )
                    } else if (patternDotsShape == "Star") {
                        val path = Path().apply {
                            moveTo(center.x, center.y - dotRadius.toPx() * 0.8f)
                            lineTo(center.x + dotRadius.toPx() * 0.8f, center.y)
                            lineTo(center.x, center.y + dotRadius.toPx() * 0.8f)
                            lineTo(center.x - dotRadius.toPx() * 0.8f, center.y)
                            close()
                        }
                        drawPath(path = path, color = dotOutlineColor)
                    } else {
                        drawCircle(
                            color = dotOutlineColor,
                            radius = dotRadius.toPx() * 0.5f,
                            center = center
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(isWrong) {
        if (isWrong) {
            autoClearJob?.cancel()
        } else {
            selectedDots = emptyList()
        }
    }
}

private fun addPassedDotsInLine(currentList: List<Int>, nextDot: Int): List<Int>? {
    val lastSelected = currentList.lastOrNull() ?: return null
    val row1 = lastSelected / 3
    val col1 = lastSelected % 3
    val row2 = nextDot / 3
    val col2 = nextDot % 3
    
    val midRow = (row1 + row2) / 2
    val midCol = (col1 + col2) / 2
    val isTrueMid = (row1 + row2) % 2 == 0 && (col1 + col2) % 2 == 0
    
    if (isTrueMid) {
        val midDot = midRow * 3 + midCol
        if (midDot !in currentList && midDot != nextDot && midDot != lastSelected) {
            return listOf(midDot, nextDot)
        }
    }
    return null
}
