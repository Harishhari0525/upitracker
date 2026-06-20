@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.data.Transaction
import com.example.upitracker.ui.components.DeleteTransactionConfirmationDialog
import com.example.upitracker.ui.components.LottieEmptyState
import com.example.upitracker.ui.components.TransactionCardWithMenu
import com.example.upitracker.ui.components.expressive.ExpressiveSectionHeader
import com.example.upitracker.ui.components.expressive.ExpressiveTopBar
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.util.getCategoryIcon
import com.example.upitracker.util.parseColor
import com.example.upitracker.viewmodel.MainViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems

@Composable
fun ArchivedTransactionsScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    val archivedList = mainViewModel.archivedUpiTransactions.collectAsLazyPagingItems()
    val archivedCount by mainViewModel.archivedTransactionCount.collectAsState()

    val allCategories by mainViewModel.allCategories.collectAsState()
    val isSelectionMode by mainViewModel.isSelectionModeActive.collectAsState()
    val selectedIds by mainViewModel.selectedTransactionIds.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ExpressiveTopBar(
                title = "Archived",
                subtitle = "Restore or permanently delete archived transactions",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (archivedList.itemCount == 0 && archivedList.loadState.refresh is LoadState.NotLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(ExpressiveTokens.spacing.lg)
                ) {
                    ExpressiveSectionHeader(
                        title = "No archived transactions",
                        subtitle = "Archived transactions will appear here"
                    )

                    LottieEmptyState(
                        modifier = Modifier.fillMaxSize(),
                        message = stringResource(R.string.archived_empty_state),
                        lottieResourceId = R.raw.empty_box_animation
                    )
                }
            } else {
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
                            title = "Archived Transactions",
                            subtitle = "$archivedCount archived items"
                        )
                    }

                    items(
                        count = archivedList.itemCount,
                        key = { index -> archivedList.peek(index)?.id ?: "archived-placeholder-$index" }
                    ) { index ->
                        val transaction = archivedList[index] ?: return@items
                        val categoryDetails = remember(transaction.category, allCategories) {
                            allCategories.find { category ->
                                category.name.equals(
                                    transaction.category,
                                    ignoreCase = true
                                )
                            }
                        }

                        val categoryColor = remember(categoryDetails) {
                            parseColor(categoryDetails?.colorHex ?: "#808080")
                        }

                        val categoryIcon = getCategoryIcon(categoryDetails)
                        val isSelected = selectedIds.contains(transaction.id)

                        TransactionCardWithMenu(
                            transaction = transaction,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            showCheckbox = isSelectionMode,
                            onToggleSelection = {
                                mainViewModel.toggleSelection(transaction.id)
                            },
                            onShowDetails = {},
                            onDelete = { transactionToDelete = transaction },
                            onArchiveAction = {
                                mainViewModel.toggleTransactionArchiveStatus(
                                    it,
                                    archive = false
                                )
                            },
                            archiveActionText = "Restore",
                            archiveActionIcon = Icons.Default.Restore,
                            categoryColor = categoryColor,
                            categoryIcon = categoryIcon,
                            onCategoryClick = {}
                        )
                    }
                }
            }

            if (transactionToDelete != null) {
                DeleteTransactionConfirmationDialog(
                    transactionDescription = transactionToDelete!!.description,
                    onConfirm = {
                        mainViewModel.permanentlyDeleteTransaction(transactionToDelete!!)
                        transactionToDelete = null
                    },
                    onDismiss = { transactionToDelete = null }
                )
            }
        }
    }
}
