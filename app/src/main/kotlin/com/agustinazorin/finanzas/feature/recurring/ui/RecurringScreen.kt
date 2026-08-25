package com.agustinazorin.finanzas.feature.recurring.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import com.agustinazorin.finanzas.core.ui.components.MoneyText
import com.agustinazorin.finanzas.core.ui.format.label
import com.agustinazorin.finanzas.core.ui.format.parseMoneyInput
import com.agustinazorin.finanzas.engine.model.Periodicity
import com.agustinazorin.finanzas.engine.model.RecurringType
import com.agustinazorin.finanzas.feature.category.domain.Category
import com.agustinazorin.finanzas.feature.recurring.domain.RecurringTransaction

@Composable
fun RecurringScreen(viewModel: RecurringViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.recurring_add_title))
            }
        },
    ) { padding ->
        if (state.recurring.isEmpty()) {
            EmptyState(stringResource(R.string.recurring_empty), modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.recurring, key = { it.id }) { recurring ->
                    RecurringRow(recurring, onToggleActive = { viewModel.setActive(recurring.id, it) })
                }
            }
        }

        if (showAddDialog) {
            AddRecurringDialog(
                categories = state.categories,
                onDismiss = { showAddDialog = false },
                onConfirm = { type, name, amount, currency, periodicity, dueDay, categoryId ->
                    viewModel.addRecurring(type, name, amount, currency, periodicity, dueDay, categoryId, null, null)
                    showAddDialog = false
                },
            )
        }
    }
}

@Composable
private fun RecurringRow(recurring: RecurringTransaction, onToggleActive: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(recurring.name, style = MaterialTheme.typography.titleMedium)
                Text("${recurring.type.label()} · ${recurring.periodicity.label()}", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { onToggleActive(!recurring.isActive) }, contentPadding = PaddingValues(0.dp)) {
                    Text(
                        stringResource(if (recurring.isActive) R.string.recurring_deactivate else R.string.recurring_activate),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            MoneyText(recurring.estimatedAmount, recurring.currency)
        }
    }
}

@Composable
private fun AddRecurringDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (type: RecurringType, name: String, amount: Long, currency: String, periodicity: Periodicity, dueDay: Int, categoryId: Long?) -> Unit,
) {
    var type by remember { mutableStateOf(RecurringType.EXPENSE) }
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("ARS") }
    var periodicity by remember { mutableStateOf(Periodicity.MONTHLY) }
    var dueDayText by remember { mutableStateOf("1") }
    var category by remember { mutableStateOf<Category?>(null) }
    val categoryOptions: List<Category?> = listOf(null) + categories

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recurring_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledDropdown(
                    label = stringResource(R.string.common_type),
                    options = listOf(RecurringType.EXPENSE, RecurringType.INCOME),
                    selected = type,
                    optionLabel = { it.label() },
                    onSelected = { type = it },
                )
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.recurring_name)) })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text(stringResource(R.string.common_amount)) },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it.uppercase().take(3) },
                        label = { Text(stringResource(R.string.accounts_currency)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                LabeledDropdown(
                    label = stringResource(R.string.common_periodicity),
                    options = Periodicity.entries,
                    selected = periodicity,
                    optionLabel = { it.label() },
                    onSelected = { periodicity = it },
                )
                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = { dueDayText = it },
                    label = { Text(stringResource(R.string.recurring_due_day)) },
                )
                LabeledDropdown(
                    label = stringResource(R.string.common_category),
                    options = categoryOptions,
                    selected = category,
                    optionLabel = { it?.name ?: stringResource(R.string.common_none) },
                    onSelected = { category = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = parseMoneyInput(amountText) ?: return@TextButton
                    val dueDay = dueDayText.toIntOrNull() ?: 1
                    onConfirm(type, name, amount, currency, periodicity, dueDay, category?.id)
                },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
