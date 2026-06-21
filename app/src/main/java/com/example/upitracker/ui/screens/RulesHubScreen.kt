package com.example.upitracker.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.data.Category
import com.example.upitracker.data.CategorySuggestionRule
import com.example.upitracker.ui.components.AddEditRuleDialog
import com.example.upitracker.ui.components.LottieEmptyState
import com.example.upitracker.ui.components.RuleCard
import com.example.upitracker.ui.components.expressive.ExpressiveSectionHeader
import com.example.upitracker.ui.components.expressive.ExpressiveTopBar
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RulesHubScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    val pagerState = rememberPagerState { 2 }
    val coroutineScope = rememberCoroutineScope()
    val tabTitles = listOf("Category Rules", "Parsing Rules")

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ExpressiveTopBar(
                title = "Rules",
                subtitle = if (pagerState.currentPage == 0) {
                    "Auto-categorize transactions using custom rules"
                } else {
                    "Advanced SMS parsing rules"
                },
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(title) },
                        icon = {
                            Icon(
                                imageVector = if (index == 0) Icons.AutoMirrored.Filled.Rule else Icons.Filled.Tune,
                                contentDescription = title
                            )
                        }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> CategorizationRulesContent(mainViewModel = mainViewModel)
                    1 -> ParsingRulesContent(mainViewModel = mainViewModel)
                }
            }
        }
    }
}

@Composable
private fun CategorizationRulesContent(
    mainViewModel: MainViewModel
) {
    val userCategories by mainViewModel.userCategories.collectAsState(initial = emptyList())
    val rules by mainViewModel.categorySuggestionRules.collectAsState()
    val lastApplication by mainViewModel.lastRuleApplication.collectAsState()

    var ruleToEdit by remember { mutableStateOf<CategorySuggestionRule?>(null) }
    var showAddEditDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (rules.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(ExpressiveTokens.spacing.lg)
            ) {
                ExpressiveSectionHeader(
                    title = "No category rules yet",
                    subtitle = "Create rules to auto-categorize similar transactions"
                )

                LottieEmptyState(
                    modifier = Modifier.fillMaxSize(),
                    message = "Tap Add Rule to create one.",
                    lottieResourceId = R.raw.empty_box_animation
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = ExpressiveTokens.spacing.lg,
                    top = ExpressiveTokens.spacing.lg,
                    end = ExpressiveTokens.spacing.lg,
                    bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md)
            ) {
                item {
                    ExpressiveSectionHeader(
                        title = "Categorization Rules",
                        subtitle = "${rules.size} rules active"
                    )
                }

                if (lastApplication != null) {
                    item {
                        TextButton(onClick = mainViewModel::undoLastRuleApplication) {
                            Text("Undo last application (${lastApplication!!.transactionIds.size} transactions)")
                        }
                    }
                }

                items(
                    items = rules,
                    key = { it.id }
                ) { rule ->
                    RuleCard(
                        rule = rule,
                        onDelete = { mainViewModel.deleteCategoryRule(rule) },
                        onEdit = {
                            ruleToEdit = rule
                            showAddEditDialog = true
                        }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(ExpressiveTokens.spacing.lg),
            text = { Text("Add Rule") },
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            },
            onClick = {
                ruleToEdit = null
                showAddEditDialog = true
            },
            shape = ExpressiveTokens.corners.large,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

    if (showAddEditDialog) {
        AddEditRuleDialog(
            userCategories = userCategories,
            ruleToEdit = ruleToEdit,
            previewMatchCount = mainViewModel::previewRuleMatchingCount,
            onDismiss = { showAddEditDialog = false },
            onConfirm = { field, matcher, keyword, category, priority, logic ->
                if (ruleToEdit == null) {
                    mainViewModel.addCategoryRule(
                        field,
                        matcher,
                        keyword,
                        category,
                        priority,
                        logic
                    )
                } else {
                    mainViewModel.updateCategoryRule(
                        ruleToEdit!!.id,
                        field,
                        matcher,
                        keyword,
                        category,
                        priority,
                        logic
                    )
                }
                showAddEditDialog = false
            }
        )
    }
}
