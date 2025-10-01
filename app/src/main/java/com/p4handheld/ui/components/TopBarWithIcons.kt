package com.p4handheld.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import com.p4handheld.ui.compose.theme.HandheldP4WTheme

@Composable
fun TopBarWithIcons(
    isTrackingLocation: Boolean = false,
    hasUnreadMessages: Boolean = false,
    hasNotifications: Boolean = false,
    onMessageClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (isTrackingLocation) {
                // Notifications icon with indicator
                Box {
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location tracking active",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Notifications icon with indicator
            Box {
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Notification indicator
                if (hasNotifications) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Red, CircleShape)
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        // Small red dot to indicate notifications
                    }
                }
            }

            // Messages icon with unread indicator
            Box {
                IconButton(
                    onClick = onMessageClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "Messages",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Unread messages indicator
                if (hasUnreadMessages) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Red, CircleShape)
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        // Small red dot to indicate unread messages
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            val context = LocalContext.current
            val username = remember {
                context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
                    .getString("username", "") ?: ""
            }
            Text(
                text = username,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF374151)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TopBarWithIconsPreview() {
    HandheldP4WTheme {
        TopBarWithIcons(
            isTrackingLocation = true,
            hasUnreadMessages = true,
            hasNotifications = true,
            onMessageClick = {},
            onNotificationClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TopBarWithIconsNoIndicatorsPreview() {
    HandheldP4WTheme {
        TopBarWithIcons(
            isTrackingLocation = false,
            hasUnreadMessages = false,
            hasNotifications = false,
            onMessageClick = {},
            onNotificationClick = {}
        )
    }
}
