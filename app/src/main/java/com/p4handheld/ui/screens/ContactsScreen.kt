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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.p4handheld.GlobalConstants
import com.p4handheld.data.models.P4WEventType
import com.p4handheld.data.models.UserContact
import com.p4handheld.ui.components.TopBarViewModel
import com.p4handheld.ui.components.TopBarWithIcons
import com.p4handheld.ui.screens.viewmodels.ContactsViewModel
import com.p4handheld.utils.formatDateTime
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onOpenChat: (contactId: String, contactName: String) -> Unit,
    openMainMenu: () -> Unit = {}
) {
    val viewModel: ContactsViewModel = viewModel()
    val topBarViewModel: TopBarViewModel = viewModel()
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    val uiState by viewModel.uiState.collectAsState()
    val topBarState by topBarViewModel.uiState.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current

    BackHandler {
        openMainMenu();
    }

    // Check if TopBar has unread messages when ContactsScreen becomes visible
    LaunchedEffect(Unit) {
        if (topBarState.hasUnreadMessages) {
            viewModel.refresh()
        }
    }

    // Listen for incoming chat message broadcasts to update unread badges in the list
    DisposableEffect(Unit) {
        val filter = IntentFilter(GlobalConstants.Intents.FIREBASE_MESSAGE_RECEIVED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null)
                    return
                val eventType = intent.getStringExtra("eventType") ?: return

                when (eventType) {
                    P4WEventType.USER_CHAT_MESSAGE.toString() -> {
                        val payload = intent.getStringExtra("payload") ?: return
                        try {
                            val msg = json.decodeFromString<com.p4handheld.data.models.UserChatMessage>(payload)
                            viewModel.incrementUnread(msg.fromUserId)
                        } catch (e: SerializationException) {
                            e.printStackTrace()
                        }
                    }

                    "CHECK_ALL_MESSAGES_READ" -> {
                        val contactId = intent.getStringExtra("contactId")
                        // Clear unread for opened contact
                        if (contactId != null) {
                            viewModel.clearUnread(contactId)
                        }
                    }
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
            TopBarWithIcons()
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                Text(
                    text = "Contacts",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    thickness = 1.dp,
                    color = Color.LightGray
                )
            }
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
                                        viewModel.checkAndUpdateTopBarUnreadStatus(uiState.contacts.map {
                                            if (it.id == contact.id) it.copy(newMessages = 0) else it
                                        })
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