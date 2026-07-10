package com.example.ui

import android.os.Build
import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LockMethod
import com.example.util.HashUtil
import com.example.util.RealmeOemHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.animation.*
import androidx.compose.ui.zIndex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // ViewModel state bindings
    val lockMethod by viewModel.lockMethod.collectAsStateWithLifecycle()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val accentColorHex by viewModel.accentColorHex.collectAsStateWithLifecycle()
    val lockClockStyle by viewModel.lockClockStyle.collectAsStateWithLifecycle()
    val lockTimeoutMs by viewModel.lockTimeoutMs.collectAsStateWithLifecycle()
    val lockReLockBehavior by viewModel.lockReLockBehavior.collectAsStateWithLifecycle()
    val intruderSelfieEnabled by viewModel.intruderSelfieEnabled.collectAsStateWithLifecycle()
    val isShieldEnabled by viewModel.isShieldEnabled.collectAsStateWithLifecycle()
    val isAppIconHidden by viewModel.isAppIconHidden.collectAsStateWithLifecycle()
    val biometricAutoPromptEnabled by viewModel.biometricAutoPromptEnabled.collectAsStateWithLifecycle()
    val securityQuestion by viewModel.securityQuestion.collectAsStateWithLifecycle()

    val dpm = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponent = remember { ComponentName(context, com.example.service.AdminReceiver::class.java) }
    var isUninstallProtectionActive by remember { mutableStateOf(dpm.isAdminActive(adminComponent)) }
    var showUninstallVerifyDialog by remember { mutableStateOf(false) }
    var showHideIconAlert by remember { mutableStateOf(false) }
    var isEnablingTarget by remember { mutableStateOf(false) }

    // Diagnostic/Permission States cached top-level to prevent Jitters & Main-thread work during scrolling
    var isUsageStatsActive by remember { mutableStateOf(false) }
    var isCameraActive by remember { mutableStateOf(false) }
    var isNotificationActive by remember { mutableStateOf(false) }

    val refreshPermissions = remember(context) {
        {
            isUsageStatsActive = hasUsageStatsPermission(context)
            isCameraActive = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            isNotificationActive = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshPermissions()
    }

    val observerLifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(observerLifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isUninstallProtectionActive = dpm.isAdminActive(adminComponent)
                refreshPermissions()
            }
        }
        observerLifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            observerLifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Dialog triggering states
    var showPinDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showPatternDialog by remember { mutableStateOf(false) }
    var showQuestionDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showReLockBehaviorDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAccentColorDialog by remember { mutableStateOf(false) }
    var showClockStyleDialog by remember { mutableStateOf(false) }
    var showIntruderGalleryDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }

    // Permission launchers registered at the top level (never in dynamic LazyColumn views)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updateIntruderSelfieEnabled(isGranted)
        if (isGranted) {
            Toast.makeText(context, "Camera permission granted. Intruder Selfie enabled!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Camera permission is required for Intruder Selfie", Toast.LENGTH_LONG).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isCameraActive = isGranted
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isNotificationActive = isGranted
    }

    // Click and check change callbacks remembered to completely optimize scrolling
    val onPinClick = remember { { showPinDialog = true } }
    val onPatternClick = remember { { showPatternDialog = true } }
    val onPasswordClick = remember { { showPasswordDialog = true } }
    
    val onAppLaunchBiometricCheckedChange = remember(viewModel, context) {
        { checked: Boolean ->
            val hasAnyLock = viewModel.getPinHash() != null || viewModel.getPatternHash() != null || viewModel.getPasswordHash() != null
            if (checked && !hasAnyLock) {
                Toast.makeText(context, "Please configure a lock PIN, Pattern, or Password first!", Toast.LENGTH_LONG).show()
            } else {
                viewModel.updateAppLaunchBiometricEnabled(checked)
            }
        }
    }

    val onBiometricAutoPromptCheckedChange = remember(viewModel) {
        { checked: Boolean ->
            viewModel.updateBiometricAutoPromptEnabled(checked)
        }
    }

    val onQuestionClick = remember { { showQuestionDialog = true } }

    val onIntruderSelfieCheckedChange = remember(viewModel, context, cameraPermissionLauncher) {
        { checked: Boolean ->
            if (checked) {
                val hasCameraPerm = ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (hasCameraPerm) {
                    viewModel.updateIntruderSelfieEnabled(true)
                } else {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            } else {
                viewModel.updateIntruderSelfieEnabled(false)
            }
        }
    }

    val onIntruderGalleryClick = remember { { showIntruderGalleryDialog = true } }

    val onHideAppIconCheckedChange = remember(viewModel, context) {
        { checked: Boolean ->
            if (checked) {
                showHideIconAlert = true
            } else {
                val pm = context.packageManager
                val launcherComponent = ComponentName(context, "com.example.LauncherAlias")
                val disguiseComponent = ComponentName(context, "com.example.DisguiseAlias")
                pm.setComponentEnabledSetting(
                    disguiseComponent,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    launcherComponent,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
                viewModel.updateAppIconHidden(false)
                Toast.makeText(context, "App Icon restored to Launcher", Toast.LENGTH_SHORT).show()
                val stopIntent = Intent(context, com.example.service.AppLockForegroundService::class.java).apply {
                    action = com.example.service.AppLockForegroundService.ACTION_STOP
                }
                context.startService(stopIntent)
                com.example.service.AppLockForegroundService.refreshLauncher(context)
            }
        }
    }

    val onUninstallProtectionCheckedChange = remember(viewModel, context, adminComponent, dpm) {
        { checked: Boolean ->
            isEnablingTarget = checked
            
            val fragmentActivity = context.findActivity()
            val executor = ContextCompat.getMainExecutor(context)
            val biometricHelper = com.example.util.BiometricHelper()

            val executeAction = {
                if (checked) {
                    try {
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Protects SafeLock application from being uninstalled on this device.")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to launch device admin: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    try {
                        dpm.removeActiveAdmin(adminComponent)
                        isUninstallProtectionActive = false
                        Toast.makeText(context, "Uninstall Protection disabled successfully.", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            if (fragmentActivity != null) {
                biometricHelper.authenticate(
                    activity = fragmentActivity,
                    executor = executor,
                    title = "Advance Uninstall Protection Verification",
                    subtitle = "Verify with biometric key to proceed",
                    onSuccess = {
                        fragmentActivity.runOnUiThread {
                            executeAction()
                        }
                    },
                    onError = { errorCode, errString ->
                        fragmentActivity.runOnUiThread {
                            val hasAnyLock = viewModel.getPinHash() != null || viewModel.getPatternHash() != null || viewModel.getPasswordHash() != null
                            if (hasAnyLock) {
                                // Fallback to PIN/Password secure verification on biometric error/cancel
                                showUninstallVerifyDialog = true
                            } else {
                                Toast.makeText(context, "Biometric failed: $errString", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onFailed = {
                        fragmentActivity.runOnUiThread {
                            Toast.makeText(context, "Biometric evaluation failed!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                executeAction()
            }
        }
    }

    val onUsageStatsClick = remember(context) {
        {
            val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
            context.startActivity(intent)
        }
    }

    val onNotificationClick = remember(context, notificationLauncher) {
        {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                try {
                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } catch (e: Exception) {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                    Toast.makeText(context, "Please enable notifications manually", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Notifications active by default!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val onCameraPermissionClick = remember(cameraLauncher, context) {
        {
            try {
                cameraLauncher.launch(android.Manifest.permission.CAMERA)
            } catch (e: Exception) {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
                Toast.makeText(context, "Please enable camera manually", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val onOemAutoLaunchClick = remember(context) {
        {
            val launched = RealmeOemHelper.launchAutoStartSettings(context)
            if (!launched) {
                context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
            }
        }
    }

    val onThemeClick = remember { { showThemeDialog = true } }
    val onAccentColorClick = remember { { showAccentColorDialog = true } }
    val onClockStyleClick = remember { { showClockStyleDialog = true } }
    val onTimeoutClick = remember { { showTimeoutDialog = true } }
    val onReLockBehaviorClick = remember { { showReLockBehaviorDialog = true } }
    val onVibrationCheckedChange = remember(viewModel) { { checked: Boolean -> viewModel.updateVibrationEnabled(checked) } }
    
    val onRealmeOptimizationClick = remember(context) {
        {
            val launched = RealmeOemHelper.launchAutoStartSettings(context)
            if (!launched) {
                Toast.makeText(context, "Opening core systems launcher settings...", Toast.LENGTH_SHORT).show()
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    val onUserGuideClick = remember { { showGuideDialog = true } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SETTINGS",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("settings_title")
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 0. App Shield Configuration Group
            CategoryHeading(text = "APP SHIELD MASTER SWITCH")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(8.dp)
            ) {
                SettingsSwitchRow(
                    title = "App Shield Protection",
                    subtitle = "Instantly enable or disable lock screen security on all protected apps",
                    isChecked = isShieldEnabled,
                    onCheckedChange = { viewModel.updateShieldEnabled(context, it) },
                    icon = Icons.Default.Shield,
                    tag = "app_shield_master_switch"
                )
            }

            // 1. Authentication Configuration Group
            CategoryHeading(text = "AUTHENTICATION")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column {
                    // Lock Method Selectors Row
                    SelectionLockMethodRow(viewModel = viewModel)

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Change PIN action
                    SettingsActionRow(
                        title = "Change Password PIN",
                        subtitle = if (viewModel.getPinHash() == null) "Not Set" else "PIN code active",
                        icon = Icons.Default.Password,
                        onClick = onPinClick,
                        tag = "change_pin_row"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Change Pattern action
                    SettingsActionRow(
                        title = "Change Lock Pattern",
                        subtitle = if (viewModel.getPatternHash() == null) "Not Set" else "Grid pattern active",
                        icon = Icons.Default.Grid3x3,
                        onClick = onPatternClick,
                        tag = "change_pattern_row"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Change Password action
                    SettingsActionRow(
                        title = "Change Lock Password",
                        subtitle = if (viewModel.getPasswordHash() == null) "Not Set" else "Alphanumeric password active",
                        icon = Icons.Default.Lock,
                        onClick = onPasswordClick,
                        tag = "change_password_row"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    val isAppLaunchBiometricEnabled by viewModel.isAppLaunchBiometricEnabled.collectAsStateWithLifecycle()
                    SettingsSwitchRow(
                        title = "Secure App Launch (Biometric)",
                        subtitle = "Requires biometric or passcode auth when opening SafeLock",
                        isChecked = isAppLaunchBiometricEnabled,
                        onCheckedChange = onAppLaunchBiometricCheckedChange,
                        icon = Icons.Default.Fingerprint,
                        tag = "app_launch_biometric_switch"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    SettingsSwitchRow(
                        title = "Auto-Prompt Biometrics",
                        subtitle = "Show biometric authentication immediately when opening a locked app",
                        isChecked = biometricAutoPromptEnabled,
                        onCheckedChange = onBiometricAutoPromptCheckedChange,
                        icon = Icons.Default.Fingerprint,
                        tag = "biometric_auto_prompt_switch"
                    )
                }
            }

            // 2. Security validation group
            CategoryHeading(text = "SECURITY VALIDATION")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column {
                    // Security Question Config Link
                    SettingsActionRow(
                        title = "Setup Security Question",
                        subtitle = if (securityQuestion == null) "Highly recommended for password recovery" else "Active: ${securityQuestion?.question}",
                        icon = Icons.Default.QuestionMark,
                        onClick = onQuestionClick,
                        tag = "security_question_row"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Intruder Selfie Switch
                    SettingsSwitchRow(
                        title = "Captured Intruder Selfies",
                        subtitle = "Takes silently a photo on 3 failed lock challenges",
                        isChecked = intruderSelfieEnabled,
                        onCheckedChange = onIntruderSelfieCheckedChange,
                        icon = Icons.Default.CameraAlt,
                        tag = "intruder_selfie_switch"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Hide App Icon Switch
                    SettingsSwitchRow(
                        title = "Disguise App Icon (Compass)",
                        subtitle = "Disguises App Lock as Compass for privacy.",
                        isChecked = isAppIconHidden,
                        onCheckedChange = onHideAppIconCheckedChange,
                        icon = Icons.Default.VisibilityOff,
                        tag = "hide_app_icon_switch"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // View Intruder Selfies Log
                    SettingsActionRow(
                        title = "View Intruder Selfies Log",
                        subtitle = "View photographs of intruders captured on wrong entry attempts",
                        icon = Icons.Default.CameraAlt,
                        onClick = onIntruderGalleryClick,
                        tag = "view_selfies_row"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    SettingsSwitchRow(
                        title = "Advance Uninstall Protection",
                        subtitle = "Prevents unauthorized users from deleting SafeLock (Device Admin)",
                        isChecked = isUninstallProtectionActive,
                        onCheckedChange = onUninstallProtectionCheckedChange,
                        icon = Icons.Default.Security,
                        tag = "uninstall_protection_switch"
                    )
                }
            }

            // 2.5 System Permissions / Diagnostics Group
            CategoryHeading(text = "SYSTEM PERMISSIONS")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "To ensure flawless locked application tracking and device background runtime stability, configure all permission states below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // UsageStats Status Button
                    PermissionStatusRow(
                        title = "Usage Stats Monitoring",
                        description = "Required to intercept and lock app background transitions.",
                        isChecked = isUsageStatsActive,
                        onClick = onUsageStatsClick,
                        testTag = "settings_usagestats_toggle"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                    // Notification Status Button
                    PermissionStatusRow(
                        title = "Notification Alerting",
                        description = "Shows real-time status alerts for protection core.",
                        isChecked = isNotificationActive,
                        onClick = onNotificationClick,
                        testTag = "settings_notification_toggle"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                    // Camera Status Button
                    PermissionStatusRow(
                        title = "Camera Captivity (Selfies)",
                        description = "Required to silently record intruder wrong attempts pictures.",
                        isChecked = isCameraActive,
                        onClick = onCameraPermissionClick,
                        testTag = "settings_camera_toggle"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                    // Auto launch item
                    PermissionStatusRow(
                        title = "Configure OEM Auto-Launch",
                        description = "ColorOS / Realme background protection auto start.",
                        isChecked = true,
                        onClick = onOemAutoLaunchClick,
                        testTag = "settings_autolaunch_toggle"
                    )
                }
            }

            // 3. User Experience & System group
            CategoryHeading(text = "SYSTEM & UX OPTIONS")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column {
                    // Change Theme
                    SettingsActionRow(
                        title = "App theme",
                        subtitle = when (themeMode) {
                            "LIGHT" -> "Light Theme"
                            "DARK" -> "Dark Theme"
                            else -> "Follow System Preference"
                        },
                        icon = Icons.Default.Palette,
                        onClick = onThemeClick,
                        tag = "change_theme_row"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // App accent color
                    SettingsActionRow(
                        title = "App accent color",
                        subtitle = when (accentColorHex) {
                            "Default" -> "Default"
                            "#FFFF7A17" -> "Claude Orange"
                            "#10B981" -> "Premium Green"
                            "#E55050" -> "Faded Red"
                            "#007AFF" -> "Apple Blue"
                            "#8B5CF6" -> "Electric Purple"
                            "#EC4899" -> "Neon Pink"
                            "#06B6D4" -> "Cyber Teal"
                            "#D97706" -> "Sahara Gold"
                            "#6366F1" -> "Lavender Blue"
                            "#7E8C8D" -> "Slate Gray"
                            "#DC2626" -> "Crimson Red"
                            "#1E3A8A" -> "Midnight Navy"
                            else -> "Default"
                        },
                        icon = Icons.Default.ColorLens,
                        onClick = onAccentColorClick,
                        tag = "change_accent_color_row"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Lock Screen Clock Style
                    SettingsActionRow(
                        title = "Lock screen clock style",
                        subtitle = lockClockStyle,
                        icon = Icons.Default.AccessTime,
                        onClick = onClockStyleClick,
                        tag = "change_clock_style_row"
                    )



                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Lock Timeout Setting
                    SettingsActionRow(
                        title = "Lockout Timeout period",
                        subtitle = when (lockTimeoutMs) {
                            0L -> "Immediate lock on background"
                            60000L -> "1 minute buffer"
                            300000L -> "5 minutes buffer"
                            600000L -> "10 minutes buffer"
                            else -> "Immediate"
                        },
                        icon = Icons.Default.Timer,
                        onClick = onTimeoutClick,
                        tag = "change_timeout_row"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Lock Re-Lock Behavior Setting
                    SettingsActionRow(
                        title = "App Locking Behavior Mode",
                        subtitle = when (lockReLockBehavior) {
                            "Until Screen Off" -> "Once Unlocked, Stays Unlocked (Until Screen Off)"
                            "Lock When Exit" -> "Lock app when exit the app"
                            else -> "Once Unlocked, Stays Unlocked (Until Screen Off)"
                        },
                        icon = Icons.Default.Security,
                        onClick = onReLockBehaviorClick,
                        tag = "change_relock_behavior_row"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Vibration Switch
                    SettingsSwitchRow(
                        title = "Vibrate on Interaction",
                        subtitle = "Controls haptic vibration feedback inside pin/patterns",
                        isChecked = vibrationEnabled,
                        onCheckedChange = onVibrationCheckedChange,
                        icon = Icons.Default.Vibration,
                        tag = "vibrate_toggle_row"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Realme OEM optimization launcher button
                    SettingsActionRow(
                        title = "Realme Background Optimization",
                        subtitle = "Click to open native battery and startup controls",
                        icon = Icons.Default.Settings,
                        onClick = onRealmeOptimizationClick,
                        tag = "realme_helper_row"
                    )
                }
            }

            // 3.5 Beginner Help Guide Section
            CategoryHeading(text = "HELP & USER MANUAL")
            Card(
                modifier = Modifier.fillMaxWidth().testTag("help_guide_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                SettingsActionRow(
                    title = "Beginner's Help Guide",
                    subtitle = "Learn how to use all features & unlock performance",
                    icon = Icons.Default.AutoStories,
                    onClick = onUserGuideClick,
                    tag = "view_user_guide_row"
                )
            }

            // 4. About card and Version details
            CategoryHeading(text = "ABOUT PROTOCOL")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "App Lock Console",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "v2.0.0 — Stable Build",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.testTag("personal_use_badge")
                    ) {
                        Text(
                            text = "built for personal use",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // PIN CHANGING FLOW DIALOG
    if (showPinDialog) {
        PinChangeDialog(
            viewModel = viewModel,
            onDismiss = { showPinDialog = false }
        )
    }

    // PATTERN CHANGING FLOW DIALOG
    if (showPatternDialog) {
        PatternChangeDialog(
            viewModel = viewModel,
            onDismiss = { showPatternDialog = false }
        )
    }

    // SECURITY QUESTION SETUP DIALOG
    if (showQuestionDialog) {
        SecurityQuestionSetupDialog(
            viewModel = viewModel,
            onDismiss = { showQuestionDialog = false }
        )
    }

    // THEME SELECTION STATE DIALOG
    if (showThemeDialog) {
        ThemeSelectorDialog(
            currentValue = themeMode,
            onSelected = {
                viewModel.updateThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    // ACCENT COLOR SELECTION DIALOG
    if (showAccentColorDialog) {
        AccentColorSelectorDialog(
            currentValue = accentColorHex,
            onSelected = {
                viewModel.updateAccentColorHex(it)
                showAccentColorDialog = false
            },
            onDismiss = { showAccentColorDialog = false }
        )
    }

    // CLOCK STYLE SELECTION DIALOG
    if (showClockStyleDialog) {
        ClockStyleSelectorDialog(
            currentValue = lockClockStyle,
            onSelected = {
                viewModel.updateLockClockStyle(it)
                showClockStyleDialog = false
            },
            onDismiss = { showClockStyleDialog = false }
        )
    }

    // LOCK TIMEOUT SELECTOR DIALOG
    if (showTimeoutDialog) {
        TimeoutSelectorDialog(
            currentValue = lockTimeoutMs,
            onSelected = {
                viewModel.updateLockTimeoutMs(it)
                showTimeoutDialog = false
            },
            onDismiss = { showTimeoutDialog = false }
        )
    }

    // LOCK RE-LOCK BEHAVIOR DIALOG
    if (showReLockBehaviorDialog) {
        ReLockBehaviorSelectorDialog(
            currentValue = lockReLockBehavior,
            onSelected = {
                viewModel.updateLockReLockBehavior(it)
                showReLockBehaviorDialog = false
            },
            onDismiss = { showReLockBehaviorDialog = false }
        )
    }

    // PASSWORD CHANGING FLOW DIALOG
    if (showPasswordDialog) {
        PasswordChangeDialog(
            viewModel = viewModel,
            onDismiss = { showPasswordDialog = false }
        )
    }

    // INTRUDER snap log DIALOG
    if (showIntruderGalleryDialog) {
        IntruderGalleryDialog(
            onDismiss = { showIntruderGalleryDialog = false }
        )
    }

    if (showHideIconAlert) {
        AlertDialog(
            onDismissRequest = { showHideIconAlert = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Disguise App Icon")
                }
            },
            text = {
                Text("Because of Android 15 policies, apps can no longer completely vanish from the launcher. Instead, your App Icon will be disguised as a 'Compass' to ensure complete privacy without leaving a suspicious 'App Info' shortcut behind.\n\nYou can restore the original icon anytime by long-pressing 'Restore Icon' on the persistent Notification.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showHideIconAlert = false
                    val pm = context.packageManager
                    val launcherComponent = ComponentName(context, "com.example.LauncherAlias")
                    val disguiseComponent = ComponentName(context, "com.example.DisguiseAlias")
                    
                    pm.setComponentEnabledSetting(
                        disguiseComponent,
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        android.content.pm.PackageManager.DONT_KILL_APP
                    )
                    pm.setComponentEnabledSetting(
                        launcherComponent,
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        android.content.pm.PackageManager.DONT_KILL_APP
                    )
                    
                    viewModel.updateAppIconHidden(true)
                    Toast.makeText(context, "App disguised as Compass. See Notification to restore.", Toast.LENGTH_LONG).show()
                    val startIntent = Intent(context, com.example.service.AppLockForegroundService::class.java).apply {
                        action = com.example.service.AppLockForegroundService.ACTION_START
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(startIntent)
                    } else {
                        context.startService(startIntent)
                    }
                    com.example.service.AppLockForegroundService.refreshLauncher(context)
                }) {
                    Text("Disguise Icon")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHideIconAlert = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showUninstallVerifyDialog) {
        UninstallProtectionVerifyDialog(
            viewModel = viewModel,
            onSuccess = {
                try {
                    if (isEnablingTarget) {
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Protects SafeLock application from being uninstalled on this device.")
                        }
                        context.startActivity(intent)
                    } else {
                        dpm.removeActiveAdmin(adminComponent)
                        isUninstallProtectionActive = false
                        Toast.makeText(context, "Uninstall Protection disabled successfully.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
                showUninstallVerifyDialog = false
            },
            onDismiss = { showUninstallVerifyDialog = false }
        )
    }

    AnimatedVisibility(
        visible = showGuideDialog,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.zIndex(10f)
    ) {
        UserGuideView(
            onDismiss = { showGuideDialog = false }
        )
    }
}

@Composable
fun UninstallProtectionVerifyDialog(
    viewModel: MainViewModel,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val configuredMethod by viewModel.lockMethod.collectAsStateWithLifecycle()
    
    var textInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    val isVibrationEnabled by viewModel.vibrationEnabled.collectAsStateWithLifecycle()
    
    var isWrong by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text(
                text = "Deactivate Uninstall Protection",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Please verify your App Lock credentials to disable Uninstall Protection.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (configuredMethod == LockMethod.PIN || configuredMethod == LockMethod.PASSWORD) {
                    val isPin = configuredMethod == LockMethod.PIN
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = {
                            textInput = it
                            errorMsg = ""
                        },
                        label = { Text(if (isPin) "Enter PIN" else "Enter Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (isPin) KeyboardType.Number else KeyboardType.Password
                        ),
                        singleLine = true,
                        isError = errorMsg.isNotEmpty(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = com.example.ui.theme.LocalAccentColor.current,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = com.example.ui.theme.LocalAccentColor.current,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = com.example.ui.theme.LocalAccentColor.current,
                            errorBorderColor = com.example.ui.theme.LocalAccentColor.current,
                            errorLabelColor = com.example.ui.theme.LocalAccentColor.current,
                            errorCursorColor = com.example.ui.theme.LocalAccentColor.current,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("uninstall_verify_input")
                    )
                } else if (configuredMethod == LockMethod.PATTERN) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PatternCanvas(
                            isVibrationEnabled = isVibrationEnabled,
                            isWrong = isWrong,
                            onPatternComplete = { dots ->
                                val dotsStr = dots.joinToString(",")
                                val verified = com.example.util.HashUtil.verify(dotsStr, viewModel.getPatternHash() ?: "")
                                if (verified) {
                                    onSuccess()
                                } else {
                                    isWrong = true
                                    errorMsg = "Incorrect pattern!"
                                    coroutineScope.launch {
                                        delay(1500)
                                        isWrong = false
                                    }
                                }
                            }
                        )
                    }
                } else {
                    Text(
                        text = "No passcode configured. Please configure an App Lock passcode first.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }

                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp).testTag("uninstall_verify_error")
                    )
                }
            }
        },
        confirmButton = {
            if (configuredMethod == LockMethod.PIN || configuredMethod == LockMethod.PASSWORD) {
                Button(
                    onClick = {
                        val isPin = configuredMethod == LockMethod.PIN
                        val targetHash = if (isPin) viewModel.getPinHash() else viewModel.getPasswordHash()
                        val verified = com.example.util.HashUtil.verify(textInput, targetHash ?: "")
                        
                        if (verified) {
                            onSuccess()
                        } else {
                            errorMsg = "Incorrect ${if (isPin) "PIN" else "Password"}!"
                        }
                    },
                    modifier = Modifier.testTag("uninstall_verify_confirm")
                ) {
                    Text("Verify")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("uninstall_verify_dismiss")
            ) {
                Text("Cancel")
            }
        }
    )
}

// Subordinate widgets definitions
@Composable
fun CategoryHeading(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
    )
}

@Composable
fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag + "_switch")
        )
    }
}

@Composable
fun SelectionLockMethodRow(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val currentSelection by viewModel.lockMethod.collectAsStateWithLifecycle()
    val isMultiLockEnabled by viewModel.isMultiLockEnabled.collectAsStateWithLifecycle()
    val multiSequence by viewModel.multiLockSequence.collectAsStateWithLifecycle()

    val settingsLockTypeColor = MaterialTheme.colorScheme.primary
    
    val isLightColor = remember(settingsLockTypeColor) {
        val luminance = 0.2126 * settingsLockTypeColor.red + 0.7152 * settingsLockTypeColor.green + 0.0722 * settingsLockTypeColor.blue
        luminance > 0.45
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // Active Locking Protocol Header with Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Active Locking Protocol",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isMultiLockEnabled) "Priority-based multi authentication" else "Single unlock method preset",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            
            // Toggle for select multiple
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Select Multiple",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                val onMultiLockToggleChange = remember(viewModel, currentSelection) {
                    { isEnabled: Boolean ->
                        viewModel.updateMultiLockEnabled(isEnabled)
                        if (isEnabled) {
                            viewModel.updateMultiLockSequence(listOf(LockMethod.BIOMETRIC, LockMethod.PIN))
                        }
                    }
                }
                Switch(
                    checked = isMultiLockEnabled,
                    onCheckedChange = onMultiLockToggleChange,
                    modifier = Modifier.testTag("select_multiple_toggle")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // True sequence order
        val sortedSelected = remember(isMultiLockEnabled, multiSequence) {
            if (isMultiLockEnabled) {
                multiSequence.distinct()
            } else {
                emptyList()
            }
        }

        // Grid of options
        val availableMethods = remember(isMultiLockEnabled) {
            if (isMultiLockEnabled) {
                LockMethod.values().filter { it != LockMethod.DEFAULT }
            } else {
                LockMethod.values().toList()
            }
        }

        val columns = 3
        val chunks = remember(availableMethods) {
            availableMethods.chunked(columns)
        }

        chunks.forEachIndexed { rowIndex, chunk ->
            if (rowIndex > 0) Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chunk.forEach { method ->
                    val isSelected = if (isMultiLockEnabled) {
                        multiSequence.contains(method)
                    } else {
                        method == currentSelection
                    }
                    
                    val priorityNo = if (isMultiLockEnabled && isSelected) {
                        val index = sortedSelected.indexOf(method)
                        if (index != -1) index + 1 else -1
                    } else {
                        -1
                    }

                    val onMethodClick = remember(isMultiLockEnabled, multiSequence, currentSelection, method) {
                        {
                            if (isMultiLockEnabled) {
                                val currentSeq = multiSequence.toMutableList()
                                if (currentSeq.contains(method)) {
                                    if (currentSeq.size > 1) {
                                        currentSeq.remove(method)
                                        viewModel.updateMultiLockSequence(currentSeq)
                                    } else {
                                        Toast.makeText(context, "At least one lock type must remain active", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    currentSeq.add(method)
                                    viewModel.updateMultiLockSequence(currentSeq)
                                }
                            } else {
                                viewModel.updateLockMethod(method)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) settingsLockTypeColor else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable(onClick = onMethodClick)
                            .padding(vertical = 12.dp)
                            .testTag("lock_method_${method.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val localFgColor = if (isSelected) {
                                if (isLightColor) Color.Black else Color.White
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                            val localTextColor = if (isSelected) {
                                if (isLightColor) Color.Black else Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Icon(
                                imageVector = when (method) {
                                    LockMethod.PIN -> Icons.Default.Password
                                    LockMethod.PATTERN -> Icons.Default.Grid3x3
                                    LockMethod.BIOMETRIC -> Icons.Default.Fingerprint
                                    LockMethod.DEFAULT -> Icons.Default.Lock
                                    else -> Icons.Default.Lock
                                },
                                contentDescription = null,
                                tint = localFgColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (method == LockMethod.DEFAULT) "Default" else method.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = localTextColor
                            )
                        }

                        // Top right badge for numbering
                        if (isMultiLockEnabled && isSelected && priorityNo != -1) {
                            val badgeBgColor = if (isLightColor) Color.Black else Color.White
                            val badgeTextColor = if (isLightColor) Color.White else settingsLockTypeColor
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 4.dp, end = 6.dp)
                                    .size(16.dp)
                                    .background(badgeBgColor, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = priorityNo.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = badgeTextColor
                                )
                            }
                        }
                    }
                }

                val missingCount = columns - chunk.size
                if (missingCount > 0) {
                    repeat(missingCount) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// 1. PIN CHANGE STATE MACHINE DIALOG
@Composable
fun PinChangeDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasOldPin = remember { viewModel.getPinHash() != null }
    
    var step by remember { mutableStateOf(if (hasOldPin) 1 else 2) } // Step 1: verify, Step 2: enter new, Step 3: confirm
    
    var inputVal by remember { mutableStateOf("") }
    var newPinEntered by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text(
                text = when (step) {
                    1 -> "Verify Current PIN"
                    2 -> "Enter New PIN"
                    else -> "Confirm New PIN"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = when (step) {
                        1 -> "Verify your existing PIN code to configure a substitute."
                        2 -> "Type a safe new PIN code (minimum 4, maximum 16 digits)."
                        else -> "Re-enter the new PIN code to eliminate typos."
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = inputVal,
                    onValueChange = {
                        if (it.length <= 16 && it.all { ch -> ch.isDigit() }) {
                            inputVal = it
                            errorMsg = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_flow_input"),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    placeholder = { Text("Enter PIN (4-16 digits)") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = com.example.ui.theme.LocalAccentColor.current,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = com.example.ui.theme.LocalAccentColor.current,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = com.example.ui.theme.LocalAccentColor.current,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent
                    )
                )

                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inputVal.length < 4 || inputVal.length > 16) {
                        errorMsg = "PIN code must be between 4 and 16 digits"
                        return@Button
                    }
                    when (step) {
                        1 -> {
                            val verified = HashUtil.verify(inputVal, viewModel.getPinHash() ?: "")
                            if (verified) {
                                step = 2
                                inputVal = ""
                            } else {
                                errorMsg = "Incorrect existing PIN"
                            }
                        }
                        2 -> {
                            newPinEntered = inputVal
                            step = 3
                            inputVal = ""
                        }
                        3 -> {
                            if (inputVal == newPinEntered) {
                                viewModel.savePin(inputVal)
                                Toast.makeText(context, "Password PIN code updated successfully!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                errorMsg = "PINs do not match. Please restart new PIN setup."
                                step = 2
                                inputVal = ""
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.example.ui.theme.LocalAccentColor.current,
                    contentColor = if (com.example.ui.theme.LocalAccentColor.current.luminance() > 0.5f) Color.Black else Color.White
                ),
                modifier = Modifier.testTag("pin_flow_confirm")
            ) {
                Text(if (step == 3) "Save PIN" else "Continue")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = com.example.ui.theme.LocalAccentColor.current
                ),
                modifier = Modifier.testTag("pin_flow_cancel")
            ) {
                Text("Cancel")
            }
        }
    )
}

// 2. PATTERN CHANGE STATE MACHINE DIALOG
@Composable
fun PatternChangeDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasOldPattern = remember { viewModel.getPatternHash() != null }
    
    var step by remember { mutableStateOf(if (hasOldPattern) 1 else 2) } // Step 1: verify, Step 2: enter new, Step 3: confirm
    
    var currentDrawnDots by remember { mutableStateOf<List<Int>>(emptyList()) }
    var newPatternEntered by remember { mutableStateOf<List<Int>>(emptyList()) }
    var errorMsg by remember { mutableStateOf("") }
    var isWrong by remember { mutableStateOf(false) }
    var resetCounter by remember { mutableStateOf(0) }
    var autoTransitionJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val coroutineScope = rememberCoroutineScope()

    fun processDrawnPattern(dots: List<Int>) {
        val patternString = dots.joinToString(",")
        if (dots.size < 4) {
            isWrong = true
            errorMsg = "Connect at least 4 dots"
            currentDrawnDots = emptyList()
            resetCounter++
            coroutineScope.launch {
                delay(1500)
                isWrong = false
            }
            return
        }

        when (step) {
            1 -> {
                val verifieds = HashUtil.verify(patternString, viewModel.getPatternHash() ?: "")
                if (verifieds) {
                    step = 2
                    currentDrawnDots = emptyList()
                    resetCounter++
                    errorMsg = ""
                } else {
                    isWrong = true
                    errorMsg = "Incorrect existing pattern"
                    currentDrawnDots = emptyList()
                    resetCounter++
                    coroutineScope.launch {
                        delay(1500)
                        isWrong = false
                    }
                }
            }
            2 -> {
                newPatternEntered = dots
                step = 3
                currentDrawnDots = emptyList()
                resetCounter++
                errorMsg = ""
            }
            3 -> {
                if (dots == newPatternEntered) {
                    viewModel.savePattern(patternString)
                    Toast.makeText(context, "Lock Pattern set successfully!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                } else {
                    isWrong = true
                    errorMsg = "Patterns do not match. Re-draw your new pattern from start."
                    step = 2
                    currentDrawnDots = emptyList()
                    newPatternEntered = emptyList()
                    resetCounter++
                    coroutineScope.launch {
                        delay(1500)
                        isWrong = false
                    }
                }
            }
        }
    }

    // Clean up any running transition timer when step is modified
    LaunchedEffect(step) {
        autoTransitionJob?.cancel()
    }

    AlertDialog(
        onDismissRequest = {
            autoTransitionJob?.cancel()
            onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text(
                text = when (step) {
                    1 -> "Verify Current Pattern"
                    2 -> "Draw New Pattern"
                    else -> "Confirm New Pattern"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (step) {
                        1 -> "Swipe across nodes in your current pattern sequence."
                        2 -> "Swipe to connect at least 4 nodes in a freehand pattern."
                        else -> "Swipe across the same pattern node sequence to confirm."
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.runtime.key(step, resetCounter) {
                        PatternCanvas(
                            isVibrationEnabled = true,
                            isWrong = isWrong,
                            onPatternComplete = { dots ->
                                if (dots.size < 4) {
                                    isWrong = true
                                    errorMsg = "Connect at least 4 dots"
                                    currentDrawnDots = emptyList()
                                    resetCounter++
                                    coroutineScope.launch {
                                        delay(1500)
                                        isWrong = false
                                    }
                                } else {
                                    currentDrawnDots = dots
                                    errorMsg = ""
                                    autoTransitionJob?.cancel()
                                    autoTransitionJob = coroutineScope.launch {
                                        // Wait 2 seconds (matching when canvas lines disappear)
                                        delay(2000)
                                        processDrawnPattern(dots)
                                    }
                                }
                            },
                            onResetWrongState = {
                                isWrong = false
                                errorMsg = ""
                                autoTransitionJob?.cancel()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            autoTransitionJob?.cancel()
                            currentDrawnDots = emptyList()
                            resetCounter++
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear Path")
                    }

                    if (currentDrawnDots.isNotEmpty()) {
                        Text(
                            text = "${currentDrawnDots.size} nodes connected",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "Swipe to draw",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    autoTransitionJob?.cancel()
                    if (currentDrawnDots.size < 4) {
                        errorMsg = "Connect at least 4 dots"
                    } else {
                        processDrawnPattern(currentDrawnDots)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.example.ui.theme.LocalAccentColor.current,
                    contentColor = if (com.example.ui.theme.LocalAccentColor.current.luminance() > 0.5f) Color.Black else Color.White
                ),
                modifier = Modifier.testTag("pattern_flow_confirm")
            ) {
                Text(if (step == 3) "Save Pattern" else "Continue")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    autoTransitionJob?.cancel()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = com.example.ui.theme.LocalAccentColor.current
                ),
                modifier = Modifier.testTag("pattern_flow_cancel")
            ) {
                Text("Cancel")
            }
        }
    )
}

// 3. SECURITY QUESTION SETUP DIALOG
@Composable
fun SecurityQuestionSetupDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentQuestion by viewModel.securityQuestion.collectAsStateWithLifecycle()

    val questions = listOf(
        "What was the name of your first childhood pet?",
        "In which specific town or city did your parents meet?",
        "What was the mascot of your high school?",
        "Write my own custom question..."
    )

    var isEditing by remember { mutableStateOf(currentQuestion == null) }
    var expandedSpinner by remember { mutableStateOf(false) }
    var selectedQuestionIndex by remember { mutableStateOf(0) }
    var customQuestionText by remember { mutableStateOf("") }
    var customAnswer by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    // Synchronize editing state if the question database state changes
    LaunchedEffect(currentQuestion) {
        if (currentQuestion == null) {
            isEditing = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        icon = {
            Icon(
                imageVector = if (isEditing) Icons.Default.Lock else Icons.Default.Shield,
                contentDescription = "Security Shield",
                tint = com.example.ui.theme.LocalAccentColor.current,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = if (!isEditing) "Security Recovery Question" else "Setup Recovery Question",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (!isEditing && currentQuestion != null) {
                    Text(
                        text = "Your offline password recovery question is currently active. You can reset or update your question and secret answer below.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, com.example.ui.theme.LocalAccentColor.current.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "ACTIVE QUESTION:",
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.LocalAccentColor.current,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentQuestion?.question ?: "",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    Text(
                        text = "If you forget your lock code, this security question provides an offline fallback to reset your credentials. Answers are locally secured.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    // Custom Dropdown Spinner
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedSpinner = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("question_spinner_trigger"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = questions[selectedQuestionIndex],
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = com.example.ui.theme.LocalAccentColor.current)
                            }
                        }

                        DropdownMenu(
                            expanded = expandedSpinner,
                            onDismissRequest = { expandedSpinner = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            questions.forEachIndexed { index, q ->
                                DropdownMenuItem(
                                    text = { Text(q, fontSize = 13.sp) },
                                    onClick = {
                                        selectedQuestionIndex = index
                                        expandedSpinner = false
                                        errorMsg = ""
                                    }
                                )
                            }
                        }
                    }

                    // If "Write my own" is selected, show custom input field
                    if (selectedQuestionIndex == questions.size - 1) {
                        OutlinedTextField(
                            value = customQuestionText,
                            onValueChange = {
                                customQuestionText = it
                                errorMsg = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_question_input"),
                            label = { Text("Custom Security Question") },
                            placeholder = { Text("e.g., What is my grandmother's dream?") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = com.example.ui.theme.LocalAccentColor.current,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = com.example.ui.theme.LocalAccentColor.current,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = com.example.ui.theme.LocalAccentColor.current,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }

                    // Answer entry textfield
                    OutlinedTextField(
                        value = customAnswer,
                        onValueChange = {
                            customAnswer = it
                            errorMsg = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("question_answer_input"),
                        label = { Text("Your Secret Answer") },
                        placeholder = { Text("Must be at least 3 characters") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = com.example.ui.theme.LocalAccentColor.current,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = com.example.ui.theme.LocalAccentColor.current,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            cursorColor = com.example.ui.theme.LocalAccentColor.current,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    if (errorMsg.isNotEmpty()) {
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!isEditing) {
                Button(
                    onClick = {
                        isEditing = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.ui.theme.LocalAccentColor.current,
                        contentColor = if (com.example.ui.theme.LocalAccentColor.current.luminance() > 0.5f) Color.Black else Color.White
                    ),
                    modifier = Modifier.testTag("question_edit_button")
                ) {
                    Text("Update Question")
                }
            } else {
                Button(
                    onClick = {
                        val finalQuestion = if (selectedQuestionIndex == questions.size - 1) {
                            customQuestionText.trim()
                        } else {
                            questions[selectedQuestionIndex]
                        }

                        if (selectedQuestionIndex == questions.size - 1 && finalQuestion.isEmpty()) {
                            errorMsg = "Custom question cannot be empty"
                            return@Button
                        }

                        if (customAnswer.trim().length < 3) {
                            errorMsg = "Answer must be at least 3 characters long"
                            return@Button
                        }

                        viewModel.setupSecurityQuestion(finalQuestion, customAnswer)
                        Toast.makeText(context, "Recovery question updated successfully!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.ui.theme.LocalAccentColor.current,
                        contentColor = if (com.example.ui.theme.LocalAccentColor.current.luminance() > 0.5f) Color.Black else Color.White
                    ),
                    modifier = Modifier.testTag("question_save_button")
                ) {
                    Text("Save Recovery")
                }
            }
        },
        dismissButton = {
            if (!isEditing && currentQuestion != null) {
                TextButton(
                    onClick = {
                        viewModel.deleteSecurityQuestion()
                        Toast.makeText(context, "Recovery question disabled & reset!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("question_reset_button")
                ) {
                    Text("Disable & Reset")
                }
            } else {
                TextButton(
                    onClick = {
                        if (currentQuestion != null) {
                            isEditing = false
                        } else {
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = com.example.ui.theme.LocalAccentColor.current
                    ),
                    modifier = Modifier.testTag("question_cancel_button")
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

// 4. THEME SELECTOR DIALOG
@Composable
fun ThemeSelectorDialog(
    currentValue: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text("Select Application Theme", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("LIGHT" to "Light Mode", "DARK" to "Dark Mode", "SYSTEM" to "System Settings Fallback").forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(option.first) }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                            .testTag("theme_opt_" + option.first),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option.first == currentValue,
                            onClick = { onSelected(option.first) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = com.example.ui.theme.LocalAccentColor.current,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(option.second, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = com.example.ui.theme.LocalAccentColor.current
                )
            ) { Text("Close") }
        }
    )
}

// 4b. ACCENT COLOR SELECTOR DIALOG
@Composable
fun AccentColorSelectorDialog(
    currentValue: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = com.example.ui.theme.LocalAccentColor.current
    // Dynamic theme selection depending on background color luminance
    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.run {
        0.299f * red + 0.587f * green + 0.114f * blue
    } < 0.5f
    val options = listOf(
        "Default" to "Default (Theme Default)",
        "#FFFF7A17" to "Claude Orange",
        "#10B981" to "Premium Green",
        "#E55050" to "Faded Red",
        "#007AFF" to "Apple Blue",
        "#8B5CF6" to "Electric Purple",
        "#EC4899" to "Neon Pink",
        "#06B6D4" to "Cyber Teal",
        "#D97706" to "Sahara Gold",
        "#6366F1" to "Lavender Blue",
        "#7E8C8D" to "Slate Gray",
        "#DC2626" to "Crimson Red",
        "#1E3A8A" to "Midnight Navy"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text("Select Accent Color", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { option ->
                    val colorObj = try {
                        if (option.first == "Default") {
                            if (isDark) Color.White else Color.Black
                        } else {
                            Color(android.graphics.Color.parseColor(option.first))
                        }
                    } catch (e: Exception) {
                        Color(0xFFFF7A17)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(option.first) }
                            .padding(vertical = 10.dp, horizontal = 8.dp)
                            .testTag("accent_opt_" + option.first.replace("#", "")),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option.first.equals(currentValue, ignoreCase = true),
                            onClick = { onSelected(option.first) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentColor,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        // Visual color circle preview
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(colorObj)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(option.second, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = accentColor
                )
            ) { Text("Close") }
        }
    )
}

// 4c. CLOCK STYLE SELECTOR DIALOG
@Composable
fun ClockStyleMiniaturePreview(style: String, accentColor: Color, isDark: Boolean) {
    val bgCard = if (isDark) Color(0xFF111111) else Color(0xFFF0F0F3)
    val textMain = if (isDark) Color.White else Color.Black
    
    Box(
        modifier = Modifier
            .size(width = 110.dp, height = 48.dp)
            .background(bgCard, RoundedCornerShape(8.dp))
            .border(1.5.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = textMain.copy(alpha = 0.25f),
                modifier = Modifier.size(8.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            
            when (style) {
                "Default" -> {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("09:41", color = textMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("AM", color = accentColor, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
                "Standard AM/PM" -> {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("09:41", color = textMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("AM", color = accentColor, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
                "24-Hour Digital" -> {
                    Text("21:41", color = textMain, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
                "Two-Line Stacked" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("09", color = textMain, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text("/", color = accentColor.copy(alpha = 0.5f), fontSize = 9.sp, modifier = Modifier.padding(horizontal = 2.dp))
                        Text("41", color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                "Retro Minimalist hh.mm" -> {
                    Text("09.41", color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
                "Dotted Center hh•mm" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("09", color = textMain, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.padding(horizontal = 3.dp).size(4.dp).background(accentColor, CircleShape))
                        Text("41", color = textMain, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                "Military Modern" -> {
                    Text("0941", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                }
                "Seconds Accent" -> {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("09:41", color = textMain, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("35", color = accentColor, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
                "Aesthetic Dash" -> {
                    Text("09—41", color = textMain, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                }
                "Double Bracket" -> {
                    Text("[ 09:41 ]", color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                "Lowercase Modern" -> {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("09:41", color = textMain, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("am", color = accentColor, fontSize = 7.sp, fontWeight = FontWeight.Normal)
                    }
                }
                else -> {
                    Text("09:41", color = textMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ClockStyleSelectorDialog(
    currentValue: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val accentColor = com.example.ui.theme.LocalAccentColor.current
    // Dynamic theme selection depending on background color luminance
    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.run {
        0.299f * red + 0.587f * green + 0.114f * blue
    } < 0.5f
    val options = listOf(
        "Default",
        "Standard AM/PM",
        "24-Hour Digital",
        "Two-Line Stacked",
        "Retro Minimalist hh.mm",
        "Dotted Center hh•mm",
        "Military Modern",
        "Seconds Accent",
        "Aesthetic Dash",
        "Double Bracket",
        "Lowercase Modern"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text("Select Lock Clock Style", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(style) }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag("clock_style_opt_" + style.replace(" ", "_")),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            RadioButton(
                                selected = style == currentValue,
                                onClick = { onSelected(style) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = accentColor,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = style,
                                fontSize = 13.sp,
                                fontWeight = if (style == currentValue) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        ClockStyleMiniaturePreview(
                            style = style,
                            accentColor = accentColor,
                            isDark = isDark
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = accentColor
                )
            ) { Text("Close") }
        }
    )
}

// 5. LOCKOUT TIMEOUT SELECTOR DIALOG
@Composable
fun TimeoutSelectorDialog(
    currentValue: Long,
    onSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val timeoutOptions = listOf(
        0L to "Immediate Lockout",
        60000L to "1 Minute buffer",
        300000L to "5 Minutes buffer",
        600000L to "10 Minutes buffer"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text("Configure Lockout Timeout", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                timeoutOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(option.first) }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                            .testTag("timeout_opt_" + option.first),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option.first == currentValue,
                            onClick = { onSelected(option.first) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = com.example.ui.theme.LocalAccentColor.current,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(option.second, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = com.example.ui.theme.LocalAccentColor.current
                )
            ) { Text("Close") }
        }
    )
}

@Composable
fun PermissionStatusRow(
    title: String,
    description: String,
    isChecked: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = CircleShape,
            color = if (isChecked) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
            },
            modifier = Modifier.clickable { onClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isChecked) "Enabled" else "Configure",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun hasUsageStatsPermission(context: android.content.Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    }
    return mode == android.app.AppOpsManager.MODE_ALLOWED
}

@Composable
fun PasswordChangeDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasOldPassword = remember { viewModel.getPasswordHash() != null }
    
    var step by remember { mutableStateOf(if (hasOldPassword) 1 else 2) } // Step 1: verify, Step 2: enter new, Step 3: confirm
    
    var inputVal by remember { mutableStateOf("") }
    var newPasswordEntered by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text(
                text = when (step) {
                    1 -> "Verify Current Password"
                    2 -> "Enter New Password"
                    else -> "Confirm New Password"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = when (step) {
                        1 -> "Verify your existing secure alphanumeric password."
                        2 -> "Type a safe new lock password (minimum 4 characters)."
                        else -> "Re-enter the new password to eliminate typos."
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = inputVal,
                    onValueChange = {
                        inputVal = it
                        errorMsg = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_flow_input"),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    placeholder = { Text("Enter Password (min 4 chars)") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = com.example.ui.theme.LocalAccentColor.current,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = com.example.ui.theme.LocalAccentColor.current,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = com.example.ui.theme.LocalAccentColor.current,
                        errorBorderColor = com.example.ui.theme.LocalAccentColor.current,
                        errorLabelColor = com.example.ui.theme.LocalAccentColor.current,
                        errorCursorColor = com.example.ui.theme.LocalAccentColor.current,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent
                    ),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                tint = com.example.ui.theme.LocalAccentColor.current
                            )
                        }
                    }
                )

                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inputVal.length < 4) {
                        errorMsg = "Password must be at least 4 characters"
                        return@Button
                    }
                    when (step) {
                        1 -> {
                            val verified = HashUtil.verify(inputVal, viewModel.getPasswordHash() ?: "")
                            if (verified) {
                                step = 2
                                inputVal = ""
                            } else {
                                errorMsg = "Incorrect existing password"
                            }
                        }
                        2 -> {
                            newPasswordEntered = inputVal
                            step = 3
                            inputVal = ""
                        }
                        3 -> {
                            if (inputVal == newPasswordEntered) {
                                viewModel.savePassword(inputVal)
                                Toast.makeText(context, "Password lock updated successfully!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                errorMsg = "Passwords do not match. Please restart password setup."
                                step = 2
                                inputVal = ""
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.example.ui.theme.LocalAccentColor.current,
                    contentColor = if (com.example.ui.theme.LocalAccentColor.current.luminance() > 0.5f) Color.Black else Color.White
                ),
                modifier = Modifier.testTag("password_flow_confirm")
            ) {
                Text(if (step == 3) "Save Password" else "Continue")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = com.example.ui.theme.LocalAccentColor.current
                ),
                modifier = Modifier.testTag("password_flow_cancel")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun IntruderGalleryDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val storageDir = remember { File(context.filesDir, "intruder_selfies") }
    
    var selfieFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedFileForFullView by remember { mutableStateOf<File?>(null) }

    fun refreshSelfies() {
        if (storageDir.exists()) {
            selfieFiles = (storageDir.listFiles { file -> file.isFile && file.extension == "jpg" } ?: emptyArray())
                .sortedByDescending { it.lastModified() }
        } else {
            selfieFiles = emptyList()
        }
    }

    LaunchedEffect(Unit) {
        refreshSelfies()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Intruder Selfies Log",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selfieFiles.isNotEmpty()) {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                        onClick = {
                            selfieFiles.forEach { it.delete() }
                            refreshSelfies()
                        }
                    ) {
                        Text("Clear All")
                    }
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                if (selfieFiles.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No intruder selfies captured yet",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(selfieFiles.size) { index ->
                            val file = selfieFiles[index]
                            val timestampString = remember(file) {
                                try {
                                    val time = file.lastModified()
                                    val date = Date(time)
                                    SimpleDateFormat("MMM d, yyyy - hh:mm a", Locale.getDefault()).format(date)
                                } catch (e: Exception) {
                                    "Unknown date"
                                }
                            }
                            
                            val bitmap = remember(file) {
                                try {
                                    android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFileForFullView = file },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                    ) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Intruder image",
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Gray.copy(alpha = 0.3f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                file.delete()
                                                refreshSelfies()
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(28.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = timestampString,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = com.example.ui.theme.LocalAccentColor.current
                )
            ) {
                Text("Close")
            }
        }
    )

    if (selectedFileForFullView != null) {
        val file = selectedFileForFullView!!
        val bitmap = remember(file) {
            try {
                android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                null
            }
        }
        
        AlertDialog(
            onDismissRequest = { selectedFileForFullView = null },
            title = {
                Text("Intruder Selfie Snapshot", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Full render",
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedFileForFullView = null }) {
                    Text("Back")
                }
            }
        )
    }
}

@Composable
fun SettingsLockTypeColorSelectorRow(
    viewModel: MainViewModel,
    onColorChange: (String) -> Unit
) {
    val context = LocalContext.current
    var currentColorHex by remember { mutableStateOf(viewModel.encryptedPrefs.getSettingsLockTypeColor()) }
    
    val predefinedColors = listOf(
        "Default (Black)" to "#000000",
        "White" to "#FFFFFF",
        "Classic Orange" to "#FFFF7A17",
        "Slate Red" to "#FFE53E3E",
        "Ocean Blue" to "#FF3182CE",
        "Emerald Green" to "#FF319795",
        "Neon Purple" to "#FF805AD5",
        "Amber Yellow" to "#FFD69E2E"
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Protocol Selection Accent Color",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Reset Default",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable {
                        currentColorHex = "Default"
                        viewModel.encryptedPrefs.saveSettingsLockTypeColor("Default")
                        onColorChange("Default")
                        Toast.makeText(context, "Color reset to default", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Swatches
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            predefinedColors.forEach { (name, hex) ->
                val isSelected = currentColorHex == hex
                val displayColor = if (hex == "Default" || hex == "#000000") {
                    Color.Black
                } else {
                    try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.White }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(displayColor)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                        .clickable {
                            currentColorHex = hex
                            viewModel.encryptedPrefs.saveSettingsLockTypeColor(hex)
                            onColorChange(hex)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (hex == "#FFFFFF") {
                        Text("W", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    } else if (hex == "#000000") {
                        Text("B", color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    } else if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (hex == "#FFFFFF") Color.Black else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Slider for Hue
        val validHexForSlider = if (currentColorHex == "Default" || currentColorHex == "#FFFFFF" || currentColorHex == "#000000") "#FFFF7A17" else currentColorHex
        var activeHue by remember(currentColorHex) { mutableStateOf(settingsHueFromColorHex(validHexForSlider)) }
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Fine Tune Color Hue Slider",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
                                    Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF),
                                    Color(0xFFFF0000)
                                )
                            )
                        )
                )
                Slider(
                    value = activeHue,
                    onValueChange = { hue ->
                        activeHue = hue
                        val hex = settingsColorHexFromHue(hue)
                        currentColorHex = hex
                        viewModel.encryptedPrefs.saveSettingsLockTypeColor(hex)
                        onColorChange(hex)
                    },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                        thumbColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun settingsHueFromColorHex(hex: String): Float {
    return try {
        val colorInt = android.graphics.Color.parseColor(hex)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(colorInt, hsv)
        hsv[0]
    } catch (e: Exception) {
        0f
    }
}

private fun settingsColorHexFromHue(hue: Float): String {
    val hsv = floatArrayOf(hue, 1f, 1f)
    val colorInt = android.graphics.Color.HSVToColor(hsv)
    return String.format("#FF%06X", 0xFFFFFF and colorInt)
}

private fun settingsIsColorLight(color: Color): Boolean {
    val luminance = 0.2126 * color.red + 0.7152 * color.green + 0.0722 * color.blue
    return luminance > 0.45
}

@Composable
fun ReLockBehaviorSelectorDialog(
    currentValue: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        "Until Screen Off" to "Once Unlocked, Stays Unlocked (Until Screen Off)",
        "Lock When Exit" to "Lock app when exit the app"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text(
                text = "Configure Locking Behavior",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(option.first) }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                            .testTag("relock_opt_" + option.first),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option.first == currentValue,
                            onClick = { onSelected(option.first) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = com.example.ui.theme.LocalAccentColor.current,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = option.second,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = com.example.ui.theme.LocalAccentColor.current)
            ) {
                Text("Dismiss", fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun Context.findActivity(): androidx.fragment.app.FragmentActivity? {
    var cur = this
    while (cur is android.content.ContextWrapper) {
        if (cur is androidx.fragment.app.FragmentActivity) {
            return cur
        }
        cur = cur.baseContext
    }
    return null
}

