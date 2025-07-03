package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.RuleField
import com.example.upitracker.data.RuleMatcher
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (field: RuleField, matcher: RuleMatcher, keyword: String, category: String, priority: Int) -> Unit
) {
    var keyword by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var selectedField by remember { mutableStateOf(RuleField.DESCRIPTION) }
    var selectedMatcher by remember { mutableStateOf(RuleMatcher.CONTAINS) }

    var isFieldExpanded by remember { mutableStateOf(false) }
    var isMatcherExpanded by remember { mutableStateOf(false) }
    var priorityText by remember { mutableStateOf("0") }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Rule") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Dropdown for Field to Match
                ExposedDropdownMenuBox(expanded = isFieldExpanded, onExpandedChange = { isFieldExpanded = it }) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        readOnly = true, value = selectedField.name, onValueChange = {}, label = { Text("Field to Check") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFieldExpanded) }
                    )
                    ExposedDropdownMenu(expanded = isFieldExpanded, onDismissRequest = { isFieldExpanded = false }) {
                        RuleField.entries.forEach { field ->
                            DropdownMenuItem(
                                text = { Text(field.name) },
                                onClick = { selectedField = field; isFieldExpanded = false }
                            )
                        }
                    }
                }

                // Dropdown for Matcher type
                ExposedDropdownMenuBox(expanded = isMatcherExpanded, onExpandedChange = { isMatcherExpanded = it }) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        readOnly = true, value = selectedMatcher.name, onValueChange = {}, label = { Text("Condition") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isMatcherExpanded) }
                    )
                    ExposedDropdownMenu(expanded = isMatcherExpanded, onDismissRequest = { isMatcherExpanded = false }) {
                        RuleMatcher.entries.forEach { matcher ->
                            DropdownMenuItem(
                                text = { Text(matcher.name) },
                                onClick = { selectedMatcher = matcher; isMatcherExpanded = false }
                            )
                        }
                    }
                }

                // Keyword to search for
                OutlinedTextField(
                    value = keyword, onValueChange = { keyword = it },
                    label = { Text("Keyword (e.g. Zomato)") },
                    singleLine = true
                )

                // Category to apply
                OutlinedTextField(
                    value = category, onValueChange = { category = it },
                    label = { Text("Category to Apply (e.g. Food)") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true
                )
                OutlinedTextField(
                    value = priorityText,
                    onValueChange = { priorityText = it.filter(Char::isDigit) },
                    label = { Text("Priority (higher number wins)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (keyword.isNotBlank() && category.isNotBlank()) {
                        onConfirm(selectedField, selectedMatcher, keyword, category, priorityText.toIntOrNull() ?: 0)
                    }
                }
            ) { Text("Save Rule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}