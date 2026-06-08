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
import com.example.upitracker.sms.parseUpiSms
import com.example.upitracker.sms.SmsProcessingService

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
    val context = LocalContext.current
    val matchFoundText = stringResource(R.string.regex_editor_test_result_match_found)
    val groupTemplate = stringResource(R.string.regex_editor_test_result_group_template)
    val noMatchText = stringResource(R.string.regex_editor_test_result_no_match)
    val invalidRegexText = stringResource(R.string.regex_editor_test_result_invalid_regex)

    var newRegex by remember { mutableStateOf("") }
    var sampleSmsText by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTestMatchFound by remember { mutableStateOf<Boolean?>(null) }
    
    // New states for detailed results
    var detectedBank by remember { mutableStateOf<String?>(null) }
    var parsedAmount by remember { mutableStateOf<Double?>(null) }
    var parsedType by remember { mutableStateOf<String?>(null) }
    var parsedMerchant by remember { mutableStateOf<String?>(null) }

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
                title = "Test Parser",
                subtitle = "Validate how the app will read your SMS",
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
                label = { Text("Custom Pattern (Optional)") },
                placeholder = { Text("Enter regex to override defaults") },
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
                                """(?:Rs\.?|INR|₹)\s*([\d,]+\.?\d*)""",
                                """credited with RS\s*([\d,]+\.?\d*)""",
                                """debited with RS\s*([\d,]+\.?\d*)""",
                                """spent RS\s*([\d,]+\.?\d*)""",
                                """paid RS\s*([\d,]+\.?\d*)""",
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
                                testResult = "Suggested a basic amount pattern."
                            } else {
                                testResult = "Could not suggest a pattern."
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = sampleSmsText.isNotBlank(),
                    shape = ExpressiveTokens.corners.large
                ) {
                    Text("Suggest")
                }

                Button(
                    onClick = {
                        if (sampleSmsText.isNotBlank()) {
                            try {
                                val bank = SmsProcessingService.resolveBankName(sender = "BANK-SMS", body = sampleSmsText)
                                detectedBank = bank

                                val customList = if (newRegex.isNotBlank()) {
                                    listOf(Regex(newRegex.trim(), RegexOption.IGNORE_CASE))
                                } else emptyList()

                                val result = parseUpiSms(
                                    message = sampleSmsText,
                                    sender = "BANK-SMS",
                                    smsDate = System.currentTimeMillis(),
                                    customRegexList = customList,
                                    bankName = bank
                                )

                                if (result != null) {
                                    isTestMatchFound = true
                                    parsedAmount = result.amount
                                    parsedType = result.type
                                    parsedMerchant = result.senderOrReceiver
                                    
                                    val builder = StringBuilder("Extraction Successful!")
                                    testResult = builder.toString()
                                } else {
                                    isTestMatchFound = false
                                    testResult = "No UPI transaction detected."
                                    parsedAmount = null
                                    parsedType = null
                                    parsedMerchant = null
                                }
                            } catch (e: Exception) {
                                isTestMatchFound = false
                                testResult = "Error: ${e.message}"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = sampleSmsText.isNotBlank(),
                    shape = ExpressiveTokens.corners.large
                ) {
                    Text("Test Parser")
                }
            }
        }

        if (isTestMatchFound == true) {
            item {
                Button(
                    onClick = { onSavePattern(newRegex) },
                    enabled = newRegex.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveTokens.corners.large
                ) {
                    Text("Save Custom Pattern")
                }
            }
        }

        testResult?.let { result ->
            item {
                DetailedTestResultCard(
                    message = result,
                    isMatchFound = isTestMatchFound == true,
                    bank = detectedBank,
                    amount = parsedAmount,
                    type = parsedType,
                    merchant = parsedMerchant
                )
            }
        }
    }
}

@Composable
private fun DetailedTestResultCard(
    message: String,
    isMatchFound: Boolean,
    bank: String?,
    amount: Double?,
    type: String?,
    merchant: String?
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
                text = "Parser Results",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.sm))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(ExpressiveTokens.spacing.md),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ResultRow("Status", message)
                if (bank != null) ResultRow("Detected Bank", bank)
                if (amount != null) ResultRow("Amount", "₹${"%.2f".format(amount)}")
                if (type != null) ResultRow("Type", type)
                if (merchant != null) ResultRow("Merchant/Party", merchant)
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}