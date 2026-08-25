package com.agustinazorin.finanzas.feature.patrimonio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.core.ui.components.LabeledDropdown
import com.agustinazorin.finanzas.core.ui.components.MoneyText
import com.agustinazorin.finanzas.core.ui.format.label
import com.agustinazorin.finanzas.core.ui.format.parseMoneyInput
import com.agustinazorin.finanzas.core.ui.theme.CommittedAmber
import com.agustinazorin.finanzas.core.ui.theme.ExpenseRed
import com.agustinazorin.finanzas.core.ui.theme.IncomeGreen
import com.agustinazorin.finanzas.engine.model.AssetCategory
import com.agustinazorin.finanzas.engine.model.LiabilityType
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMember
import com.agustinazorin.finanzas.feature.patrimonio.domain.Asset
import com.agustinazorin.finanzas.feature.patrimonio.domain.FinancialSnapshot
import com.agustinazorin.finanzas.feature.patrimonio.domain.Liability

private const val BASE_CURRENCY = "ARS"

@Composable
fun PatrimonioScreen(viewModel: PatrimonioViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val evolution by viewModel.evolution.collectAsState()
    val members by viewModel.members.collectAsState()
    var showAddAssetDialog by remember { mutableStateOf(false) }
    var showAddLiabilityDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { NetWorthCard(state.netWorth) }

        if (evolution.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.patrimonio_evolution_title)) }
            items(evolution, key = { "evolution_${it.id}" }) { EvolutionRow(it) }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.patrimonio_assets_title),
                total = state.totalAssets,
                onAdd = { showAddAssetDialog = true },
                addLabel = stringResource(R.string.patrimonio_add_asset),
            )
        }
        if (state.accountAssetItems.isEmpty() && state.customAssets.isEmpty()) {
            item { Text(stringResource(R.string.patrimonio_assets_empty), style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(state.accountAssetItems, key = { "acc_asset_${it.label}" }) { BreakdownRow(it.label, it.amount, IncomeGreen) }
            items(state.customAssets, key = { "asset_${it.id}" }) { asset ->
                CustomAssetRow(asset, onRemove = { viewModel.removeAsset(asset.id) })
            }
        }

        item {
            SectionHeader(
                title = stringResource(R.string.patrimonio_liabilities_title),
                total = state.totalLiabilities,
                onAdd = { showAddLiabilityDialog = true },
                addLabel = stringResource(R.string.patrimonio_add_liability),
            )
        }
        if (state.accountLiabilityItems.isEmpty() && state.customLiabilities.isEmpty()) {
            item { Text(stringResource(R.string.patrimonio_liabilities_empty), style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(state.accountLiabilityItems, key = { "acc_liability_${it.label}" }) { BreakdownRow(it.label, it.amount, ExpenseRed) }
            items(state.customLiabilities, key = { "liability_${it.id}" }) { liability ->
                CustomLiabilityRow(liability, onRemove = { viewModel.removeLiability(liability.id) })
            }
        }
    }

    if (showAddAssetDialog) {
        AddAssetDialog(
            members = members,
            onDismiss = { showAddAssetDialog = false },
            onConfirm = { name, category, currentValue, owner ->
                viewModel.addAsset(name, category, BASE_CURRENCY, currentValue, owner)
                showAddAssetDialog = false
            },
        )
    }

    if (showAddLiabilityDialog) {
        AddLiabilityDialog(
            members = members,
            onDismiss = { showAddLiabilityDialog = false },
            onConfirm = { name, type, outstandingAmount, owner ->
                viewModel.addLiability(name, type, BASE_CURRENCY, outstandingAmount, owner)
                showAddLiabilityDialog = false
            },
        )
    }
}

@Composable
private fun NetWorthCard(netWorth: Money) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.patrimonio_net_worth), style = MaterialTheme.typography.labelLarge)
            MoneyText(netWorth.minorUnits, netWorth.currency, style = MaterialTheme.typography.headlineMedium, color = CommittedAmber)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun SectionHeader(title: String, total: Money, onAdd: () -> Unit, addLabel: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            MoneyText(total.minorUnits, total.currency, style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = onAdd) { Text(addLabel) }
    }
}

@Composable
private fun EvolutionRow(snapshot: FinancialSnapshot) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(snapshot.date.toString(), style = MaterialTheme.typography.bodyMedium)
        MoneyText(snapshot.netWorth, snapshot.currency, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BreakdownRow(label: String, amount: Money, color: Color) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            MoneyText(amount.minorUnits, amount.currency, color = color)
        }
    }
}

@Composable
private fun CustomAssetRow(asset: Asset, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(asset.name, style = MaterialTheme.typography.bodyLarge)
                Text(asset.category.label(), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onRemove, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.common_delete), style = MaterialTheme.typography.labelLarge)
                }
            }
            MoneyText(asset.currentValue, asset.currency, color = IncomeGreen)
        }
    }
}

@Composable
private fun CustomLiabilityRow(liability: Liability, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(liability.name, style = MaterialTheme.typography.bodyLarge)
                Text(liability.type.label(), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onRemove, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.common_delete), style = MaterialTheme.typography.labelLarge)
                }
            }
            MoneyText(liability.outstandingAmount, liability.currency, color = ExpenseRed)
        }
    }
}

@Composable
private fun AddAssetDialog(
    members: List<HouseholdMember>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: AssetCategory, currentValue: Long, ownerMemberId: Long?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(AssetCategory.OTHER) }
    var valueText by remember { mutableStateOf("0") }
    var owner by remember { mutableStateOf<HouseholdMember?>(null) }
    val ownerOptions: List<HouseholdMember?> = listOf(null) + members

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.patrimonio_add_asset)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.patrimonio_item_name)) })
                LabeledDropdown(
                    label = stringResource(R.string.common_type),
                    options = AssetCategory.entries,
                    selected = category,
                    optionLabel = { it.label() },
                    onSelected = { category = it },
                )
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text(stringResource(R.string.patrimonio_current_value)) },
                    modifier = Modifier.fillMaxWidth(),
                )
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
                    val minorUnits = parseMoneyInput(valueText) ?: 0L
                    if (name.isNotBlank()) onConfirm(name, category, minorUnits, owner?.id)
                },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun AddLiabilityDialog(
    members: List<HouseholdMember>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: LiabilityType, outstandingAmount: Long, ownerMemberId: Long?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(LiabilityType.OTHER) }
    var amountText by remember { mutableStateOf("0") }
    var owner by remember { mutableStateOf<HouseholdMember?>(null) }
    val ownerOptions: List<HouseholdMember?> = listOf(null) + members

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.patrimonio_add_liability)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.patrimonio_item_name)) })
                LabeledDropdown(
                    label = stringResource(R.string.common_type),
                    options = LiabilityType.entries,
                    selected = type,
                    optionLabel = { it.label() },
                    onSelected = { type = it },
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.patrimonio_outstanding_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                )
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
                    val minorUnits = parseMoneyInput(amountText) ?: 0L
                    if (name.isNotBlank()) onConfirm(name, type, minorUnits, owner?.id)
                },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
