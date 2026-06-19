package com.statusvault.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.statusvault.app.R
import com.statusvault.app.data.entity.RecoveredMessage
import com.statusvault.app.ui.components.EmptyState
import com.statusvault.app.ui.components.MessageItem
import com.statusvault.app.ui.components.formatTimestamp
import com.statusvault.app.ui.viewmodel.MessagesViewModel
import com.statusvault.app.util.PermissionsUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: MessagesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showDeletedOnly by viewModel.showDeletedOnly.collectAsState()
    val deletedCount by viewModel.deletedCount.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val focusManager = LocalFocusManager.current

    var showSearch by remember { mutableStateOf(false) }
    val notificationAccess = remember { PermissionsUtil.hasNotificationAccess(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text(stringResource(R.string.search_messages)) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                focusManager.clearFocus()
                            }),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(stringResource(R.string.nav_messages))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    if (!showSearch) {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    } else {
                        IconButton(onClick = {
                            showSearch = false
                            viewModel.setSearchQuery("")
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter chips
            if (totalCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !showDeletedOnly,
                        onClick = { viewModel.setShowDeletedOnly(false) },
                        label = { Text(stringResource(R.string.all_messages)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = showDeletedOnly,
                        onClick = { viewModel.setShowDeletedOnly(true) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.deleted_only))
                                if (deletedCount > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Badge {
                                        Text("$deletedCount")
                                    }
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${messages.size} messages",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Content
            if (!notificationAccess) {
                ServiceNotRunningCard(
                    onEnableClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    }
                )
            }

            if (messages.isEmpty() && totalCount == 0) {
                EmptyState(
                    icon = Icons.Outlined.Message,
                    title = stringResource(R.string.no_messages),
                    description = stringResource(R.string.messages_empty_desc)
                )
            } else if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showDeletedOnly) "No deleted messages found" else "No messages match your search",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Group messages by date sections
                val groupedMessages = groupMessagesByDate(messages)

                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    groupedMessages.forEach { (dateLabel, msgs) ->
                        item(key = "header_$dateLabel") {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        items(
                            items = msgs,
                            key = { it.id }
                        ) { message ->
                            MessageItem(
                                message = message,
                                onDelete = { viewModel.deleteMessage(message.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceNotRunningCard(
    onEnableClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.service_not_running),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.service_enable_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onEnableClick) {
                Text(stringResource(R.string.grant_access))
            }
        }
    }
}

private fun groupMessagesByDate(
    messages: List<RecoveredMessage>
): Map<String, List<RecoveredMessage>> {
    val now = System.currentTimeMillis()
    val oneDay = 86_400_000L
    val twoDays = oneDay * 2
    val sevenDays = oneDay * 7
    val thirtyDays = oneDay * 30

    return messages.groupBy { msg ->
        val diff = now - msg.timestamp
        when {
            diff < oneDay -> "Today"
            diff < twoDays -> "Yesterday"
            diff < sevenDays -> "This Week"
            diff < thirtyDays -> "This Month"
            else -> "Older"
        }
    }
}
