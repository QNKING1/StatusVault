package com.statusvault.app.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.statusvault.app.R
import com.statusvault.app.data.entity.StatusFile
import com.statusvault.app.ui.components.EmptyState
import com.statusvault.app.ui.components.StatusGridItem
import com.statusvault.app.ui.viewmodel.StatusViewModel
import com.statusvault.app.util.PermissionsUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    onImageClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: StatusViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val imageStatuses by viewModel.imageStatuses.collectAsState()
    val videoStatuses by viewModel.videoStatuses.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showPermissionCard by remember { mutableStateOf(false) }

    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            viewModel.refreshStatuses()
            pullToRefreshState.endRefresh()
        }
    }

    // Listen for events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is StatusViewModel.StatusEvent.SavedCount -> {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.items_saved, event.count)
                    )
                }
                is StatusViewModel.StatusEvent.DeletedCount -> {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.items_deleted, event.count)
                    )
                }
                is StatusViewModel.StatusEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
        }
    }

    // Check permissions
    LaunchedEffect(Unit) {
        showPermissionCard = !PermissionsUtil.hasAllFilesPermission(context)
    }

    val currentStatuses = if (selectedTab == 0) imageStatuses else videoStatuses

    Scaffold(
        topBar = {
            if (selectionMode) {
                SelectionAppBar(
                    selectedCount = selectedItems.size,
                    onClearSelection = { viewModel.exitSelectionMode() },
                    onSelectAll = { viewModel.selectAll(currentStatuses) },
                    onSave = { viewModel.saveSelected() },
                    onDelete = { viewModel.deleteSelected() }
                )
            } else {
                StatusTopAppBar(
                    onRefresh = { viewModel.refreshStatuses() },
                    onMenuClick = { showMenu = true },
                    showMenu = showMenu,
                    onMenuDismiss = { showMenu = false },
                    onSettingsClick = { /* Navigate to settings */ }
                )
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(onClick = { viewModel.refreshStatuses() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            Column {
                // Permission card
                AnimatedVisibility(
                    visible = showPermissionCard,
                    enter = slideInVertically(),
                    exit = slideOutVertically()
                ) {
                    PermissionCard(
                        onGrantClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } else {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        },
                        onDismiss = { showPermissionCard = false }
                    )
                }

                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        text = {
                            Text(
                                text = "${stringResource(R.string.tab_images)} (${imageStatuses.size})"
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = {
                            Text(
                                text = "${stringResource(R.string.tab_videos)} (${videoStatuses.size})"
                            )
                        }
                    )
                }

                // Content
                if (currentStatuses.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.Collections,
                        title = stringResource(R.string.no_statuses_found),
                        description = stringResource(R.string.statuses_empty_desc)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = currentStatuses,
                            key = { it.id }
                        ) { status ->
                            StatusGridItem(
                                status = status,
                                isSelected = status.id in selectedItems,
                                selectionMode = selectionMode,
                                onClick = {
                                    if (selectionMode) {
                                        viewModel.toggleSelection(status.id)
                                    } else {
                                        when (status.type) {
                                            StatusFile.StatusType.IMAGE -> onImageClick(status.path)
                                            StatusFile.StatusType.VIDEO -> onVideoClick(status.path)
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (!selectionMode) {
                                        viewModel.enterSelectionMode()
                                        viewModel.toggleSelection(status.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Loading indicator
            if (isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Pull to refresh indicator
            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusTopAppBar(
    onRefresh: () -> Unit,
    onMenuClick: () -> Unit,
    showMenu: Boolean,
    onMenuDismiss: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(R.string.nav_status)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
            Box {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = onMenuDismiss
                ) {
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            onSettingsClick()
                            onMenuDismiss()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionAppBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "$selectedCount selected",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = "Clear selection")
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = "Select all")
            }
            IconButton(onClick = onSave) {
                Icon(Icons.Default.SdStorage, contentDescription = "Save selected")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete selected")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun PermissionCard(
    onGrantClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.permission_required),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.all_files_permission_rationale),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onGrantClick) {
                    Text(stringResource(R.string.go_to_settings))
                }
            }
        }
    }
}
