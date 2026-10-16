package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.theme.SafeLockHero
import com.example.ui.theme.SafeLockHeroAccent
import com.example.ui.theme.SafeLockHeroInk
import com.example.ui.theme.SafeLockHeroMuted
import com.example.ui.theme.SafeLockHeroRaised
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val appItems by viewModel.appItems.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val loadError by viewModel.loadError.collectAsStateWithLifecycle()
    val lockedCount by viewModel.lockedAppCount.collectAsStateWithLifecycle()
    val shieldEnabled by viewModel.isShieldEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showLockedOnly by rememberSaveable { mutableStateOf(false) }
    var showDropdown by rememberSaveable { mutableStateOf(false) }

    val displayedItems = remember(appItems, showLockedOnly) {
        if (showLockedOnly) appItems.filter { it.isLocked } else appItems
    }

    fun relockAndReport() {
        viewModel.relockApps()
        val message = context.getString(
            if (viewModel.encryptedPrefs.isShieldEnabled()) R.string.relock_done
            else R.string.relock_shield_off
        )
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SafeLockMark(
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SafeLock",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "PERSONAL VAULT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.15.sp
                    )
                }
                IconButton(
                    onClick = viewModel::refreshInstalledApps,
                    enabled = !isLoading,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .testTag("refresh_apps_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.refresh_apps),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    IconButton(
                        onClick = { showDropdown = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .testTag("sort_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Options and sorting",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false },
                        shape = MaterialTheme.shapes.large
                    ) {
                        val alphabetical = sortOption == SortOption.ALPHABETICAL
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    Icons.Default.SortByAlpha,
                                    contentDescription = null,
                                    tint = if (alphabetical) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            text = { Text("Alphabetical order", fontWeight = if (alphabetical) FontWeight.SemiBold else FontWeight.Normal) },
                            trailingIcon = { if (alphabetical) Icon(Icons.Default.CheckCircle, contentDescription = null) },
                            onClick = {
                                viewModel.sortOption.value = SortOption.ALPHABETICAL
                                showDropdown = false
                            },
                            modifier = Modifier.testTag("sort_alphabetical")
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (sortOption == SortOption.LOCKED_FIRST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            text = {
                                Text(
                                    "Locked apps first",
                                    fontWeight = if (sortOption == SortOption.LOCKED_FIRST) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            trailingIcon = {
                                if (sortOption == SortOption.LOCKED_FIRST) Icon(Icons.Default.CheckCircle, contentDescription = null)
                            },
                            onClick = {
                                viewModel.sortOption.value = SortOption.LOCKED_FIRST
                                showDropdown = false
                            },
                            modifier = Modifier.testTag("sort_locked_first")
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.relock_apps)) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            onClick = {
                                showDropdown = false
                                relockAndReport()
                            },
                            modifier = Modifier.testTag("relock_apps")
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                AppProtectionOverview(
                    lockedCount = lockedCount,
                    totalCount = appItems.size,
                    searchQuery = searchQuery,
                    isShieldEnabled = shieldEnabled,
                    isLoading = isLoading,
                    onRelock = ::relockAndReport
                )

                Spacer(modifier = Modifier.height(15.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_search_bar"),
                    placeholder = { Text("Find an app in your library") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search apps",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        FilterChip(
                            selected = !showLockedOnly,
                            onClick = { showLockedOnly = false },
                            label = { Text("All apps") },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (!showLockedOnly) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.testTag("filter_all_apps")
                        )
                        FilterChip(
                            selected = showLockedOnly,
                            onClick = { showLockedOnly = true },
                            label = { Text("Locked") },
                            leadingIcon = {
                                if (showLockedOnly) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                                }
                            },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (showLockedOnly) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.testTag("filter_locked_apps")
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier.testTag("locked_stats_indicator")
                    ) {
                        Text(
                            text = pluralStringResource(R.plurals.locked_app_count, lockedCount, lockedCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 13.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (showLockedOnly) "Your protected apps" else "Your app library",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.testTag("app_list_title")
                        )
                        Text(
                            text = when {
                                isLoading -> "Gathering your app collection"
                                searchQuery.isNotBlank() -> "${displayedItems.size} matches"
                                else -> "A little care for the apps you use most"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(5) { ShimmerAppRow() }
                    }
                    loadError || displayedItems.isEmpty() -> {
                        val emptyTitle = stringResource(when {
                            loadError -> R.string.app_list_error_title
                            searchQuery.isNotBlank() -> R.string.app_list_no_results
                            showLockedOnly -> R.string.app_list_no_locks
                            else -> R.string.app_list_no_apps
                        })
                        val emptyDescription = stringResource(when {
                            loadError -> R.string.app_list_error_description
                            searchQuery.isNotBlank() -> R.string.app_list_search_hint
                            showLockedOnly -> R.string.app_list_lock_hint
                            else -> R.string.app_list_refresh_hint
                        })
                        VaultEmptyState(
                            title = emptyTitle,
                            description = emptyDescription,
                            actionText = stringResource(when {
                                loadError -> R.string.retry
                                searchQuery.isNotBlank() -> R.string.clear_search
                                showLockedOnly -> R.string.show_all_apps
                                else -> R.string.refresh_apps
                            }),
                            modifier = Modifier.testTag("app_list_empty_state"),
                            actionModifier = Modifier.testTag("app_list_empty_action"),
                            onAction = {
                                when {
                                    loadError -> viewModel.refreshInstalledApps()
                                    searchQuery.isNotBlank() -> viewModel.searchQuery.value = ""
                                    showLockedOnly -> showLockedOnly = false
                                    else -> viewModel.refreshInstalledApps()
                                }
                            }
                        )
                    }
                    else -> Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.78f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 5.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            itemsIndexed(
                                items = displayedItems,
                                key = { _, item -> item.packageName },
                                contentType = { _, _ -> "app_row" }
                            ) { index, item ->
                                val onToggled = remember(item.packageName) {
                                    { isLocked: Boolean -> viewModel.toggleAppLock(item.packageName, isLocked) }
                                }
                                AppRowItem(item = item, onLockToggled = onToggled)
                                if (index < displayedItems.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 79.dp, end = 18.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.68f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppProtectionOverview(
    lockedCount: Int,
    totalCount: Int,
    searchQuery: String,
    isShieldEnabled: Boolean,
    isLoading: Boolean,
    onRelock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = SafeLockHero),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 19.dp, top = 17.dp, bottom = 17.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isShieldEnabled) Color(0xFF91C49E) else Color(0xFFD9A27D))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AnimatedContent(targetState = isShieldEnabled, label = "guard_status") { active ->
                        Text(
                            text = if (active) "GUARD IS ACTIVE" else "GUARD IS PAUSED",
                            style = MaterialTheme.typography.labelSmall,
                            color = SafeLockHeroMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.05.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(11.dp))
                Text(
                    text = "Your apps,\njust for you.",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 27.sp,
                        letterSpacing = (-0.45).sp
                    ),
                    color = SafeLockHeroInk
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedContent(targetState = lockedCount, label = "protected_count") { count ->
                        Text(
                            text = if (isLoading) "—" else count.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 38.sp, lineHeight = 42.sp),
                            color = SafeLockHeroAccent,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "apps protected",
                        style = MaterialTheme.typography.bodySmall,
                        color = SafeLockHeroMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text(
                    text = when {
                        isLoading -> "Gathering your app collection"
                        searchQuery.isNotBlank() -> "$totalCount matches in your library"
                        totalCount == 0 -> "Your app collection is ready when you are."
                        else -> "$totalCount apps in your library"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SafeLockHeroMuted.copy(alpha = 0.84f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRelock,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SafeLockHeroAccent,
                        contentColor = SafeLockHero
                    ),
                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp),
                    modifier = Modifier.heightIn(min = 34.dp)
                ) {
                    Text("Lock again", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(5.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.width(5.dp))
            VaultIllustration(
                modifier = Modifier
                    .size(104.dp)
                    .graphicsLayer { alpha = 0.96f },
                accent = SafeLockHeroAccent,
                containerColor = SafeLockHeroRaised.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun VaultEmptyState(
    title: String,
    description: String,
    actionText: String,
    modifier: Modifier = Modifier,
    actionModifier: Modifier = Modifier,
    onAction: () -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = 30.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VaultIllustration(
            modifier = Modifier.size(138.dp),
            accent = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(15.dp))
        Button(onClick = onAction, shape = CircleShape, modifier = actionModifier) {
            Text(actionText)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRowItem(
    item: AppItem,
    onLockToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val rowColor by animateColorAsState(
        targetValue = if (item.isLocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(240),
        label = "app_row_color"
    )
    val lockTint by animateColorAsState(
        targetValue = if (item.isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(240),
        label = "app_lock_tint"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowColor)
            .toggleable(value = item.isLocked, role = Role.Switch, onValueChange = onLockToggled)
            .testTag("app_item_${item.packageName}")
            .heightIn(min = 76.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bitmapState = produceState(AppIconCache.getIconFromCache(item.packageName), item.packageName) {
            value = withContext(Dispatchers.IO) { AppIconCache.getIcon(context, item.packageName) }
        }
        val bitmap = bitmapState.value
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(49.dp)
                    .clip(RoundedCornerShape(15.dp))
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(49.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = item.name.firstOrNull()?.uppercase() ?: "A",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = item.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = item.isLocked,
            onCheckedChange = null,
            thumbContent = {
                AnimatedContent(targetState = item.isLocked, label = "app_switch_icon") { locked ->
                    Icon(
                        imageVector = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = lockTint,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.clearAndSetSemantics { }.testTag("app_switch_${item.packageName}")
        )
    }
}

@Composable
fun ShimmerAppRow() {
    val transition = rememberInfiniteTransition(label = "app_row_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.34f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    Card(
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(49.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.48f)
                        .height(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.78f)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                Modifier
                    .width(46.dp)
                    .height(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

object AppIconCache {
    private val cache = android.util.LruCache<String, ImageBitmap>(128)

    fun getIconFromCache(packageName: String): ImageBitmap? = cache.get(packageName)

    fun getIcon(context: android.content.Context, packageName: String): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val drawable = appInfo.loadIcon(pm)
            val bitmap = android.graphics.Bitmap.createBitmap(96, 96, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap().also { cache.put(packageName, it) }
        } catch (_: Exception) {
            null
        }
    }
}
