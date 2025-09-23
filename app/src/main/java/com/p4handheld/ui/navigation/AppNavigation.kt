package com.p4handheld.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.p4handheld.ui.screens.ActionScreen
import com.p4handheld.ui.screens.LoginScreen
import com.p4handheld.ui.screens.MenuScreen
import com.p4handheld.ui.screens.TenantSelectScreen
import com.p4handheld.ui.screens.MessagesScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.TenantSelect.route
) {
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
                    navController.navigate(Screen.Action.createRoute(menuItemLabel, menuItemState))
                },
                onNavigateToMessages = {
                    navController.navigate(Screen.Messages.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Menu.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Messages.route) {
            MessagesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Action.route,
            arguments = Screen.Action.arguments
        ) { backStackEntry ->
            val menuItemLabel =
                backStackEntry.arguments?.getString("menuItemLabel") ?: "Unknown Action"
            val menuItemState = backStackEntry.arguments?.getString("menuItemState") ?: ""

            ActionScreen(
                menuItemLabel = menuItemLabel,
                menuItemState = menuItemState,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
