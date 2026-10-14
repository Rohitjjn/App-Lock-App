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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import com.example.util.ForegroundTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var usageStatsManager: UsageStatsManager
    @Volatile private var isScreenOn = true
    private val cacheReady = CompletableDeferred<Unit>()

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
        isScreenOn = (getSystemService(Context.POWER_SERVICE) as android.os.PowerManager).isInteractive
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
                cacheReady.complete(Unit)
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
            // Never consume the initial resume event against an unhydrated lock cache.
            cacheReady.await()
            val target = ForegroundTarget()
            var lastTrackedApp: String? = null
            val homePackage = resolveHomePackage()
            var lastEventTime = System.currentTimeMillis() - 1000L * 60 * 60 // Start 1 hour back for first initialization
            
            while (isActive) {
                try {
                    val queryEndTime = System.currentTimeMillis()
                    val events = usageStatsManager.queryEvents(lastEventTime, queryEndTime)
                    val event = UsageEvents.Event()
                    var currentApp: String? = null
                    
                    while (events.hasNextEvent()) {
                        events.getNextEvent(event)
                        if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                            currentApp = event.packageName
                        }
                    }
                    
                    // A stable foreground app still needs checks after cache/settings changes,
                    // timeout expiry, or an OS-suppressed launch. Pending/display guards dedupe.
                    target.update(currentApp)?.let { packageName ->
                        if (packageName != lastTrackedApp) {
                            LockSessionManager.updateForegroundPackage(packageName, applicationContext)
                            lastTrackedApp = packageName
                        }
                        if (isScreenOn) checkAppAndLock(packageName, homePackage)
                    }
                    
                    lastEventTime = queryEndTime // Shift sliding window strictly forward
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    Log.e(TAG, "Polling error", e)
                }
                delay(100) // 100ms polling interval
            }
        }
    }
    
    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun checkAppAndLock(packageName: String, homePackage: String?) {
        if (packageName == applicationContext.packageName || packageName == homePackage) return
        val prefs = (applicationContext as AppLockApplication).encryptedPrefs
        if (prefs.isShieldEnabled() && LockSessionManager.isPackageInLockedCache(packageName) &&
            LockSessionManager.isAppLocked(packageName, prefs.getLockTimeoutMs())) {
            launchLockScreen(packageName)
        }
    }

    // Resolve once per screen-on polling session, not on every 100ms check.
    private fun resolveHomePackage(): String? = try {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
    } catch (error: Exception) {
        null
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
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        try {
            startActivity(intent)
        } catch (error: Exception) {
            LockSessionManager.clearLaunchPending(lockedPackageName)
            throw error // The polling loop logs and retries; do not record a successful transition.
        }

        // Safe fallback: Clear pending flag after 1 second in case startActivity fails or LockActivity is killed early
        scope.launch {
            delay(1000)
            LockSessionManager.clearLaunchPending(lockedPackageName)
        }
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
        scope.cancel() // Also cancel the Room observer and delayed launch cleanup jobs.
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
