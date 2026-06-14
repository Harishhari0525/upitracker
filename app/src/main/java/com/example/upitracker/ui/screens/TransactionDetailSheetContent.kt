@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.upitracker.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.text.NumberFormat
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import com.example.upitracker.util.ExpressiveTokens
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.upitracker.ui.components.MerchantDnaCard
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
    var showRuleCreatorDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

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

    LaunchedEffect(transaction) {
        transaction?.let {
            // Only load if it's a real merchant (not Manual Entry)
            if (it.senderOrReceiver != "Manual Entry") {
                mainViewModel.loadMerchantDna(it.senderOrReceiver)
            }
        }
    }

    // Clear on exit
    DisposableEffect(Unit) {
        onDispose { mainViewModel.clearMerchantDna() }
    }

    if (transaction == null) {
        Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val isManualEntry = transaction!!.senderOrReceiver == "Manual Entry"

    val merchantDna by mainViewModel.merchantDna.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = ExpressiveTokens.spacing.lg,
                end = ExpressiveTokens.spacing.lg,
                top = ExpressiveTokens.spacing.sm,
                bottom = ExpressiveTokens.spacing.lg
            )
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm)
    ) {
        if (isEditMode) {
            val title = if (isManualEntry) "Edit Transaction Details" else "Edit Category Details"

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Update category, note, and receipt details",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            var isCategoryExpanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = isCategoryExpanded,
                onExpandedChange = { isCategoryExpanded = it }
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                        .fillMaxWidth(),
                    value = categoryText,
                    onValueChange = {
                        categoryText = it.filter { char -> char.isLetterOrDigit() || char.isWhitespace() }
                        isCategoryExpanded = true
                    },
                    label = { Text("Category") },
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    shape = ExpressiveTokens.corners.large
                )

                val filteredSuggestions = userCategories.filter {
                    it.name.contains(categoryText, ignoreCase = true)
                }

                if (filteredSuggestions.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = isCategoryExpanded,
                        onDismissRequest = { isCategoryExpanded = false }
                    ) {
                        filteredSuggestions.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    categoryText = category.name
                                    isCategoryExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (isManualEntry) {
                // For manual entries, show editable TextFields
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveTokens.corners.large
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    prefix = { Text("₹") },
                    shape = ExpressiveTokens.corners.large
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
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = ExpressiveTokens.corners.large
            )

            val imageToShow = imageUri ?: receiptPath?.let { Uri.fromFile(File(it)) }
            if (imageToShow != null) {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = imageToShow,
                    contentDescription = "Receipt",
                    modifier = Modifier.fillMaxWidth().height(120.dp).clip(ExpressiveTokens.corners.large),
                    contentScale = ContentScale.Crop
                )
            }
            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = ExpressiveTokens.corners.large
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(if (imageToShow != null) "Change Receipt" else "Add Receipt")
            }


            Spacer(Modifier.height(8.dp))

            // Action buttons for Edit Mode
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { isEditMode = false },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = ExpressiveTokens.corners.large) {
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
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = ExpressiveTokens.corners.large
                ) {
                    Text("Save")
                }
            }
        } else {
            // --- VIEW MODE UI ---
            val allTransactions by mainViewModel.transactions.collectAsState()
            val isDebit = transaction!!.type == "DEBIT"
            val linkedTxn = if (isDebit) {
                transaction!!.linkedTransactionId?.let { linkId ->
                    allTransactions.find { it.id == linkId }
                }
            } else {
                allTransactions.find { it.linkedTransactionId == transaction!!.id }
            }

            TransactionDetailHeader(transaction = transaction!!, linkedTransaction = linkedTxn)

            // ✨ DNA Card if available
            if (merchantDna != null) {
                MerchantDnaCard(
                    merchantName = transaction!!.senderOrReceiver,
                    dna = merchantDna!!
                )
            }

            var showLinkDialog by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ExpressiveTokens.corners.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(vertical = ExpressiveTokens.spacing.sm)) {
                    DetailRow(label = stringResource(R.string.detail_label_description), value = transaction!!.description)
                    DetailRow(label = stringResource(R.string.detail_label_category), value = transaction!!.category ?: "Uncategorized")
                    DetailRow(label = stringResource(R.string.detail_label_party), value = transaction!!.senderOrReceiver)
                    DetailRow(label = stringResource(R.string.detail_label_date_time), value = formatFullDateTime(transaction!!.date))
                    if (transaction!!.note.isNotBlank()) {
                        DetailRow(label = stringResource(R.string.detail_label_note), value = transaction!!.note)
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = ExpressiveTokens.spacing.lg, vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    if (isDebit) {
                        val linkedRefund = transaction!!.linkedTransactionId?.let { linkId ->
                            allTransactions.find { it.id == linkId }
                        }
                        if (linkedRefund != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = ExpressiveTokens.spacing.lg, vertical = ExpressiveTokens.spacing.sm)
                            ) {
                                Text(
                                    text = "Linked Refund",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "+₹${"%.2f".format(linkedRefund.amount)} on ${formatFullDateTime(linkedRefund.date)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF16A34A)
                                        )
                                        Text(
                                            text = linkedRefund.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(onClick = { mainViewModel.unlinkRefund(transaction!!.id) }) {
                                        Text("Unlink", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = ExpressiveTokens.spacing.lg, vertical = ExpressiveTokens.spacing.sm)
                            ) {
                                Text(
                                    text = "Refund Tracking",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { showLinkDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = ExpressiveTokens.corners.medium
                                ) {
                                    Text("Link a Refund / Return")
                                }
                            }
                        }
                    } else {
                        val linkedDebit = allTransactions.find { it.linkedTransactionId == transaction!!.id }
                        if (linkedDebit != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = ExpressiveTokens.spacing.lg, vertical = ExpressiveTokens.spacing.sm)
                            ) {
                                Text(
                                    text = "Refund for Transaction",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "-₹${"%.2f".format(linkedDebit.amount)} on ${formatFullDateTime(linkedDebit.date)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = linkedDebit.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(onClick = { mainViewModel.unlinkRefund(linkedDebit.id) }) {
                                        Text("Unlink", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = ExpressiveTokens.spacing.lg, vertical = ExpressiveTokens.spacing.sm)
                            ) {
                                Text(
                                    text = "Refund Association",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { showLinkDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = ExpressiveTokens.corners.medium
                                ) {
                                    Text("Link to Original Expense")
                                }
                            }
                        }
                    }
                }
            }

            if (showLinkDialog) {
                LinkTransactionDialog(
                    currentTransaction = transaction!!,
                    candidates = allTransactions,
                    onDismiss = { showLinkDialog = false },
                    onLinkSelected = { targetId ->
                        if (isDebit) {
                            mainViewModel.linkTransactions(debitId = transaction!!.id, creditId = targetId)
                        } else {
                            mainViewModel.linkTransactions(debitId = targetId, creditId = transaction!!.id)
                        }
                        showLinkDialog = false
                    }
                )
            }

            if (!transaction!!.receiptImagePath.isNullOrBlank()) {
                Text(
                    text = "Receipt",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                AsyncImage(
                    model = File(transaction!!.receiptImagePath!!),
                    contentDescription = "Receipt",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(ExpressiveTokens.corners.large)
                        .clickable { showFullScreenImage = true },
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(8.dp))

            val buttonModifier = Modifier.weight(1f).height(50.dp)

            if (showFullScreenImage) {
                FullScreenImageViewer(
                    imagePath = transaction!!.receiptImagePath!!,
                    onDismiss = { showFullScreenImage = false }
                )
            }

            if (showRuleCreatorDialog) {
                QuickRuleCreatorDialog(
                    transaction = transaction!!,
                    userCategories = userCategories,
                    onDismiss = { showRuleCreatorDialog = false },
                    onSaveRule = { field, keyword, category ->
                        mainViewModel.addCategoryRule(
                            field = field,
                            matcher = com.example.upitracker.data.RuleMatcher.CONTAINS,
                            keyword = keyword,
                            category = category,
                            priority = 0,
                            logic = com.example.upitracker.data.RuleLogic.ANY
                        )
                        showRuleCreatorDialog = false
                    }
                )
            }

            if (!isManualEntry) {
                Button(
                    onClick = { showRuleCreatorDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = ExpressiveTokens.corners.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Create Auto-Category Rule")
                }
                Spacer(Modifier.height(8.dp))
            }

            // Action Buttons for View Mode
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { isEditMode = true },
                    modifier = buttonModifier,
                    shape = ExpressiveTokens.corners.large
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit")
                }

                FilledTonalButton(
                    onClick = { shareTransactionDetails(context, transaction!!) },
                    modifier = buttonModifier,
                    shape = ExpressiveTokens.corners.large
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
            }

            // Row 2: Archive & (Auto-Cat OR Delete)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        mainViewModel.toggleTransactionArchiveStatus(transaction!!, archive = true)
                        onDismiss()
                    },
                    modifier = buttonModifier,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = ExpressiveTokens.corners.large
                ) {
                    Icon(Icons.Default.Archive, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Archive")
                }

                // Logic: If Uncategorized, show "Auto-Cat" here.
                // If Categorized, show "Delete" here (to keep it compact).
                if (transaction!!.category.isNullOrBlank()) {
                    FilledTonalButton(
                        onClick = {
                            mainViewModel.reapplyRulesToTransaction(transaction!!)
                            onDismiss()
                        },
                        modifier = buttonModifier,
                        shape = ExpressiveTokens.corners.large
                    ) {
                        Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Auto-Cat")
                    }
                } else {
                    // Categorized? Delete button goes here.
                    DeleteButton(
                        onClick = {
                            mainViewModel.deleteTransaction(transaction!!)
                            onDismiss()
                        },
                        modifier = buttonModifier
                    )
                }
            }

            // Row 3: Delete (Only if Uncategorized)
            // Since "Auto-Cat" took the spot above, we need a dedicated Delete button below.
            if (transaction!!.category.isNullOrBlank()) {
                DeleteButton(
                    onClick = {
                        mainViewModel.deleteTransaction(transaction!!)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                )
            }
        }
    }
}

