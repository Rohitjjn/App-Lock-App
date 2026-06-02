package com.example.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.HashUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordEntryPad(
    passwordHash: String?,
    isVibrationEnabled: Boolean,
    onUnlockSuccess: () -> Unit,
    onIncorrectAttempt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var enteredPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isWrong by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

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
                // Ignore
            }
        }
    }

    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background == Color(0xFF1A1A1A)
    val accentColor = Color(0xFFFF6B6B)
    val textColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1A1A1A)

    fun handleUnlockVerify() {
        playClickVibration()
        if (enteredPassword.isEmpty()) {
            errorMessage = "Password cannot be empty"
            return
        }

        if (passwordHash == null || HashUtil.verify(enteredPassword, passwordHash)) {
            onUnlockSuccess()
        } else {
            isWrong = true
            errorMessage = "Incorrect password"
            enteredPassword = ""
            coroutineScope.launch {
                triggerShake()
                delay(1000)
                isWrong = false
                errorMessage = ""
            }
            onIncorrectAttempt()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .graphicsLayer { translationX = shakeOffset.value }
    ) {
        Icon(
            imageVector = Icons.Default.LockOpen,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Enter Password",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = enteredPassword,
            onValueChange = {
                enteredPassword = it
                errorMessage = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("password_unlock_input"),
            label = { Text("App Password") },
            placeholder = { Text("Enter your secure password") },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { handleUnlockVerify() }
            ),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                focusedLabelColor = accentColor,
                unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                cursorColor = accentColor
            ),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                    )
                }
            }
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { handleUnlockVerify() },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("password_unlock_submit_btn"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Unlock App",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
