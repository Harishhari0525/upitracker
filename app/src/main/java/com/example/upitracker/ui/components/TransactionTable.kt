package com.example.upitracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource // ✨
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.upitracker.R // ✨
import com.example.upitracker.data.Transaction
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionTable(
    modifier: Modifier = Modifier,
    transactions: List<Transaction>
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yy", Locale.getDefault()) }

    if (transactions.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.empty_state_no_upi_transactions), // ✨ (or a more table-specific empty message)
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(modifier = modifier.padding(horizontal = 8.dp)) {
        // Header Row
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                // ✨ Using placeholder strings for table headers for now, as they weren't in strings.xml
                // You should add R.string.table_header_date, R.string.table_header_type etc.
                TableCell(text = "Date", weight = 2f, style = MaterialTheme.typography.titleSmall)
                TableCell(text = "Type", weight = 1f, style = MaterialTheme.typography.titleSmall)
                TableCell(text = "Amount (₹)", weight = 1.5f, style = MaterialTheme.typography.titleSmall, alignment = TextAlign.End)
                TableCell(text = "Description", weight = 3f, style = MaterialTheme.typography.titleSmall)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }

        // Transaction Rows
        itemsIndexed(transactions, key = { _, txn -> txn.id }) { index, txn ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayDate = try {
                    dateFormat.format(Date(txn.date))
                } catch (e: Exception) { "N/A" }

                val amountColor = when {
                    txn.type.contains("DEBIT", ignoreCase = true) || txn.type.contains("SENT", ignoreCase = true) -> MaterialTheme.colorScheme.error
                    txn.type.contains("CREDIT", ignoreCase = true) || txn.type.contains("RECVD", ignoreCase = true) -> MaterialTheme.colorScheme.tertiary
                    else -> LocalContentColor.current
                }

                TableCell(text = displayDate, weight = 2f, style = MaterialTheme.typography.bodySmall)
                TableCell(text = txn.type.uppercase(Locale.getDefault()), weight = 1f, style = MaterialTheme.typography.labelMedium)
                TableCell(
                    text = try { "%.2f".format(txn.amount) } catch (e: Exception) { "0.00" },
                    weight = 1.5f,
                    alignment = TextAlign.End,
                    color = amountColor,
                    style = MaterialTheme.typography.bodyMedium
                )
                TableCell(
                    text = txn.description.trim(),
                    weight = 3f,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (index < transactions.size - 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float,
    alignment: TextAlign = TextAlign.Start,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = LocalContentColor.current,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        textAlign = alignment,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow
    )
}