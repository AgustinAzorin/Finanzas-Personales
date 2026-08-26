package com.agustinazorin.finanzas.feature.category.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.core.ui.components.EmptyState
import com.agustinazorin.finanzas.core.ui.components.LabeledDropdown
import com.agustinazorin.finanzas.feature.category.domain.Category

@Composable
fun CategoriesScreen(viewModel: CategoriesViewModel = hiltViewModel()) {
    val categories by viewModel.categories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.categories_add_title))
            }
        },
    ) { padding ->
        if (categories.isEmpty()) {
            EmptyState(stringResource(R.string.categories_empty), modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                categories.forEach { group ->
                    item { Text(group.category.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
                    items(group.children, key = { it.id }) { child ->
                        Text(child.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                    }
                }
            }
        }

        if (showAddDialog) {
            AddCategoryDialog(
                roots = categories.map { it.category },
                onDismiss = { showAddDialog = false },
                onConfirm = { name, parentId ->
                    viewModel.addCategory(name, parentId)
                    showAddDialog = false
                },
            )
        }
    }
}

@Composable
private fun AddCategoryDialog(
    roots: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, parentCategoryId: Long?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var parent by remember { mutableStateOf<Category?>(null) }
    val parentOptions: List<Category?> = listOf(null) + roots

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.categories_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.categories_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                LabeledDropdown(
                    label = stringResource(R.string.categories_parent),
                    options = parentOptions,
                    selected = parent,
                    optionLabel = { it?.name ?: stringResource(R.string.common_none) },
                    onSelected = { parent = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, parent?.id) }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
