package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.RecurringRule
import com.example.upitracker.ui.components.AddEditRecurringRuleDialog
import com.example.upitracker.ui.components.RecurringRuleCard
import com.example.upitracker.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val recurringRules by mainViewModel.recurringRules.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    var ruleToEdit by remember { mutableStateOf<RecurringRule?>(null) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add new recurring rule")
            }
        }
    ) { paddingValues ->
        if (recurringRules.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "You have no recurring transactions.\nTap the '+' button to add one.",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recurringRules, key = { it.id }) { rule ->
                    RecurringRuleCard(
                        rule = rule,
                        onDelete = { mainViewModel.deleteRecurringRule(rule) },
                        onEdit = { ruleToEdit = rule }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditRecurringRuleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { description, amount, category, period, day ->
                mainViewModel.addRecurringRule(description, amount, category, period, day)
                showAddDialog = false
            },
            ruleToEdit = ruleToEdit
        )
    }
}