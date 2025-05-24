package com.example.upitracker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.upitracker.util.RegexPreference
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegexEditorScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var regexList by remember { mutableStateOf(listOf<String>()) }
    var newRegex by remember { mutableStateOf("") }

    // Load regex patterns from DataStore
    LaunchedEffect(Unit) {
        RegexPreference.getRegexPatterns(context).collect {
            regexList = it.toList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit UPI SMS Regex") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
            Text("Active Patterns:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
            ) {
                items(regexList.size) { idx ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(regexList[idx], Modifier.weight(1f))
                        IconButton(onClick = {
                            scope.launch {
                                val updated = regexList.toMutableList().also { it.removeAt(idx) }
                                RegexPreference.setRegexPatterns(context, updated.toSet())
                            }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // Fix for interactionSource/type and visualTransformation issues
            BasicTextField(
                value = newRegex,
                onValueChange = { newRegex = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true,
                decorationBox = { innerTextField ->
                    TextFieldDefaults.DecorationBox(
                        value = newRegex,
                        visualTransformation = VisualTransformation.None,
                        innerTextField = innerTextField,
                        enabled = true,
                        singleLine = true,
                        placeholder = { Text("Paste regex here...") },
                        label = null,
                        leadingIcon = null,
                        trailingIcon = null,
                        interactionSource = remember { MutableInteractionSource() },
                        supportingText = null,
                        shape = TextFieldDefaults.shape,
                        colors = TextFieldDefaults.colors()
                    )
                }
            )
            Button(
                onClick = {
                    scope.launch {
                        if (newRegex.isNotBlank()) {
                            val updated = regexList.toMutableList().also { it.add(newRegex) }
                            RegexPreference.setRegexPatterns(context, updated.toSet())
                            newRegex = ""
                        } else {
                            Toast.makeText(context, "Regex cannot be empty.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) { Text("Add") }
        }
    }
}
