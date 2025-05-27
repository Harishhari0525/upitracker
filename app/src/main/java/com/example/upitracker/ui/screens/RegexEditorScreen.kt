@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource // ✨ Import stringResource ✨
import androidx.compose.ui.unit.dp
import com.example.upitracker.R // ✨ Import your app's R class ✨
import com.example.upitracker.util.RegexPreference
import com.example.upitracker.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun RegexEditorScreen(
    onBack: () -> Unit,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val regexListState = remember { mutableStateListOf<String>() }
    var newRegex by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        val patterns = RegexPreference.getRegexPatterns(context).first()
        regexListState.clear()
        regexListState.addAll(patterns)
        isLoading = false
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.regex_editor_top_bar_title)) }, // ✨
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back_button_description)) // ✨
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                RegexPreference.setRegexPatterns(context, regexListState.toSet())
                                mainViewModel.postSnackbarMessage(context.getString(R.string.regex_editor_patterns_saved_message)) // ✨
                                onBack()
                            }
                        },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Filled.Done, contentDescription = stringResource(R.string.regex_editor_save_patterns_desc)) // ✨
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                stringResource(R.string.regex_editor_description), // ✨
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = newRegex,
                onValueChange = { newRegex = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.regex_editor_new_pattern_label)) }, // ✨
                placeholder = { Text(stringResource(R.string.regex_editor_new_pattern_placeholder)) }, // ✨
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val trimmedRegex = newRegex.trim()
                            if (trimmedRegex.isNotBlank()) {
                                if (!regexListState.contains(trimmedRegex)) {
                                    regexListState.add(0, trimmedRegex)
                                    newRegex = ""
                                } else {
                                    mainViewModel.postSnackbarMessage(context.getString(R.string.regex_editor_pattern_exists_message)) // ✨
                                }
                            } else {
                                mainViewModel.postSnackbarMessage(context.getString(R.string.regex_editor_pattern_empty_message)) // ✨
                            }
                        },
                        enabled = newRegex.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.regex_editor_add_button_desc)) // ✨
                    }
                }
            )
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.regex_editor_active_patterns_title), style = MaterialTheme.typography.titleMedium) // ✨
            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (regexListState.isEmpty()) {
                Text(
                    stringResource(R.string.regex_editor_empty_patterns), // ✨
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 20.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(regexListState, key = { _, item -> item }) { index, regexPattern ->
                        ListItem(
                            headlineContent = { Text(regexPattern, style = MaterialTheme.typography.bodyLarge) },
                            trailingContent = {
                                IconButton(onClick = {
                                    regexListState.removeAt(index)
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.regex_editor_delete_pattern_desc), tint = MaterialTheme.colorScheme.error) // ✨
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (index < regexListState.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }
            }
        }
    }
}