package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppLockApplication
import com.example.data.model.LockMethod
import com.example.data.model.LockedApp
import com.example.data.model.SecurityQuestion
import com.example.data.repository.AppLockRepository
import com.example.util.HashUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@androidx.compose.runtime.Immutable
data class AppItem(
    val packageName: String,
    val name: String,
    val isLocked: Boolean
)

enum class SortOption {
    ALPHABETICAL,
    LOCKED_FIRST
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AppLockApplication
    private val repository: AppLockRepository = app.repository
    val encryptedPrefs = app.encryptedPrefs

    // Cache list of installed launcher apps
    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    // Search and Sort options
    val searchQuery = MutableStateFlow("")
    val sortOption = MutableStateFlow(SortOption.ALPHABETICAL)

    // Current lock configurations
    val lockMethod = MutableStateFlow(encryptedPrefs.getLockMethod())
    val vibrationEnabled = MutableStateFlow(encryptedPrefs.isVibrationEnabled())
    val themeMode = MutableStateFlow(encryptedPrefs.getThemeMode())
    val accentColorHex = MutableStateFlow(encryptedPrefs.getAccentColorHex())
    val lockTimeoutMs = MutableStateFlow(encryptedPrefs.getLockTimeoutMs())
    val lockReLockBehavior = MutableStateFlow(encryptedPrefs.getLockReLockBehavior())
    val intruderSelfieEnabled = MutableStateFlow(encryptedPrefs.isIntruderSelfieEnabled())
    val isShieldEnabled = MutableStateFlow(encryptedPrefs.isShieldEnabled())
    val isAppIconHidden = MutableStateFlow(encryptedPrefs.isAppIconHidden())
    val isMultiLockEnabled = MutableStateFlow(encryptedPrefs.isMultiLockEnabled())
    val multiLockSequence = MutableStateFlow(encryptedPrefs.getMultiLockSequence())
    val isAppLaunchBiometricEnabled = MutableStateFlow(encryptedPrefs.isAppLaunchBiometricEnabled())
    val isSingleBiometricEnabled = MutableStateFlow(encryptedPrefs.isSingleBiometricEnabled())
    val biometricAutoPromptEnabled = MutableStateFlow(encryptedPrefs.isBiometricAutoPromptEnabled())

    // Custom lock screen customisation flows
    val lockClockStyle = MutableStateFlow(encryptedPrefs.getLockClockStyle())
    val lockClockFont = MutableStateFlow(encryptedPrefs.getLockClockFont())
    val lockClockSize = MutableStateFlow(encryptedPrefs.getLockClockSize())
    val lockClockWeight = MutableStateFlow(encryptedPrefs.getLockClockWeight())
    val lockClockPosition = MutableStateFlow(encryptedPrefs.getLockClockPosition())
    val lockBackgroundTheme = MutableStateFlow(encryptedPrefs.getLockBackgroundTheme())
    val lockLiquidGlassLook = MutableStateFlow(encryptedPrefs.getLockLiquidGlassLook())
    val lockButtonShape = MutableStateFlow(encryptedPrefs.getLockButtonShape())

    fun updateSingleBiometricEnabled(enabled: Boolean) {
        isSingleBiometricEnabled.value = enabled
        encryptedPrefs.setSingleBiometricEnabled(enabled)
    }

    fun updateAppLaunchBiometricEnabled(enabled: Boolean) {
        isAppLaunchBiometricEnabled.value = enabled
        encryptedPrefs.setAppLaunchBiometricEnabled(enabled)
    }

    fun updateBiometricAutoPromptEnabled(enabled: Boolean) {
        biometricAutoPromptEnabled.value = enabled
        encryptedPrefs.setBiometricAutoPromptEnabled(enabled)
    }


