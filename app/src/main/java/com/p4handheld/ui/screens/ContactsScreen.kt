package com.p4handheld.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.p4handheld.data.models.UserContact
import com.p4handheld.ui.components.TopBarWithIcons
import com.p4handheld.ui.screens.viewmodels.ContactsViewModel
import com.p4handheld.utils.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onOpenChat: (contactId: String, contactName: String) -> Unit,
    hasUnreadMessages: Boolean = false,
    hasNotifications: Boolean = false,
    isTrackingLocation: Boolean = false,
    onMessageClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    openMainMenu: () -> Unit = {}
) {
    val viewModel: ContactsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    BackHandler() {
        openMainMenu();
    }

    // Listen for incoming chat message broadcasts to update unread badges in the list
    val ctx = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(Unit) {
        val filter = IntentFilter("com.p4handheld.FIREBASE_MESSAGE_RECEIVED")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                val eventType = intent.getStringExtra("eventType") ?: return
                if (eventType != "USER_CHAT_MESSAGE") return
                val payload = intent.getStringExtra("payload") ?: return
                try {
                    val json = Gson()
                    val msg = json.fromJson(payload, com.p4handheld.data.models.UserChatMessage::class.java)
                    // Increment unread count for sender contact
                    viewModel.incrementUnread(msg.fromUserId)
                } catch (_: JsonSyntaxException) {
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
        topBar = {
            TopBarWithIcons(
                isTrackingLocation = isTrackingLocation,
                hasUnreadMessages = hasUnreadMessages,
                hasNotifications = hasNotifications,
                onMessageClick = onMessageClick,
                onNotificationClick = onNotificationClick
            )

        },
        modifier = Modifier
            .navigationBarsPadding()
            .statusBarsPadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(0.dp)
            ) {
                if (uiState.isLoading) {
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
                                    onClick = {
                                        viewModel.clearUnread(contact.id)
                                        onOpenChat(contact.id, contact.username)
                                    }
                                )
                                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
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
private fun ContactRow(contact: UserContact, onClick: () -> Unit) {
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
                val statusText = if (contact.isOnline) {
                    "Online"
                } else {
                    val human = contact.lastSeen?.let { formatDateTime(it) }
                    human?.let { "Last seen: $it" } ?: "Offline"
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun PreviewContactsScree2n() {
    val contacts = listOf(
        UserContact(
            id = "1",
            lastSeen = "10 min ago",
            username = "Alice",
            isOnline = true,
            newMessages = 2
        ),
        UserContact(
            id = "2",
            lastSeen = "just now",
            username = "Bob",
            isOnline = true,
            newMessages = 0
        ),
        UserContact(
            id = "3",
            lastSeen = "yesterday",
            username = "Charlie",
            isOnline = false,
            newMessages = 5
        )
    )
    MaterialTheme {
        Scaffold(
            topBar = {
                TopBarWithIcons(
                    isTrackingLocation = true,
                    hasUnreadMessages = true,
                    hasNotifications = true,
                    onMessageClick = { },
                    onNotificationClick = { }
                )
//                TopAppBar(
//                    title = { Text("Contacts") }
//                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {

                    LazyColumn {
                        items(contacts) { contact ->
                            ContactRow(
                                contact = contact,
                                onClick = { }
                            )
                            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                        }
                    }
                }

            }
        }
    }
}