package com.p4handheld.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.p4handheld.data.models.UserChatMessage
import com.p4handheld.data.models.UserContact
import com.p4handheld.ui.screens.viewmodels.MessagesUiState
import com.p4handheld.ui.screens.viewmodels.MessagesViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreenContent(
    uiState: MessagesUiState,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats") },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            // Contacts list
            Text(
                text = "Contacts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                if (uiState.isLoadingContacts) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp), contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (uiState.contacts.isEmpty()) {
                        Text(
                            text = "No contacts yet",
                            modifier = Modifier.padding(16.dp),
                            color = Color.Gray
                        )
                    } else {
                        LazyColumn {
                            items(uiState.contacts) { contact ->
                                ContactRow(
                                    contact = contact,
                                    isSelected = uiState.selectedContact?.id == contact.id,
                                    onClick = { }
                                )
                                Divider()
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Messages for selected contact
            Text(
                text = uiState.selectedContact?.username?.let { "Messages with $it" } ?: "Messages",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                if (uiState.isLoadingMessages) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (uiState.messages.isEmpty()) {
                        Text(
                            text = if (uiState.selectedContact == null) "Select a contact to view messages" else "No messages",
                            modifier = Modifier.padding(16.dp),
                            color = Color.Gray
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            items(uiState.messages) { msg ->
                                MessageBubble(message = msg, isMine = false)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            uiState.errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: MessagesViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        // ensure contacts loaded on entry (init already loads)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // simple back chevron using less dependency: draw text
                        Text("<")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            // Contacts list
            Text(
                text = "Contacts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                if (uiState.isLoadingContacts) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp), contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (uiState.contacts.isEmpty()) {
                        Text(
                            text = "No contacts yet",
                            modifier = Modifier.padding(16.dp),
                            color = Color.Gray
                        )
                    } else {
                        LazyColumn {
                            items(uiState.contacts) { contact ->
                                ContactRow(
                                    contact = contact,
                                    isSelected = uiState.selectedContact?.id == contact.id,
                                    onClick = { viewModel.selectContact(contact) }
                                )
                                Divider()
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Messages for selected contact
            Text(
                text = uiState.selectedContact?.username?.let { "Messages with $it" } ?: "Messages",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                if (uiState.isLoadingMessages) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    if (uiState.messages.isEmpty()) {
                        Text(
                            text = if (uiState.selectedContact == null) "Select a contact to view messages" else "No messages",
                            modifier = Modifier.padding(16.dp),
                            color = Color.Gray
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            items(uiState.messages) { msg ->
                                MessageBubble(message = msg, isMine = false)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            uiState.errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ContactRow(contact: UserContact, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Online indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(if (contact.isOnline) Color(0xFF10B981) else Color(0xFF9CA3AF), CircleShape)
            )
            Spacer(modifier = Modifier.size(10.dp))
            Column {
                Text(text = contact.username, style = MaterialTheme.typography.bodyLarge)
                val statusText = if (contact.isOnline) "Online" else (contact.lastSeen?.let { "Last seen: $it" } ?: "Offline")
                Text(text = statusText, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        if (contact.newMessages > 0) {
            // Unread badge
            Box(
                modifier = Modifier
                    .background(Color(0xFFEF4444), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = contact.newMessages.toString(), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: UserChatMessage, isMine: Boolean) {
    // We don't know current user ID here; keeping simple styling
    val containerColor = if (isMine) Color(0xFFD1FAE5) else Color(0xFFF3F4F6)
    Column(
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message.fromUsername,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
        Box(
            modifier = Modifier
                .background(containerColor, shape = MaterialTheme.shapes.medium)
                .padding(10.dp)
                .fillMaxWidth()
        ) {
            Text(text = message.message)
        }
        Text(
            text = message.timestamp,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}


@Preview(showBackground = true)
@Composable
fun MessagesScreenPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            PreviewMessagesContent()
        }
    }
}

@Composable
private fun PreviewMessagesContent() {
    // Fake contacts
    val contacts = listOf(
        UserContact(id = "1", username = "Alice", isOnline = true, lastSeen = null, newMessages = 2),
        UserContact(id = "2", username = "Bob", isOnline = false, lastSeen = "10m ago", newMessages = 0),
    )

    // Fake messages
    val messages = listOf(
        UserChatMessage(
            messageId = "m1",
            timestamp = "09:30",
            fromUserId = "1",
            fromUsername = "Alice",
            toUserId = "me",
            toUsername = "Me",
            isNew = false,
            message = "Hey! How’s it going?"
        ),
        UserChatMessage(
            messageId = "m2",
            timestamp = "09:32",
            fromUserId = "me",
            fromUsername = "Me",
            toUserId = "1",
            toUsername = "Alice",
            isNew = false,
            message = "All good, just working on the project."
        ),
        UserChatMessage(
            messageId = "m3",
            timestamp = "09:34",
            fromUserId = "1",
            fromUsername = "Alice",
            toUserId = "me",
            toUsername = "Me",
            isNew = true,
            message = "Cool! Can’t wait to see it."
        )
    )

    val uiState = MessagesUiState(
        isLoadingContacts = false,
        isLoadingMessages = false,
        contacts = contacts,
        selectedContact = contacts.first(),
        messages = messages,
        errorMessage = null
    )

    MessagesScreenContent(
        uiState = uiState,
        onNavigateBack = {}
    )
}