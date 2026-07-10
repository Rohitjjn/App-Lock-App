package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
import com.example.AppLockApplication
import com.example.data.repository.AppLockRepository
import com.example.ui.LockActivity
import com.example.util.LockSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppLockAccessibilityService : AccessibilityService() {

    lateinit var repository: AppLockRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val screenOffReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                Log.d("AppLockService", "Screen off event detected. Locking all sessions.")
                LockSessionManager.clearSessions()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = (application as AppLockApplication).repository
        try {
            val filter = android.content.IntentFilter(Intent.ACTION_SCREEN_OFF)
            ContextCompat.registerReceiver(
                this,
                screenOffReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            Log.e("AppLockService", "Failed to register screen off receiver", e)
        }

        // Keep local memory cache of user-locked packages in sync automatically
        serviceScope.launch {
            repository.getLockedAppsOnly().collect { appsList ->
                val securedNames = appsList.map { it.packageName }.toSet()
                LockSessionManager.updateLockedPackagesCache(securedNames)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("AppLockService", "Accessibility service connected")
        
        // Ensure standard accessibility configurations are set dynamically
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = 0 // Removed FLAG_INCLUDE_NOT_IMPORTANT_VIEWS and FLAG_REPORT_VIEW_IDS to bypass banking app restrictions
            notificationTimeout = 80
        }
        this.serviceInfo = info
    }

    private val bankingApps = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "phonepe.app", // PhonePe
        "net.one97.paytm", // Paytm
        "com.sbi.SBIFreedomPlus", // YONO SBI
        "com.sbi.YONOSBI",
        "com.hdfcbank.smarthub",
        "com.icicibank.pockets",
        "com.csam.icici.bank.imobile"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            if (bankingApps.contains(packageName)) {
                return
            }

            // Feed package name to the session manager to track foreground transitions and auto-lock on minimize/exit
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                LockSessionManager.updateForegroundPackage(packageName, applicationContext)
            }

            // Intercept if the user cancelled the validation of current app, so we force-back out of it
            val cancelPendingPkg = LockSessionManager.cancelActionPendingFor
            if (cancelPendingPkg != null) {
                if (packageName == cancelPendingPkg) {
                    LockSessionManager.cancelActionPendingFor = null
                    Log.d("AppLockService", "Performing auto-back on unauthorized app: $packageName")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    return
                } else if (packageName != this.packageName && !packageName.startsWith(this.packageName)) {
                    // Stale cancel pending, clear it
                    LockSessionManager.cancelActionPendingFor = null
                }
            }

            // Never lock own app or home launcher
            if (packageName == this.packageName || packageName.startsWith(this.packageName) ||
                packageName == "com.example" || packageName.startsWith("com.example.") ||
                isLauncherApp(packageName)) {
                return
            }
            
            // Instantly check locked status synchronously on the Main Thread to avoid async locks buildup
            val encryptedPrefs = (application as AppLockApplication).encryptedPrefs

            if (encryptedPrefs.isShieldEnabled() && LockSessionManager.isPackageInLockedCache(packageName)) {
                val timeoutMs = encryptedPrefs.getLockTimeoutMs()

                if (LockSessionManager.isAppLocked(packageName, timeoutMs)) {
                    synchronized(this) {
                        if (LockSessionManager.currentlyDisplayingLockFor == packageName || 
                            LockSessionManager.isLaunchPendingFor(packageName)) {
                            Log.d("AppLockService", "Skipping duplicate lock request for $packageName (active/pending).")
                            return
                        }
                        LockSessionManager.setLaunchPending(packageName)
                    }
                    Log.d("AppLockService", "App launch detected: $packageName. Requesting secure lock.")
                    launchLockScreen(packageName)
                }
            }
        }
    }

    private fun isLauncherApp(packageName: String): Boolean {
        if (packageName.contains("settings", ignoreCase = true)) {
            return false
        }
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfoList = packageManager.queryIntentActivities(
                intent, 
                PackageManager.MATCH_DEFAULT_ONLY
            )
            for (info in resolveInfoList) {
                if (info.activityInfo.packageName == packageName) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private var lastLaunchTime = 0L

    private fun launchLockScreen(packageName: String) {
        val now = System.currentTimeMillis()
        if (now - lastLaunchTime < 300) {
            Log.d("AppLockService", "Launch throttled for $packageName")
            return
        }
        lastLaunchTime = now
        LockSessionManager.currentlyDisplayingLockFor = packageName
        val intent = Intent(this, LockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            putExtra(LockActivity.EXTRA_PACKAGE_NAME, packageName)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        Log.d("AppLockService", "Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: Exception) {
            // ignore
        }
        serviceJob.cancel()
    }
}
