package com.example.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.util.RealmeOemHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Onboarding States
enum class AppScreenState {
    SPLASH,
    INTRO,
    PERMISSIONS,
    MAIN
}

@Composable
fun SplashScreenContent(
    isFirstLaunch: Boolean,
    onTransition: (AppScreenState) -> Unit
) {
    val transition = rememberInfiniteTransition(label = "splash_mark")
    val markScale by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splash_mark_scale"
    )

    LaunchedEffect(Unit) {
        delay(if (isFirstLaunch) 1_500L else 900L)
        onTransition(if (isFirstLaunch) AppScreenState.INTRO else AppScreenState.MAIN)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 35.dp, end = 6.dp)
                .size(190.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 34.dp)
                .size(150.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f))
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .size(104.dp)
                    .graphicsLayer { scaleX = markScale; scaleY = markScale }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SafeLockMark(
                        modifier = Modifier.size(58.dp),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "SafeLock shield"
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "SafeLock",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "A little more peace of mind.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(30.dp))
            PageEyebrow(text = "PRIVATE BY DESIGN")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntroScreensContent(
    onFinished: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val introPages = listOf(
        IntroPageData(
            title = "A thoughtful little shield",
            description = stringResource(R.string.intro_protection),
            icon = Icons.Default.Shield,
            colorAccent = MaterialTheme.colorScheme.primary
        ),
        IntroPageData(
            title = "Know when someone tries",
            description = stringResource(R.string.intro_camera),
            icon = Icons.Default.CameraAlt,
            colorAccent = MaterialTheme.colorScheme.tertiary
        ),
        IntroPageData(
            title = "Protection that fits you",
            description = "Choose one unlock method or combine a few. Your security should work the way you do.",
            icon = Icons.Default.Fingerprint,
            colorAccent = MaterialTheme.colorScheme.secondary
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 22.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SafeLockMark(modifier = Modifier.size(26.dp), contentDescription = null)
            Spacer(modifier = Modifier.width(9.dp))
            Text("SafeLock", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onFinished) {
                Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            val pageData = introPages[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                FeatureIllustration(
                    icon = pageData.icon,
                    accent = pageData.colorAccent,
                    modifier = Modifier.size(196.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                PageEyebrow(text = "A SAFER EVERYDAY")
                Spacer(modifier = Modifier.height(11.dp))
                Text(
                    text = pageData.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = pageData.description,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 25.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val selected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (selected) 24.dp else 7.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "intro_page_dot"
                    )
                    Box(
                        modifier = Modifier
                            .height(7.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        }
                    ) {
                        Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else onFinished()
                    },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 13.dp)
                ) {
                    Text(if (pagerState.currentPage == 2) "Continue" else "Next")
                    Spacer(modifier = Modifier.width(7.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

data class IntroPageData(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val colorAccent: androidx.compose.ui.graphics.Color
)

@Composable
fun OnboardingPermissionsScreenContent(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isUsageStatsActive by remember { mutableStateOf(false) }
    var isOverlayActive by remember { mutableStateOf(false) }
    var isCameraActive by remember { mutableStateOf(false) }
    var isNotificationActive by remember { mutableStateOf(false) }

    val updateChecks = {
        isUsageStatsActive = hasUsageStatsPermission(context)
        isOverlayActive = Settings.canDrawOverlays(context)
        isCameraActive = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        isNotificationActive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    LaunchedEffect(Unit) {
        updateChecks()
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                updateChecks()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Contracts
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isCameraActive = isGranted
        updateChecks()
        if (isGranted) {
            Toast.makeText(context, "Camera permission active", Toast.LENGTH_SHORT).show()
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isNotificationActive = isGranted
        updateChecks()
        if (isGranted) {
            Toast.makeText(context, "Notification permission active", Toast.LENGTH_SHORT).show()
        }
    }

    // Determine if mandatory is active (either Notification on older SDK is fine, UsageStats & Overlay are mandatory everywhere)
    val mandatoryPermissionsActive = isUsageStatsActive && isOverlayActive && isNotificationActive

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FeatureIllustration(
                icon = Icons.Default.Shield,
                modifier = Modifier.size(132.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            PageEyebrow(text = "ONE LAST STEP")
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = "Give SafeLock the access it needs",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = "A couple of Android permissions help your shield recognize when a protected app opens.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Required access is explained before each Android settings hand-off.
            Text(
                text = "REQUIRED ACCESS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 10.dp)
            )

            // 1. Usage Stats Service Card
            PermissionCard(
                title = "Usage Stats Monitoring",
                description = "Required to accurately and safely detect foreground application changes.",
                icon = Icons.Default.QueryStats,
                isActive = isUsageStatsActive,
                onClick = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                    Toast.makeText(context, "Enable Usage Access for 'SafeLock'", Toast.LENGTH_LONG).show()
                },
                accentColor = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionCard(
                title = "Display Over Other Apps",
                description = "Required to securely display the lock screen over restricted background transitions.",
                icon = Icons.Default.OpenInNew,
                isActive = isOverlayActive,
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:" + context.packageName)
                    )
                    context.startActivity(intent)
                    Toast.makeText(context, "Enable Display Over Other Apps for 'SafeLock'", Toast.LENGTH_LONG).show()
                },
                accentColor = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Notification Card (For Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionCard(
                    title = "Notifications Delivery",
                    description = "Required to run foreground security watchdogs stably in Android 13+.",
                    icon = Icons.Default.NotificationsActive,
                    isActive = isNotificationActive,
                    onClick = {
                        try {
                            notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.parse("package:" + context.packageName)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                Toast.makeText(context, "Please grant notifications in settings manually.", Toast.LENGTH_LONG).show()
                            } catch (ex: Exception) {
                                Toast.makeText(context, "Error launching settings.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    accentColor = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Text(
                text = "OPTIONAL FEATURES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 10.dp)
            )

            // 3. Camera Card (Optional)
            PermissionCard(
                title = "Camera Capture (Intruder)",
                description = "Required only if you wish to silently record front selfies on wrong unlock attempts.",
                icon = Icons.Default.CameraAlt,
                isActive = isCameraActive,
                onClick = {
                    try {
                        cameraLauncher.launch(android.Manifest.permission.CAMERA)
                    } catch (e: Exception) {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.parse("package:" + context.packageName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            Toast.makeText(context, "Please grant camera in settings manually.", Toast.LENGTH_LONG).show()
                        } catch (ex: Exception) {
                            Toast.makeText(context, "Error launching settings.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                accentColor = com.example.ui.theme.LocalAccentColor.current
            )
        }

        // Floating Footer for Skip & Continue triggers
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!mandatoryPermissionsActive) {
                    Text(
                        text = "Usage access and display-over-apps are needed for locking to work. You can continue setup now.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "All required access is ready. Your shield is good to go.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = {
                        onFinished()
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("onboarding_continue")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Continue",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    val cardColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = tween(220),
        label = "permission_card_color"
    )
    val borderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isActive) accentColor.copy(alpha = 0.32f) else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(220),
        label = "permission_border_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .border(1.dp, borderColor, MaterialTheme.shapes.large)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.secondary else accentColor,
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isActive) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open permission settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                    modifier = Modifier.size(22.dp)
                )
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
