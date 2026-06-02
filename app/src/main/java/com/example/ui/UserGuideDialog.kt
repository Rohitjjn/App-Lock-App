package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Backward-compatible Dialog wrapper if used anywhere else in the project.
 */
@Composable
fun UserGuideDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            UserGuideView(onDismiss = onDismiss)
        }
    }
}

/**
 * Main Full-Screen User Guide Composable.
 * Renders a highly polished, fully English setup and operations manual.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserGuideView(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("❓ How It Works", "🛡️ Feature Directory", "⚙️ Setup Guidelines")

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "SafeLock Operations Guide",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("guide_view_title")
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("guide_view_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Close Guide",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Segmented Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("guide_tab_row_view")
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        },
                        modifier = Modifier.testTag("guide_tab_$index")
                    )
                }
            }

            // Scrollable Manual Document Region
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GuideHeaderCard(selectedTab)

                    when (selectedTab) {
                        0 -> IntroTabContent()
                        1 -> FeaturesManualTabContent()
                        2 -> SetupFixesTabContent()
                    }

                    // Bottom Spacer to prevent cutoff by floating Android navigation overlays
                    Spacer(modifier = Modifier.height(110.dp))
                }
            }
        }
    }
}

@Composable
fun GuideHeaderCard(tabIndex: Int) {
    val subtitle = when (tabIndex) {
        0 -> "Learn how SafeLock's overlay background protection engine keeps your device securely shielded on a core system level."
        1 -> "Explore PIN codes, custom swipe patterns, bottom-relocated fingerprint triggers, and stealth photo logs."
        else -> "Crucial steps for persistent protection: system-level configurations, battery exemptions, and OEM overrides."
    }

    val icon = when (tabIndex) {
        0 -> Icons.Default.HelpCenter
        1 -> Icons.Default.Security
        else -> Icons.Default.Construction
    }

    val headerTitle = when (tabIndex) {
        0 -> "System Architecture"
        1 -> "Feature Configurations"
        else -> "Reliability & Permissions"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headerTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun IntroTabContent() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DetailGuideCard(
            title = "1. SafeLock Overview",
            icon = Icons.Default.Info,
            color = Color(0xFF00AA5B),
            descriptionText = "SafeLock is an advanced, offline-first application locker. It intercepts and blocks unauthorized access to private chats, system settings, galleries, and payment platforms using lightweight, real-time background protection layers."
        )

        DetailGuideCard(
            title = "2. Background Intercept Engine",
            icon = Icons.Default.Memory,
            color = Color(0xFF2E86DE),
            descriptionText = "When a secure app (like WhatsApp, Settings or a customized selection) is launched, the SafeLock Accessibility Service instantly detects the target transition. It immediately overlays our custom high-security validation screen, preventing unauthorized inputs."
        )

        DetailGuideCard(
            title = "3. Active Session Retention",
            icon = Icons.Default.CheckCircle,
            color = Color(0xFFFA8231),
            descriptionText = "Once verified, an unlocked app session is securely retained on memory and stays fully bypass-unlocked for smooth multitasking. Unlocked states are strictly purged as soon as you turn off or lock your hardware screen, ensuring zero security leaks."
        )
    }
}

@Composable
fun FeaturesManualTabContent() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DetailGuideCard(
            title = "🎮 App Protection Hub",
            icon = Icons.Default.AppShortcut,
            color = Color(0xFF10AC84),
            descriptionText = "Toggle security guards on individual packages. Simply head over to the main screen, use the search block to filter apps, and click the lock toggle. Secured apps are styled with bright green indicators."
        )

        DetailGuideCard(
            title = "🔑 Local Credentials Hashing",
            icon = Icons.Default.LockOpen,
            color = Color(0xFF9B5DE5),
            descriptionText = "We support multiple credential modes: alphanumeric PINs, custom swipe patterns on a fluid 3x3 touch grid, or custom text passwords. All passwords and choices are securely hashed locally using industry-grade SHA-256."
        )

        DetailGuideCard(
            title = "👆 Relocated Biometric Trigger",
            icon = Icons.Default.Fingerprint,
            color = Color(0xFFE94560),
            descriptionText = "For maximum one-handed ergonomic comfort, we've relocated the physical biometric sensor button to the lower bottom-right corner. Tap it to call the native device fingerprint system effortlessly."
        )

        DetailGuideCard(
            title = "📸 Silent Intruder Camera Selfie",
            icon = Icons.Default.CameraAlt,
            color = Color(0xFFE2C35D),
            descriptionText = "Triggered automatically when a trespasser tries 3 consecutive wrong pass entries. SafeLock silently snaps a photo using the front camera and stores it in our localized gallery database. View unauthorized capture events under Settings > 'View Intruder Gallery'."
        )
    }
}

@Composable
fun SetupFixesTabContent() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SetupStepWidget(
            stepNumber = "STEP 1",
            title = "Configure Accessibility Service",
            icon = Icons.Default.SystemUpdateAlt,
            color = Color(0xFFE94560),
            instructions = "1. Go to Settings and tap 'CLICK HERE TO CONFIGURE SYSTEM ACCESS'.\n2. Locate 'SafeLock' under the Installed Apps list.\n3. Turn the service switch ON to grant core launch-intercept capabilities.",
            importanceFocus = "Crucial Requirement! Accessibility serves as the core intercept mechanism. Without this granted permission, SafeLock cannot block application launches."
        )

        SetupStepWidget(
            stepNumber = "STEP 2",
            title = "Secure Settings App First",
            icon = Icons.Default.SettingsSuggest,
            color = Color(0xFFFF9F43),
            instructions = "1. Open the primary App Lock security list.\n2. Locate the native system 'Settings' application.\n3. Click to LOCK 'Settings' immediately.",
            importanceFocus = "Highly Recommended! Securing Settings prevents guest users or intruders from navigating to system app details and forcefully stopping or disabling SafeLock."
        )

        SetupStepWidget(
            stepNumber = "STEP 3",
            title = "Ignore Battery Optimizations",
            icon = Icons.Default.BatteryAlert,
            color = Color(0xFF10AC84),
            instructions = "1. Deep system-sleep monitors can kill background helper apps to save juice.\n2. Tap 'Ignore Battery Optimizations' inside the Settings page.\n3. Turn on the exception exception to grant safe background operations.",
            importanceFocus = "Prevents OS RAM-cleaner services from aggressively destroying or interrupting background surveillance filters."
        )

        SetupStepWidget(
            stepNumber = "STEP 4",
            title = "Realme & OEM Auto-Start Guide",
            icon = Icons.Default.Bolt,
            color = Color(0xFF2E86DE),
            instructions = "1. Scroll down Settings to locate the Realme/Oem custom guide trigger.\n2. Click the background auto-start guide option.\n3. Turn ON explicit auto-start permission toggles for SafeLock.",
            importanceFocus = "Essential for Narzo N53 and heavy Custom OS layers (ColorOS/Xiaomi) to keep intercepts active upon device reboots."
        )
    }
}

@Composable
fun DetailGuideCard(
    title: String,
    icon: ImageVector,
    color: Color,
    descriptionText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = descriptionText,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SetupStepWidget(
    stepNumber: String,
    title: String,
    icon: ImageVector,
    color: Color,
    instructions: String,
    importanceFocus: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = color,
                    contentColor = Color.White
                ) {
                    Text(
                        text = stepNumber,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = instructions,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.08f))
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = importanceFocus,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
