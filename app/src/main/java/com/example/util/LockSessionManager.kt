package com.example.util

object LockSessionManager {
    // Maps packageName -> timestamp of when it was successfully unlocked
    private val unlockedPackages = mutableMapOf<String, Long>()

    // The package that was unlocked most recently (for grace period checks)
    var lastUnlockedPackage: String? = null
        private set
    var lastUnlockedTime: Long = 0L
        private set

    // Tracks the package currently being prompt-locked/displayed on the screen to prevent redundant intents
    @Volatile
    var currentlyDisplayingLockFor: String? = null

    // Tracks if the user canceled validation for a package to trigger a safe back intercept
    @Volatile
    var cancelActionPendingFor: String? = null

    // Tracks currently pending lock launches to prevent race conditions during overlaps
    private val pendingLaunches = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    // Thread-safe memory cache of user's locked package names for instant main thread lookup
    private val lockedPackagesCache = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /**
     * Updates the local memory cache of user-secured package names.
     */
    fun updateLockedPackagesCache(packageNames: Set<String>) {
        lockedPackagesCache.clear()
        lockedPackagesCache.addAll(packageNames)
    }

    /**
     * Retrieves the current internal cache of locked package names
     */
    fun getLockedPackageNamesFromCache(context: android.content.Context): Set<String> {
        return lockedPackagesCache
    }

    /**
     * Checks if a package name is in the user's list of locked apps cached in memory.
     */
    fun isPackageInLockedCache(packageName: String): Boolean {
        return lockedPackagesCache.contains(packageName)
    }

    /**
     * Checks if a lock screen launch is currently pending for a package to throttle duplicates.
     */
    fun isLaunchPendingFor(packageName: String): Boolean {
        return pendingLaunches.contains(packageName)
    }

    fun setLaunchPending(packageName: String) {
        pendingLaunches.add(packageName)
    }

    fun clearLaunchPending(packageName: String) {
        pendingLaunches.remove(packageName)
    }

    /**
     * Empty placeholder to satisfy the legacy pipeline.
     */
    fun cleanDeadSessions(context: android.content.Context) {
        // Disabled to prevent aggressive, premature re-logging caused by OS restrictions on process queries.
    }

    /**
     * Track active foreground package changes.
     * When the user transitions AWAY from a locked package, we re-lock it immediately (remove from unlockedPackages)
     * if the user has configured Immediate locking or if they exited the application.
     */
    fun updateForegroundPackage(packageName: String, context: android.content.Context) {
        if (packageName.isBlank()) return

        val app = context.applicationContext as? com.example.AppLockApplication
        val behavior = app?.encryptedPrefs?.getLockReLockBehavior() ?: "Until Screen Off"
        val timeoutMs = app?.encryptedPrefs?.getLockTimeoutMs() ?: 0L
        
        if (behavior == "Until Screen Off" || timeoutMs > 0L) {
            // If "Until Screen Off" is selected, or if a grace period timeout is active,
            // we do NOT aggressively clear the unlocked packages here.
            // We let `isAppLocked` handle the timeout expiration, or let `clearSessions` handle screen off.
            return
        }

        // Only for "Lock When Exit" WITH "Immediate Lockout (0L)", we clear aggressively!
        if (packageName == "com.aistudio.applock.vqpstz" || packageName.startsWith("com.example.") || isLauncherPackage(packageName, context)) {
            synchronized(unlockedPackages) {
                unlockedPackages.clear()
            }
            return
        }

        // If another locked app or background app becomes active, lock other apps that were unlocked
        synchronized(unlockedPackages) {
            val iterator = unlockedPackages.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.key != packageName) {
                    iterator.remove()
                }
            }
        }
    }

    private fun isLauncherPackage(packageName: String, context: android.content.Context): Boolean {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            intent.addCategory(android.content.Intent.CATEGORY_HOME)
            val resolveInfo = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo?.activityInfo?.packageName == packageName) {
                return true
            }
        } catch (e: Exception) {
            // Fallback
        }
        val lower = packageName.lowercase()
        return lower.contains("launcher") || lower.contains("home") || lower.contains("pixel") || lower.contains("trebuchet") || lower.contains("carlauncher")
    }

    /**
     * Mark an app as temporary unlocked.
     */
    fun setAppUnlocked(packageName: String) {
        val now = System.currentTimeMillis()
        synchronized(unlockedPackages) {
            unlockedPackages[packageName] = now
        }
        lastUnlockedPackage = packageName
        lastUnlockedTime = now
        clearLaunchPending(packageName)
    }

    /**
     * Check if a package is locked.
     * Requirement: Once unlocked, it remains unlocked until screen goes off.
     */
    fun isAppLocked(packageName: String, timeoutMs: Long): Boolean {
        // 1. Do not lock the App Lock application itself
        if (packageName == "com.aistudio.applock.vqpstz" || packageName.startsWith("com.example.")) {
            return false
        }

        // 2. Identify if there is a recorded unlock session
        val unlockTime = synchronized(unlockedPackages) {
            unlockedPackages[packageName]
        }

        if (unlockTime != null) {
            // Check if timeout has expired (only if timeoutMs > 0)
            if (timeoutMs > 0L && (System.currentTimeMillis() - unlockTime > timeoutMs)) {
                synchronized(unlockedPackages) {
                    unlockedPackages.remove(packageName)
                }
                return true
            }
            // Unlocked in the current session and within timeout!
            return false
        }

        return true
    }

    /**
     * Clears all temporary unlocked packages when the screen goes off.
     */
    fun clearSessions() {
        synchronized(unlockedPackages) {
            unlockedPackages.clear()
        }
        pendingLaunches.clear()
        lastUnlockedPackage = null
        lastUnlockedTime = 0L
    }
}
