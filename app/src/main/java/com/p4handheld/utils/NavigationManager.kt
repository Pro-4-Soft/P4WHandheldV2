package com.p4handheld.utils

import androidx.navigation.NavController
import com.p4handheld.ui.navigation.Screen

object NavigationManager {
    private var navController: NavController? = null

    fun setNavController(controller: NavController) {
        navController = controller
    }

    fun navigateToLogin() {
        navController?.navigate(Screen.Login.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    fun clearNavController() {
        navController = null
    }
}
