@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.upitracker.R
import com.example.upitracker.util.RegexPreference
import com.example.upitracker.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun ParsingRulesContent(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val regexListState = remember { mutableStateListOf<String>() }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // State to control the visibility of our new bottom sheet
    var showTestSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        val patterns = RegexPreference.getRegexPatterns(context).first()
        regexListState.clear()
        regexListState.addAll(patterns.sorted())
        isLoading = false
    }

    // A Scaffold now wraps our content to hold the FloatingActionButton
    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Test Pattern") },
                icon = { Icon(Icons.Default.Science, contentDescription = "Test Pattern") },
                onClick = { showTestSheet = true }
            )
        }
    ) { paddingValues ->
        // The list of existing rules remains on the main screen.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.regex_editor_active_patterns_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    "For advanced users. These are used to find transactions if the app's default parsers fail.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (regexListState.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.regex_editor_empty_patterns),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp)
                    )
                }
            } else {
                items(regexListState, key = { it }) { regexPattern ->
                    ListItem(
                        headlineContent = { Text(regexPattern, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace)) },
                        trailingContent = {
                            IconButton(onClick = {
                                regexListState.remove(regexPattern)
                                coroutineScope.launch {
                                    RegexPreference.setRegexPatterns(context, regexListState.toSet())
                                    mainViewModel.postPlainSnackbarMessage("Pattern removed.")
                                }
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.regex_editor_delete_pattern_desc),
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    // This is our new Bottom Sheet for testing patterns
    if (showTestSheet) {
        Dialog(
            onDismissRequest = { showTestSheet = false },
            // This DialogProperties makes the dialog behave like a full-screen destination
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false // Important for edge-to-edge
            )
        ) {
            // We add our own Surface and handle the insets safely here
            Surface(modifier = Modifier.fillMaxSize()) {
                TestPatternSheetContent(
                    onClose = { showTestSheet = false },
                    onSavePattern = { patternFromSheet ->
                        if (patternFromSheet.isNotBlank() && !regexListState.contains(patternFromSheet)) {
                            regexListState.add(0, patternFromSheet)
                            coroutineScope.launch {
                                RegexPreference.setRegexPatterns(context, regexListState.toSet())
                                mainViewModel.postPlainSnackbarMessage("Pattern saved!")
                            }
                        }
                        showTestSheet = false
                    }
                )
            }
        }
    }
    }

/**
 * FINAL version. This composable uses LazyColumn to ensure scrolling works
 * correctly with the keyboard inside a ModalBottomSheet. It also has
 * consistently styled buttons.
 */
@Composable
private fun TestPatternSheetContent(
    onClose: () -> Unit,
    onSavePattern: (String) -> Unit
) {
    val context = LocalContext.current
    var newRegex by remember { mutableStateOf("") }
    var sampleSmsText by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTestMatchFound by remember { mutableStateOf<Boolean?>(null) }

    // Using LazyColumn is the most robust way to handle scrolling inside a bottom sheet.
    LazyColumn(
        modifier = Modifier.fillMaxWidth()
        .windowInsetsPadding(WindowInsets.systemBars),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp)
    ) {

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.regex_editor_test_area_title), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // --- Input Fields ---
        item {
            OutlinedTextField(
                value = sampleSmsText,
                onValueChange = { sampleSmsText = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                label = { Text(stringResource(R.string.regex_editor_test_sms_label)) },
                placeholder = { Text(stringResource(R.string.regex_editor_test_sms_placeholder)) },
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
        item {
            OutlinedTextField(
                value = newRegex,
                onValueChange = { newRegex = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.regex_editor_new_pattern_label)) },
                singleLine = true
            )
        }
        item { Spacer(Modifier.height(16.dp)) }

        // --- Buttons Row (Corrected and Consistent) ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Using OutlinedButton for secondary actions
                OutlinedButton(
                    onClick = {
                        // ... Full Suggest Logic ...
                        if (sampleSmsText.isNotBlank()) {
                            val patternsToTry = listOf(
                                """credited with Rs\.?\s*([\d,]+\.?\d*)""",
                                """debited with Rs\.?\s*([\d,]+\.?\d*)""",
                                """spent Rs\.?\s*([\d,]+\.?\d*)""",
                                """paid Rs\.?\s*([\d,]+\.?\d*)""",
                                """(?:Rs\.?|INR)\s*([\d,]+\.?\d*)""",
                                """([\d,]+\.\d{2})"""
                            )
                            var suggestedPattern = ""
                            for (pattern in patternsToTry) {
                                if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(sampleSmsText)) {
                                    suggestedPattern = pattern
                                    break
                                }
                            }
                            if (suggestedPattern.isNotBlank()) {
                                newRegex = suggestedPattern
                                testResult = "Suggested a pattern based on your sample text."
                                isTestMatchFound = null
                            } else {
                                testResult = "Could not find a reliable pattern in the sample text."
                                isTestMatchFound = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = sampleSmsText.isNotBlank()
                ) { Text("Suggest") }

                OutlinedButton(
                    onClick = {
                        // ... Full Test Logic ...
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
                    modifier = Modifier.weight(1f),
                    enabled = newRegex.isNotBlank() && sampleSmsText.isNotBlank()
                ) { Text("Test") }
            }
        }

        item {
            // Using a filled Button for the primary "Save" action
            Button(
                onClick = { onSavePattern(newRegex) },
                enabled = newRegex.isNotBlank() && isTestMatchFound == true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text("Save Active Pattern") }
        }

        // --- Test Results Section ---
        testResult?.let { result ->
            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.regex_editor_test_results_title), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = result,
                        color = if (isTestMatchFound == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)).padding(8.dp)
                    )
                }
            }
        }
    }
}