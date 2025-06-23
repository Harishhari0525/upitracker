@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.util.RegexPreference
import com.example.upitracker.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun ParsingRulesContent(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val regexListState = remember { mutableStateListOf<String>() }
    var newRegex by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // --- State for Test Area ---
    var sampleSmsText by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTestMatchFound by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        val patterns = RegexPreference.getRegexPatterns(context).first()
        regexListState.clear()
        regexListState.addAll(patterns.sorted()) // Keep list sorted for consistency
        isLoading = false
    }

    Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // --- Active Patterns Section ---
            Text(stringResource(R.string.regex_editor_active_patterns_title), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp))
            Text("For advanced users. These are used to find transactions if the app's default parsers fail.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (regexListState.isEmpty()) {
                Text(
                    stringResource(R.string.regex_editor_empty_patterns),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    regexListState.forEach { regexPattern ->
                        ListItem(
                            headlineContent = { Text(regexPattern,
                                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace)) },
                            trailingContent = {

                                IconButton(onClick = {
                                    val patternToRemove = regexPattern
                                    regexListState.remove(patternToRemove)
                                    coroutineScope.launch {
                                        RegexPreference.setRegexPatterns(context, regexListState.toSet())
                                        mainViewModel.postPlainSnackbarMessage("Pattern removed.")
                                    }
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription
                                    = stringResource(R.string.regex_editor_delete_pattern_desc),
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        )
                        HorizontalDivider()
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // --- Add New Regex Field ---
            OutlinedTextField(
                value = newRegex,
                onValueChange = { newRegex = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.regex_editor_new_pattern_label)) },
                placeholder = { Text(stringResource(R.string.regex_editor_new_pattern_placeholder)) },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val trimmedRegex = newRegex.trim()
                            if (trimmedRegex.isNotBlank()) {
                                if (!regexListState.contains(trimmedRegex)) {
                                    regexListState.add(0, trimmedRegex)
                                    newRegex = ""
                                    coroutineScope.launch {
                                        RegexPreference.setRegexPatterns(context, regexListState.toSet())
                                        mainViewModel.postPlainSnackbarMessage("Pattern saved!")
                                    }
                                } else {
                                    mainViewModel.postPlainSnackbarMessage(context.getString(R.string.regex_editor_pattern_exists_message))
                                }
                            } else {
                                mainViewModel.postPlainSnackbarMessage(context.getString(R.string.regex_editor_pattern_empty_message))
                            }
                        },
                        enabled = newRegex.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.regex_editor_add_button_desc))
                    }
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- ✨ New: Test Area ✨ ---
            Text(stringResource(R.string.regex_editor_test_area_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = sampleSmsText,
                onValueChange = { sampleSmsText = it },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                label = { Text(stringResource(R.string.regex_editor_test_sms_label)) },
                placeholder = { Text(stringResource(R.string.regex_editor_test_sms_placeholder)) },
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (newRegex.isNotBlank() && sampleSmsText.isNotBlank()) {
                        try {
                            val regex = Regex(newRegex.trim(), RegexOption.IGNORE_CASE)
                            val match = regex.find(sampleSmsText)
                            if (match != null) {
                                isTestMatchFound = true
                                val resultBuilder = StringBuilder(context.getString(R.string.regex_editor_test_result_match_found))
                                match.groupValues.forEachIndexed { index, value ->
                                    resultBuilder.appendLine()
                                    resultBuilder.append(context.getString(R.string.regex_editor_test_result_group_template, index, value))
                                }
                                testResult = resultBuilder.toString()
                            } else {
                                isTestMatchFound = false
                                testResult = context.getString(R.string.regex_editor_test_result_no_match)
                            }
                        } catch (_: Exception) {
                            isTestMatchFound = false
                            testResult = context.getString(R.string.regex_editor_test_result_invalid_regex)
                        }
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = newRegex.isNotBlank() && sampleSmsText.isNotBlank()
            ) {
                Text(stringResource(R.string.regex_editor_test_button))
            }
            Spacer(Modifier.height(8.dp))
            testResult?.let { result ->
                Text(
                    stringResource(R.string.regex_editor_test_results_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = result,
                    color = if (isTestMatchFound == true) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(8.dp)
                )
            }
        }
    }