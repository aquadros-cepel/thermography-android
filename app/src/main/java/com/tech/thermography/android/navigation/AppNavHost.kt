package com.tech.thermography.android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tech.thermography.android.ui.auth.login.LoginScreen
import com.tech.thermography.android.ui.home.HomeScreen
import com.tech.thermography.android.ui.sync.SyncScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                onLoginSuccess = { navController.navigate("sync") },
                onNavigateToCreateAccount = { /* TODO */ }
            )
        }

        composable("sync") {
            SyncScreen(onSyncComplete = { navController.navigate("home") })
        }

        composable("home") {
            HomeScreen()
        }
    }
}
