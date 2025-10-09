package com.p4handheld.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.p4handheld.data.repository.AuthRepository
import com.p4handheld.ui.screens.ActionScreen
import com.p4handheld.ui.screens.ChatScreen
import com.p4handheld.ui.screens.ContactsScreen
import com.p4handheld.ui.screens.LoginScreen
import com.p4handheld.ui.screens.MenuScreen
import com.p4handheld.ui.screens.TenantSelectScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.TenantSelect.route
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val isTrackingLocation = remember { authRepository.shouldTrackLocation() }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.TenantSelect.route) {
            TenantSelectScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.TenantSelect.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToMenu = {
                    navController.navigate(Screen.Menu.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToTenantConfig = {
                    navController.navigate(Screen.TenantSelect.route)
                }
            )
        }

        composable(Screen.Menu.route) {
            MenuScreen(
                onNavigateToAction = { menuItemLabel, menuItemState ->
                    if (menuItemState == "main.contacts") {
                        navController.navigate(Screen.Contacts.route)
                    } else {
                        navController.navigate(Screen.Action.createRoute(menuItemLabel, menuItemState))
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Menu.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Messages.route) {
            ContactsScreen(
                onOpenChat = { id, name ->
                    navController.navigate(Screen.Chat.createRoute(id, name))
                },
                hasUnreadMessages = false,
                isTrackingLocation = isTrackingLocation,
                onMessageClick = { navController.navigate(Screen.Contacts.route) },
                openMainMenu = {
                    navController.navigate(Screen.Menu.route) {
                        popUpTo(Screen.Contacts.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Contacts.route) {
            ContactsScreen(
                onOpenChat = { id, name ->
                    navController.navigate(Screen.Chat.createRoute(id, name))
                },
                hasUnreadMessages = false,
                isTrackingLocation = isTrackingLocation,
                onMessageClick = { /* already here */ },
                openMainMenu = {
                    navController.navigate(Screen.Menu.route) {
                        popUpTo(Screen.Contacts.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = Screen.Chat.arguments
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId").orEmpty()
            val contactName = backStackEntry.arguments?.getString("contactName").orEmpty()
            ChatScreen(
                contactId = contactId,
                contactName = contactName,
                hasUnreadMessages = false,
                isTrackingLocation = isTrackingLocation,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Menu.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Action.route,
            arguments = Screen.Action.arguments
        ) { backStackEntry ->
            val menuItemLabel = backStackEntry.arguments?.getString("menuItemLabel") ?: "Unknown Action"
            val menuItemState = backStackEntry.arguments?.getString("menuItemState") ?: ""

            ActionScreen(
                menuItemLabel = menuItemLabel,
                initialPageKey = menuItemState,
                onNavigateBack = {
                    if (menuItemState == "main.myTasks") {
                        navController.navigate(Screen.Menu.route) {
                            popUpTo(Screen.Contacts.route) { inclusive = true }
                        }
                    } else
                        navController.popBackStack()
                },
                hasUnreadMessages = false,
                isTrackingLocation = isTrackingLocation,
                onMessageClick = { navController.navigate(Screen.Contacts.route) },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Menu.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
