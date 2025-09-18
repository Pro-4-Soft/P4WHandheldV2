package com.p4handheld.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.p4handheld.data.models.*
import com.p4handheld.ui.compose.theme.HandheldP4WTheme
import com.p4handheld.ui.screens.viewmodels.FirebaseMessagesUiState
import com.p4handheld.ui.screens.viewmodels.FirebaseMessagesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseMessagesScreen(
    onNavigateBack: () -> Unit = {}
) {
    val viewModel: FirebaseMessagesViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    FirebaseMessagesContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onEventSelected = { eventType ->

        },
        onSubscribeToEvent = { eventType ->
            viewModel.subscribeToEvent(eventType)
        },
        onUnsubscribeFromEvent = { eventType ->
            viewModel.unsubscribeFromEvent(eventType)
        },
        onMessageClick = { messageId ->
        },
        onRefresh = {
            viewModel.loadEventTypes()
        },
        onClearError = {
            viewModel.clearError()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseMessagesContent(
    uiState: FirebaseMessagesUiState,
    onNavigateBack: () -> Unit = {},
    onEventSelected: (String?) -> Unit = {},
    onSubscribeToEvent: (String) -> Unit = {},
    onUnsubscribeFromEvent: (String) -> Unit = {},
    onMessageClick: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onClearError: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        // Header
        TopAppBar(
            title = {
                Text(
                    text = "All Messages"
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onRefresh) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }
        )

        // Error message
        uiState.errorMessage?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    IconButton(onClick = onClearError) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Event types filter
        if (uiState.eventTypes.isNotEmpty()) {
            EventTypesFilterRow(
                eventTypes = uiState.eventTypes,
                selectedEventType = uiState.selectedEventType,
                onEventSelected = onEventSelected,
                onSubscribeToEvent = onSubscribeToEvent,
                onUnsubscribeFromEvent = onUnsubscribeFromEvent
            )
        }

        // Messages list
        if (uiState.messages.isEmpty() && !uiState.isLoading) {
            EmptyMessagesView()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages) { message ->
                    MessageCard(
                        message = message,
                        onClick = { onMessageClick(message.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun EventTypesFilterRow(
    eventTypes: List<FirebaseEventType>,
    selectedEventType: String?,
    onEventSelected: (String?) -> Unit,
    onSubscribeToEvent: (String) -> Unit,
    onUnsubscribeFromEvent: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All messages chip
        item {
            FilterChip(
                onClick = { onEventSelected(null) },
                label = { Text("All Messages") },
                selected = selectedEventType == null
            )
        }

        // Event type chips
        items(eventTypes) { eventType ->
            EventTypeChip(
                eventType = eventType,
                isSelected = selectedEventType == eventType.eventType,
                onSelected = { onEventSelected(eventType.eventType) },
                onSubscribe = { onSubscribeToEvent(eventType.eventType) },
                onUnsubscribe = { onUnsubscribeFromEvent(eventType.eventType) }
            )
        }
    }
}

@Composable
fun EventTypeChip(
    eventType: FirebaseEventType,
    isSelected: Boolean,
    onSelected: () -> Unit,
    onSubscribe: () -> Unit,
    onUnsubscribe: () -> Unit
) {
    var showSubscriptionDialog by remember { mutableStateOf(false) }

    FilterChip(
        onClick = {
            if (eventType.isSubscribed) {
                onSelected()
            } else {
                showSubscriptionDialog = true
            }
        },
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(eventType.eventName)
                if (!eventType.isSubscribed) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Subscribe",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        selected = isSelected,
        enabled = eventType.isSubscribed
    )

    if (showSubscriptionDialog) {
        AlertDialog(
            onDismissRequest = { showSubscriptionDialog = false },
            title = { Text("Subscribe to ${eventType.eventName}?") },
            text = { Text(eventType.description ?: "Subscribe to receive notifications for this event type.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSubscribe()
                        showSubscriptionDialog = false
                    }
                ) {
                    Text("Subscribe")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubscriptionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MessageCard(
    message: FirebaseMessage,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (message.isRead) Color.White else Color(0xFFF0F9FF)
        ),
        border = if (!message.isRead) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MessageTypeIcon(messageType = message.messageType)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!message.isRead) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = message.body,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    MessagePriorityBadge(priority = message.priority)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatTimestamp(message.timestamp),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Event type info
            message.eventType?.let { eventType ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Event: $eventType",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun MessageTypeIcon(messageType: MessageType) {
    val (icon, color) = when (messageType) {
        MessageType.USER_CHAT_MESSAGE -> Icons.Default.Message to Color(0xFF8B5CF6)
        MessageType.TASKS_CHANGED -> Icons.Default.Assignment to Color(0xFF10B981)
        MessageType.SCREEN_REQUESTED -> Icons.Default.ScreenShare to Color(0xFFF59E0B)
        MessageType.SYSTEM -> Icons.Default.Settings to Color(0xFF6B7280)
        MessageType.ALERT -> Icons.Default.Warning to Color(0xFFEF4444)
        MessageType.NOTIFICATION -> Icons.Default.Notifications to Color(0xFF3B82F6)
    }

    Icon(
        imageVector = icon,
        contentDescription = messageType.name,
        tint = color,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
fun MessagePriorityBadge(priority: MessagePriority) {
    val (text, color) = when (priority) {
        MessagePriority.LOW -> "Low" to Color(0xFF6B7280)
        MessagePriority.NORMAL -> "Normal" to Color(0xFF3B82F6)
        MessagePriority.HIGH -> "High" to Color(0xFFF59E0B)
        MessagePriority.URGENT -> "Urgent" to Color(0xFFEF4444)
    }

    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier
            .background(color, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun EmptyMessagesView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Message,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No messages yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Messages will appear here when you receive them",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}

// Preview composables
@Preview(showBackground = true)
@Composable
fun FirebaseMessagesScreenPreview() {
    HandheldP4WTheme {
        val sampleMessages = listOf(
            FirebaseMessage(
                id = "1",
                title = "System Update",
                body = "A new system update is available for your device.",
                messageType = MessageType.SYSTEM,
                priority = MessagePriority.NORMAL,
                timestamp = System.currentTimeMillis() - 300000,
                isRead = false
            ),
            FirebaseMessage(
                id = "2",
                title = "Location Request",
                body = "Please share your current location for task assignment.",
                priority = MessagePriority.HIGH,
                timestamp = System.currentTimeMillis() - 600000,
                isRead = true,
            )
        )



        FirebaseMessagesContent(
            uiState = FirebaseMessagesUiState(
                messages = sampleMessages,
                isLoading = false
            )
        )
    }
}
