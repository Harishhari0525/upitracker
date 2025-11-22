package com.example.upitracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.Category
import com.example.upitracker.ui.components.AddEditCategoryDialog
import com.example.upitracker.util.getCategoryIcon
import com.example.upitracker.util.parseColor
import com.example.upitracker.viewmodel.MainViewModel
import com.example.upitracker.ui.components.LottieEmptyState
import com.example.upitracker.R
import com.example.upitracker.ui.components.CategoryIconView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit
) {
    val categories by mainViewModel.allCategories.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    val listState = rememberLazyListState()
    val isScrollingUp = listState.isScrollingUp()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton( // Use Extended instead of standard FAB
                text = { Text("Add Category") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = {
                    categoryToEdit = null
                    showDialog = true
                },
                expanded = isScrollingUp,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    ) { paddingValues ->
        if (categories.isEmpty()) {
            LottieEmptyState(
                modifier = Modifier.padding(paddingValues),
                message = "No custom categories found.\nTap '+' to create your own.",
                lottieResourceId = R.raw.empty_box_animation
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    CategoryListItem(
                        category = category,
                        onEdit = {
                            categoryToEdit = it
                            showDialog = true
                        },
                        onDelete = { categoryToDelete = it }
                    )
                }
            }
        }
    }

    if (showDialog) {
        AddEditCategoryDialog(
            categoryToEdit = categoryToEdit,
            onDismiss = { showDialog = false },
            onConfirm = { name, icon, color ->
                if (categoryToEdit == null) {
                    mainViewModel.addCategory(name, icon, color)
                } else {
                    mainViewModel.updateCategory(categoryToEdit!!, name, icon, color)
                }
                showDialog = false
            }
        )
    }

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category?") },
            text = { Text("Are you sure you want to delete the '${categoryToDelete!!.name}' category? This will un-assign it from all existing transactions.") },
            confirmButton = {
                Button(
                    onClick = {
                        mainViewModel.deleteCategory(categoryToDelete!!)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CategoryListItem(
    category: Category,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    val categoryIcon = getCategoryIcon(category)
    val categoryColor = parseColor(hex = category.colorHex)

    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // ✨ START: THIS IS THE UPDATED LOGIC ✨
                CategoryIconView(
                    categoryIcon = categoryIcon,
                    size = 24.dp,
                    iconTint = categoryColor, // Use tint for vector icons
                    // For emojis/letters, we usually want the background to match the category color
                    // But for simplicity in this list item, we can keep the default or customize:
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                // ✨ END: THIS IS THE UPDATED LOGIC ✨
                Spacer(Modifier.width(16.dp))
                Text(category.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Row {
                IconButton(onClick = { onEdit(category) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Category")
                }
                IconButton(onClick = { onDelete(category) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Category", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
@Composable
private fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}