package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.data.model.LockMethod
import java.security.SecureRandom

class EncryptedPrefs(private val context: Context) {

    private val masterKeyAlias by lazy {
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    }

    private var isFallback = false

    private val prefs: SharedPreferences by lazy {
        try {
            isFallback = false
            EncryptedSharedPreferences.create(
                "applock_encrypted_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to private preferences if Keystore on old devices/broken ROMs raises errors
            isFallback = true
            context.getSharedPreferences("applock_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val KEY_DB_PASSPHRASE = "db_passphrase"
        private const val KEY_LOCK_PIN_HASH = "lock_pin_hash"
        private const val KEY_LOCK_PIN_LENGTH = "lock_pin_length"
        private const val KEY_LOCK_PASSWORD_HASH = "lock_password_hash"
        private const val KEY_LOCK_PATTERN_HASH = "lock_pattern_hash"
        private const val KEY_LOCK_METHOD = "lock_method"
        private const val KEY_TIMEOUT = "lock_timeout"
        private const val KEY_VIBRATION = "lock_vibration"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_INTRUDER_SELFIE = "intruder_selfie"
        private const val KEY_WRONG_ATTEMPTS = "wrong_attempts"
        private const val KEY_COOLDOWN_END_TIME = "cooldown_end_time"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_APP_LAUNCH_PIN_HASH = "app_launch_pin_hash"
        private const val KEY_APP_LAUNCH_BIOMETRIC_ENABLED = "app_launch_biometric_enabled"
    }

    // Get or Create Database passphrase
    fun getOrCreateDatabasePassphrase(): String {
        var passphrase = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (isFallback && passphrase != null) {
            passphrase = deobfuscate(passphrase)
        }
        if (passphrase == null) {
            passphrase = generateSecurePassphrase()
            val toSave = if (isFallback) obfuscate(passphrase) else passphrase
            prefs.edit().putString(KEY_DB_PASSPHRASE, toSave).apply()
        }
        return passphrase
    }

    private fun obfuscate(value: String): String {
        val key = (android.os.Build.ID + android.os.Build.DEVICE + "SafeLockSalt").hashCode()
        val result = StringBuilder()
        for (i in value.indices) {
            result.append((value[i].code xor (key + i)).toChar())
        }
        return android.util.Base64.encodeToString(result.toString().toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
    }

    private fun deobfuscate(value: String): String {
        return try {
            val decoded = String(android.util.Base64.decode(value, android.util.Base64.NO_WRAP), Charsets.UTF_8)
            val key = (android.os.Build.ID + android.os.Build.DEVICE + "SafeLockSalt").hashCode()
            val result = StringBuilder()
            for (i in decoded.indices) {
                result.append((decoded[i].code xor (key + i)).toChar())
            }
            result.toString()
        } catch (e: Exception) {
            value
        }
    }

    private fun generateSecurePassphrase(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return bytes.joinToString("") { String.format("%02x", it) }
    }

    // PIN hash
    fun getPinHash(): String? = prefs.getString(KEY_LOCK_PIN_HASH, null)
    fun savePinHash(hash: String) = prefs.edit().putString(KEY_LOCK_PIN_HASH, hash).apply()
    fun clearPinHash() = prefs.edit().remove(KEY_LOCK_PIN_HASH).apply()

    fun getPinLength(): Int = prefs.getInt(KEY_LOCK_PIN_LENGTH, 4)
    fun savePinLength(length: Int) = prefs.edit().putInt(KEY_LOCK_PIN_LENGTH, length).apply()
    fun clearPinLength() = prefs.edit().remove(KEY_LOCK_PIN_LENGTH).apply()

    // Password hash
    fun getPasswordHash(): String? = prefs.getString(KEY_LOCK_PASSWORD_HASH, null)
    fun savePasswordHash(hash: String) = prefs.edit().putString(KEY_LOCK_PASSWORD_HASH, hash).apply()
    fun clearPasswordHash() = prefs.edit().remove(KEY_LOCK_PASSWORD_HASH).apply()

    // Pattern hash
    fun getPatternHash(): String? = prefs.getString(KEY_LOCK_PATTERN_HASH, null)
    fun savePatternHash(hash: String) = prefs.edit().putString(KEY_LOCK_PATTERN_HASH, hash).apply()
    fun clearPatternHash() = prefs.edit().remove(KEY_LOCK_PATTERN_HASH).apply()

    // Lock Method
    fun getLockMethod(): LockMethod {
        val name = prefs.getString(KEY_LOCK_METHOD, LockMethod.DEFAULT.name) ?: LockMethod.DEFAULT.name
        return try {
            LockMethod.valueOf(name)
        } catch (e: Exception) {
            LockMethod.DEFAULT
        }
    }
    fun saveLockMethod(method: LockMethod) = prefs.edit().putString(KEY_LOCK_METHOD, method.name).apply()

    // Timeout: immediate = 0, 1 min = 60000, 5 min = 300000
    fun getLockTimeoutMs(): Long = prefs.getLong(KEY_TIMEOUT, 0L)
    fun saveLockTimeoutMs(timeoutMs: Long) = prefs.edit().putLong(KEY_TIMEOUT, timeoutMs).apply()

    // Vibration Toggle
    fun isVibrationEnabled(): Boolean = prefs.getBoolean(KEY_VIBRATION, true)
    fun setVibrationEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()

    // Theme Mode: "SYSTEM", "LIGHT", "DARK"
    fun getThemeMode(): String = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
    fun saveThemeMode(mode: String) = prefs.edit().putString(KEY_THEME_MODE, mode).apply()

    // Intruder Selfie Toggle
    fun isIntruderSelfieEnabled(): Boolean = prefs.getBoolean(KEY_INTRUDER_SELFIE, false)
    fun setIntruderSelfieEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_INTRUDER_SELFIE, enabled).apply()

    // Wrong Attempts Persistent Counter & Cooldown
    fun getWrongAttempts(): Int = prefs.getInt(KEY_WRONG_ATTEMPTS, 0)
    fun setWrongAttempts(count: Int) = prefs.edit().putInt(KEY_WRONG_ATTEMPTS, count).apply()

    fun getCooldownEndTime(): Long = prefs.getLong(KEY_COOLDOWN_END_TIME, 0L)
    fun setCooldownEndTime(timestamp: Long) = prefs.edit().putLong(KEY_COOLDOWN_END_TIME, timestamp).apply()

    // First Launch Wizard State Tracker
    fun isFirstLaunch(): Boolean = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    fun setFirstLaunch(first: Boolean) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, first).apply()

    fun getAppLaunchPinHash(): String? = prefs.getString(KEY_APP_LAUNCH_PIN_HASH, null)
    fun saveAppLaunchPinHash(hash: String) = prefs.edit().putString(KEY_APP_LAUNCH_PIN_HASH, hash).apply()
    fun clearAppLaunchPinHash() = prefs.edit().remove(KEY_APP_LAUNCH_PIN_HASH).apply()

    fun isAppLaunchBiometricEnabled(): Boolean = prefs.getBoolean(KEY_APP_LAUNCH_BIOMETRIC_ENABLED, false)
    fun setAppLaunchBiometricEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_APP_LAUNCH_BIOMETRIC_ENABLED, enabled).apply()

    fun isBiometricAutoPromptEnabled(): Boolean = prefs.getBoolean("biometric_auto_prompt", true)
    fun setBiometricAutoPromptEnabled(enabled: Boolean) = prefs.edit().putBoolean("biometric_auto_prompt", enabled).apply()

    // Multi-lock configuration
    fun isMultiLockEnabled(): Boolean = prefs.getBoolean("is_multi_lock_enabled", false)
    fun setMultiLockEnabled(enabled: Boolean) = prefs.edit().putBoolean("is_multi_lock_enabled", enabled).apply()

    fun getMultiLockSequence(): List<LockMethod> {
        val raw = prefs.getString("multi_lock_sequence", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull {
            try { LockMethod.valueOf(it) } catch (e: Exception) { null }
        }
    }

    fun saveMultiLockSequence(sequence: List<LockMethod>) {
        val raw = sequence.joinToString(",") { it.name }
        prefs.edit().putString("multi_lock_sequence", raw).apply()
    }
}
