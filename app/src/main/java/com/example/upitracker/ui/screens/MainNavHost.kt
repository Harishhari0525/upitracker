package com.example.upitracker.ui.screens

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.upitracker.util.expressivePopEnter
import com.example.upitracker.util.expressivePopExit
import com.example.upitracker.util.expressiveSlideIn
import com.example.upitracker.util.expressiveSlideOut
import com.example.upitracker.viewmodel.MainViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainNavHost(
    rootNavController: NavController,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = viewModel(),
    onImportOldSms: () -> Unit,
    onRefreshSmsArchive: () -> Unit,
    onBackupDatabase: () -> Unit,
    onRestoreDatabase: () -> Unit
) {
    // This NavHost now correctly handles the top-level navigation,
    // and we re-introduce the animations here.
    NavHost(
        navController = rootNavController as NavHostController,
        startDestination = "main_app_shell",
        modifier = modifier
    ) {
        composable(
            route = "main_app_shell"
        ) {
            BottomNavScaffold(
                rootNavController = rootNavController, // Pass the root controller down
                mainViewModel = mainViewModel,
                onImportOldSms = onImportOldSms,
                onRefreshSmsArchive = onRefreshSmsArchive,
                onBackupDatabase = onBackupDatabase,
                onRestoreDatabase = onRestoreDatabase
            )
        }
        composable(
            "rule_management",
            enterTransition = { expressiveSlideIn() },
            exitTransition = { expressiveSlideOut() },
            popEnterTransition = { expressivePopEnter() },
            popExitTransition = { expressivePopExit() }
        ) {
            RulesHubScreen(
                onBack = { rootNavController.popBackStack() },
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
                onBack = { rootNavController.popBackStack() }
            )
        }
        composable(
            "category_management",
            enterTransition = { expressiveSlideIn() },
            exitTransition = { expressiveSlideOut() },
            popEnterTransition = { expressivePopEnter() },
            popExitTransition = { expressivePopExit() }
        ) {
            CategoryManagementScreen(
                mainViewModel = mainViewModel,
                onBack = { rootNavController.popBackStack() }
            )
        }
    }
}


@OptIn(ExperimentalAnimationGraphicsApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
private fun BottomNavScaffold(
    rootNavController: NavController,
    mainViewModel: MainViewModel,
    onImportOldSms: () -> Unit,
    onRefreshSmsArchive: () -> Unit,
    onBackupDatabase: () -> Unit,
    onRestoreDatabase: () -> Unit,
) {
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Graphs,
        BottomNavItem.Budget,
        BottomNavItem.History,
        BottomNavItem.AppSettings
    )
    val contentNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by contentNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    val isSelected = currentDestination?.route == screen.route
                    NavigationBarItem(
                        icon = {
                            if (screen.animatedIconRes != null) {
                                val painter = rememberAnimatedVectorPainter(
                                    animatedImageVector = AnimatedImageVector.animatedVectorResource(id = screen.animatedIconRes),
                                    atEnd = isSelected
                                )
                                Icon(painter, contentDescription = stringResource(screen.labelResId))
                            } else {
                                Icon(screen.icon, contentDescription = stringResource(screen.labelResId))
                            }
                        },
                        label = { Text(stringResource(screen.labelResId), style = MaterialTheme.typography.labelSmall) },
                        selected = isSelected,
                        onClick = {
                            if (currentDestination?.route != screen.route) {
                                contentNavController.navigate(screen.route) {
                                    popUpTo(contentNavController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
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
    ) { innerPadding ->
        NavHost(
            navController = contentNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            composable(BottomNavItem.Home.route) {
                CurrentMonthExpensesScreen(
                    mainViewModel = mainViewModel,
                    onViewAllClick = { contentNavController.navigate(BottomNavItem.History.route) },
                    onRefresh = onRefreshSmsArchive
                )
            }
            composable(BottomNavItem.Graphs.route) {
                GraphsScreen(mainViewModel = mainViewModel)
            }
            composable(BottomNavItem.History.route) {
                TransactionHistoryScreen(mainViewModel = mainViewModel)
            }
            composable(BottomNavItem.Budget.route) {
                BudgetScreen(mainViewModel = mainViewModel)
            }
            composable(BottomNavItem.AppSettings.route) {
                SettingsScreen(
                    mainViewModel = mainViewModel,
                    onImportOldSms = onImportOldSms,
                    onRefreshSmsArchive = onRefreshSmsArchive,
                    onNavigateToRules = { rootNavController.navigate("rule_management") },
                    onNavigateToArchive = { rootNavController.navigate("archived_transactions") },
                    onNavigateToCategories = { rootNavController.navigate("category_management") },
                    onBackupDatabase = onBackupDatabase,
                    onRestoreDatabase = onRestoreDatabase
                )
            }
        }
    }
}
