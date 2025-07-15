@file:OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationGraphicsApi::class
)

package com.example.upitracker.ui.screens

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.upitracker.util.expressivePopEnter
import com.example.upitracker.util.expressivePopExit
import com.example.upitracker.util.expressiveSlideIn
import com.example.upitracker.util.expressiveSlideOut
import com.example.upitracker.viewmodel.MainViewModel

@Composable
fun MainNavHost(
    rootNavController: NavHostController,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    onImportOldSms: () -> Unit,
    onRefreshSmsArchive: () -> Unit,
    onBackupDatabase: () -> Unit,
    onRestoreDatabase: () -> Unit,
    onShowAddTransactionDialog: () -> Unit
) {
    val bottomNavItems = listOf(
        BottomNavItem.Home, BottomNavItem.Graphs, BottomNavItem.Budget,
        BottomNavItem.History, BottomNavItem.AppSettings
    )

    val navBackStackEntry by rootNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                if (screen.animatedIconRes != null) {
                                    val painter = rememberAnimatedVectorPainter(
                                        AnimatedImageVector.animatedVectorResource(id = screen.animatedIconRes),
                                        atEnd = isSelected
                                    )
                                    Icon(painter, contentDescription = stringResource(screen.labelResId))
                                } else {
                                    Icon(screen.icon, contentDescription = stringResource(screen.labelResId))
                                }
                            },
                            label = {
                                Text(stringResource(screen.labelResId), style = MaterialTheme.typography.labelSmall)
                            },
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    rootNavController.navigate(screen.route) {
                                        popUpTo(rootNavController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == BottomNavItem.History.route) {
                FloatingActionButton(onClick = onShowAddTransactionDialog,
                    shape = RoundedCornerShape(16.dp) ) {
                    Icon(Icons.Filled.Add, "Add new transaction")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = rootNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Home screen with unified navigation builder for “View All”
            composable(BottomNavItem.Home.route) {
                CurrentMonthExpensesScreen(
                    mainViewModel = mainViewModel,
                    onViewAllClick = {
                        rootNavController.navigate(BottomNavItem.History.route) {
                            popUpTo(rootNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onRefresh = onImportOldSms
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
                    onNavigateToPassbook = { rootNavController.navigate("passbook_screen") },
                    onBackupDatabase = onBackupDatabase,
                    onRestoreDatabase = onRestoreDatabase
                )
            }

            // Non-bottom screens with slide animations
            composable(
                "rule_management",
                enterTransition = { expressiveSlideIn() }, exitTransition = { expressiveSlideOut() },
                popEnterTransition = { expressivePopEnter() }, popExitTransition = { expressivePopExit() }
            ) {
                RulesHubScreen(onBack = { rootNavController.popBackStack() }, mainViewModel = mainViewModel)
            }

            composable(
                "archived_transactions",
                enterTransition = { expressiveSlideIn() }, exitTransition = { expressiveSlideOut() },
                popEnterTransition = { expressivePopEnter() }, popExitTransition = { expressivePopExit() }
            ) {
                ArchivedTransactionsScreen(mainViewModel = mainViewModel, onBack = { rootNavController.popBackStack() })
            }

            composable(
                "category_management",
                enterTransition = { expressiveSlideIn() }, exitTransition = { expressiveSlideOut() },
                popEnterTransition = { expressivePopEnter() }, popExitTransition = { expressivePopExit() }
            ) {
                CategoryManagementScreen(mainViewModel = mainViewModel, onBack = { rootNavController.popBackStack() })
            }
            composable(
                "passbook_screen",
                enterTransition = { expressiveSlideIn() },
                exitTransition = { expressiveSlideOut() },
                popEnterTransition = { expressivePopEnter() },
                popExitTransition = { expressivePopExit() }
            ) {
                // We will create this composable in the next step
                PassbookScreen(onBack = { rootNavController.popBackStack() })
            }
        }
    }
}
