@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.data.Transaction
import com.example.upitracker.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionDetailSheetContent(
    mainViewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val transaction by mainViewModel.selectedTransaction.collectAsState()
    val userCategories by mainViewModel.userCategories.collectAsState()

    // --- STATE MANAGEMENT ---
    var isEditMode by remember { mutableStateOf(false) }
    var descriptionText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("") }

    // This effect syncs the local state with the selected transaction
    // whenever the transaction changes or we enter edit mode.
    LaunchedEffect(transaction, isEditMode) {
        transaction?.let {
            descriptionText = it.description
            amountText = it.amount.toString()
            categoryText = it.category ?: ""
        }
    }

    if (transaction == null) {
        Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val isManualEntry = transaction!!.senderOrReceiver == "Manual Entry"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (isEditMode) {
            val title = if (isManualEntry) "Edit Transaction Details" else "Edit Category Details"

            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

            // Category field and suggestions are now first for better UX
            OutlinedTextField(
                value = categoryText,
                onValueChange = { categoryText = it.filter { char ->
                    char.isLetterOrDigit() || char.isWhitespace()
                } },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))

            val filteredSuggestions = remember(categoryText, userCategories) {
                if (categoryText.isBlank()) userCategories else userCategories.filter { it.contains(categoryText, ignoreCase = true) }
            }
            if (filteredSuggestions.isNotEmpty()) {
                Text("Suggestions", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredSuggestions.forEach { category ->
                        FilterChip(
                            selected = categoryText.equals(category, ignoreCase = true),
                            onClick = { categoryText = category },
                            label = { Text(category) }
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            if (isManualEntry) {
                // For manual entries, show editable TextFields
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("₹") }
                )
            } else {
                // For SMS entries, show non-editable DetailRow
                DetailRow(label = stringResource(R.string.detail_label_description), value = transaction!!.description)
                DetailRow(label = stringResource(R.string.detail_label_amount), value = "₹${"%.2f".format(transaction!!.amount)}")
            }
            Spacer(Modifier.height(24.dp))

            // Action buttons for Edit Mode
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { isEditMode = false }, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val newAmount = amountText.toDoubleOrNull()
                        if (newAmount != null) {
                            mainViewModel.updateTransactionDetails(
                                transactionId = transaction!!.id,
                                newDescription = descriptionText,
                                newAmount = newAmount,
                                newCategory = categoryText
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        } else {
            // --- VIEW MODE UI ---
            TransactionDetailHeader(transaction!!)
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            DetailRow(label = stringResource(R.string.detail_label_description), value = transaction!!.description)
            DetailRow(label = stringResource(R.string.detail_label_category), value = transaction!!.category ?: "Uncategorized")
            DetailRow(label = stringResource(R.string.detail_label_party), value = transaction!!.senderOrReceiver)
            DetailRow(label = stringResource(R.string.detail_label_date_time), value = formatFullDateTime(transaction!!.date))
            if (transaction!!.note.isNotBlank()) {
                DetailRow(label = stringResource(R.string.detail_label_note), value = transaction!!.note)
            }
            Spacer(Modifier.height(24.dp))

            // Action Buttons for View Mode
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { isEditMode = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    val buttonText = if (isManualEntry) "Edit Transaction" else "Edit Category"
                    Text(buttonText)
                }
                OutlinedButton(
                    onClick = {
                        mainViewModel.toggleTransactionArchiveStatus(transaction!!, archive = true)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Archive, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.button_archive))
                }
                TextButton(
                    onClick = {
                        mainViewModel.deleteTransaction(transaction!!)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// --- Helper Composables ---

@Composable
private fun TransactionDetailHeader(transaction: Transaction) {
    val creditColor = if (isSystemInDarkTheme()) Color(0xFF63DC94) else Color(0xFF006D3D)
    val amountColor = if (transaction.type.equals("CREDIT", ignoreCase = true)) creditColor else MaterialTheme.colorScheme.error
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(text = transaction.type.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = "₹${"%.2f".format(transaction.amount)}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = amountColor)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatFullDateTime(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (_: Exception) {
        "Invalid Date"
    }
}