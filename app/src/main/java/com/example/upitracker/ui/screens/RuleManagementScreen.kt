package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.upitracker.ui.components.AddRuleDialog
import com.example.upitracker.ui.components.RuleCard
import com.example.upitracker.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleManagementScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    val rules by mainViewModel.categorySuggestionRules.collectAsState()
    var showAddRuleDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorization Rules") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddRuleDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add new rule")
            }
        }
    ) { paddingValues ->
        if (rules.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(
                    text = "You have no custom rules.\nTap '+' to create one.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rules, key = { it.id }) { rule ->
                    RuleCard(
                        rule = rule,
                        onDelete = { mainViewModel.deleteCategoryRule(rule) }
                    )
                }
            }
        }
    }

    if (showAddRuleDialog) {
        AddRuleDialog(
            onDismiss = { showAddRuleDialog = false },
            onConfirm = { field, matcher, keyword, category, priority ->
                mainViewModel.addCategoryRule(field, matcher, keyword, category, priority)
                showAddRuleDialog = false
            }
        )
    }
}