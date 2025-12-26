package com.idcard.ocr.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.idcard.ocr.ui.screens.CameraScreen
import com.idcard.ocr.ui.screens.ResultScreen

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object Camera : Screen("camera")
    object Result : Screen("result/{frontBase64}/{backBase64}") {
        fun createRoute(frontBase64: String, backBase64: String): String {
            return "result/$frontBase64/$backBase64"
        }
    }
}

/**
 * Main navigation composable
 */
@Composable
fun IDCardOCRNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Camera.route
    ) {
        composable(Screen.Camera.route) {
            CameraScreen(
                onNavigateToResult = { frontBase64, backBase64 ->
                    navController.navigate(Screen.Result.createRoute(frontBase64, backBase64))
                },
                onPermissionDenied = {
                    // Handle permission denied - show message or retry UI
                }
            )
        }

        composable(Screen.Result.route) { backStackEntry ->
            val frontBase64 = backStackEntry.arguments?.getString("frontBase64") ?: ""
            val backBase64 = backStackEntry.arguments?.getString("backBase64") ?: ""

            ResultScreen(
                frontBase64 = frontBase64,
                backBase64 = backBase64,
                onNavigateBack = {
                    navController.popBackStack(Screen.Camera.route, inclusive = false)
                },
                onScanAnother = {
                    navController.popBackStack(Screen.Camera.route, inclusive = false)
                }
            )
        }
    }
}
