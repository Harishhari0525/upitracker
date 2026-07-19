package com.example.upitracker.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.example.upitracker.data.CategorySuggestionRule
import com.example.upitracker.data.RuleField
import com.example.upitracker.data.RuleLogic
import com.example.upitracker.data.RuleMatcher
import com.example.upitracker.util.ExpressiveTokens
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRuleDialog(
    userCategories: List<com.example.upitracker.data.Category>,
    ruleToEdit: CategorySuggestionRule?,
    onDismiss: () -> Unit,
    previewMatchCount: suspend (RuleField, RuleMatcher, String, RuleLogic) -> Int = { _, _, _, _ -> 0 },
    onConfirm: (
        field: RuleField,
        matcher: RuleMatcher,
        keyword: String,
        category: String,
        priority: Int,
        logic: RuleLogic,
        applyToExisting: Boolean
    ) -> Unit
) {
    var keyword by remember { mutableStateOf(ruleToEdit?.keyword ?: "") }
    var category by remember { mutableStateOf(ruleToEdit?.categoryName ?: "") }
    var selectedField by remember { mutableStateOf(ruleToEdit?.fieldToMatch ?: RuleField.DESCRIPTION) }
    var selectedMatcher by remember { mutableStateOf(ruleToEdit?.matcher ?: RuleMatcher.CONTAINS) }
    var priorityText by remember { mutableStateOf(ruleToEdit?.priority?.toString() ?: "0") }
    var selectedLogic by remember { mutableStateOf(ruleToEdit?.logic ?: RuleLogic.ANY) }
    var applyToExisting by remember { mutableStateOf(ruleToEdit == null) }

    var isFieldExpanded by remember { mutableStateOf(false) }
    var isMatcherExpanded by remember { mutableStateOf(false) }
    var matchingCount by remember { mutableStateOf(0) }

    LaunchedEffect(selectedField, selectedMatcher, keyword, selectedLogic) {
        delay(250.milliseconds)
        matchingCount = if (keyword.isBlank()) 0 else previewMatchCount(selectedField, selectedMatcher, keyword, selectedLogic)
    }

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

                Text(
                    text = "$matchingCount currently uncategorized transactions match this rule.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                val shortTerms = keyword.split(',').map(String::trim).filter { it.length in 1..2 }
                if (shortTerms.isNotEmpty()) {
                    Text(
                        text = "Warning: very short keywords (${shortTerms.joinToString()}) can match unrelated transactions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (matchingCount >= 25) {
                    Text(
                        text = "Warning: this rule matches many existing transactions. Review the keyword before applying.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                var isCategoryExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = isCategoryExpanded,
                    onExpandedChange = { isCategoryExpanded = it }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                            .fillMaxWidth(),
                        value = category,
                        onValueChange = {
                            category = it
                            isCategoryExpanded = true
                        },
                        label = { Text("Category to apply") },
                        placeholder = { Text("e.g., Shopping, Dining") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        shape = ExpressiveTokens.corners.medium
                    )

                    val filteredCategories = userCategories
                        .filter { it.name.contains(category, ignoreCase = true) }
                        .sortedWith(
                            compareBy<com.example.upitracker.data.Category> {
                                if (it.name.startsWith(category, ignoreCase = true)) 0 else 1
                            }.thenBy { it.name.lowercase() }
                        )

                    ExposedDropdownMenu(
                        expanded = isCategoryExpanded,
                        onDismissRequest = { isCategoryExpanded = false }
                    ) {
                        if (filteredCategories.isNotEmpty()) {
                            filteredCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        category = cat.name
                                        isCategoryExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        } else {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (category.isBlank()) {
                                            "No categories yet"
                                        } else {
                                            "No match. Save to create \"$category\""
                                        }
                                    )
                                },
                                onClick = {},
                                enabled = false,
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Apply to existing uncategorized transactions",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Turn off to use this rule only for future SMS imports.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = applyToExisting,
                        onCheckedChange = { applyToExisting = it }
                    )
                }

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
                            selectedLogic,
                            applyToExisting
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
