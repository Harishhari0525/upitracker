@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.data.Transaction
import com.example.upitracker.ui.components.DeleteTransactionConfirmationDialog
import com.example.upitracker.ui.components.EditCategoryDialog
import com.example.upitracker.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * This is the content that will be displayed inside the ModalBottomSheet.
 */
@Composable
fun TransactionDetailSheetContent(
    mainViewModel: MainViewModel,
    onDismiss: () -> Unit // Used to close the sheet from a button inside
) {
    val transaction by mainViewModel.selectedTransaction.collectAsState()
    val userCategories by mainViewModel.userCategories.collectAsState()

    // Show a loading indicator while data is being fetched, or if transaction is null
    if (transaction == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp), // Give it a sensible height
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // This is the actual content for the sheet
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Add padding for the content inside the sheet
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
    ) {
        // State for nested dialogs
        var showEditCategoryDialog by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }

        // Standard M3 drag handle for the bottom sheet
       // BottomSheetDefaults.DragHandle()

        Spacer(Modifier.height(8.dp))

        TransactionDetailHeader(transaction!!)
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Transaction details section
        DetailRow(label = stringResource(R.string.detail_label_description), value = transaction!!.description)
        DetailRow(label = stringResource(R.string.detail_label_category), value = transaction!!.category ?: "Uncategorized")
        DetailRow(label = stringResource(R.string.detail_label_party), value = transaction!!.senderOrReceiver)
        DetailRow(label = stringResource(R.string.detail_label_date_time), value = formatFullDateTime(transaction!!.date))
        if (transaction!!.note.isNotBlank()) {
            DetailRow(label = stringResource(R.string.detail_label_note), value = transaction!!.note)
        }

        Spacer(Modifier.height(24.dp))

        // Action Buttons (stacked vertically)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showEditCategoryDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.button_edit_category))
            }
            OutlinedButton(
                onClick = {
                    mainViewModel.toggleTransactionArchiveStatus(transaction!!, archive = !transaction!!.isArchived)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Archive, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(if (transaction!!.isArchived) stringResource(R.string.button_restore) else stringResource(R.string.button_archive))
            }
            TextButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
            }
        }

        // Handle showing the nested dialogs for Edit and Delete actions
        if (showEditCategoryDialog) {
            EditCategoryDialog(
                transaction = transaction!!,
                onDismiss = { showEditCategoryDialog = false },
                suggestionCategories = userCategories,
                onSaveCategory = { transactionId, newCategory ->
                    mainViewModel.updateTransactionCategory(transactionId, newCategory)
                    showEditCategoryDialog = false
                }
            )
        }
        if (showDeleteDialog) {
            DeleteTransactionConfirmationDialog(
                transactionDescription = transaction!!.description,
                onConfirm = {
                    mainViewModel.deleteTransaction(transaction!!)
                    showDeleteDialog = false
                    onDismiss() // Close the bottom sheet after deleting
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}


// These helper composables can stay at the bottom of the file
@Composable
private fun TransactionDetailHeader(transaction: Transaction) {
    val creditColor = if (isSystemInDarkTheme()) Color(0xFF63DC94) else Color(0xFF006D3D)
    val amountColor = if (transaction.type.equals("CREDIT", ignoreCase = true)) creditColor else MaterialTheme.colorScheme.error

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = transaction.type.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "₹${"%.2f".format(transaction.amount)}",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
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