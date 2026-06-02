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
     * Empty/simple updates for active packagename tracking.
     */
    fun updateForegroundPackage(packageName: String, context: android.content.Context) {
        // Sessions are preserved reliably until the physical device screen is locked.
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
        if (packageName == "com.example" || packageName.startsWith("com.example.")) {
            return false
        }

        // 2. Identify if there is a recorded unlock session
        val isUnlocked = synchronized(unlockedPackages) {
            unlockedPackages.containsKey(packageName)
        }

        if (isUnlocked) {
            // Unlocked in the current session! Keep unlocked until screen lock/turn off occurs.
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
