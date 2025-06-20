@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class) // Added ExperimentalAnimationApi

package com.example.upitracker.ui.screens

import android.util.Log // ✨ Add Log import for debugging
import androidx.compose.animation.core.tween // Animation imports
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.upitracker.R
import com.example.upitracker.ui.components.AddTransactionDialog
import com.example.upitracker.viewmodel.MainViewModel

@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class) // Can also be at function level
@Composable
fun MainAppScreen(
    mainViewModel: MainViewModel,
    rootNavController: NavController,
    onImportOldSms: () -> Unit,
    onRefreshSmsArchive: () -> Unit,
    onBackupDatabase: () -> Unit, // ✨ ADD THIS
    onRestoreDatabase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Graphs,
        BottomNavItem.Budget,
        BottomNavItem.History,
        BottomNavItem.AppSettings
    )
    val contentNavController = rememberNavController()
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    val navBackStackEntry by contentNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                // windowInsets = WindowInsets(0),
                title = {
                    val navBackStackEntry by contentNavController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val currentScreen = bottomNavItems.find { it.route == currentRoute }
                    Text(
                        text = currentScreen?.labelResId?.let { stringResource(it) }
                            ?: stringResource(R.string.app_name)
                    )
                },
                actions = {
                    if (currentRoute == BottomNavItem.History.route) {
                        IconButton(onClick = { mainViewModel.onFilterClick() }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Show Filters"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            ) {
                val navBackStackEntry by contentNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // ✨ Log current destination route ✨
                LaunchedEffect(currentDestination) {
                    Log.d("BottomNav", "Current Destination Route: ${currentDestination?.route}")
                }

                bottomNavItems.forEach { screen ->
                    // ✨ Simplify selection logic for debugging, then try hierarchy if needed ✨
                    val isSelected = currentDestination?.route == screen.route
                    // val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                    // ✨ Log selection status for each item ✨
                    Log.d("BottomNav", "Item: ${stringResource(screen.labelResId)}, Route: ${screen.route}, IsSelected: $isSelected")

                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = stringResource(screen.labelResId)) },
                        label = { Text(stringResource(screen.labelResId), style = MaterialTheme.typography.labelSmall) },
                        selected = isSelected,
                        onClick = {
                            if (currentDestination?.route != screen.route) { // Avoid re-navigating to the same screen
                                contentNavController.navigate(screen.route) {
                                    popUpTo(contentNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
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
        },
        floatingActionButton = {
            if (currentRoute == BottomNavItem.Home.route) {
                FloatingActionButton(
                    onClick = { showAddTransactionDialog = true }
                ) {
                    Icon(Icons.Filled.Add, "Add new transaction")
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
            composable(
                BottomNavItem.Home.route,
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
                CurrentMonthExpensesScreen(
                    mainViewModel = mainViewModel,
                    onImportOldSms = onImportOldSms,
                    modifier = Modifier.fillMaxSize(),
                    onViewAllClick = {
                        // This action uses the contentNavController
                        contentNavController.navigate(BottomNavItem.History.route) {
                            popUpTo(contentNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onTransactionClick = { transactionId ->
                        // This action uses the rootNavController
                        rootNavController.navigate("transaction_detail/$transactionId")
                    }
                )
            }
            composable(
                BottomNavItem.Graphs.route,
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
                GraphsScreen(
                    mainViewModel = mainViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(
                BottomNavItem.History.route,
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
                TransactionHistoryScreen(
                    mainViewModel = mainViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }

            composable(
                BottomNavItem.Budget.route,
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
                BudgetScreen(
                    mainViewModel = mainViewModel
                )
            }

            composable(
                BottomNavItem.AppSettings.route,
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
                SettingsScreen(
                    mainViewModel = mainViewModel,
                    onImportOldSms = onImportOldSms,
                    onRefreshSmsArchive = onRefreshSmsArchive,
                    onNavigateToRules = { rootNavController.navigate("rule_management") },
                    onNavigateToArchive = { rootNavController.navigate("archived_transactions")},
                        onBack = {
                        if (contentNavController.previousBackStackEntry != null) {
                            contentNavController.popBackStack()
                        } else {
                            // Optionally, navigate to home if there's no backstack within bottom nav
                            // This depends on desired UX
                        }
                    },
                    onBackupDatabase = onBackupDatabase, // ✨ PASS IT DOWN
                    onRestoreDatabase = onRestoreDatabase,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    if (showAddTransactionDialog) {
        AddTransactionDialog(
            onDismiss = { showAddTransactionDialog = false },
            onConfirm = { amount, type, description, category ->
                // We will add the ViewModel logic here in the next step
                // For now, it just closes
                mainViewModel.addManualTransaction(amount, type, description, category)
                showAddTransactionDialog = false
            }
        )
    }
}