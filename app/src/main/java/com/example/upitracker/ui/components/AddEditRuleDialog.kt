package com.example.upitracker.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.example.upitracker.data.CategorySuggestionRule
import com.example.upitracker.data.RuleField
import com.example.upitracker.data.RuleLogic
import com.example.upitracker.data.RuleMatcher
import com.example.upitracker.util.ExpressiveTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRuleDialog(
    ruleToEdit: CategorySuggestionRule?,
    onDismiss: () -> Unit,
    onConfirm: (
        field: RuleField,
        matcher: RuleMatcher,
        keyword: String,
        category: String,
        priority: Int,
        logic: RuleLogic
    ) -> Unit
) {
    var keyword by remember { mutableStateOf(ruleToEdit?.keyword ?: "") }
    var category by remember { mutableStateOf(ruleToEdit?.categoryName ?: "") }
    var selectedField by remember { mutableStateOf(ruleToEdit?.fieldToMatch ?: RuleField.DESCRIPTION) }
    var selectedMatcher by remember { mutableStateOf(ruleToEdit?.matcher ?: RuleMatcher.CONTAINS) }
    var priorityText by remember { mutableStateOf(ruleToEdit?.priority?.toString() ?: "0") }
    var selectedLogic by remember { mutableStateOf(ruleToEdit?.logic ?: RuleLogic.ANY) }

    var isFieldExpanded by remember { mutableStateOf(false) }
    var isMatcherExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ExpressiveTokens.corners.extraLarge,
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.xs)
            ) {
                Text(
                    text = if (ruleToEdit == null) "Add Rule" else "Edit Rule",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Define how transactions should be categorized.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md)
            ) {
                ExposedDropdownMenuBox(
                    expanded = isFieldExpanded,
                    onExpandedChange = { isFieldExpanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        value = selectedField.name,
                        onValueChange = {},
                        label = { Text("Field to check") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = isFieldExpanded
                            )
                        },
                        shape = ExpressiveTokens.corners.medium,
                        singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded = isFieldExpanded,
                        onDismissRequest = { isFieldExpanded = false }
                    ) {
                        RuleField.entries.forEach { field ->
                            DropdownMenuItem(
                                text = { Text(field.name.replace("_", " ")) },
                                onClick = {
                                    selectedField = field
                                    isFieldExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = isMatcherExpanded,
                    onExpandedChange = { isMatcherExpanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        value = selectedMatcher.name,
                        onValueChange = {},
                        label = { Text("Condition") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = isMatcherExpanded
                            )
                        },
                        shape = ExpressiveTokens.corners.medium,
                        singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded = isMatcherExpanded,
                        onDismissRequest = { isMatcherExpanded = false }
                    ) {
                        RuleMatcher.entries.forEach { matcher ->
                            DropdownMenuItem(
                                text = { Text(matcher.name.replace("_", " ")) },
                                onClick = {
                                    selectedMatcher = matcher
                                    isMatcherExpanded = false
                                }
                            )
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.xs)
                ) {
                    Text(
                        text = "Matching logic",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            onClick = { selectedLogic = RuleLogic.ANY },
                            selected = selectedLogic == RuleLogic.ANY
                        ) {
                            Text("ANY")
                        }

                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            onClick = { selectedLogic = RuleLogic.ALL },
                            selected = selectedLogic == RuleLogic.ALL
                        ) {
                            Text("ALL")
                        }
                    }

                    Text(
                        text = "ANY means OR. ALL means AND.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("Keywords") },
                    supportingText = {
                        Text("Example: Zomato, Swiggy. Separate multiple keywords with comma.")
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    shape = ExpressiveTokens.corners.medium,
                    singleLine = true
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category to apply") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    shape = ExpressiveTokens.corners.medium,
                    singleLine = true
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = priorityText,
                    onValueChange = { priorityText = it.filter(Char::isDigit) },
                    label = { Text("Priority") },
                    supportingText = {
                        Text("Higher number wins when multiple rules match.")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    shape = ExpressiveTokens.corners.medium,
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (keyword.isNotBlank() && category.isNotBlank()) {
                        onConfirm(
                            selectedField,
                            selectedMatcher,
                            keyword,
                            category,
                            priorityText.toIntOrNull() ?: 0,
                            selectedLogic
                        )
                    }
                },
                shape = ExpressiveTokens.corners.medium
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}