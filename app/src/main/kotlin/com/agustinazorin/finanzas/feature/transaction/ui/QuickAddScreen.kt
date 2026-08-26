package com.agustinazorin.finanzas.feature.transaction.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.core.ui.components.EmptyState
import com.agustinazorin.finanzas.core.ui.components.LabeledDropdown
import com.agustinazorin.finanzas.core.ui.format.label
import com.agustinazorin.finanzas.core.ui.format.parseMoneyInput
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.feature.account.domain.Account
import com.agustinazorin.finanzas.feature.category.domain.Category
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddScreen(onDone: () -> Unit, viewModel: QuickAddViewModel = hiltViewModel()) {
    val options by viewModel.options.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.saved.collect { onDone() }
    }

    if (options.accounts.isEmpty()) {
        EmptyState(stringResource(R.string.quick_add_no_accounts))
        return
    }

    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<Category?>(null) }
    var accountId by remember(options.defaultAccountId) { mutableStateOf(options.defaultAccountId) }
    var toAccountId by remember { mutableStateOf<Long?>(null) }
    var showDetails by remember { mutableStateOf(false) }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var payerId by remember(options.defaultMemberId) { mutableStateOf(options.defaultMemberId) }
    var showShareWith by remember { mutableStateOf(false) }
    var sharedMemberIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val account = options.accounts.firstOrNull { it.id == accountId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val types = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)
                types.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = type == option,
                        onClick = { type = option },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                    ) { Text(option.label()) }
                }
            }
        }

        item {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(R.string.common_amount)) },
                placeholder = { Text(stringResource(R.string.quick_add_amount_hint)) },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 32.sp, textAlign = TextAlign.Start),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (type == TransactionType.TRANSFER) {
            item {
                LabeledDropdown(
                    label = stringResource(R.string.quick_add_transfer_from),
                    options = options.accounts,
                    selected = account,
                    optionLabel = { it.name },
                    onSelected = { accountId = it.id },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                LabeledDropdown(
                    label = stringResource(R.string.quick_add_transfer_to),
                    options = options.accounts.filter { it.id != accountId },
                    selected = options.accounts.firstOrNull { it.id == toAccountId },
                    optionLabel = { it.name },
                    onSelected = { toAccountId = it.id },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(options.categories, key = { it.id }) { option ->
                        FilterChip(
                            selected = category?.id == option.id,
                            onClick = { category = if (category?.id == option.id) null else option },
                            label = { Text(option.name) },
                        )
                    }
                }
            }
            item {
                LabeledDropdown(
                    label = stringResource(R.string.common_account),
                    options = options.accounts,
                    selected = account,
                    optionLabel = { it.name },
                    onSelected = { accountId = it.id },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (options.members.isNotEmpty()) {
                item {
                    val payerOptions: List<HouseholdMember?> = listOf(null) + options.members
                    LabeledDropdown(
                        label = stringResource(R.string.quick_add_payer),
                        options = payerOptions,
                        selected = options.members.firstOrNull { it.id == payerId },
                        optionLabel = { it?.name ?: stringResource(R.string.quick_add_payer_unassigned) },
                        onSelected = { payerId = it?.id },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (type == TransactionType.EXPENSE && options.members.size >= 2) {
                item {
                    TextButton(onClick = { showShareWith = !showShareWith }) {
                        Text(stringResource(R.string.quick_add_share_toggle))
                    }
                }
                if (showShareWith) {
                    item {
                        Text(stringResource(R.string.quick_add_share_members), style = MaterialTheme.typography.bodySmall)
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(options.members, key = { it.id }) { member ->
                                FilterChip(
                                    selected = member.id in sharedMemberIds,
                                    onClick = {
                                        sharedMemberIds = if (member.id in sharedMemberIds) sharedMemberIds - member.id else sharedMemberIds + member.id
                                    },
                                    label = { Text(member.name) },
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            TextButton(onClick = { showDetails = !showDetails }) {
                Text(stringResource(R.string.quick_add_merchant_note_toggle))
            }
        }

        if (showDetails) {
            item {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text(stringResource(R.string.common_merchant)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                // Sugiere la categoría aprendida para este comercio, sin pisar una elección manual (CLAUDE.md, sección 39).
                LaunchedEffect(merchant) {
                    if (merchant.isNotBlank() && category == null) {
                        val suggestedId = viewModel.suggestCategoryId(merchant)
                        category = options.categories.firstOrNull { it.id == suggestedId }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.common_note)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            val amount = parseMoneyInput(amountText)
            val canSave = when (type) {
                TransactionType.EXPENSE, TransactionType.INCOME -> amount != null && amount > 0 && accountId != null
                TransactionType.TRANSFER -> amount != null && amount > 0 && accountId != null && toAccountId != null
                TransactionType.ADJUSTMENT -> false
            }
            Button(
                onClick = {
                    val fromId = accountId ?: return@Button
                    val amountValue = amount ?: return@Button
                    when (type) {
                        TransactionType.EXPENSE -> viewModel.saveExpense(
                            fromId, amountValue, account?.currency ?: "ARS", category?.id, merchant.ifBlank { null }, note.ifBlank { null },
                            ownerMemberId = payerId, sharedWithMemberIds = sharedMemberIds.toList(),
                        )
                        TransactionType.INCOME -> viewModel.saveIncome(fromId, amountValue, account?.currency ?: "ARS", category?.id, merchant.ifBlank { null }, note.ifBlank { null }, ownerMemberId = payerId)
                        TransactionType.TRANSFER -> {
                            val toId = toAccountId ?: return@Button
                            viewModel.saveTransfer(fromId, toId, amountValue, note.ifBlank { null })
                        }
                        TransactionType.ADJUSTMENT -> Unit
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.quick_add_save)) }
        }
    }
}
