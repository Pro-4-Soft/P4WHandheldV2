package com.p4handheld.ui.screens.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.p4handheld.data.models.MenuItem
import com.p4handheld.ui.compose.theme.HandheldP4WTheme
import com.p4handheld.ui.screens.MenuScreenContent
import com.p4handheld.ui.screens.viewmodels.MenuUiState


// ==================== PREVIEW COMPOSABLES ====================

// Sample data for previews
private val sampleMenuItems = listOf(
    MenuItem(label = "Home", icon = "fa-folder", children = emptyList(), state = "active"), // fixed
    MenuItem(label = "Settings", icon = "fa-cogs", children = emptyList(), state = null), // fixed
    MenuItem(
        label = "Users", icon = "fa-user", children = listOf(
            MenuItem(
                label = "User Management",
                icon = "fa-user",
                children = emptyList(),
                state = "active"
            ),
            MenuItem(
                label = "Permissions",
                icon = "fa-lock", // ✅ valid in FA5
                children = emptyList(),
                state = "inactive"
            ),
            MenuItem(label = "Groups", icon = "fa-users", children = emptyList(), state = "active") // fixed
        ), state = null
    ),
    MenuItem(
        label = "Reports", icon = "fa-tasks", children = listOf( // fixed
            MenuItem(
                label = "Sales Report",
                icon = "fa-clipboard-list", // fixed
                children = emptyList(),
                state = "active"
            ),
            MenuItem(
                label = "User Report",
                icon = "fa-user",
                children = emptyList(),
                state = "active"
            )
        ), state = null
    ),
    MenuItem(label = "Notifications", icon = "fa-bell", children = emptyList(), state = "active"),
    MenuItem(label = "Calendar", icon = "fa-clipboard-list", children = emptyList(), state = "inactive"),
    MenuItem(label = "Messages", icon = "fa-comments", children = emptyList(), state = "active"),
    MenuItem(label = "Search", icon = "fa-barcode", children = emptyList(), state = "active"),
    MenuItem(label = "Profile", icon = "fa-user", children = emptyList(), state = "active")
)

private val sampleUiState = MenuUiState(
    menuItems = sampleMenuItems
)

@Preview(name = "Menu Screen Content", showBackground = true)
@Composable
fun MenuScreenContentPreview() {
    HandheldP4WTheme {
        MenuScreenContent(
            uiState = sampleUiState,
            selectedMenuItem = null,
            logout = {},
            onMenuItemClick = {}
        )
    }
}

@Preview(name = "Menu Screen - With Breadcrumbs", showBackground = true)
@Composable
fun MenuScreenContentWithBreadcrumbsPreview() {
    HandheldP4WTheme {
        MenuScreenContent(
            uiState = sampleUiState.copy(),
            selectedMenuItem = null,
            logout = {},
            onMenuItemClick = {}
        )
    }
}


@Preview(name = "Menu Screen - Error", showBackground = true)
@Composable
fun MenuScreenContentErrorPreview() {
    HandheldP4WTheme {
        MenuScreenContent(
            uiState = sampleUiState.copy(
                errorMessage = "Failed to load menu items. Please check your connection and try again.",
            ),
            selectedMenuItem = null,
            logout = {},
            onMenuItemClick = {}
        )
    }
}