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

    val prefs: SharedPreferences by lazy {
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

    // Re-lock behavior: "Until Screen Off" (Once unlocked, stays unlocked until device is locked/screen off) VS "Immediate"
    fun getLockReLockBehavior(): String = prefs.getString("lock_re_lock_behavior", "Until Screen Off") ?: "Until Screen Off"
    fun saveLockReLockBehavior(behavior: String) = prefs.edit().putString("lock_re_lock_behavior", behavior).apply()

    // Vibration Toggle
    fun isVibrationEnabled(): Boolean = prefs.getBoolean(KEY_VIBRATION, true)
    fun setVibrationEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()

    // Theme Mode: "SYSTEM", "LIGHT", "DARK"
    fun getThemeMode(): String = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
    fun saveThemeMode(mode: String) = prefs.edit().putString(KEY_THEME_MODE, mode).apply()

    // Dynamic Accent Color selection (Default)
    fun getAccentColorHex(): String = prefs.getString("accent_color_hex", "Default") ?: "Default"
    fun saveAccentColorHex(hex: String) = prefs.edit().putString("accent_color_hex", hex).apply()

    // Lock Screen Customisation Options
    fun getLockClockStyle(): String = prefs.getString("lock_clock_style", "Default") ?: "Default"
    fun saveLockClockStyle(style: String) = prefs.edit().putString("lock_clock_style", style).apply()

    fun getLockClockFont(): String = prefs.getString("lock_clock_font", "System") ?: "System"
    fun saveLockClockFont(font: String) = prefs.edit().putString("lock_clock_font", font).apply()

    fun getLockClockSize(): String = prefs.getString("lock_clock_size", "Standard") ?: "Standard"
    fun saveLockClockSize(size: String) = prefs.edit().putString("lock_clock_size", size).apply()

    fun getLockClockWeight(): String = prefs.getString("lock_clock_weight", "Bold") ?: "Bold"
    fun saveLockClockWeight(weight: String) = prefs.edit().putString("lock_clock_weight", weight).apply()

    fun getLockClockPosition(): String = prefs.getString("lock_clock_position", "Top") ?: "Top"
    fun saveLockClockPosition(position: String) = prefs.edit().putString("lock_clock_position", position).apply()

    fun getLockBackgroundTheme(): String = prefs.getString("lock_bg_theme", "Default Accent Theme") ?: "Default Accent Theme"
    fun saveLockBackgroundTheme(theme: String) = prefs.edit().putString("lock_bg_theme", theme).apply()

    fun getLockLiquidGlassLook(): String = prefs.getString("lock_liquid_glass", "None") ?: "None"
    fun saveLockLiquidGlassLook(look: String) = prefs.edit().putString("lock_liquid_glass", look).apply()

    fun getLockButtonShape(): String = prefs.getString("lock_btn_shape", "Circle") ?: "Circle"
    fun saveLockButtonShape(shape: String) = prefs.edit().putString("lock_btn_shape", shape).apply()

    fun getLockClockFontSize(): Float = prefs.getFloat("lock_clock_font_size", 44f)
    fun saveLockClockFontSize(size: Float) = prefs.edit().putFloat("lock_clock_font_size", size).apply()

    fun getLockClockColor(): String = prefs.getString("lock_clock_color", "Default") ?: "Default"
    fun saveLockClockColor(color: String) = prefs.edit().putString("lock_clock_color", color).apply()

    fun getLockAppIconShape(): String = prefs.getString("lock_app_icon_shape", "Rounded Square") ?: "Rounded Square"
    fun saveLockAppIconShape(shape: String) = prefs.edit().putString("lock_app_icon_shape", shape).apply()

    fun getLockAllLocksShape(): String = prefs.getString("lock_all_locks_shape", "Circle") ?: "Circle"
    fun saveLockAllLocksShape(shape: String) = prefs.edit().putString("lock_all_locks_shape", shape).apply()

    fun getLockAllLocksColor(): String = prefs.getString("lock_all_locks_color", "Default") ?: "Default"
    fun saveLockAllLocksColor(color: String) = prefs.edit().putString("lock_all_locks_color", color).apply()

    fun getLockAllLocksSize(): Float = prefs.getFloat("lock_all_locks_size", 24f)
    fun saveLockAllLocksSize(size: Float) = prefs.edit().putFloat("lock_all_locks_size", size).apply()

    fun getLockPinFont(): String = prefs.getString("lock_pin_font", "System") ?: "System"
    fun saveLockPinFont(font: String) = prefs.edit().putString("lock_pin_font", font).apply()

    fun getLockPinFontColor(): String = prefs.getString("lock_pin_color", "Default") ?: "Default"
    fun saveLockPinFontColor(color: String) = prefs.edit().putString("lock_pin_color", color).apply()

    fun getLockPinFontSize(): Float = prefs.getFloat("lock_pin_size", 24f)
    fun saveLockPinFontSize(size: Float) = prefs.edit().putFloat("lock_pin_size", size).apply()

    fun getLockPinBtnShape(): String = prefs.getString("lock_pin_btn_shape", "Circle") ?: "Circle"
    fun saveLockPinBtnShape(shape: String) = prefs.edit().putString("lock_pin_btn_shape", shape).apply()

    fun getLockPinDotShape(): String = prefs.getString("lock_pin_dot_shape", "Circle") ?: "Circle"
    fun saveLockPinDotShape(shape: String) = prefs.edit().putString("lock_pin_dot_shape", shape).apply()

    fun getLockPinDotOutlineColor(): String = prefs.getString("lock_pin_dot_outline_color", "Default") ?: "Default"
    fun saveLockPinDotOutlineColor(color: String) = prefs.edit().putString("lock_pin_dot_outline_color", color).apply()

    fun getLockPinDotFilledColor(): String = prefs.getString("lock_pin_dot_filled_color", "Default") ?: "Default"
    fun saveLockPinDotFilledColor(color: String) = prefs.edit().putString("lock_pin_dot_filled_color", color).apply()

    fun getLockPasswordIconColor(): String = prefs.getString("lock_password_icon_color", "Default") ?: "Default"
    fun saveLockPasswordIconColor(color: String) = prefs.edit().putString("lock_password_icon_color", color).apply()

    fun getLockPasswordBorderColor(): String = prefs.getString("lock_password_border_color", "Default") ?: "Default"
    fun saveLockPasswordBorderColor(color: String) = prefs.edit().putString("lock_password_border_color", color).apply()

    fun getLockPasswordEyeColor(): String = prefs.getString("lock_password_eye_color", "Default") ?: "Default"
    fun saveLockPasswordEyeColor(color: String) = prefs.edit().putString("lock_password_eye_color", color).apply()

    fun getLockPasswordUnlockButtonColor(): String = prefs.getString("lock_password_unlock_btn_color", "Default") ?: "Default"
    fun saveLockPasswordUnlockButtonColor(color: String) = prefs.edit().putString("lock_password_unlock_btn_color", color).apply()

    fun getLockPatternDotsColor(): String = prefs.getString("lock_pattern_dots_color", "Default") ?: "Default"
    fun saveLockPatternDotsColor(color: String) = prefs.edit().putString("lock_pattern_dots_color", color).apply()

    fun getLockPatternDotsSize(): Float = prefs.getFloat("lock_pattern_dots_size", 8f)
    fun saveLockPatternDotsSize(size: Float) = prefs.edit().putFloat("lock_pattern_dots_size", size).apply()

    fun getLockPatternDotsShape(): String = prefs.getString("lock_pattern_dots_shape", "Circle") ?: "Circle"
    fun saveLockPatternDotsShape(shape: String) = prefs.edit().putString("lock_pattern_dots_shape", shape).apply()

    fun getLockPatternLineColor(): String = prefs.getString("lock_pattern_line_color", "Default") ?: "Default"
    fun saveLockPatternLineColor(color: String) = prefs.edit().putString("lock_pattern_line_color", color).apply()

    fun getLockPatternAnimation(): String = prefs.getString("lock_pattern_animation", "Pulse") ?: "Pulse"
    fun saveLockPatternAnimation(anim: String) = prefs.edit().putString("lock_pattern_animation", anim).apply()

    fun getLockBiometricAnimation(): String = prefs.getString("lock_biometric_animation", "Pulse") ?: "Pulse"
    fun saveLockBiometricAnimation(anim: String) = prefs.edit().putString("lock_biometric_animation", anim).apply()

    fun getLockBiometricSize(): Float = prefs.getFloat("lock_biometric_size", 64f)
    fun saveLockBiometricSize(size: Float) = prefs.edit().putFloat("lock_biometric_size", size).apply()

    fun getLockBiometricColor(): String = prefs.getString("lock_biometric_color", "Default") ?: "Default"
    fun saveLockBiometricColor(color: String) = prefs.edit().putString("lock_biometric_color", color).apply()

    fun getLockSecuredTextColor(): String = prefs.getString("lock_secured_text_color", "Default") ?: "Default"
    fun saveLockSecuredTextColor(color: String) = prefs.edit().putString("lock_secured_text_color", color).apply()

    fun getLockBottomIconColor(): String = prefs.getString("lock_bottom_icon_color", "Default") ?: "Default"
    fun saveLockBottomIconColor(color: String) = prefs.edit().putString("lock_bottom_icon_color", color).apply()

    fun getSettingsLockTypeColor(): String = prefs.getString("settings_lock_type_color", "Default") ?: "Default"
    fun saveSettingsLockTypeColor(color: String) = prefs.edit().putString("settings_lock_type_color", color).apply()

    fun getLockBiometricIconColor(): String = prefs.getString("lock_biometric_icon_color", "Default") ?: "Default"
    fun saveLockBiometricIconColor(color: String) = prefs.edit().putString("lock_biometric_icon_color", color).apply()

    fun getLockTypeColorBgPin(): String = prefs.getString("lock_type_color_bg_pin", "Default") ?: "Default"
    fun saveLockTypeColorBgPin(color: String) = prefs.edit().putString("lock_type_color_bg_pin", color).apply()
    fun getLockTypeColorIcPin(): String = prefs.getString("lock_type_color_ic_pin", "Default") ?: "Default"
    fun saveLockTypeColorIcPin(color: String) = prefs.edit().putString("lock_type_color_ic_pin", color).apply()

    fun getLockTypeColorBgPassword(): String = prefs.getString("lock_type_color_bg_password", "Default") ?: "Default"
    fun saveLockTypeColorBgPassword(color: String) = prefs.edit().putString("lock_type_color_bg_password", color).apply()
    fun getLockTypeColorIcPassword(): String = prefs.getString("lock_type_color_ic_password", "Default") ?: "Default"
    fun saveLockTypeColorIcPassword(color: String) = prefs.edit().putString("lock_type_color_ic_password", color).apply()

    fun getLockTypeColorBgPattern(): String = prefs.getString("lock_type_color_bg_pattern", "Default") ?: "Default"
    fun saveLockTypeColorBgPattern(color: String) = prefs.edit().putString("lock_type_color_bg_pattern", color).apply()
    fun getLockTypeColorIcPattern(): String = prefs.getString("lock_type_color_ic_pattern", "Default") ?: "Default"
    fun saveLockTypeColorIcPattern(color: String) = prefs.edit().putString("lock_type_color_ic_pattern", color).apply()

    fun getLockTypeColorBgBiometric(): String = prefs.getString("lock_type_color_bg_biometric", "Default") ?: "Default"
    fun saveLockTypeColorBgBiometric(color: String) = prefs.edit().putString("lock_type_color_bg_biometric", color).apply()
    fun getLockTypeColorIcBiometric(): String = prefs.getString("lock_type_color_ic_biometric", "Default") ?: "Default"
    fun saveLockTypeColorIcBiometric(color: String) = prefs.edit().putString("lock_type_color_ic_biometric", color).apply()

    fun getLockTextFontSecured(): String = prefs.getString("lock_text_font_secured", "System") ?: "System"
    fun saveLockTextFontSecured(font: String) = prefs.edit().putString("lock_text_font_secured", font).apply()
    fun getLockTextColorSecured(): String = prefs.getString("lock_text_color_secured", "Default") ?: "Default"
    fun saveLockTextColorSecured(color: String) = prefs.edit().putString("lock_text_color_secured", color).apply()

    fun getLockTextFontDraw(): String = prefs.getString("lock_text_font_draw", "System") ?: "System"
    fun saveLockTextFontDraw(font: String) = prefs.edit().putString("lock_text_font_draw", font).apply()
    fun getLockTextColorDraw(): String = prefs.getString("lock_text_color_draw", "Default") ?: "Default"
    fun saveLockTextColorDraw(color: String) = prefs.edit().putString("lock_text_color_draw", color).apply()

    fun getLockTextFontForgot(): String = prefs.getString("lock_text_font_forgot", "System") ?: "System"
    fun saveLockTextFontForgot(font: String) = prefs.edit().putString("lock_text_font_forgot", font).apply()
    fun getLockTextColorForgot(): String = prefs.getString("lock_text_color_forgot", "Default") ?: "Default"
    fun saveLockTextColorForgot(color: String) = prefs.edit().putString("lock_text_color_forgot", color).apply()

    fun getLockTextFontTapScan(): String = prefs.getString("lock_text_font_tap_scan", "System") ?: "System"
    fun saveLockTextFontTapScan(font: String) = prefs.edit().putString("lock_text_font_tap_scan", font).apply()
    fun getLockTextColorTapScan(): String = prefs.getString("lock_text_color_tap_scan", "Default") ?: "Default"
    fun saveLockTextColorTapScan(color: String) = prefs.edit().putString("lock_text_color_tap_scan", color).apply()

    fun getLockTextFontTouchSensor(): String = prefs.getString("lock_text_font_touch_sensor", "System") ?: "System"
    fun saveLockTextFontTouchSensor(font: String) = prefs.edit().putString("lock_text_font_touch_sensor", font).apply()
    fun getLockTextColorTouchSensor(): String = prefs.getString("lock_text_color_touch_sensor", "Default") ?: "Default"
    fun saveLockTextColorTouchSensor(color: String) = prefs.edit().putString("lock_text_color_touch_sensor", color).apply()

    fun getLockTextFontEnterPassword(): String = prefs.getString("lock_text_font_enter_password", "System") ?: "System"
    fun saveLockTextFontEnterPassword(font: String) = prefs.edit().putString("lock_text_font_enter_password", font).apply()
    fun getLockTextColorEnterPassword(): String = prefs.getString("lock_text_color_enter_password", "Default") ?: "Default"
    fun saveLockTextColorEnterPassword(color: String) = prefs.edit().putString("lock_text_color_enter_password", color).apply()

    fun getLockBgSliderColor(): String = prefs.getString("lock_bg_slider_color", "#0F0F10") ?: "#0F0F10"
    fun saveLockBgSliderColor(color: String) = prefs.edit().putString("lock_bg_slider_color", color).apply()

    fun getLockBgMixType(): String = prefs.getString("lock_bg_mix_type", "Default Theme") ?: "Default Theme"
    fun saveLockBgMixType(type: String) = prefs.edit().putString("lock_bg_mix_type", type).apply()

    fun getLockBgCustomWallpaperPath(): String = prefs.getString("lock_bg_custom_wallpaper", "") ?: ""
    fun saveLockBgCustomWallpaperPath(path: String) = prefs.edit().putString("lock_bg_custom_wallpaper", path).apply()

    fun resetLockCustomisationToDefault() {
        prefs.edit()
            .remove("lock_clock_style")
            .remove("lock_clock_font")
            .remove("lock_clock_size")
            .remove("lock_clock_weight")
            .remove("lock_clock_position")
            .remove("lock_bg_theme")
            .remove("lock_liquid_glass")
            .remove("lock_btn_shape")
            .remove("lock_clock_font_size")
            .remove("lock_clock_color")
            .remove("lock_app_icon_shape")
            .remove("lock_all_locks_shape")
            .remove("lock_all_locks_color")
            .remove("lock_all_locks_size")
            .remove("lock_pin_font")
            .remove("lock_pin_color")
            .remove("lock_pin_size")
            .remove("lock_pin_btn_shape")
            .remove("lock_pin_dot_shape")
            .remove("lock_pin_dot_outline_color")
            .remove("lock_pin_dot_filled_color")
            .remove("lock_password_icon_color")
            .remove("lock_password_border_color")
            .remove("lock_password_eye_color")
            .remove("lock_password_unlock_btn_color")
            .remove("lock_pattern_dots_color")
            .remove("lock_pattern_dots_size")
            .remove("lock_pattern_dots_shape")
            .remove("lock_pattern_line_color")
            .remove("lock_pattern_animation")
            .remove("lock_biometric_animation")
            .remove("lock_biometric_size")
            .remove("lock_biometric_color")
            .remove("lock_secured_text_color")
            .remove("lock_bottom_icon_color")
            .remove("settings_lock_type_color")
            .remove("lock_biometric_icon_color")
            .remove("lock_bg_slider_color")
            .remove("lock_bg_mix_type")
            .remove("lock_bg_custom_wallpaper")
            .remove("lock_type_color_bg_pin")
            .remove("lock_type_color_ic_pin")
            .remove("lock_type_color_bg_password")
            .remove("lock_type_color_ic_password")
            .remove("lock_type_color_bg_pattern")
            .remove("lock_type_color_ic_pattern")
            .remove("lock_type_color_bg_biometric")
            .remove("lock_type_color_ic_biometric")
            .remove("lock_text_font_secured")
            .remove("lock_text_color_secured")
            .remove("lock_text_font_draw")
            .remove("lock_text_color_draw")
            .remove("lock_text_font_forgot")
            .remove("lock_text_color_forgot")
            .remove("lock_text_font_tap_scan")
            .remove("lock_text_color_tap_scan")
            .remove("lock_text_font_touch_sensor")
            .remove("lock_text_color_touch_sensor")
            .remove("lock_text_font_enter_password")
            .remove("lock_text_color_enter_password")
            .putString("accent_color_hex", "Default") // Default Accent (Black/White)
            .apply()
    }

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

    // App Shield enablement state
    fun isShieldEnabled(): Boolean = prefs.getBoolean("is_shield_enabled", true)
    fun setShieldEnabled(enabled: Boolean) = prefs.edit().putBoolean("is_shield_enabled", enabled).apply()

    // App Icon hidden state
    fun isAppIconHidden(): Boolean = prefs.getBoolean("is_app_icon_hidden", false)
    fun setAppIconHidden(hidden: Boolean) = prefs.edit().putBoolean("is_app_icon_hidden", hidden).apply()
}
