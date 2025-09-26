package com.p4handheld.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.net.Uri

sealed class Screen(val route: String) {
    object TenantSelect : Screen("tenant_select")
    object Login : Screen("login")
    object Menu : Screen("menu")
    object Messages : Screen("messages")
    object Contacts : Screen("contacts")
    object Chat : Screen("chat/{contactId}/{contactName}") {
        fun createRoute(contactId: String, contactName: String): String {
            // Encode contactName for safe navigation
            val encodedName = Uri.encode(contactName)
            return "chat/$contactId/$encodedName"
        }

        val arguments = listOf(
            navArgument("contactId") { type = NavType.StringType },
            navArgument("contactName") { type = NavType.StringType }
        )
    }
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
