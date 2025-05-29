@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding // Optional: for bottom bar
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding // ✨ Import for status bar padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
// import androidx.navigation.NavHostController // Not strictly needed if using NavController type
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.upitracker.R
// import com.example.upitracker.ui.navigation.BottomNavItem
import com.example.upitracker.viewmodel.MainViewModel

@Composable
fun MainAppScreen(
    mainViewModel: MainViewModel,
    rootNavController: NavController,
    onImportOldSms: () -> Unit,
    modifier: Modifier = Modifier // This modifier comes from MainActivity's Scaffold innerPadding
) {
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Graphs,
        BottomNavItem.History,
        BottomNavItem.AppSettings
    )
    val contentNavController = rememberNavController()

    Scaffold(
        // The modifier from parent (MainActivity's Scaffold padding) is applied here.
        // This ensures MainAppScreen respects the area given by MainActivity (e.g. for snackbars).
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    val navBackStackEntry by contentNavController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val currentScreen = bottomNavItems.find { it.route == currentRoute }
                    Text(
                        text = currentScreen?.labelResId?.let { stringResource(it) }
                            ?: stringResource(R.string.app_name)
                    )
                },
                // ✨ Apply status bar padding to the TopAppBar itself ✨
                modifier = Modifier.statusBarsPadding()
                // Optional: colors = TopAppBarDefaults.topAppBarColors(...)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                // Optional: If you want the bottom navigation bar to also avoid system navigation gestures
                // modifier = Modifier.navigationBarsPadding() // Typically only needed if nav bar is translucent/transparent
            ) {
                val navBackStackEntry by contentNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = stringResource(screen.labelResId)) },
                        label = { Text(stringResource(screen.labelResId), style = MaterialTheme.typography.labelSmall) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            contentNavController.navigate(screen.route) {
                                popUpTo(contentNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding -> // Padding from MainAppScreen's Scaffold (TopAppBar, BottomNavBar)
        NavHost(
            navController = contentNavController,
            startDestination = BottomNavItem.Home.route,
            // Apply the padding from this Scaffold to the NavHost content area
            modifier = Modifier.padding(innerPadding)
            // Optional: If your content within NavHost also needs to be aware of system nav bar
            // .navigationBarsPadding() // Use if bottom content is obscured and bottom bar isn't opaque
        ) {
            composable(BottomNavItem.Home.route) {
                CurrentMonthExpensesScreen(
                    mainViewModel = mainViewModel,
                    onImportOldSms = onImportOldSms,
                    navController = contentNavController // ✨ Pass the contentNavController ✨
                )
            }
            composable(BottomNavItem.Graphs.route) {
                GraphsScreen(mainViewModel, modifier = Modifier.fillMaxSize())
            }
            composable(BottomNavItem.History.route) {
                TransactionHistoryScreen(mainViewModel, rootNavController, modifier = Modifier.fillMaxSize())
            }
            composable(BottomNavItem.AppSettings.route) {
                SettingsScreen(
                    mainViewModel = mainViewModel,
                    onImportOldSms = onImportOldSms,
                    onEditRegex = { rootNavController.navigate("regexEditor") },
                    onBack = { contentNavController.popBackStack() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}