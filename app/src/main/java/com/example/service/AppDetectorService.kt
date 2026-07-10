package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.AppLockApplication
import com.example.ui.LockActivity
import com.example.util.LockSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppDetectorService : Service() {

    companion object {
        private const val CHANNEL_ID = "applock_detector_channel"
        private const val NOTIFICATION_ID = 8815
        private const val TAG = "AppDetectorService"
        
        fun start(context: Context) {
            try {
                val intent = Intent(context, AppDetectorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AppDetectorService", e)
            }
        }
    }

    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var usageStatsManager: UsageStatsManager
    private var isScreenOn = true

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen off. Pausing detector.")
                    isScreenOn = false
                    stopPolling()
                    LockSessionManager.clearSessions() // Lock all sessions when screen turns off
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    Log.d(TAG, "Screen on/user present. Resuming detector.")
                    isScreenOn = true
                    startPolling()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        createNotificationChannel()
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenStateReceiver, filter)

        // Observe Room DB for locked apps and update the in-memory cache
        val repository = (applicationContext as AppLockApplication).repository
        scope.launch {
            repository.getLockedAppsOnly().collect { apps ->
                val packageNames = apps.map { it.packageName }.toSet()
                LockSessionManager.updateLockedPackagesCache(packageNames)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (isScreenOn) {
            startPolling()
        }
        
        return START_STICKY
    }
    
    private fun startPolling() {
        if (pollingJob?.isActive == true) return
        
        pollingJob = scope.launch {
            var lastCheckedApp = ""
            var isFirstRun = true
            while (isActive) {
                try {
                    var currentApp: String? = null
                    if (isFirstRun) {
                        val endTime = System.currentTimeMillis()
                        val beginTime = endTime - 1000L * 60 * 60 * 24 // 24 hours back
                        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
                        var recentStats: android.app.usage.UsageStats? = null
                        if (stats != null) {
                            for (stat in stats) {
                                if (recentStats == null || stat.lastTimeUsed > recentStats.lastTimeUsed) {
                                    recentStats = stat
                                }
                            }
                            currentApp = recentStats?.packageName
                        }
                    } else {
                        currentApp = getForegroundApp()
                    }

                    if (currentApp != null && currentApp != lastCheckedApp) {
                        checkAppAndLock(currentApp)
                        lastCheckedApp = currentApp
                    }
                    isFirstRun = false
                } catch (e: Exception) {
                    Log.e(TAG, "Polling error", e)
                }
                delay(200) // 200ms polling interval
            }
        }
    }
    
    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun getForegroundApp(): String? {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 2000 // Look back 2 seconds
        
        val events = usageStatsManager.queryEvents(beginTime, endTime)
        var currentApp: String? = null
        val event = UsageEvents.Event()
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // Event type 1 is ACTIVITY_RESUMED
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentApp = event.packageName
            }
        }
        return currentApp
    }
    
    private fun checkAppAndLock(packageName: String) {
        // Exclude own app package to avoid infinite loops, and usually home launchers
        val myPackage = applicationContext.packageName
        if (packageName == myPackage || isHomeLauncher(packageName)) {
            LockSessionManager.updateForegroundPackage(packageName, applicationContext)
            return
        }

        val encryptedPrefs = (applicationContext as AppLockApplication).encryptedPrefs
        if (!encryptedPrefs.isShieldEnabled()) {
            LockSessionManager.updateForegroundPackage(packageName, applicationContext)
            return
        }
        
        val lockedPackages = LockSessionManager.getLockedPackageNamesFromCache(applicationContext)
        if (lockedPackages.contains(packageName)) {
            val isAppLocked = LockSessionManager.isAppLocked(packageName, encryptedPrefs.getLockTimeoutMs())
            if (!isAppLocked) {
                // If it's already in unlocked session, let it be. But we can update foreground tracker
                LockSessionManager.updateForegroundPackage(packageName, applicationContext)
            } else {
                launchLockScreen(packageName)
            }
        } else {
            // App is not locked, just track it as the foreground app
            LockSessionManager.updateForegroundPackage(packageName, applicationContext)
        }
    }

    private fun isHomeLauncher(packageName: String): Boolean {
        // Can be cached or just standard names
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    private fun launchLockScreen(lockedPackageName: String) {
        synchronized(this) {
            if (LockSessionManager.currentlyDisplayingLockFor == lockedPackageName || 
                LockSessionManager.isLaunchPendingFor(lockedPackageName)) {
                return
            }
            LockSessionManager.setLaunchPending(lockedPackageName)
        }
        
        val intent = Intent(this, LockActivity::class.java).apply {
            putExtra(LockActivity.EXTRA_PACKAGE_NAME, lockedPackageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        startActivity(intent)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            1285,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafeLock is Active")
            .setContentText("App Shield is monitoring locked apps.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Lock Detector",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps the app detector running in the background."
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
