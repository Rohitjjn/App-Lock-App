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
import android.util.Log
import android.widget.Toast
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
import java.util.concurrent.Executors

class LockActivity : FragmentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        private const val TAG = "LockActivity"
    }

    private var lockedPackageName: String = ""
    private lateinit var encryptedPrefs: EncryptedPrefs
    private val biometricHelper by lazy { BiometricHelper() }
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // FLAG_SECURE to prevent screenshots and screen leaks in recent apps previews
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )

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

        lockedPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        if (lockedPackageName.isEmpty()) {
            finish()
            return
        }

        LockSessionManager.currentlyDisplayingLockFor = lockedPackageName

        val app = application as AppLockApplication
        encryptedPrefs = app.encryptedPrefs

        // 3. Jetpack Compose error layout safe fallback rendering catch blocks
        try {
            setContent {
                val themeMode = remember { encryptedPrefs.getThemeMode() }
                val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
                val useDarkTheme = remember(themeMode, isSystemDark) {
                    when (themeMode) {
                        "LIGHT" -> false
                        "DARK" -> true
                        else -> isSystemDark
                    }
                }
                MyApplicationTheme(darkTheme = useDarkTheme) {
                    LockScreenContent()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Security composing validation loop crash fallback triggers active", e)
            Toast.makeText(this, "Security validation services failure, resetting display.", Toast.LENGTH_LONG).show()
            handleUnlockCancel()
            return
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // If the user navigates away (e.g. clicks Home button), handle this as a secure cancel
        handleUnlockCancel()
    }

    private fun triggerBiometricAuth(onSuccess: () -> Unit) {
        biometricHelper.authenticate(
            activity = this,
            executor = executor,
            title = "App Lock Biometrics",
            subtitle = "Verify to unlock application",
            onSuccess = {
                runOnUiThread {
                    onSuccess()
                }
            },
            onError = { errorCode, errString ->
                runOnUiThread {
                    // Ignore user canceled, canceled by system/back, and negative (cancel) button click codes
                    if (errorCode != 5 && errorCode != 10 && errorCode != 13) {
                        Toast.makeText(this, "Biometrics: $errString", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onFailed = {
                runOnUiThread {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun handleUnlockSuccess() {
        LockSessionManager.setAppUnlocked(lockedPackageName)
        finish()
        overridePendingTransition(0, 0)
    }

    private fun handleUnlockCancel() {
        // Safe redirect to main home screen to prevent unauthorized access
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        
        // Kill background process of target locked app because they did not unlock it!
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.killBackgroundProcesses(lockedPackageName)
            Log.d(TAG, "Successfully killed background processes of unauthorized app: $lockedPackageName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to kill background process for $lockedPackageName", e)
        }
        
        finish()
    }

    override fun onBackPressed() {
        // Instead of letting them back into the app, redirect to home
        handleUnlockCancel()
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
            onUnlockSuccess = { handleUnlockSuccess() },
            onIncorrectAttempt = { triggerVibration() },
            onTriggerBiometric = { onSuccessCallback -> triggerBiometricAuth(onSuccessCallback) },
            onCancel = { handleUnlockCancel() }
        )
    }

    private fun getAppName(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            "External App"
        }
    }

    private fun getAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            val pm = context.packageManager
            pm.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (LockSessionManager.currentlyDisplayingLockFor == lockedPackageName) {
            LockSessionManager.currentlyDisplayingLockFor = null
        }
        LockSessionManager.clearLaunchPending(lockedPackageName)
        try {
            executor.shutdown()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
