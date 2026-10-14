package com.example.util

import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class LockSessionManagerTest {
    @After fun clear() = LockSessionManager.clearSessions()

    @Test fun thirdPartyExamplePackagesAreNotExempt() {
        assertTrue(LockSessionManager.isAppLocked("com.example.protected", 0))
        assertFalse(LockSessionManager.isAppLocked(com.example.BuildConfig.APPLICATION_ID, 0))
    }

    @Test fun screenOffClearsUnlocksAndPendingLaunches() {
        LockSessionManager.setAppUnlocked("protected")
        LockSessionManager.setLaunchPending("pending")
        assertFalse(LockSessionManager.isAppLocked("protected", 0))
        LockSessionManager.clearSessions()
        assertTrue(LockSessionManager.isAppLocked("protected", 0))
        assertFalse(LockSessionManager.isLaunchPendingFor("pending"))
    }
    @Test fun manualRelockPreservesSavedLockCache() {
        LockSessionManager.updateLockedPackagesCache(setOf("protected"))
        LockSessionManager.setAppUnlocked("protected")
        LockSessionManager.clearSessions()
        assertTrue(LockSessionManager.isAppLocked("protected", 0))
        assertTrue(LockSessionManager.isPackageInLockedCache("protected"))
        LockSessionManager.updateLockedPackagesCache(emptySet())
    }
}
