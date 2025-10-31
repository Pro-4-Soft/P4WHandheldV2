package com.p4handheld.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.p4handheld.GlobalConstants
import com.p4handheld.data.ChatStateManager
import com.p4handheld.data.models.UserChatMessage
import com.p4handheld.ui.components.TopBarWithIcons
import com.p4handheld.ui.screens.viewmodels.ChatViewModel
import com.p4handheld.utils.formatChatTimestamp
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

//@SuppressLint("UnsafeImplicitIntentLaunch")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    contactId: String,
    contactName: String,
    onNavigateToLogin: () -> Unit = {}
) {
    val viewModel: ChatViewModel = viewModel()
    // JSON configuration for Kotlinx Serialization
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val ctx = LocalContext.current

    var hasInitiallyScrolled by remember { mutableStateOf(false) }
    var previousMessageCount by remember { mutableIntStateOf(0) }
    var scrollIndexBeforeLoad by remember { mutableIntStateOf(0) }

    // Register this chat screen as active for the contact
    DisposableEffect(contactId) {
        ChatStateManager.setActiveChatContact(contactId)
        onDispose {
            ChatStateManager.clearActiveChatContact()
        }
    }

    //if 401 - navigate to login screen
    LaunchedEffect(viewModel.unauthorizedEvent) {
        viewModel.unauthorizedEvent.collect {
            onNavigateToLogin()
        }
    }

    // Infinite scroll: load older messages when near top
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (index <= 2 && !uiState.isLoadingMore) {
                    // Capture current position before loading more
                    scrollIndexBeforeLoad = listState.firstVisibleItemIndex
                    previousMessageCount = uiState.messages.size
                    viewModel.loadMore()
                }
            }
    }

    LaunchedEffect(contactId) {
        // Reset scroll flags for new contact
        hasInitiallyScrolled = false
        previousMessageCount = 0
        scrollIndexBeforeLoad = 0

        viewModel.loadMessages(contactId)
        // Clear unread badge when opening a chat
        try {
            val intent = Intent(GlobalConstants.Intents.FIREBASE_MESSAGE_RECEIVED)
            intent.setPackage(ctx.packageName)
            ctx.sendBroadcast(intent)
        } catch (_: Exception) {
        }
    }

    // Listen for incoming FCM broadcasts and append messages to this chat if they match
    DisposableEffect(contactId) {
        val filter = IntentFilter(GlobalConstants.Intents.FIREBASE_MESSAGE_RECEIVED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                val eventType = intent.getStringExtra("eventType") ?: return
                if (eventType != "USER_CHAT_MESSAGE") return
                val payload = intent.getStringExtra("payload") ?: return
                try {
                    val msg = json.decodeFromString<UserChatMessage>(payload)
                    // Append only if the message involves this contact
                    if (msg.fromUserId == contactId || msg.toUserId == contactId) {
                        viewModel.appendIncomingMessage(msg)
                        // Auto-scroll to bottom when new message is received
                        coroutineScope.launch {
                            listState.animateScrollToItem(Int.MAX_VALUE)
                        }
                    }
                } catch (e: SerializationException) {
                    e.printStackTrace()
                }
            }
        }
        ContextCompat.registerReceiver(ctx, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            try {
                ctx.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .navigationBarsPadding()
            .statusBarsPadding(),
        topBar = {
            TopBarWithIcons()
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        )
        {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RectangleShape // removes rounded corners
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(4.dp), // adjust padding as needed
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = contactName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                if (uiState.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (uiState.messages.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "No messages", color = Color.Gray)
                        }
                    } else {
                        val context = LocalContext.current
                        val currentUsername = remember {
                            context.getSharedPreferences(GlobalConstants.AppPreferences.AUTH_PREFS, Context.MODE_PRIVATE)
                                .getString("username", null)
                        }

                        // Pre-compute chat items with date headers in composable scope
                        val chatItems = remember(uiState.messages) { buildChatItemsWithDates(uiState.messages) }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uiState.isLoadingMore) {
                                item(key = "loadingTop") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            itemsIndexed(chatItems) { index, item ->
                                when (item) {
                                    is ChatItem.DateHeader -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = item.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    is ChatItem.MessageItem -> {
                                        val message = item.message
                                        val isMine = currentUsername != null && message.fromUsername == currentUsername
                                        MessageBubble(message = message, isMine = isMine)
                                        Spacer(modifier = Modifier.size(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Scroll to bottom only on initial load to show newest messages
            LaunchedEffect(contactId, uiState.isLoading, hasInitiallyScrolled) {
                if (!uiState.isLoading && uiState.messages.isNotEmpty() && !hasInitiallyScrolled) {
                    coroutineScope.launch {
                        listState.scrollToItem(uiState.messages.size - 1)
                        hasInitiallyScrolled = true
                    }
                }
            }

            // Maintain scroll position when loading older messages
            LaunchedEffect(uiState.messages.size, uiState.isLoadingMore) {
                if (!uiState.isLoadingMore && previousMessageCount > 0 && uiState.messages.size > previousMessageCount) {
                    // Calculate how many new messages were added at the top and adjust scroll position to maintain visual continuity
                    val newMessagesAdded = uiState.messages.size - previousMessageCount
                    val newScrollIndex = scrollIndexBeforeLoad + newMessagesAdded

                    coroutineScope.launch {
                        listState.scrollToItem(newScrollIndex)
                    }

                    // Reset tracking variables
                    previousMessageCount = 0
                    scrollIndexBeforeLoad = 0
                }
            }

            var messageText by remember { mutableStateOf("") }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Type a message") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (messageText.isNotBlank() && !uiState.isSending) {
                                val content = messageText
                                viewModel.sendMessage(contactId, content)
                                {
                                    messageText = ""
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(Int.MAX_VALUE)
                                    }
                                }
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        errorContainerColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (messageText.isNotBlank() && !uiState.isSending) {
                            val content = messageText
                            viewModel.sendMessage(contactId, content) {
                                messageText = ""
                                coroutineScope.launch {
                                    listState.animateScrollToItem(Int.MAX_VALUE)
                                }
                            }
                        }
                    },
                    enabled = messageText.isNotBlank() && !uiState.isSending,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(5.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text(text = if (uiState.isSending) "Sending..." else "Send", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: UserChatMessage, isMine: Boolean) {
    val containerColor = if (isMine) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
    val textColor = Color(0xFF111827)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
        ) {
            Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                Text(
                    text = message.fromUsername,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Box(
                    modifier = Modifier
                        .background(containerColor, shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = message.message, color = textColor)
                }
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    text = formatChatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

//region Chat list helpers
private sealed class ChatItem {
    data class DateHeader(val label: String) : ChatItem()
    data class MessageItem(val message: UserChatMessage) : ChatItem()
}

private fun buildChatItemsWithDates(messages: List<UserChatMessage>): List<ChatItem> {
    if (messages.isEmpty()) return emptyList()
    val items = mutableListOf<ChatItem>()
    val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    var lastDate: LocalDate? = null
    for (msg in messages) {
        val date = try {
            OffsetDateTime.parse(msg.timestamp).toLocalDate()
        } catch (_: Exception) {
            // If parsing fails, don't insert a header based on this message
            null
        }
        if (date != null && date != lastDate) {
            items += ChatItem.DateHeader(date.format(formatter))
            lastDate = date
        }
        items += ChatItem.MessageItem(msg)
    }
    return items
}
//endregion

@Preview(showBackground = true)
@Composable
fun MessageBubblePreview() {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column {
            MessageBubble(
                message = UserChatMessage(
                    fromUsername = "Alice",
                    message = "Hello! How are you?",
                    timestamp = "2025-09-21T18:29:20.120349-06:00",
                    messageId = "1",
                    fromUserId = "2",
                    toUserId = "3",
                    toUsername = "hh",
                    isNew = true
                ),
                isMine = false
            )
            MessageBubble(
                message = UserChatMessage(
                    fromUsername = "Me",
                    message = "I’m good, thanks! What about you?",
                    timestamp = "10:33 AM",
                    messageId = "2",
                    fromUserId = "3",
                    toUserId = "2",
                    toUsername = "hh",
                    isNew = true
                ),
                isMine = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    MaterialTheme {
        ChatScreen(
            contactId = "1",
            contactName = "Alice"
        )
    }
}