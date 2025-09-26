package com.p4handheld.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material3.Surface
import androidx.compose.ui.tooling.preview.Preview
import com.p4handheld.data.models.UserContact
import com.p4handheld.ui.screens.viewmodels.ContactsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onNavigateBack: () -> Unit,
    onOpenChat: (contactId: String, contactName: String) -> Unit
) {
    val viewModel: ContactsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts") },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
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
                                    onClick = { onOpenChat(contact.id, contact.username) }
                                )
                                Divider()
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

@Preview(showBackground = true)
@Composable
fun PreviewContactRow() {
    MaterialTheme {
        Surface {
            ContactRow(
                contact = UserContact(
                    id = "1",
                    username = "Alice Johnson",
                    isOnline = true,
                    lastSeen = null,
                    newMessages = 3
                ),
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewContactsScreen() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            // Fake UI state instead of ViewModel
            Column {
                ContactRow(
                    contact = UserContact(
                        id = "2",
                        username = "Bob Smith",
                        isOnline = false,
                        lastSeen = "Yesterday",
                        newMessages = 0
                    ),
                    onClick = {}
                )
                ContactRow(
                    contact = UserContact(
                        id = "3",
                        username = "Charlie",
                        isOnline = true,
                        lastSeen = null,
                        newMessages = 12
                    ),
                    onClick = {}
                )
            }
        }
    }
}