@Composable
private fun TransactionDetailHeader(transaction: Transaction, linkedTransaction: Transaction? = null) {
    val creditColor = if (isSystemInDarkTheme()) {
        Color(0xFF63DC94)
    } else {
        Color(0xFF006D3D)
    }

    val amountColor = if (transaction.type.equals("CREDIT", ignoreCase = true)) {
        creditColor
    } else {
        MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ExpressiveTokens.corners.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = ExpressiveTokens.elevation.card
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ExpressiveTokens.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = transaction.type.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )

            Text(
                text = "₹${"%.2f".format(transaction.amount)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = amountColor
            )

            if (linkedTransaction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val isDebit = transaction.type.equals("DEBIT", ignoreCase = true)
                val netAmount = if (isDebit) {
                    transaction.amount - linkedTransaction.amount
                } else {
                    linkedTransaction.amount - transaction.amount
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
                    shape = ExpressiveTokens.corners.medium,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isDebit) "Net Spent:" else "Net Expense:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "₹${"%.2f".format(netAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDebit) {
                                if (netAmount > 0) MaterialTheme.colorScheme.error else creditColor
                            } else {
                                if (netAmount > 0) MaterialTheme.colorScheme.error else creditColor
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = transaction.senderOrReceiver,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

private fun shareTransactionDetails(context: Context, transaction: Transaction) {
    val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(transaction.date))
    val text = """
        Transaction Details:
        Amount: ₹${transaction.amount}
        To/From: ${transaction.senderOrReceiver}
        Date: $date
        Description: ${transaction.description}
        Category: ${transaction.category ?: "Uncategorized"}
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Transaction"))
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = ExpressiveTokens.spacing.lg,
                vertical = ExpressiveTokens.spacing.sm
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
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
@Composable
private fun DeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.error
        ),
        shape = ExpressiveTokens.corners.large
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null
        )

        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
fun QuickRuleCreatorDialog(
    transaction: Transaction,
    userCategories: List<com.example.upitracker.data.Category>,
    onDismiss: () -> Unit,
    onSaveRule: (com.example.upitracker.data.RuleField, String, String) -> Unit
) {
    var selectedField by remember { mutableStateOf(com.example.upitracker.data.RuleField.SENDER_OR_RECEIVER) }
    var keywordText by remember { mutableStateOf(transaction.senderOrReceiver) }
    var selectedCategory by remember { mutableStateOf(transaction.category ?: "") }
    var isCategoryMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Auto-Categorization Rule") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Automatically categorize future transactions matching this keyword.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Match Field Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.example.upitracker.data.RuleField.entries.forEach { field ->
                        val label =
                            if (field == com.example.upitracker.data.RuleField.SENDER_OR_RECEIVER) "Merchant" else "SMS Text"
                        FilterChip(
                            selected = selectedField == field,
                            onClick = {
                                selectedField = field
                                keywordText =
                                    if (field == com.example.upitracker.data.RuleField.SENDER_OR_RECEIVER) {
                                        transaction.senderOrReceiver
                                    } else {
                                        transaction.description
                                    }
                            },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Keyword Input
                OutlinedTextField(
                    value = keywordText,
                    onValueChange = { keywordText = it },
                    label = { Text("Match Keyword") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = ExpressiveTokens.corners.large
                )

                // Category Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = isCategoryMenuExpanded,
                    onExpandedChange = { isCategoryMenuExpanded = !isCategoryMenuExpanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        value = selectedCategory.ifBlank { "Select Category" },
                        onValueChange = {},
                        label = { Text("Target Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryMenuExpanded) },
                        shape = ExpressiveTokens.corners.large
                    )

                    ExposedDropdownMenu(
                        expanded = isCategoryMenuExpanded,
                        onDismissRequest = { isCategoryMenuExpanded = false }
                    ) {
                        userCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category.name
                                    isCategoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (keywordText.isNotBlank() && selectedCategory.isNotBlank()) {
                        onSaveRule(selectedField, keywordText, selectedCategory)
                    }
                },
                enabled = keywordText.isNotBlank() && selectedCategory.isNotBlank()
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LinkTransactionDialog(
    currentTransaction: Transaction,
    candidates: List<Transaction>,
    onDismiss: () -> Unit,
    onLinkSelected: (Int) -> Unit
) {
    var showAllMerchants by remember { mutableStateOf(false) }
    
    val filteredCandidates = remember(candidates, currentTransaction, showAllMerchants) {
        candidates.filter { txn ->
            txn.id != currentTransaction.id &&
            txn.type != currentTransaction.type &&
            txn.linkedTransactionId == null &&
            (showAllMerchants || txn.senderOrReceiver.equals(currentTransaction.senderOrReceiver, ignoreCase = true))
        }.sortedByDescending { it.date }
    }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.Builder()
        .setLanguage("en")
        .setRegion("IN")
        .build()) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Link Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filter by same merchant", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = !showAllMerchants,
                        onCheckedChange = { showAllMerchants = !it }
                    )
                }

                if (filteredCandidates.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No matching transactions found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCandidates, key = { it.id }) { txn ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onLinkSelected(txn.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = txn.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${dateFormat.format(Date(txn.date))} • ${txn.senderOrReceiver}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = (if (txn.type == "CREDIT") "+" else "-") + currencyFormatter.format(txn.amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (txn.type == "CREDIT") Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}