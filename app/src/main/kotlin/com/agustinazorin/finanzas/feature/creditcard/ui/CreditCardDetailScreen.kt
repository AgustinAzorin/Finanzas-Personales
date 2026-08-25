package com.agustinazorin.finanzas.feature.creditcard.ui

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
import androidx.compose.material3.Button
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
import com.agustinazorin.finanzas.core.ui.components.LabeledDropdown
import com.agustinazorin.finanzas.core.ui.components.MoneyText
import com.agustinazorin.finanzas.core.ui.format.formatAsMoney
import com.agustinazorin.finanzas.core.ui.format.label
import com.agustinazorin.finanzas.core.ui.format.parseMoneyInput
import com.agustinazorin.finanzas.engine.model.CreditCardStatementStatus
import com.agustinazorin.finanzas.feature.account.domain.Account
import com.agustinazorin.finanzas.feature.category.domain.Category
import com.agustinazorin.finanzas.feature.creditcard.domain.CreditCardStatement
import com.agustinazorin.finanzas.feature.creditcard.domain.usecase.CreditCardOverview
import com.agustinazorin.finanzas.feature.installment.domain.Installment
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM")

@Composable
fun CreditCardDetailScreen(viewModel: CreditCardDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val account = state.account ?: return
    val overview = state.overview

    var showAddPurchase by remember { mutableStateOf(false) }
    var payingStatement by remember { mutableStateOf<CreditCardStatement?>(null) }

    Scaffold(
        floatingActionButton = {
            if (state.isConfigured) {
                FloatingActionButton(onClick = { showAddPurchase = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.credit_card_add_purchase))
                }
            }
        },
    ) { padding ->
        if (!state.isConfigured) {
            CreditCardSetupForm(
                modifier = Modifier.fillMaxSize().padding(padding),
                onConfirm = { closingDay, dueDay, limit -> viewModel.configure(closingDay, dueDay, limit) },
            )
        } else if (overview != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { CreditCardSummaryCard(account.name, overview) }

                item { Text(stringResource(R.string.credit_card_statements_title), style = MaterialTheme.typography.titleMedium) }
                items(overview.statements, key = { it.id }) { statement ->
                    StatementRow(statement, overview.currency, onPay = { payingStatement = statement })
                }

                item { Text(stringResource(R.string.credit_card_upcoming_installments_title), style = MaterialTheme.typography.titleMedium) }
                if (overview.upcomingInstallments.isEmpty()) {
                    item { Text(stringResource(R.string.credit_card_no_upcoming_installments), style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(overview.upcomingInstallments, key = { it.id }) { installment ->
                        InstallmentRow(installment, overview.currency)
                    }
                }
            }
        }

        if (showAddPurchase) {
            AddCreditCardPurchaseDialog(
                categories = state.categories,
                onDismiss = { showAddPurchase = false },
                onConfirm = { amount, installments, categoryId, merchant, note ->
                    viewModel.addPurchase(amount, account.currency, LocalDate.now(), installments, categoryId, merchant, note)
                    showAddPurchase = false
                },
            )
        }

        payingStatement?.let { statement ->
            PayStatementDialog(
                statement = statement,
                currency = overview?.currency ?: account.currency,
                payFromAccounts = state.payFromAccounts,
                onDismiss = { payingStatement = null },
                onConfirm = { fromAccountId, amount ->
                    viewModel.payStatement(statement.id, fromAccountId, amount)
                    payingStatement = null
                },
            )
        }
    }
}

@Composable
private fun CreditCardSummaryCard(accountName: String, overview: CreditCardOverview) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(accountName, style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.credit_card_available), style = MaterialTheme.typography.bodySmall)
                    MoneyText(overview.availableCredit, overview.currency)
                }
                Column {
                    Text(stringResource(R.string.credit_card_limit), style = MaterialTheme.typography.bodySmall)
                    MoneyText(overview.creditCard.creditLimit, overview.currency, style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text(stringResource(R.string.credit_card_debt), style = MaterialTheme.typography.bodySmall)
                    MoneyText(overview.outstandingBalance, overview.currency, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun StatementRow(statement: CreditCardStatement, currency: String, onPay: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    stringResource(R.string.credit_card_statement_due, DATE_FORMAT.format(statement.dueDate)),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(statement.status.label(), style = MaterialTheme.typography.bodySmall)
                if (statement.outstandingAmount > 0 && statement.status != CreditCardStatementStatus.OPEN) {
                    TextButton(onClick = onPay, contentPadding = PaddingValues(0.dp)) {
                        Text(stringResource(R.string.credit_card_pay_statement), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            MoneyText(statement.totalAmount, currency)
        }
    }
}

@Composable
private fun InstallmentRow(installment: Installment, currency: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(R.string.credit_card_installment_label, installment.installmentNumber, installment.totalInstallments) +
                " · " + DATE_FORMAT.format(installment.dueDate),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(installment.amount.formatAsMoney(currency), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CreditCardSetupForm(modifier: Modifier = Modifier, onConfirm: (closingDay: Int, dueDay: Int, creditLimit: Long) -> Unit) {
    var closingDayText by remember { mutableStateOf("") }
    var dueDayText by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("") }

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.credit_card_setup_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.credit_card_setup_description), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = closingDayText,
            onValueChange = { closingDayText = it },
            label = { Text(stringResource(R.string.credit_card_closing_day)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = dueDayText,
            onValueChange = { dueDayText = it },
            label = { Text(stringResource(R.string.credit_card_due_day)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = limitText,
            onValueChange = { limitText = it },
            label = { Text(stringResource(R.string.credit_card_limit)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                val closingDay = closingDayText.toIntOrNull() ?: return@Button
                val dueDay = dueDayText.toIntOrNull() ?: return@Button
                val limit = parseMoneyInput(limitText) ?: return@Button
                onConfirm(closingDay, dueDay, limit)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.common_save)) }
    }
}

@Composable
private fun AddCreditCardPurchaseDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, installments: Int, categoryId: Long?, merchant: String?, note: String?) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var installmentsText by remember { mutableStateOf("1") }
    var merchant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<Category?>(null) }
    val categoryOptions: List<Category?> = listOf(null) + categories

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.credit_card_add_purchase)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.common_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = installmentsText,
                    onValueChange = { installmentsText = it },
                    label = { Text(stringResource(R.string.credit_card_installments_count)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text(stringResource(R.string.common_merchant)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                LabeledDropdown(
                    label = stringResource(R.string.common_category),
                    options = categoryOptions,
                    selected = category,
                    optionLabel = { it?.name ?: stringResource(R.string.common_none) },
                    onSelected = { category = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = parseMoneyInput(amountText) ?: return@TextButton
                    val installments = installmentsText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    onConfirm(amount, installments, category?.id, merchant.ifBlank { null }, null)
                },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun PayStatementDialog(
    statement: CreditCardStatement,
    currency: String,
    payFromAccounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (fromAccountId: Long, amount: Long) -> Unit,
) {
    var fromAccount by remember { mutableStateOf(payFromAccounts.firstOrNull()) }
    var amountText by remember { mutableStateOf((statement.outstandingAmount.formatAsMoney(currency)).dropWhile { !it.isDigit() }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.credit_card_pay_statement)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledDropdown(
                    label = stringResource(R.string.credit_card_pay_from),
                    options = payFromAccounts,
                    selected = fromAccount,
                    optionLabel = Account::name,
                    onSelected = { fromAccount = it },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.common_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val accountId = fromAccount?.id ?: return@TextButton
                    val amount = parseMoneyInput(amountText) ?: return@TextButton
                    onConfirm(accountId, amount)
                },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
