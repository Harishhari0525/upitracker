package com.example.upitracker.ui.screens

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.upitracker.viewmodel.MainViewModel

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
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) +
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(300)) +
                fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(300)) +
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300)) +
                fadeOut(animationSpec = tween(300))
            }
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
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) +
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(300)) +
                fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(300)) +
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300)) +
                fadeOut(animationSpec = tween(300))
            }
        ) {
            RulesHubScreen( // ✨ Call our NEW unified hub screen here ✨
                onBack = { navController.popBackStack() },
                mainViewModel = mainViewModel
            )
        }
        composable(
            "archived_transactions",
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) +
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(300)) +
                fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(300)) +
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300)) +
                fadeOut(animationSpec = tween(300))
            }
        ) {
            ArchivedTransactionsScreen(
                mainViewModel = mainViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            "category_management", // ✨ ADD THIS NEW ROUTE
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) +
                        fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(300)) +
                        fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            }
        ) {
            // This will call the screen we create in the next step
            CategoryManagementScreen(
                mainViewModel = mainViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}