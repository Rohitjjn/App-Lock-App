package com.example.data.local

import android.app.Application
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Synthetic preference fixtures only; not a real Keystore or SQLCipher migration test. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class EncryptedPrefsAttemptTest {
    @Test fun legacyCooldownKeysAreRespectedWithoutChangingCredentialsOrDatabaseKey() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = EncryptedPrefs(context)
        prefs.prefs.edit().clear()
            .putInt("wrong_attempts", 3)
            .putLong("cooldown_end_time", System.currentTimeMillis() + 60_000L)
            .putString("lock_pin_hash", "synthetic-hash-sentinel")
            .putString("db_passphrase", "synthetic-key-sentinel")
            .commit()
        assertFalse(prefs.authAttempts.acceptSuccess())
        assertEquals(0, prefs.authAttempts.recordFailure())
        val reopened = EncryptedPrefs(context)
        assertTrue(reopened.authAttempts.remainingSeconds() > 0)
        assertEquals("synthetic-hash-sentinel", reopened.getPinHash())
        assertEquals("synthetic-key-sentinel", reopened.prefs.getString("db_passphrase", null))
    }

    @Test fun failuresPersistAcrossPreferenceWrapperRecreation() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = EncryptedPrefs(context)
        prefs.prefs.edit().clear().commit()
        assertEquals(1, prefs.authAttempts.recordFailure())
        val reopened = EncryptedPrefs(context)
        assertEquals(2, reopened.authAttempts.recordFailure())
        assertEquals(3, reopened.authAttempts.recordFailure())
        assertFalse(EncryptedPrefs(context).authAttempts.acceptSuccess())
    }
}
