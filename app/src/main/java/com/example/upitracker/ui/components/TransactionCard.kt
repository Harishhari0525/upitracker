package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.Transaction // Ensure this path is correct
import java.text.SimpleDateFormat
import java.util.*
import java.text.ParseException
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.stringResource // ✨ Ensure this is imported
import com.example.upitracker.R

@Composable
fun TransactionCard(
    modifier: Modifier = Modifier,
    transaction: Transaction,
    onClick: (Transaction) -> Unit
) {
    val displayDate = remember(transaction.date) {
        try {
            // Using a slightly more detailed and common format
            SimpleDateFormat("dd MMM yy, hh:mm a", Locale.getDefault()).format(Date(transaction.date))
        } catch (e: ParseException) { "Invalid Date" }
        catch (e: IllegalArgumentException) { "Invalid Date" }
    }

    val amountColor = when {
        transaction.type.contains("DEBIT", ignoreCase = true) || transaction.type.contains("SENT", ignoreCase = true) -> MaterialTheme.colorScheme.error
        transaction.type.contains("CREDIT", ignoreCase = true) || transaction.type.contains("RECVD", ignoreCase = true) || transaction.type.contains("RECEIVED", ignoreCase = true) -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface // Default
    }

    Card(
        modifier = modifier.fillMaxWidth()
            .clickable{
                onClick(transaction)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), // Subtle elevation
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            // Alternative expressive background:
            // containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp) // Standard M3 padding
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.type.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelLarge, // Good for tags/types
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "₹${try { "%.2f".format(transaction.amount) } catch (e: Exception) { "0.00" }}",
                    style = MaterialTheme.typography.titleMedium, // Prominent amount
                    color = amountColor
                )
            }
            Spacer(Modifier.height(6.dp)) // Consistent spacing

            Text(
                text = transaction.description.trim(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Display category if it exists
            transaction.category?.let { categoryValue ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.transaction_card_category_label, categoryValue),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary, // Use a distinct color for category
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }


            if (transaction.senderOrReceiver.isNotBlank()) {
                Spacer(Modifier.height(if (transaction.category == null) 8.dp else 4.dp)) // Adjust spacing based on category presence
                Text(
                    text = transaction.senderOrReceiver.trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                // Add spacer if category is present but sender/receiver is not, to maintain date position
                if (transaction.category != null) {
                    Spacer(Modifier.height(4.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = displayDate,
                style = MaterialTheme.typography.bodySmall, // Smallest detail
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}