@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding // ✨ Import for status bar padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.upitracker.R
import com.example.upitracker.viewmodel.MainViewModel
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager // Import Pager
import androidx.compose.foundation.pager.rememberPagerState // Import PagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch


@Composable
fun MainAppScreen(
    mainViewModel: MainViewModel,
    rootNavController: NavController,
    onImportOldSms: () -> Unit,
    modifier: Modifier = Modifier, // This modifier comes from MainActivity's Scaffold innerPadding
    onRefreshSmsArchive: () -> Unit
) {
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Graphs,
        BottomNavItem.History,
        BottomNavItem.AppSettings
    )
    val pagerState = rememberPagerState { bottomNavItems.size }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        // The modifier from parent (MainActivity's Scaffold padding) is applied here.
        // This ensures MainAppScreen respects the area given by MainActivity (e.g. for snackbars).
        modifier = modifier.fillMaxSize().fillMaxHeight().fillMaxWidth(),
        topBar = {
            TopAppBar(
                title = {
                    // val navBackStackEntry by contentNavController.currentBackStackEntryAsState()
                    // val currentRoute = navBackStackEntry?.destination?.route
                    // val currentScreen = bottomNavItems.find { it.route == currentRoute }
                    val currentScreen = bottomNavItems.getOrNull(pagerState.currentPage)
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
                bottomNavItems.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = stringResource(screen.labelResId)) },
                        label = { Text(stringResource(screen.labelResId), style = MaterialTheme.typography.labelSmall) },
                        selected = pagerState.currentPage == index, // ✨ Selected based on Pager's current page ✨
                        onClick = {
                            // ✨ Clicking a bottom nav item scrolls the Pager ✨
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
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
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(innerPadding) // Apply padding from this Scaffold
                .fillMaxSize() // Pager fills the content area
        ) { pageIndex ->
            // Each page composable is called directly based on pageIndex
            // Pass the rootNavController if these screens need to navigate to destinations
            // not part of this bottom bar + pager flow (e.g., a detail screen or RegexEditor)
            when (val screen = bottomNavItems[pageIndex]) {
                is BottomNavItem.Home -> CurrentMonthExpensesScreen(
                    mainViewModel = mainViewModel,
                    onImportOldSms = onImportOldSms,
                    navController = rootNavController, // Pass root for potential further navigation
                    modifier = Modifier.fillMaxSize()
                )
                is BottomNavItem.Graphs -> GraphsScreen(
                    mainViewModel = mainViewModel,
                    modifier = Modifier.fillMaxSize()
                )
                is BottomNavItem.History -> TransactionHistoryScreen(
                    mainViewModel = mainViewModel,
                    navController = rootNavController, // Pass root for potential further navigation
                    modifier = Modifier.fillMaxSize()
                )
                is BottomNavItem.AppSettings -> SettingsScreen(
                    mainViewModel = mainViewModel,
                    onImportOldSms = onImportOldSms,
                    onEditRegex = { rootNavController.navigate("regexEditor") }, // Use rootNavController
                    onBack = { /* Settings might not need an onBack if it's a top-level section */
                        // If SettingsScreen had its own back navigation, it would use rootNavController
                        // For now, assuming it's a root screen in the pager.
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRefreshSmsArchive = onRefreshSmsArchive
                )
            }
        }
    }
}