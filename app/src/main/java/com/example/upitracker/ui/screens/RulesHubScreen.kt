package com.example.upitracker.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.data.CategorySuggestionRule
import com.example.upitracker.ui.components.AddEditRuleDialog
import com.example.upitracker.ui.components.LottieEmptyState
import com.example.upitracker.ui.components.RuleCard
import com.example.upitracker.ui.components.expressive.ExpressiveSectionHeader
import com.example.upitracker.ui.components.expressive.ExpressiveTopBar
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesHubScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ExpressiveTopBar(
                title = "Payment recognition",
                subtitle = "Teach the app how to organize payments",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        CategorizationRulesContent(
            mainViewModel = mainViewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@Composable
fun AdvancedSmsParserScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ExpressiveTopBar(
                title = "Message diagnostics",
                subtitle = "Review unsupported payment messages",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        ParsingRulesContent(
            mainViewModel = mainViewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@Composable
private fun CategorizationRulesContent(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allCategories by mainViewModel.allCategories.collectAsState(initial = emptyList())
    val rules by mainViewModel.categorySuggestionRules.collectAsState()
    val lastApplication by mainViewModel.lastRuleApplication.collectAsState()

    var ruleToEdit by remember { mutableStateOf<CategorySuggestionRule?>(null) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var showPresetConfirmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize()
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

                TextButton(onClick = { showPresetConfirmDialog = true }) {
                    Text("Add popular presets")
                }

                LottieEmptyState(
                    modifier = Modifier.fillMaxSize(),
                    message = "Tap Add Rule to create one, or add presets for Swiggy, Zomato, bus tickets, shopping, bills, and more.",
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

                item {
                    TextButton(onClick = { showPresetConfirmDialog = true }) {
                        Text("Add popular presets")
                    }
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
            userCategories = allCategories,
            ruleToEdit = ruleToEdit,
            previewMatchCount = mainViewModel::previewRuleMatchingCount,
            onDismiss = { showAddEditDialog = false },
            onConfirm = { field, matcher, keyword, category, priority, logic, applyToExisting ->
                if (ruleToEdit == null) {
                    mainViewModel.addCategoryRule(
                        field,
                        matcher,
                        keyword,
                        category,
                        priority,
                        logic,
                        applyToExisting
                    )
                } else {
                    mainViewModel.updateCategoryRule(
                        ruleToEdit!!.id,
                        field,
                        matcher,
                        keyword,
                        category,
                        priority,
                        logic,
                        applyToExisting
                    )
                }
                showAddEditDialog = false
            }
        )
    }

    if (showPresetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPresetConfirmDialog = false },
            title = { Text("Add popular presets?") },
            text = {
                Text("This adds built-in rules for common merchants like Swiggy, Zomato, redBus, IRCTC, Amazon, Flipkart, Airtel, Netflix, and more. Existing categorized transactions will not be changed; only matching uncategorized transactions may be updated.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mainViewModel.addPopularCategoryRulePresets()
                        showPresetConfirmDialog = false
                    }
                ) {
                    Text("Add presets")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPresetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
