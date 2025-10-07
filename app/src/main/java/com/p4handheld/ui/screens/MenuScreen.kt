package com.p4handheld.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.p4handheld.data.models.MenuItem
import com.p4handheld.ui.components.TopBarWithIcons
import com.p4handheld.ui.compose.FontAwesome
import com.p4handheld.ui.compose.getFontAwesomeIcon
import com.p4handheld.ui.screens.viewmodels.MenuUiState
import com.p4handheld.ui.screens.viewmodels.MenuViewModel

@Composable
fun MenuScreen(
    onNavigateToAction: (String, String) -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val viewModel: MenuViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    var currentMenuItems by remember { mutableStateOf<List<MenuItem>>(emptyList()) }
    var menuStack by remember { mutableStateOf<List<List<MenuItem>>>(emptyList()) }
    var breadcrumbStack by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedMenuItem by remember { mutableStateOf<MenuItem?>(null) }

    BackHandler(enabled = menuStack.isNotEmpty()) {
        viewModel.navigateBack()
        if (menuStack.isNotEmpty()) {
            currentMenuItems = menuStack.last()
            menuStack = menuStack.dropLast(1)
            breadcrumbStack = breadcrumbStack.dropLast(1)
            selectedMenuItem = null
        }
    }

    LaunchedEffect(uiState.menuItems) {
        if (uiState.menuItems.isNotEmpty() && currentMenuItems.isEmpty()) {
            currentMenuItems = uiState.menuItems
        }
        if (uiState.httpStatusCode == 401) {
            onNavigateToLogin()
        }
    }

    LaunchedEffect(viewModel.unauthorizedEvent) {
        viewModel.unauthorizedEvent.collect {
            onNavigateToLogin()
        }
    }

    MenuScreenContent(
        uiState = uiState,
        currentMenuItems = currentMenuItems,
        selectedMenuItem = selectedMenuItem,
        breadcrumbStack = breadcrumbStack,
        refreshMenu = { viewModel.refreshMenu() },
        onNavigateToLogin = onNavigateToLogin,
        onNavigateToMessages = onNavigateToMessages,
        logout = { viewModel.logout() },
        onMenuItemClick = { item: MenuItem ->
            if (item.children.isNotEmpty()) {
                // Navigate deeper into menu hierarchy
                menuStack = menuStack + listOf(currentMenuItems)
                breadcrumbStack = breadcrumbStack + listOf(item.label)
                currentMenuItems = item.children
            } else {
                selectedMenuItem = item
                onNavigateToAction(item.label, item.state ?: "")
            }
        }
    )
}

@Composable
fun MenuScreenContent(
    uiState: MenuUiState,
    currentMenuItems: List<MenuItem>,
    selectedMenuItem: MenuItem?,
    breadcrumbStack: List<String>,
    refreshMenu: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToMessages: () -> Unit,
    logout: () -> Unit,
    onMenuItemClick: (MenuItem) -> Unit = {}
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .navigationBarsPadding()
            .statusBarsPadding()
    )
    {
        // Top bar with geolocation and message icons
        TopBarWithIcons(
            isTrackingLocation = uiState.isTrackingLocation,
            hasUnreadMessages = uiState.hasUnreadMessages,
            onMessageClick = onNavigateToMessages
        )

        //region Header with back button and breadcrumb
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                //region Title or breadcrumb
                Column(
                    modifier = Modifier.weight(1f)
                )
                {
                    if (selectedMenuItem != null) {
                        Text(
                            text = selectedMenuItem.label,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset((20).dp)
                        )
                    } else if (breadcrumbStack.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            breadcrumbStack.forEachIndexed { index, item ->
                                Text(
                                    text = item,
                                    fontSize = if (index < breadcrumbStack.size - 1) 14.sp else 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .offset((20).dp)
                                )
                                if (index < breadcrumbStack.size - 1) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = null,
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Main Menu",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset((20).dp)
                        )
                    }
                }
                //endregion

                //region Refresh button
                IconButton(
                    onClick = { refreshMenu() },
                    enabled = !uiState.isLoading
                )
                {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
                //endregion
            }
        }
        //endregion

        //region Error message
        uiState.errorMessage?.let { errorMessage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        //endregion

        //region Menu content
        if (selectedMenuItem == null) {
            if (currentMenuItems.isNotEmpty()) {

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentMenuItems) { menuItem ->
                        Box(
                            modifier = Modifier.padding(1.dp) // extra spacing inside each cell
                        ) {
                            MenuTileCard(
                                menuItem = menuItem,
                                onItemClick = { item -> onMenuItemClick(item) }
                            )
                        }
                        Spacer(modifier = Modifier.size(20.dp))
                    }
                }
            } else if (!uiState.isLoading) {
                //region No menu items available Fallback content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No menu items available",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
                //endregion
            }
        }
        //endregion

        //region Action buttons at bottom (Messages + Logout)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {

            if (uiState.selectedMenuItem == null || (selectedMenuItem?.children?.size ?: 0) > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Messages button
                    FloatingActionButton(
                        onClick = {
                            if (!uiState.isLoading) {
                                onNavigateToMessages()
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.alpha(if (uiState.isLoading) 0.5f else 1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Message,
                            contentDescription = "Messages",
                            tint = Color.White
                        )
                    }

                    // Logout button
                    FloatingActionButton(
                        onClick = {
                            if (!uiState.isLoading) {
                                logout()
                                onNavigateToLogin()
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.alpha(if (uiState.isLoading) 0.5f else 1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                }
            }
        }
        //endregion
    }
}

@Composable
fun MenuTileCard(
    menuItem: MenuItem,
    onItemClick: (MenuItem) -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onItemClick(menuItem) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color(MaterialTheme.colorScheme.outline.value)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        ) {
            // Icon container
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = getFontAwesomeIcon(menuItem.icon?.lowercase() ?: ""),
                    fontFamily = FontAwesome,
                    fontSize = 32.sp,           // match your previous Icon size
                    color = Color(0xFF475569)
                )

                if (menuItem.children.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .offset(x = 12.dp, y = (-12).dp)
                            .size(16.dp)
                            .background(Color(0xFF10B981), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = menuItem.children.size.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.offset(x = 0.dp, y = (-4).dp)
                        )
                    }
                }
            }

            AutoResizeMenuLabelText(text = menuItem.label)
        }
    }
}

@Composable
fun AutoResizeMenuLabelText(
    text: String,
    modifier: Modifier = Modifier,
    initialFontSize: TextUnit = 19.sp,
) {
    var fontSize by remember { mutableStateOf(initialFontSize) }
    val maxLines = if (text.trim().contains(" ")) 2 else 1

    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        modifier = modifier,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow) {
                fontSize *= 0.9f
            }
        }
    )
}
