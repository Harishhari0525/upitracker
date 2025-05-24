package com.example.upitracker.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.upitracker.viewmodel.MainViewModel


@Composable
fun MainNavHost(
    mainViewModel: MainViewModel = viewModel(),
    onImportOldSms: () -> Unit,

) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            TabbedHomeScreen(mainViewModel = mainViewModel, navController = navController)
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                mainViewModel = mainViewModel,
                onImportOldSms = onImportOldSms,
                onEditRegex = { navController.navigate("regexEditor") }
            )
        }
        composable("regexEditor") {
            RegexEditorScreen(onBack = { navController.popBackStack() })
        }
    }
}
