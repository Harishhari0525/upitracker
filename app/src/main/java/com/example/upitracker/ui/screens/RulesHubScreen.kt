package com.example.upitracker.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.upitracker.R
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.CategorySuggestionRule
import com.example.upitracker.ui.components.AddEditRuleDialog
import com.example.upitracker.ui.components.LottieEmptyState
import com.example.upitracker.ui.components.RuleCard
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
    val tabTitles = listOf("Categorization Rules", "Parsing Rules (Advanced)")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Rules") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> CategorizationRulesContent(mainViewModel = mainViewModel)
                    // ✨ FIX: We now call our reusable composable directly ✨
                    1 -> ParsingRulesContent(mainViewModel = mainViewModel)
                }
            }
        }
    }
}

@Composable
private fun CategorizationRulesContent(mainViewModel: MainViewModel) {
    val rules by mainViewModel.categorySuggestionRules.collectAsState()
    var ruleToEdit by remember { mutableStateOf<CategorySuggestionRule?>(null) }
    var showAddEditDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (rules.isEmpty()) {
            LottieEmptyState(
                message = "You have no custom category rules.\nTap '+' to create one.",
                lottieResourceId = R.raw.empty_box_animation
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rules, key = { it.id }) { rule ->
                    RuleCard(
                        rule = rule,
                        onDelete = { mainViewModel.deleteCategoryRule(rule) },
                        onEdit = {
                            // ✨ Set the rule to edit and show the dialog ✨
                            ruleToEdit = rule
                            showAddEditDialog = true
                        }
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = {
                // ✨ Clear rule to edit (for "Add" mode) and show dialog ✨
                ruleToEdit = null
                showAddEditDialog = true
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add new category rule")
        }
    }

    if (showAddEditDialog) {
        AddEditRuleDialog(
            ruleToEdit = ruleToEdit,
            onDismiss = { showAddEditDialog = false },
            onConfirm = { field, matcher, keyword, category, priority, logic ->
                if (ruleToEdit == null) {
                    // Add new rule
                    mainViewModel.addCategoryRule(field, matcher, keyword, category, priority, logic)
                } else {
                    // Update existing rule
                    mainViewModel.updateCategoryRule(ruleToEdit!!.id, field, matcher, keyword, category, priority, logic)
                }
                showAddEditDialog = false
            }
        )
    }
}