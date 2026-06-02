package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
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
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 80
        }
        this.serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Feed package name to the session manager to track foreground transitions and auto-lock on minimize/exit
            LockSessionManager.updateForegroundPackage(packageName, applicationContext)

            // Never lock own app or home launcher
            if (packageName == this.packageName || packageName.startsWith(this.packageName) ||
                packageName == "com.example" || packageName.startsWith("com.example.") ||
                isLauncherApp(packageName)) {
                return
            }
            
            // Instantly check locked status synchronously on the Main Thread to avoid async locks buildup
            if (LockSessionManager.isPackageInLockedCache(packageName)) {
                val encryptedPrefs = (application as AppLockApplication).encryptedPrefs
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
