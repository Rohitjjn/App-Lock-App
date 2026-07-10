package com.example


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.KeepAliveService
import com.example.ui.AppListScreen
import com.example.ui.MainViewModel
import com.example.ui.SettingsScreen
import com.example.ui.AppScreenState
import com.example.ui.SplashScreenContent
import com.example.ui.IntroScreensContent
import com.example.ui.OnboardingPermissionsScreenContent
import com.example.ui.theme.MyApplicationTheme
import com.example.util.RealmeOemHelper

class MainActivity : FragmentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Actively cancel keep-alive service notification and stop it if running to satisfy user requirement
        try {
            val nm = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            nm?.cancel(8812)
            stopService(android.content.Intent(this, KeepAliveService::class.java))
        } catch (e: Exception) {
            // Safe fall-through
        }

        // Initialize shared MainViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setContent {
            // Read theme preferences reactively and map to Theme configuration
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val accentColorHex by viewModel.accentColorHex.collectAsStateWithLifecycle()
            val isSystemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemDark
            }

            MyApplicationTheme(darkTheme = useDarkTheme, accentColorHex = accentColorHex) {
                MainLayout(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasUsageStatsPermission(this)) {
            try {
                com.example.service.AppDetectorService.start(this)
            } catch (e: Exception) {}
        }
    }

    private fun startKeepAliveCore() {
        // Disabled permanently to remove the persistent foreground notification on startup
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun MainLayout(viewModel: MainViewModel) {
        val context = LocalContext.current
        val isFirstLaunch = remember { viewModel.isFirstLaunch() }
        var currentScreenState by remember { mutableStateOf(AppScreenState.SPLASH) }

        Crossfade(targetState = currentScreenState, label = "screen_navigation") { screenState ->
            when (screenState) {
                AppScreenState.SPLASH -> {
                    SplashScreenContent(
                        isFirstLaunch = isFirstLaunch,
                        onTransition = { currentScreenState = it }
                    )
                }
                AppScreenState.INTRO -> {
                    IntroScreensContent(
                        onFinished = { currentScreenState = AppScreenState.PERMISSIONS }
                    )
                }
                AppScreenState.PERMISSIONS -> {
                    OnboardingPermissionsScreenContent(
                        onFinished = {
                            viewModel.setFirstLaunch(false)
                            currentScreenState = AppScreenState.MAIN
                        }
                    )
                }
                AppScreenState.MAIN -> {
                    val appLaunchSecEnabled by viewModel.isAppLaunchBiometricEnabled.collectAsStateWithLifecycle()
                    val hasAnyLock = remember { viewModel.getPinHash() != null || viewModel.getPatternHash() != null || viewModel.getPasswordHash() != null }
                    var isAppUnlocked by remember { mutableStateOf(!appLaunchSecEnabled || !hasAnyLock) }

                    if (appLaunchSecEnabled && hasAnyLock && !isAppUnlocked) {
                        val appName = remember {
                            try {
                                context.getString(com.example.R.string.app_name)
                            } catch (e: Exception) {
                                "SafeLock"
                            }
                        }
                        val appIconBytes = remember {
                            try {
                                context.packageManager.getApplicationIcon(context.packageName)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        val configuredMethod = remember { viewModel.lockMethod.value }
                        val pinHash = remember { viewModel.getPinHash() }
                        val patternHash = remember { viewModel.getPatternHash() }
                        val isVibrationEnabled = remember { viewModel.vibrationEnabled.value }

                        val executor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }
                        val biometricHelper = remember { com.example.util.BiometricHelper() }

                        com.example.ui.LockScreen(
                            appName = appName,
                            appIcon = appIconBytes,
                            configuredLockMethod = configuredMethod,
                            pinHash = pinHash,
                            patternHash = patternHash,
                            isVibrationEnabled = isVibrationEnabled,
                            onUnlockSuccess = { isAppUnlocked = true },
                            onIncorrectAttempt = {
                                if (isVibrationEnabled) {
                                    try {
                                        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                                            vibratorManager?.defaultVibrator
                                        } else {
                                            @Suppress("DEPRECATION")
                                            context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                        }
                                        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(80, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            },
                            onTriggerBiometric = { onSuccessCallback ->
                                val fragmentActivity = context.findActivity()
                                if (fragmentActivity != null) {
                                    biometricHelper.authenticate(
                                        activity = fragmentActivity,
                                        executor = executor,
                                        title = "SafeLock Shield",
                                        subtitle = "Verify with biometric key to open SafeLock",
                                        onSuccess = {
                                            fragmentActivity.runOnUiThread {
                                                onSuccessCallback()
                                            }
                                        },
                                        onError = { _, _ -> },
                                        onFailed = {}
                                    )
                                }
                            },
                            onCancel = {
                                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_HOME)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(homeIntent)
                            }
                        )
                    } else {
                        MainAppScaffold(viewModel = viewModel)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
    @Composable
    fun MainAppScaffold(viewModel: MainViewModel) {
        val context = LocalContext.current
        var selectedTab by remember { mutableStateOf(0) }
        var showDiagnosticDialog by remember { mutableStateOf(false) }

        // Automatically prompt diagnostic check if usage stats is inactive inside the main app
        LaunchedEffect(Unit) {
            val isUsageStatsActive = hasUsageStatsPermission(context)
            if (!isUsageStatsActive) {
                showDiagnosticDialog = true
            } else {
                com.example.service.AppDetectorService.start(context)
            }
        }

        Scaffold(
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav"),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("App List") },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Filled.Lock else Icons.Outlined.Lock,
                                contentDescription = "App list protection tab"
                            )
                        },
                        modifier = Modifier.testTag("tab_app_list")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("Settings") },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Security and locks settings tab"
                            )
                        },
                        modifier = Modifier.testTag("tab_settings")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (selectedTab == 0) {
                    AppListScreen(viewModel = viewModel)
                } else {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }

        // Diagnostics Dashboard launched from floating hub or configurations
        if (showDiagnosticDialog) {
            DiagnosticsDashboardDialog(onDismiss = { showDiagnosticDialog = false })
        }
    }

    // HIGH-AVAILABILITY SECURITY CONTROL AND FIRST-START PERMISSIONS WIZARD
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DiagnosticsDashboardDialog(onDismiss: () -> Unit) {
        val context = LocalContext.current
        val isFirst = remember { viewModel.isFirstLaunch() }

        var isUsageStatsActive by remember { mutableStateOf(false) }
        var isOverlayActive by remember { mutableStateOf(false) }
        var isCameraPermActive by remember { mutableStateOf(false) }
        var isNotificationPermActive by remember { mutableStateOf(false) }
        var showOemInstructionsDialog by remember { mutableStateOf(false) }

        val checkPermissions = {
            isUsageStatsActive = hasUsageStatsPermission(context)
            isOverlayActive = Settings.canDrawOverlays(context)
            isCameraPermActive = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            
            isNotificationPermActive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        LaunchedEffect(Unit) {
            checkPermissions()
        }

        // Action launcher contracts
        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            isCameraPermActive = isGranted
            viewModel.updateIntruderSelfieEnabled(isGranted)
            if (isGranted) {
                Toast.makeText(context, "Camera permission granted. Capturing selfies enabled!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Camera is optional for intruder capture.", Toast.LENGTH_LONG).show()
            }
        }

        val notificationLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            isNotificationPermActive = isGranted
            if (isGranted) {
                Toast.makeText(context, "Notification permission granted!", Toast.LENGTH_SHORT).show()
            }
        }

        AlertDialog(
            onDismissRequest = {
                // Do not dismiss wizard on outside clicks unless usage stats is active
                if (!isFirst || isUsageStatsActive) {
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxWidth(0.95f),
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = !isFirst || isUsageStatsActive,
                dismissOnClickOutside = !isFirst || isUsageStatsActive
            ),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isFirst) "SafeLock Protection Setup" else "Diagnostics Engine",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.testTag("app_title")
                        )
                        if (isFirst) {
                            Text(
                                text = "Setup all permissions easily in one single panel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                    IconButton(onClick = { checkPermissions() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh status")
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(440.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (isFirst) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "⚡ Welcome to SafeLock App Protection",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Please complete the setup to start locking applications. Usage Stats Monitoring is required to protect your privacy.",
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "CORE REQUISITES (MANDATORY)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 1. Usage Stats Service Card
                    item {
                        StatusIndicatorCard(
                            title = "Usage Stats Monitoring",
                            description = "Required to instantly intercept and secure access to selected apps.",
                            isActive = isUsageStatsActive,
                            activeLabel = "ACTIVE & PROTECTING",
                            inactiveLabel = "DISABLED",
                            testTag = "usagestats_status_card"
                        )
                        if (!isUsageStatsActive) {
                            Spacer(modifier = Modifier.height(6.dp))
                            ActionLaunchCard(
                                title = "Configure & Enable Usage Access",
                                icon = Icons.Default.Settings,
                                onClick = {
                                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    context.startActivity(intent)
                                },
                                tag = "usagestats_btn"
                            )
                        }
                    }

                    item {
                        StatusIndicatorCard(
                            title = "Display Over Other Apps",
                            description = "Required to securely display the lock screen over restricted background transitions.",
                            isActive = isOverlayActive,
                            activeLabel = "OVERLAY PERMITTED",
                            inactiveLabel = "BLOCKED",
                            testTag = "overlay_status_card"
                        )
                        if (!isOverlayActive) {
                            Spacer(modifier = Modifier.height(6.dp))
                            ActionLaunchCard(
                                title = "Enable Display Over Other Apps",
                                icon = Icons.Default.OpenInNew,
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:" + context.packageName)
                                    )
                                    context.startActivity(intent)
                                },
                                tag = "overlay_btn"
                            )
                        }
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "INTEGRATED PROTECTION PERMISSIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 2. Camera Permission Card (Intruder selfie)
                    item {
                        StatusIndicatorCard(
                            title = "Camera Permission",
                            description = "Necessary for silently capturing selfie logs on wrong attempt violations.",
                            isActive = isCameraPermActive,
                            activeLabel = "SELFIE SNAP PREPARED",
                            inactiveLabel = "DISABLED (Selfies Offline)",
                            testTag = "camera_status_card"
                        )
                        if (!isCameraPermActive) {
                            Spacer(modifier = Modifier.height(6.dp))
                            ActionLaunchCard(
                                title = "Grant Camera Permission Inline",
                                icon = Icons.Default.CameraAlt,
                                onClick = { cameraLauncher.launch(android.Manifest.permission.CAMERA) },
                                tag = "camera_grant_btn"
                            )
                        }
                    }

                    // 3. Post Notifications Permission Card
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        item {
                            StatusIndicatorCard(
                                title = "Notification Alerting",
                                description = "Allows real-time foreground security alerts on app events.",
                                isActive = isNotificationPermActive,
                                activeLabel = "NOTIFICATIONS ENABLED",
                                inactiveLabel = "BLOCKED",
                                testTag = "notification_status_card"
                            )
                            if (!isNotificationPermActive) {
                                Spacer(modifier = Modifier.height(6.dp))
                                ActionLaunchCard(
                                    title = "Grant Notifications Inline",
                                    icon = Icons.Default.Notifications,
                                    onClick = { notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) },
                                    tag = "notifications_grant_btn"
                                )
                            }
                        }
                    }                    // OEM instructions Card
                    item {
                        Button(
                            onClick = { showOemInstructionsDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("oem_guide_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = "Open guide")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "View ColorOS Background Guide", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isUsageStatsActive) {
                            Toast.makeText(context, "Usage Stats Monitoring is mandatory to activate locking!", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (!isOverlayActive) {
                            Toast.makeText(context, "Display Over Other Apps is mandatory to show the lock screen!", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (isFirst) {
                            viewModel.setFirstLaunch(false)
                            Toast.makeText(context, "Setup secure lock wizard finished!", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    },
                    modifier = Modifier.testTag("diagnostics_dismiss_btn")
                ) {
                    Text(if (isFirst) "Complete Setup & Guard" else "Close Panel")
                }
            }
        )

        // OEM Instructions Dialog
        if (showOemInstructionsDialog) {
            AlertDialog(
                onDismissRequest = { showOemInstructionsDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "ColorOS Background Guide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.height(280.dp)
                    ) {
                        Text(
                            text = "To keep App Lock working without background terminations on ColorOS / Realme:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        RealmeOemHelper.getRealmeSettingsInstructions().forEachIndexed { index, instruction ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (index + 1).toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = instruction,
                                        style = MaterialTheme.typography.bodySmall,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showOemInstructionsDialog = false },
                        modifier = Modifier.testTag("oem_dialog_confirm")
                    ) {
                        Text("I Understand")
                    }
                }
            )
        }
    }

    @Composable
    fun StatusIndicatorCard(
        title: String,
        description: String,
        isActive: Boolean,
        activeLabel: String,
        inactiveLabel: String,
        testTag: String
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Status pill
                    val pillColor = if (isActive) Color(0xFF2E7D32) else Color(0xFFC62828)
                    val textPillColor = Color.White
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(pillColor)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isActive) activeLabel else inactiveLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = textPillColor,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }

    @Composable
    fun ActionLaunchCard(
        title: String,
        icon: ImageVector,
        description: String? = null,
        onClick: () -> Unit,
        tag: String
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .testTag(tag),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
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
}