    // Unified security question flow
    val securityQuestion: StateFlow<SecurityQuestion?> = repository.getSecurityQuestion()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Flow combining everything to display the reactive app list
    val appItems: StateFlow<List<AppItem>> = combine(
        _installedApps,
        repository.getAllLockedApps(),
        searchQuery,
        sortOption
    ) { installed, lockedList, query, sort ->
        val lockedPackages = lockedList.associateBy { it.packageName }
        
        val mappedList = installed.map { appItem ->
            val isLocked = lockedPackages[appItem.packageName]?.isLocked == true
            appItem.copy(isLocked = isLocked)
        }

        // Apply filters
        val filtered = if (query.isBlank()) {
            mappedList
        } else {
            mappedList.filter { it.name.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
        }

        // Apply sorting
        when (sort) {
            SortOption.ALPHABETICAL -> filtered.sortedBy { it.name.lowercase() }
            SortOption.LOCKED_FIRST -> filtered.sortedWith(
                compareByDescending<AppItem> { it.isLocked }
                    .thenBy { it.name.lowercase() }
            )
        }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInstalledLauncherApps()
    }

    private fun loadInstalledLauncherApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val context = getApplication<Application>()
            val pm = context.packageManager
            val seen = mutableSetOf<String>()
            val list = mutableListOf<AppItem>()

            try {
                // Determine active launcher package to avoid locking it
                val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                }
                val defaultLauncherPackage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.resolveActivity(launcherIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
                }?.activityInfo?.packageName ?: ""

                val packages = pm.getInstalledPackages(0)
                for (packageInfo in packages) {
                    val packageName = packageInfo.packageName
                    if (packageName == context.packageName) continue // Skip our own app
                    if (!seen.add(packageName)) continue

                    // Blacklist critical device services that could cause crashes or soft-locks if intercepted
                    val isCritical = when {
                        packageName == "android" -> true
                        packageName == "com.android.systemui" -> true
                        packageName == "com.android.keyguard" -> true
                        packageName == "com.android.settings.intelligence" -> true
                        packageName == "com.google.android.gms" -> true
                        packageName == "com.google.android.gsf" -> true
                        packageName == "com.android.providers.settings" -> true
                        packageName == "com.android.providers.telephony" -> true
                        packageName == "com.android.phone" -> true
                        packageName == "com.android.server.telecom" -> true
                        packageName == "com.android.incallui" -> true
                        packageName == "com.android.packageinstaller" -> true
                        packageName == "com.google.android.packageinstaller" -> true
                        packageName == "com.android.permissioncontroller" -> true
                        packageName == "com.google.android.permissioncontroller" -> true
                        packageName == defaultLauncherPackage -> true
                        // Blacklist system keyboards & input method editors to prevent soft-locks on password entries
                        packageName.contains("inputmethod", ignoreCase = true) -> true
                        packageName.contains("keyboard", ignoreCase = true) -> true
                        packageName.contains("latin", ignoreCase = true) -> true
                        else -> false
                    }
                    if (isCritical) continue

                    // Only show apps that can have a foreground user state (have at least one launchable intent, or is the Settings app)
                    val isLaunchable = pm.getLaunchIntentForPackage(packageName) != null || packageName == "com.android.settings"
                    if (!isLaunchable) continue

                    val label = packageInfo.applicationInfo?.loadLabel(pm)?.toString() ?: packageName
                    list.add(AppItem(packageName, label, isLocked = false))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Warm the icons concurrently in parallel via a supervisorScope FIRST so they are 100% cached
            try {
                kotlinx.coroutines.supervisorScope {
                    list.map { item ->
                        launch(Dispatchers.IO) {
                            try {
                                com.example.ui.AppIconCache.getIcon(context, item.packageName)
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }.forEach { it.join() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            _installedApps.value = list
            _isLoading.value = false
        }
    }

    fun toggleAppLock(packageName: String, isLocked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isLocked) {
                repository.insertLockedApp(
                    LockedApp(
                        packageName = packageName,
                        isLocked = true,
                        lockMethod = lockMethod.value
                    )
                )
            } else {
                val existing = repository.getLockedAppByPackageName(packageName)
                if (existing != null) {
                    repository.deleteLockedApp(existing)
                }
            }
        }
    }

    fun updateLockMethod(method: LockMethod) {
        lockMethod.value = method
        encryptedPrefs.saveLockMethod(method)
    }

    fun updateMultiLockEnabled(enabled: Boolean) {
        isMultiLockEnabled.value = enabled
        encryptedPrefs.setMultiLockEnabled(enabled)
    }

    fun updateMultiLockSequence(sequence: List<LockMethod>) {
        multiLockSequence.value = sequence
        encryptedPrefs.saveMultiLockSequence(sequence)
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        vibrationEnabled.value = enabled
        encryptedPrefs.setVibrationEnabled(enabled)
    }

    fun updateThemeMode(mode: String) {
        themeMode.value = mode
        encryptedPrefs.saveThemeMode(mode)
    }

    fun updateAccentColorHex(hex: String) {
        accentColorHex.value = hex
        encryptedPrefs.saveAccentColorHex(hex)
    }

    fun updateLockTimeoutMs(timeoutMs: Long) {
        lockTimeoutMs.value = timeoutMs
        encryptedPrefs.saveLockTimeoutMs(timeoutMs)
    }

    fun updateLockReLockBehavior(behavior: String) {
        lockReLockBehavior.value = behavior
        encryptedPrefs.saveLockReLockBehavior(behavior)
    }

    fun updateIntruderSelfieEnabled(enabled: Boolean) {
        intruderSelfieEnabled.value = enabled
        encryptedPrefs.setIntruderSelfieEnabled(enabled)
    }

    fun updateAppIconHidden(hidden: Boolean) {
        isAppIconHidden.value = hidden
        encryptedPrefs.setAppIconHidden(hidden)
    }

    fun updateShieldEnabled(context: Context, enabled: Boolean) {
        isShieldEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            encryptedPrefs.setShieldEnabled(enabled)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    android.service.quicksettings.TileService.requestListeningState(
                        context,
                        android.content.ComponentName(context, "com.example.service.SafeLockTileService")
                    )
                    withContext(Dispatchers.Main) {
                        com.example.service.SafeLockTileService.updateActiveTile()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Lock screen customisation setters
    fun updateLockClockStyle(style: String) {
        lockClockStyle.value = style
        encryptedPrefs.saveLockClockStyle(style)
    }

    fun updateLockClockFont(font: String) {
        lockClockFont.value = font
        encryptedPrefs.saveLockClockFont(font)
    }

    fun updateLockClockSize(size: String) {
        lockClockSize.value = size
        encryptedPrefs.saveLockClockSize(size)
    }

    fun updateLockClockWeight(weight: String) {
        lockClockWeight.value = weight
        encryptedPrefs.saveLockClockWeight(weight)
    }

    fun updateLockClockPosition(position: String) {
        lockClockPosition.value = position
        encryptedPrefs.saveLockClockPosition(position)
    }

    fun updateLockBackgroundTheme(theme: String) {
        lockBackgroundTheme.value = theme
        encryptedPrefs.saveLockBackgroundTheme(theme)
    }

    fun updateLockLiquidGlassLook(look: String) {
        lockLiquidGlassLook.value = look
        encryptedPrefs.saveLockLiquidGlassLook(look)
    }

    fun updateLockButtonShape(shape: String) {
        lockButtonShape.value = shape
        encryptedPrefs.saveLockButtonShape(shape)
    }

    fun resetLockCustomisationToDefault() {
        encryptedPrefs.resetLockCustomisationToDefault()
        lockClockStyle.value = encryptedPrefs.getLockClockStyle()
        lockClockFont.value = encryptedPrefs.getLockClockFont()
        lockClockSize.value = encryptedPrefs.getLockClockSize()
        lockClockWeight.value = encryptedPrefs.getLockClockWeight()
        lockClockPosition.value = encryptedPrefs.getLockClockPosition()
        lockBackgroundTheme.value = encryptedPrefs.getLockBackgroundTheme()
        lockLiquidGlassLook.value = encryptedPrefs.getLockLiquidGlassLook()
        lockButtonShape.value = encryptedPrefs.getLockButtonShape()
        accentColorHex.value = encryptedPrefs.getAccentColorHex()
    }

    fun refreshLockCustomisationFlows() {
        lockClockStyle.value = encryptedPrefs.getLockClockStyle()
        lockClockFont.value = encryptedPrefs.getLockClockFont()
        lockClockSize.value = encryptedPrefs.getLockClockSize()
        lockClockWeight.value = encryptedPrefs.getLockClockWeight()
        lockClockPosition.value = encryptedPrefs.getLockClockPosition()
        lockBackgroundTheme.value = encryptedPrefs.getLockBackgroundTheme()
        lockLiquidGlassLook.value = encryptedPrefs.getLockLiquidGlassLook()
        lockButtonShape.value = encryptedPrefs.getLockButtonShape()
    }

    // PIN preference helpers
    fun getPinHash(): String? = encryptedPrefs.getPinHash()
    
    fun savePin(pin: String) {
        val hash = HashUtil.hash(pin)
        encryptedPrefs.savePinHash(hash)
        encryptedPrefs.savePinLength(pin.length)
    }

    fun clearPin() {
        encryptedPrefs.clearPinHash()
        encryptedPrefs.clearPinLength()
    }

    // Pattern preference helpers
    fun getPatternHash(): String? = encryptedPrefs.getPatternHash()

    fun savePattern(pattern: String) {
        val hash = HashUtil.hash(pattern)
        encryptedPrefs.savePatternHash(hash)
    }

    fun clearPattern() {
        encryptedPrefs.clearPatternHash()
    }

    // Password preference helpers
    fun getPasswordHash(): String? = encryptedPrefs.getPasswordHash()

    fun savePassword(password: String) {
        val hash = HashUtil.hash(password)
        encryptedPrefs.savePasswordHash(hash)
    }

    fun clearPassword() {
        encryptedPrefs.clearPasswordHash()
    }

    // Security Question Persistence
    fun setupSecurityQuestion(question: String, answer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val answerHash = HashUtil.hash(answer.trim().lowercase())
            repository.insertSecurityQuestion(
                SecurityQuestion(
                    id = 1,
                    question = question,
                    answerHash = answerHash
                )
            )
        }
    }

    fun deleteSecurityQuestion() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSecurityQuestion()
        }
    }

    // First Launch Helper
    fun isFirstLaunch(): Boolean = encryptedPrefs.isFirstLaunch()
    fun setFirstLaunch(first: Boolean) = encryptedPrefs.setFirstLaunch(first)
}
