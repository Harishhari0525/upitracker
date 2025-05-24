@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@Composable
fun RegexEditorScreen(
    currentRegex: List<String>,
    onSave: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    var regexes by remember { mutableStateOf(currentRegex.toMutableList()) }
    var newRegex by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit UPI Regex") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onSave(regexes) }) {
                        Icon(Icons.Filled.Done, contentDescription = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            regexes.forEachIndexed { index, regex ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(regex, Modifier.weight(1f))
                    IconButton(onClick = { regexes.removeAt(index) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove")
                    }
                }
                Divider()
            }
            OutlinedTextField(
                value = newRegex,
                onValueChange = { newRegex = it },
                label = { Text("Add New Regex") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (newRegex.isNotBlank()) {
                        regexes.add(newRegex)
                        newRegex = ""
                    }
                }
            ) { Text("Add Regex") }
        }
    }
}