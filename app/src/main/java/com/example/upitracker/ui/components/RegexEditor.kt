@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.upitracker.viewmodel.MainViewModel // For snackbar or error reporting

@Composable
fun RegexEditorComponent( // Renamed to avoid conflict with screen version
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel, // For snackbars
    currentRegexList: List<String>,
    onRegexListChange: (List<String>) -> Unit // Callback with the updated list
) {
    // Use remember with currentRegexList to reset if the initial list changes from parent
    val regexList = remember(currentRegexList) { mutableStateListOf<String>().also { it.addAll(currentRegexList) } }
    var newRegex by remember { mutableStateOf("") }

    Column(modifier = modifier.padding(8.dp)) { // Reduced padding for component use
        Text(
            "Manage Patterns", // Simpler title for component context
            style = MaterialTheme.typography.titleSmall, // Appropriate for component
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = newRegex,
            onValueChange = { newRegex = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("New Regex Pattern") },
            placeholder = { Text("e.g., credited by VPA \\S+") },
            singleLine = true,
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (newRegex.isNotBlank()) {
                            if (!regexList.contains(newRegex.trim())) {
                                regexList.add(0, newRegex.trim())
                                onRegexListChange(regexList.toList()) // Notify parent of change
                                newRegex = ""
                            } else {
                                mainViewModel.postSnackbarMessage("Pattern already exists.")
                            }
                        } else {
                            mainViewModel.postSnackbarMessage("Pattern cannot be empty.")
                        }
                    },
                    enabled = newRegex.isNotBlank()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Regex")
                }
            }
        )
        Spacer(Modifier.height(12.dp))

        if (regexList.isEmpty()) {
            Text(
                "No custom patterns.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 10.dp)
            )
        } else {
            // Limit height if used in a constrained space like a dialog
            LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                itemsIndexed(regexList, key = { _, item -> item }) { index, regexPattern ->
                    ListItem(
                        headlineContent = { Text(regexPattern, style = MaterialTheme.typography.bodyMedium) },
                        trailingContent = {
                            IconButton(onClick = {
                                regexList.removeAt(index)
                                onRegexListChange(regexList.toList()) // Notify parent of change
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Pattern", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (index < regexList.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }
        }
    }
}