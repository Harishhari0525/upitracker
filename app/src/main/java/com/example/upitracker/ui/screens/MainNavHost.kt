package com.example.upitracker.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.upitracker.viewmodel.MainViewModel

@Composable
fun MainNavHost(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = viewModel(),
    onImportOldSms: () -> Unit,
    onRefreshSmsArchive: () -> Unit
) {
    val navController = rememberNavController() // This is the rootNavController

    // Example: Determine start destination based on onboarding (if not handled in MainActivity)
    // val startDestination = if (initialStartDestination == "onboarding") "onboarding_route" else "main_app_shell"
    val startDestination = "main_app_shell" // Assuming onboarding is handled before this NavHost is shown

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier // Applies padding from MainActivity's Scaffold
    ) {
        // composable("onboarding_route") {
        //     OnboardingScreen(onFinish = onOnboardingComplete)
        // }

        composable("main_app_shell") { // Route that loads the screen with bottom navigation
            MainAppScreen(
                mainViewModel = mainViewModel,
                rootNavController = navController, // Pass this NavController
                onImportOldSms = onImportOldSms,
                onRefreshSmsArchive = onRefreshSmsArchive
            )
        }
        // RegexEditorScreen is typically navigated to from SettingsScreen,
        // so SettingsScreen (inside MainAppScreen) will use the rootNavController to get here.
        composable("regexEditor") {
            RegexEditorScreen(
                onBack = { navController.popBackStack() },
                mainViewModel = mainViewModel
            )
        }
        composable("archived_transactions") {
            ArchivedTransactionsScreen(
                mainViewModel = mainViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        // You might remove the separate "settings" route here if it's solely handled by MainAppScreen's bottom nav.
    }
}