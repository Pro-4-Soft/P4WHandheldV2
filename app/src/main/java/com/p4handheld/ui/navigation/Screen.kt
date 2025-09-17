package com.p4handheld.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    object TenantSelect : Screen("tenant_select")
    object Login : Screen("login")
    object Menu : Screen("menu")
    object Messages : Screen("messages")
    object Action : Screen("action/{menuItemLabel}/{menuItemState}") {
        fun createRoute(menuItemLabel: String, menuItemState: String): String {
            return "action/$menuItemLabel/$menuItemState"
        }

        val arguments = listOf(
            navArgument("menuItemLabel") { type = NavType.StringType },
            navArgument("menuItemState") { type = NavType.StringType }
        )
    }
}
