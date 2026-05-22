@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.upitracker.R
import com.example.upitracker.ui.components.expressive.ExpressiveSectionHeader
import com.example.upitracker.ui.components.expressive.ExpressiveTopBar
import com.example.upitracker.util.ExpressiveTokens
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
    val coroutineScope = rememberCoroutineScope()

    val regexListState = remember { mutableStateListOf<String>() }

    var isLoading by remember { mutableStateOf(true) }
    var showTestSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        val patterns = RegexPreference.getRegexPatterns(context).first()
        regexListState.clear()
        regexListState.addAll(patterns.sorted())
        isLoading = false
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = ExpressiveTokens.spacing.lg,
                top = ExpressiveTokens.spacing.lg,
                end = ExpressiveTokens.spacing.lg,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md)
        ) {
            item {
                ExpressiveSectionHeader(
                    title = stringResource(R.string.regex_editor_active_patterns_title),
                    subtitle = "Advanced SMS parsing patterns used when default detection fails"
                )
            }

            item {
                ParsingInfoCard()
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ExpressiveTokens.spacing.xl),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (regexListState.isEmpty()) {
                item {
                    EmptyParsingRulesCard()
                }
            } else {
                items(
                    items = regexListState,
                    key = { it }
                ) { regexPattern ->
                    RegexPatternCard(
                        regexPattern = regexPattern,
                        onDelete = {
                            regexListState.remove(regexPattern)
                            coroutineScope.launch {
                                RegexPreference.setRegexPatterns(
                                    context,
                                    regexListState.toSet()
                                )
                                mainViewModel.postPlainSnackbarMessage("Pattern removed.")
                            }
                        }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            text = { Text("Test Pattern") },
            icon = {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = null
                )
            },
            onClick = { showTestSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(ExpressiveTokens.spacing.lg),
            shape = ExpressiveTokens.corners.large,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

    if (showTestSheet) {
        Dialog(
            onDismissRequest = { showTestSheet = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                TestPatternSheetContent(
                    onClose = { showTestSheet = false },
                    onSavePattern = { patternFromSheet ->
                        if (
                            patternFromSheet.isNotBlank() &&
                            !regexListState.contains(patternFromSheet)
                        ) {
                            regexListState.add(0, patternFromSheet)

                            coroutineScope.launch {
                                RegexPreference.setRegexPatterns(
                                    context,
                                    regexListState.toSet()
                                )
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

@Composable
private fun ParsingInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveTokens.corners.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = ExpressiveTokens.elevation.card
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ExpressiveTokens.spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.size(ExpressiveTokens.spacing.md))

            Column {
                Text(
                    text = "Advanced mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Text(
                    text = "Use this only when a bank SMS format is not detected correctly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun EmptyParsingRulesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveTokens.corners.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = ExpressiveTokens.elevation.card
        )
    ) {
        Column(
            modifier = Modifier.padding(ExpressiveTokens.spacing.lg)
        ) {
            Text(
                text = stringResource(R.string.regex_editor_empty_patterns),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.xs))

            Text(
                text = "Tap Test Pattern to create and save a new custom regex.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RegexPatternCard(
    regexPattern: String,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveTokens.corners.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = ExpressiveTokens.elevation.card
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ExpressiveTokens.spacing.lg,
                    vertical = ExpressiveTokens.spacing.md
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Pattern",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = regexPattern,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onDelete
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.regex_editor_delete_pattern_desc),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun TestPatternSheetContent(
    onClose: () -> Unit,
    onSavePattern: (String) -> Unit
) {
    val matchFoundText = stringResource(R.string.regex_editor_test_result_match_found)
    val groupTemplate = stringResource(R.string.regex_editor_test_result_group_template)
    val noMatchText = stringResource(R.string.regex_editor_test_result_no_match)
    val invalidRegexText = stringResource(R.string.regex_editor_test_result_invalid_regex)

    var newRegex by remember { mutableStateOf("") }
    var sampleSmsText by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTestMatchFound by remember { mutableStateOf<Boolean?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
        contentPadding = PaddingValues(
            start = ExpressiveTokens.spacing.lg,
            top = ExpressiveTokens.spacing.md,
            end = ExpressiveTokens.spacing.lg,
            bottom = ExpressiveTokens.spacing.xl
        ),
        verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md)
    ) {
        item {
            ExpressiveTopBar(
                title = "Test Pattern",
                subtitle = "Try a regex against a sample SMS",
                actions = {
                    FilledTonalIconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
            )
        }

        item {
            OutlinedTextField(
                value = sampleSmsText,
                onValueChange = { sampleSmsText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                label = { Text(stringResource(R.string.regex_editor_test_sms_label)) },
                placeholder = {
                    Text(stringResource(R.string.regex_editor_test_sms_placeholder))
                },
                shape = ExpressiveTokens.corners.large
            )
        }

        item {
            OutlinedTextField(
                value = newRegex,
                onValueChange = { newRegex = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.regex_editor_new_pattern_label)) },
                singleLine = true,
                shape = ExpressiveTokens.corners.large
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm)
            ) {
                OutlinedButton(
                    onClick = {
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
                                if (
                                    Regex(
                                        pattern,
                                        RegexOption.IGNORE_CASE
                                    ).containsMatchIn(sampleSmsText)
                                ) {
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
                    enabled = sampleSmsText.isNotBlank(),
                    shape = ExpressiveTokens.corners.large
                ) {
                    Text("Suggest")
                }

                OutlinedButton(
                    onClick = {
                        if (newRegex.isNotBlank() && sampleSmsText.isNotBlank()) {
                            try {
                                val regex = Regex(
                                    newRegex.trim(),
                                    RegexOption.IGNORE_CASE
                                )

                                val match = regex.find(sampleSmsText)

                                if (match != null) {
                                    isTestMatchFound = true

                                    val resultBuilder = StringBuilder(matchFoundText)

                                    match.groupValues.forEachIndexed { index, value ->
                                        resultBuilder.appendLine()
                                        resultBuilder.append(
                                            groupTemplate.format(index, value)
                                        )
                                    }

                                    testResult = resultBuilder.toString()
                                } else {
                                    isTestMatchFound = false
                                    testResult = noMatchText
                                }
                            } catch (_: Exception) {
                                isTestMatchFound = false
                                testResult = invalidRegexText
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = newRegex.isNotBlank() && sampleSmsText.isNotBlank(),
                    shape = ExpressiveTokens.corners.large
                ) {
                    Text("Test")
                }
            }
        }

        item {
            Button(
                onClick = { onSavePattern(newRegex) },
                enabled = newRegex.isNotBlank() && isTestMatchFound == true,
                modifier = Modifier.fillMaxWidth(),
                shape = ExpressiveTokens.corners.large
            ) {
                Text("Save Active Pattern")
            }
        }

        testResult?.let { result ->
            item {
                TestResultCard(
                    result = result,
                    isMatchFound = isTestMatchFound == true
                )
            }
        }
    }
}

@Composable
private fun TestResultCard(
    result: String,
    isMatchFound: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveTokens.corners.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isMatchFound) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(ExpressiveTokens.spacing.lg)
        ) {
            Text(
                text = stringResource(R.string.regex_editor_test_results_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.sm))

            Text(
                text = result,
                color = if (isMatchFound) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(ExpressiveTokens.spacing.sm)
            )
        }
    }
}