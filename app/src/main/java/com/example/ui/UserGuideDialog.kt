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
import androidx.compose.ui.res.stringResource
import com.example.R
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
    val tabs = listOf("Overview", "Features", "Setup")

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
        0 -> stringResource(R.string.guide_architecture)
        1 -> "Explore PIN codes, custom swipe patterns, bottom-relocated fingerprint triggers, and stealth photo logs."
        else -> "Crucial steps for persistent protection: system-level configurations and OEM overrides."
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
        shape = MaterialTheme.shapes.large,
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
            color = MaterialTheme.colorScheme.secondary,
            descriptionText = stringResource(R.string.guide_overview)
        )

        DetailGuideCard(
            title = "2. Background Intercept Engine",
            icon = Icons.Default.Memory,
            color = MaterialTheme.colorScheme.primary,
            descriptionText = stringResource(R.string.guide_detector)
        )

        DetailGuideCard(
            title = "3. Active Session Retention",
            icon = Icons.Default.CheckCircle,
            color = MaterialTheme.colorScheme.tertiary,
            descriptionText = stringResource(R.string.guide_sessions)
        )
    }
}

@Composable
fun FeaturesManualTabContent() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DetailGuideCard(
            title = "App Protection Hub",
            icon = Icons.Default.AppShortcut,
            color = MaterialTheme.colorScheme.secondary,
            descriptionText = "Toggle security guards on individual packages. Simply head over to the main screen, use the search block to filter apps, and click the lock toggle. Protected apps are marked with a warm accent."
        )

        DetailGuideCard(
            title = "Local credential security",
            icon = Icons.Default.LockOpen,
            color = MaterialTheme.colorScheme.primary,
            descriptionText = stringResource(R.string.guide_hashing)
        )

        DetailGuideCard(
            title = "Biometric unlock",
            icon = Icons.Default.Fingerprint,
            color = MaterialTheme.colorScheme.tertiary,
            descriptionText = stringResource(R.string.guide_biometrics)
        )

        DetailGuideCard(
            title = "Intruder photo log",
            icon = Icons.Default.CameraAlt,
            color = MaterialTheme.colorScheme.secondary,
            descriptionText = stringResource(R.string.guide_camera)
        )
    }
}

@Composable
fun SetupFixesTabContent() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SetupStepWidget(
            stepNumber = "STEP 1",
            title = "Configure Usage Access",
            icon = Icons.Default.SystemUpdateAlt,
            color = MaterialTheme.colorScheme.tertiary,
            instructions = "1. Go to Settings and tap 'CLICK HERE TO CONFIGURE SYSTEM ACCESS'.\n2. Locate 'SafeLock' under the Installed Apps list.\n3. Turn the service switch ON to grant core launch-intercept capabilities.",
            importanceFocus = "Crucial Requirement! Usage Access serves as the core intercept mechanism. Without this granted permission, SafeLock cannot block application launches."
        )

        SetupStepWidget(
            stepNumber = "STEP 2",
            title = "Secure Settings App First",
            icon = Icons.Default.SettingsSuggest,
            color = MaterialTheme.colorScheme.primary,
            instructions = "1. Open the primary App Lock security list.\n2. Locate the native system 'Settings' application.\n3. Click to LOCK 'Settings' immediately.",
            importanceFocus = stringResource(R.string.guide_settings_limit)
        )

        SetupStepWidget(
            stepNumber = "STEP 3",
            title = "Realme & OEM Auto-Start Guide",
            icon = Icons.Default.Bolt,
            color = MaterialTheme.colorScheme.primary,
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
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
