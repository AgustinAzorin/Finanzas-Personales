package com.agustinazorin.finanzas.feature.account.ui

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
import com.agustinazorin.finanzas.engine.model.AccountType
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMember

@Composable
fun AccountsScreen(viewModel: AccountsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.accounts_add_title))
            }
        },
    ) { padding ->
        if (state.accounts.isEmpty()) {
            EmptyState(stringResource(R.string.accounts_empty), modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.accounts, key = { it.account.id }) { accountUi ->
                    AccountRow(accountUi, state.members, onToggleActive = { viewModel.setAccountActive(accountUi.account.id, it) })
                }
            }
        }

        if (showAddDialog) {
            AddAccountDialog(
                members = state.members,
                onDismiss = { showAddDialog = false },
                onConfirm = { name, type, currency, initialBalance, ownerMemberId ->
                    viewModel.addAccount(name, type, currency, initialBalance, ownerMemberId)
                    showAddDialog = false
                },
            )
        }
    }
}

@Composable
private fun AccountRow(accountUi: AccountUi, members: List<HouseholdMember>, onToggleActive: (Boolean) -> Unit) {
    val account = accountUi.account
    val ownerName = members.firstOrNull { it.id == account.ownerMemberId }?.name
        ?: stringResource(R.string.accounts_owner_household)

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(account.name, style = MaterialTheme.typography.titleMedium)
                Text("${account.type.label()} · $ownerName", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { onToggleActive(!account.isActive) }, contentPadding = PaddingValues(0.dp)) {
                    Text(
                        stringResource(if (account.isActive) R.string.accounts_deactivate else R.string.accounts_activate),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            MoneyText(accountUi.balance.minorUnits, accountUi.balance.currency)
        }
    }
}

@Composable
private fun AddAccountDialog(
    members: List<HouseholdMember>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: AccountType, currency: String, initialBalance: Long, ownerMemberId: Long?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.BANK_ACCOUNT) }
    var currency by remember { mutableStateOf("ARS") }
    var initialBalanceText by remember { mutableStateOf("0") }
    var owner by remember { mutableStateOf<HouseholdMember?>(null) }

    val ownerOptions: List<HouseholdMember?> = listOf(null) + members

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.accounts_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.accounts_name)) })
                LabeledDropdown(
                    label = stringResource(R.string.common_type),
                    options = AccountType.entries,
                    selected = type,
                    optionLabel = { it.label() },
                    onSelected = { type = it },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it.uppercase().take(3) },
                        label = { Text(stringResource(R.string.accounts_currency)) },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = initialBalanceText,
                        onValueChange = { initialBalanceText = it },
                        label = { Text(stringResource(R.string.accounts_initial_balance)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                LabeledDropdown(
                    label = stringResource(R.string.accounts_owner),
                    options = ownerOptions,
                    selected = owner,
                    optionLabel = { it?.name ?: stringResource(R.string.accounts_owner_household) },
                    onSelected = { owner = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val minorUnits = parseMoneyInput(initialBalanceText) ?: 0L
                    if (name.isNotBlank()) {
                        onConfirm(name, type, currency, minorUnits, owner?.id)
                    }
                },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
