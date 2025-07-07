package com.example.upitracker.ui.screens

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.util.expressivePopEnter
import com.example.upitracker.util.expressivePopExit
import com.example.upitracker.util.expressiveSlideIn
import com.example.upitracker.util.expressiveSlideOut

@OptIn(ExperimentalAnimationApi::class) // Added for NavHost transitions
@Composable
fun MainNavHost(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = viewModel(),
    onImportOldSms: () -> Unit,
    onRefreshSmsArchive: () -> Unit,
    onBackupDatabase: () -> Unit, // ✨ ADD THIS
    onRestoreDatabase: () -> Unit
) {
    val navController = rememberNavController() // This is the rootNavController
    val startDestination = "main_app_shell" // Assuming onboarding is handled before this NavHost is shown

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier // Applies padding from MainActivity's Scaffold
    ) {
        composable(
            "main_app_shell",
            enterTransition = { expressiveSlideIn() },
            exitTransition = { expressiveSlideOut() },
            popEnterTransition = { expressivePopEnter() },
            popExitTransition = { expressivePopExit() }
        ) { // Route that loads the screen with bottom navigation
            MainAppScreen(
                mainViewModel = mainViewModel,
                rootNavController = navController, // Pass this NavController
                onImportOldSms = onImportOldSms,
                onRefreshSmsArchive = onRefreshSmsArchive,
                onBackupDatabase = onBackupDatabase, // ✨ PASS IT DOWN
                onRestoreDatabase = onRestoreDatabase
            )
        }
        // RegexEditorScreen is typically navigated to from SettingsScreen,
        // so SettingsScreen (inside MainAppScreen) will use the rootNavController to get here.
        composable(
            "rule_management",
            enterTransition = { expressiveSlideIn() },
            exitTransition = { expressiveSlideOut() },
            popEnterTransition = { expressivePopEnter() },
            popExitTransition = { expressivePopExit() }
        ) {
            RulesHubScreen( // ✨ Call our NEW unified hub screen here ✨
                onBack = { navController.popBackStack() },
                mainViewModel = mainViewModel
            )
        }
        composable(
            "archived_transactions",
            enterTransition = { expressiveSlideIn() },
            exitTransition = { expressiveSlideOut() },
            popEnterTransition = { expressivePopEnter() },
            popExitTransition = { expressivePopExit() }
        ) {
            ArchivedTransactionsScreen(
                mainViewModel = mainViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            "category_management", // ✨ ADD THIS NEW ROUTE
            enterTransition = { expressiveSlideIn() },
            exitTransition = { expressiveSlideOut() },
            popEnterTransition = { expressivePopEnter() },
            popExitTransition = { expressivePopExit() }
        ) {
            // This will call the screen we create in the next step
            CategoryManagementScreen(
                mainViewModel = mainViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}