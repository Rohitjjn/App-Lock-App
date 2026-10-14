package com.example.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.AppLockApplication
import com.example.data.local.EncryptedPrefs
import com.example.data.model.LockMethod
import com.example.ui.theme.MyApplicationTheme
import com.example.util.BiometricHelper
import com.example.util.HashUtil
import com.example.util.LockSessionManager
import androidx.core.content.ContextCompat
import com.example.util.AuthRequestGeneration

class LockActivity : FragmentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        private const val TAG = "LockActivity"
    }

    private var lockedPackageName: String = ""
    private var actionKeyword: String? = null
    private var isTileToggleAction: Boolean = false
    private lateinit var encryptedPrefs: EncryptedPrefs
    private val biometricHelper by lazy { BiometricHelper() }
    private val requests = AuthRequestGeneration()
    private var contentGeneration by mutableLongStateOf(0L)
    private var biometricPrompt: androidx.biometric.BiometricPrompt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleUnlockCancel(byBackPress = true)
            }
        })

        // 1. Prevent split-screen / multi-window bypass lock bypass vectors
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (isInMultiWindowMode) {
                Log.w(TAG, "Preventing multi-window lock screen bypass. Falling back to home.")
                handleUnlockCancel()
                return
            }
        }

        // 2. Security hardening: Purge device clipboard buffer on lock validation entry to avoid background data leakage
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            if (clipboard != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    clipboard.clearPrimaryClip()
                } else {
                    val clipData = android.content.ClipData.newPlainText("", "")
                    clipboard.setPrimaryClip(clipData)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Safe clipboard clearing operation error", e)
        }

        val app = application as AppLockApplication
        encryptedPrefs = app.encryptedPrefs

        if (!bindTarget(intent)) return

        // 3. Jetpack Compose error layout safe fallback rendering catch blocks
        try {
            setContent {
                val themeMode = remember { encryptedPrefs.getThemeMode() }
                val accentColorHex = remember { encryptedPrefs.getAccentColorHex() }
                val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
                val useDarkTheme = remember(themeMode, isSystemDark) {
                    when (themeMode) {
                        "LIGHT" -> false
                        "DARK" -> true
                        else -> isSystemDark
                    }
                }
                MyApplicationTheme(darkTheme = useDarkTheme, accentColorHex = accentColorHex) {
                    key(contentGeneration) { LockScreenContent() }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Security composing validation loop crash fallback triggers active", e)
            Toast.makeText(this, "Security validation services failure, resetting display.", Toast.LENGTH_LONG).show()
            handleUnlockCancel()
            return
        }
    }

    private fun invalidateRequests() {
        contentGeneration = requests.invalidate()
        biometricPrompt?.cancelAuthentication()
        biometricPrompt = null
        isBiometricPromptShowing = false
    }

    private fun bindTarget(newIntent: Intent): Boolean {
        isTileToggleAction = newIntent.getBooleanExtra("EXTRA_TILE_TOGGLE_ACTION", false)
        lockedPackageName = if (isTileToggleAction) "com.example.tile_toggle"
            else newIntent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        actionKeyword = newIntent.getStringExtra("ACTION_KEYWORD")
        if (lockedPackageName.isBlank()) {
            finish()
            return false
        }
        LockSessionManager.clearLaunchPending(lockedPackageName)
        LockSessionManager.currentlyDisplayingLockFor = lockedPackageName
        return true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        invalidateRequests() // Invalidate before cancellation can deliver a queued callback.
        if (LockSessionManager.currentlyDisplayingLockFor == lockedPackageName) {
            LockSessionManager.currentlyDisplayingLockFor = null
        }
        LockSessionManager.clearLaunchPending(lockedPackageName)
        setIntent(intent)
        bindTarget(intent)
    }

    private fun accepts(token: Long): Boolean = requests.accepts(token) &&
        !isFinishing && !isDestroyed &&
        lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // If the user navigates away (e.g. clicks Home button), handle this as a secure cancel
        handleUnlockCancel()
    }

    private var isBiometricPromptShowing = false

    private fun triggerBiometricAuth(onSuccess: () -> Unit) {
        if (isBiometricPromptShowing) return
        val token = requests.current
        if (!accepts(token)) return
        isBiometricPromptShowing = true
        biometricPrompt = biometricHelper.authenticate(
            activity = this,
            executor = ContextCompat.getMainExecutor(this),
            title = "App Lock Biometrics",
            subtitle = "Verify to unlock application",
            onSuccess = {
                runOnUiThread {
                    if (!accepts(token)) return@runOnUiThread
                    isBiometricPromptShowing = false
                    onSuccess()
                }
            },
            onError = { errorCode, errString ->
                runOnUiThread {
                    if (!accepts(token)) return@runOnUiThread
                    isBiometricPromptShowing = false
                    // Ignore user canceled, canceled by system/back, and negative (cancel) button click codes
                    if (errorCode != 5 && errorCode != 10 && errorCode != 13) {
                        Toast.makeText(this@LockActivity, "Biometrics: $errString", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onFailed = {
                runOnUiThread {
                    if (!accepts(token)) return@runOnUiThread
                    // A failed scan is non-terminal; the same system prompt remains active.
                    Toast.makeText(this@LockActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun handleUnlockSuccess() {
        invalidateRequests()
        if (isTileToggleAction) {
            val currentShieldState = encryptedPrefs.isShieldEnabled()
            val newShieldState = !currentShieldState
            encryptedPrefs.setShieldEnabled(newShieldState)
            
            val message = if (newShieldState) {
                "App Shield Switched ON"
            } else {
                "App Shield Switched OFF"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    TileService.requestListeningState(this, android.content.ComponentName(this, com.example.service.SafeLockTileService::class.java))
                    com.example.service.SafeLockTileService.updateActiveTile()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request tile update", e)
            }
            
            finish()
            overridePendingTransition(0, 0)
            return
        }
        LockSessionManager.setAppUnlocked(lockedPackageName)
        finish()
        overridePendingTransition(0, 0)
    }

    private fun handleUnlockCancel(byBackPress: Boolean = false) {
        invalidateRequests()
        if (isTileToggleAction) {
            finish()
            return
        }
        
        // Agar user bina pattern ya PIN dale cancel krta h (back/home press se), 
        // toh hum direct user ko safe Home Screen par bhej denge aur locked app ko terminate kr denge, 
        // taaki koi security leak ya activity flicker na ho.
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start home intent", e)
        }

        if (byBackPress) {
            // Set flag so AppDetectorService performs safe auto-back bypass on the target app
            LockSessionManager.cancelActionPendingFor = lockedPackageName
        }
        
        // Kill background process of target locked app because they did not unlock it!
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.killBackgroundProcesses(lockedPackageName)
            Log.d(TAG, "Successfully killed background processes of unauthorized app: $lockedPackageName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to kill background process for $lockedPackageName", e)
        }
        
        finish()
        overridePendingTransition(0, 0)
    }

    private fun triggerVibration() {
        if (encryptedPrefs.isVibrationEnabled()) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to vibrate", e)
            }
        }
    }

    @Composable
    fun LockScreenContent() {
        val token = requests.current
        val context = LocalContext.current
        val appName = remember { getAppName(context, lockedPackageName) }
        val appIcon = remember { getAppIcon(context, lockedPackageName) }

        val configuredMethod = remember { encryptedPrefs.getLockMethod() }
        val pinHash = remember { encryptedPrefs.getPinHash() }
        val patternHash = remember { encryptedPrefs.getPatternHash() }
        val isVibrationEnabled = remember { encryptedPrefs.isVibrationEnabled() }

        LockScreen(
            appName = appName,
            appIcon = appIcon,
            configuredLockMethod = configuredMethod,
            pinHash = pinHash,
            patternHash = patternHash,
            isVibrationEnabled = isVibrationEnabled,
            onUnlockSuccess = { if (accepts(token)) handleUnlockSuccess() },
            onIncorrectAttempt = { triggerVibration() },
            onTriggerBiometric = { onSuccessCallback -> if (accepts(token)) triggerBiometricAuth(onSuccessCallback) },
            onCancel = { handleUnlockCancel(byBackPress = true) },
            actionKeyword = actionKeyword
        )
    }

    private fun getAppName(context: Context, packageName: String): String {
        if (isTileToggleAction) {
            return "App Shield"
        }
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            "External App"
        }
    }

    private fun getAppIcon(context: Context, packageName: String): Drawable? {
        if (isTileToggleAction) {
            return try {
                context.packageManager.getApplicationIcon(context.packageName)
            } catch (e: Exception) {
                null
            }
        }
        return try {
            val pm = context.packageManager
            pm.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing && lockedPackageName.isNotBlank()) {
            LockSessionManager.clearLaunchPending(lockedPackageName)
            LockSessionManager.currentlyDisplayingLockFor = lockedPackageName
        }
    }

    override fun onStop() {
        invalidateRequests()
        super.onStop()
        if (LockSessionManager.currentlyDisplayingLockFor == lockedPackageName) {
            LockSessionManager.currentlyDisplayingLockFor = null
        }
    }

    override fun onDestroy() {
        invalidateRequests()
        super.onDestroy()
        if (LockSessionManager.currentlyDisplayingLockFor == lockedPackageName) {
            LockSessionManager.currentlyDisplayingLockFor = null
        }
        LockSessionManager.clearLaunchPending(lockedPackageName)
    }
}
