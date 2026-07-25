@file:OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class
)

package com.example.upitracker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.upitracker.ui.components.expressive.PulseAmbientBackground
import com.example.upitracker.util.expressivePopEnter
import com.example.upitracker.util.expressivePopExit
import com.example.upitracker.util.expressiveSlideIn
import com.example.upitracker.util.expressiveSlideOut
import com.example.upitracker.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainNavHost(
    rootNavController: NavHostController,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    onImportOldSms: () -> Unit,
    onRefreshSmsArchive: () -> Unit,
    onBackupDatabase: (String) -> Unit,
    onRestoreDatabase: (String) -> Unit,
    onShowAddTransactionDialog: () -> Unit
) {
    val bottomNavItems = listOf(
        BottomNavItem.Home, BottomNavItem.Graphs, BottomNavItem.Budget,
        BottomNavItem.History, BottomNavItem.AppSettings
    )

    val navBackStackEntry by rootNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    PulseAmbientBackground(modifier = modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomBar) {
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val navBorderColor = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outlineVariant
                    val navContainerColor = if (isDark) Color(0xFF0F0F16).copy(alpha = 0.92f) else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f)

                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = navBorderColor,
                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            )
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                        containerColor = navContainerColor,
                        tonalElevation = 8.dp
                    ) {
                        bottomNavItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = stringResource(screen.labelResId)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(screen.labelResId),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        rootNavController.navigate(screen.route) {
                                            popUpTo(rootNavController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = screen.route != BottomNavItem.History.route
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                                )
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentRoute == BottomNavItem.History.route) {
                    FloatingActionButton(
                        onClick = onShowAddTransactionDialog,
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ) {
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
                                popUpTo(rootNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onViewInsightsClick = {
                            rootNavController.navigate("insights_screen")
                        },
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
                        onNavigateToDataManagement = { rootNavController.navigate("data_management_screen") },
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
                    "advanced_sms_parser",
                    enterTransition = { expressiveSlideIn() }, exitTransition = { expressiveSlideOut() },
                    popEnterTransition = { expressivePopEnter() }, popExitTransition = { expressivePopExit() }
                ) {
                    AdvancedSmsParserScreen(onBack = { rootNavController.popBackStack() }, mainViewModel = mainViewModel)
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
                    PassbookScreen(onBack = { rootNavController.popBackStack() })
                }
                composable(
                    "data_management_screen",
                    enterTransition = { expressiveSlideIn() }, exitTransition = { expressiveSlideOut() },
                    popEnterTransition = { expressivePopEnter() }, popExitTransition = { expressivePopExit() }
                ) {
                    val context = LocalContext.current
                    val createCsvFileLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.CreateDocument("text/csv")
                    ) { uri ->
                        uri?.let { mainViewModel.exportTransactionsToCsv(it, context.contentResolver) }
                            ?: run { mainViewModel.postSnackbarMessage("CSV export cancelled.") }
                    }

                    DataManagementScreen(
                        onBack = { rootNavController.popBackStack() },
                        mainViewModel = mainViewModel,
                        onImportOldSms = onImportOldSms,
                        onRefreshSmsArchive = onRefreshSmsArchive,
                        onBackupDatabase = onBackupDatabase,
                        onRestoreDatabase = onRestoreDatabase,
                        onNavigateToArchive = { rootNavController.navigate("archived_transactions") },
                        onNavigateToCategories = { rootNavController.navigate("category_management") },
                        onNavigateToRules = { rootNavController.navigate("rule_management") },
                        onNavigateToAdvancedParser = { rootNavController.navigate("advanced_sms_parser") },
                        onNavigateToPassbook = { rootNavController.navigate("passbook_screen") },
                        onExportToCsv = {
                            val timestamp =
                                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            createCsvFileLauncher.launch("upi_tracker_export_$timestamp.csv")
                        }
                    )
                }

                composable(
                    "insights_screen",
                    enterTransition = { expressiveSlideIn() }, exitTransition = { expressiveSlideOut() },
                    popEnterTransition = { expressivePopEnter() }, popExitTransition = { expressivePopExit() }
                ) {
                    InsightGridScreen(
                        mainViewModel = mainViewModel,
                        onNavigateToHistory = {
                            rootNavController.navigate(BottomNavItem.History.route) {
                                popUpTo(rootNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onBackClick = {
                            rootNavController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
