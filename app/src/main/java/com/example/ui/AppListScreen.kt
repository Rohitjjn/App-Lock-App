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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        PageEyebrow(text = "APP LIBRARY")
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "Your apps",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.testTag("app_list_title")
                        )
                        Text(
                            text = "Choose what stays private.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = viewModel::refreshInstalledApps,
                        enabled = !isLoading,
                        modifier = Modifier
                            .size(46.dp)
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
                    Spacer(modifier = Modifier.width(6.dp))
                    Box {
                        IconButton(
                            onClick = { showDropdown = true },
                            modifier = Modifier
                                .size(46.dp)
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
                            shape = MaterialTheme.shapes.medium
                        ) {
                            val isAlphaSelected = sortOption == SortOption.ALPHABETICAL
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.SortByAlpha,
                                        contentDescription = null,
                                        tint = if (isAlphaSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                text = {
                                    Text(
                                        "Alphabetical order",
                                        fontWeight = if (isAlphaSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                trailingIcon = {
                                    if (isAlphaSelected) Icon(Icons.Default.CheckCircle, contentDescription = null)
                                },
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
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.relock_apps)) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                onClick = {
                                    viewModel.relockApps()
                                    val message = context.getString(
                                        if (viewModel.encryptedPrefs.isShieldEnabled()) R.string.relock_done
                                        else R.string.relock_shield_off
                                    )
                                    showDropdown = false
                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar(message)
                                    }
                                },
                                modifier = Modifier.testTag("relock_apps")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                AppProtectionOverview(
                    lockedCount = lockedCount,
                    totalCount = appItems.size,
                    searchQuery = searchQuery,
                    isShieldEnabled = shieldEnabled,
                    isLoading = isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_search_bar"),
                    placeholder = { Text("Search installed apps") },
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
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Text(
                        text = pluralStringResource(R.plurals.locked_app_count, lockedCount, lockedCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 11.dp, vertical = 7.dp)
                            .testTag("locked_stats_indicator")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        items(6) { ShimmerAppRow() }
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .padding(28.dp)
                                .testTag("app_list_empty_state")
                        ) {
                            ProtectionIllustration(modifier = Modifier.size(112.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = emptyTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = emptyDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 21.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    when {
                                        loadError -> viewModel.refreshInstalledApps()
                                        searchQuery.isNotBlank() -> viewModel.searchQuery.value = ""
                                        showLockedOnly -> showLockedOnly = false
                                        else -> viewModel.refreshInstalledApps()
                                    }
                                },
                                shape = CircleShape,
                                modifier = Modifier.testTag("app_list_empty_action")
                            ) {
                                Text(stringResource(when {
                                    loadError -> R.string.retry
                                    searchQuery.isNotBlank() -> R.string.clear_search
                                    showLockedOnly -> R.string.show_all_apps
                                    else -> R.string.refresh_apps
                                }))
                            }
                        }
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = displayedItems,
                            key = { item -> item.packageName },
                            contentType = { "app_row" }
                        ) { item ->
                            val onToggled = remember(item.packageName) {
                                { isLocked: Boolean -> viewModel.toggleAppLock(item.packageName, isLocked) }
                            }
                            AppRowItem(item = item, onLockToggled = onToggled)
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
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 17.dp, bottom = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ProtectionStatusPill(
                    label = if (isShieldEnabled) "Shield is on" else "Shield is paused",
                    positive = isShieldEnabled
                )
                Spacer(modifier = Modifier.height(11.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedContent(
                        targetState = lockedCount,
                        label = "protected_count"
                    ) { count ->
                        Text(
                            text = if (isLoading) "—" else count.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "apps protected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
                Text(
                    text = when {
                        isLoading -> "Loading your installed apps…"
                        searchQuery.isNotBlank() -> "$totalCount matching apps in your library"
                        totalCount == 0 -> "Your app list is ready when you are."
                        else -> "$totalCount apps in your library"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                )
            }
            ProtectionIllustration(
                modifier = Modifier.size(92.dp),
                accent = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
            )
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
        targetValue = if (item.isLocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        animationSpec = tween(220),
        label = "app_row_color"
    )
    val lockTint by animateColorAsState(
        targetValue = if (item.isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "app_lock_tint"
    )

    Card(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            1.dp,
            if (item.isLocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(containerColor = rowColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = item.isLocked, role = Role.Switch, onValueChange = onLockToggled)
            .testTag("app_item_${item.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
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
                        .size(48.dp)
                        .clip(RoundedCornerShape(15.dp))
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
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
                    fontWeight = FontWeight.SemiBold
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
                    AnimatedContent(
                        targetState = item.isLocked,
                        label = "app_switch_icon"
                    ) { locked ->
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
        shape = MaterialTheme.shapes.large,
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
                    .size(48.dp)
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
