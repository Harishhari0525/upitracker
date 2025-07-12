@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.upitracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoFixHigh
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
import com.example.upitracker.util.getCategoryIcon
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.upitracker.util.CategoryIcon
import java.io.File

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
    var noteText by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var receiptPath by remember { mutableStateOf<String?>(null) }

    var showFullScreenImage by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    // This effect syncs the local state with the selected transaction
    // whenever the transaction changes or we enter edit mode.
    LaunchedEffect(transaction, isEditMode) {
        transaction?.let {
            descriptionText = it.description
            amountText = it.amount.toString()
            categoryText = it.category ?: ""
            noteText = it.note
            receiptPath = it.receiptImagePath
            imageUri = null
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
                    char.isLetterOrDigit() || char.isWhitespace() } },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            val filteredSuggestions = remember(key1 = categoryText, key2 = userCategories) {
                if (categoryText.isBlank()) {
                    userCategories
                } else {
                    // Just add .name before .contains
                    userCategories.filter { it.name.contains(categoryText, ignoreCase = true) }
                }
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
                            selected = categoryText.equals(category.name, ignoreCase = true),
                            onClick = { categoryText = category.name },
                            label = { Text(category.name) },
                            leadingIcon = {
                                val categoryIcon = getCategoryIcon(category)
                                when (categoryIcon) {
                                    is CategoryIcon.VectorIcon -> {
                                        Icon(
                                            imageVector = categoryIcon.image,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                                        )
                                    }
                                    is CategoryIcon.LetterIcon -> {
                                        Box(
                                            modifier = Modifier
                                                .size(FilterChipDefaults.IconSize)
                                                .background(
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = categoryIcon.letter.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
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

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )

            Spacer(Modifier.height(16.dp))
            val imageToShow = imageUri ?: receiptPath?.let { Uri.fromFile(File(it)) }
            if (imageToShow != null) {
                AsyncImage(
                    model = imageToShow,
                    contentDescription = "Receipt",
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(8.dp))
            }
            Button(onClick = { imagePickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(if (imageToShow != null) "Change Receipt" else "Add Receipt")
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
                        val newReceiptPath = imageUri?.let { mainViewModel.saveReceiptImage(it) }
                        if (newAmount != null) {
                            mainViewModel.updateTransactionDetails(
                                transactionId = transaction!!.id,
                                newDescription = descriptionText,
                                newAmount = newAmount,
                                newCategory = categoryText,
                                newNote = noteText,
                                newReceiptPath = newReceiptPath
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

            if (!transaction!!.receiptImagePath.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text("Receipt", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                AsyncImage(
                    model = File(transaction!!.receiptImagePath!!),
                    contentDescription = "Receipt",
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.large).clickable { showFullScreenImage = true },
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { isEditMode = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Edit Note / Receipt")
                }
            }

            if (showFullScreenImage) {
                FullScreenImageViewer(
                    imagePath = transaction!!.receiptImagePath!!,
                    onDismiss = { showFullScreenImage = false }
                )
            }

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

                if (transaction!!.category.isNullOrBlank()) {
                    Button(
                        onClick = {
                            mainViewModel.reapplyRulesToTransaction(transaction!!)
                            onDismiss() // Close the sheet after trying
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Try Auto-Categorize")
                    }
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
@Composable
private fun FullScreenImageViewer(imagePath: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Important for fullscreen
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable { onDismiss() }, // Click anywhere on the background to dismiss
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = "Fullscreen Receipt",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentScale = ContentScale.Fit // Fit ensures the whole image is visible
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}