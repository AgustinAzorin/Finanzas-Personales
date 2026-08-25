package com.agustinazorin.finanzas.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.core.ui.components.EmptyState
import com.agustinazorin.finanzas.core.ui.components.MoneyText
import com.agustinazorin.finanzas.core.ui.format.formatAsMoney
import com.agustinazorin.finanzas.core.ui.format.label
import com.agustinazorin.finanzas.core.ui.theme.CommittedAmber
import com.agustinazorin.finanzas.core.ui.theme.ExpenseRed
import com.agustinazorin.finanzas.core.ui.theme.IncomeGreen
import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.model.UpcomingCommitment
import com.agustinazorin.finanzas.feature.transaction.domain.Transaction

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { NetWorthCard(state) }
        item { AvailableAndCommittedRow(state) }
        item { MonthSummaryCard(state) }
        item { SectionHeader(stringResource(R.string.home_upcoming_commitments)) }
        if (state.upcomingCommitments.isEmpty()) {
            item { EmptyState(stringResource(R.string.home_empty_upcoming)) }
        } else {
            items(state.upcomingCommitments) { UpcomingCommitmentRow(it) }
        }
        item { SectionHeader(stringResource(R.string.home_recent_transactions)) }
        if (state.recentTransactions.isEmpty()) {
            item { EmptyState(stringResource(R.string.home_empty_recent)) }
        } else {
            items(state.recentTransactions) { RecentTransactionRow(it) }
        }
    }
}

@Composable
private fun NetWorthCard(state: HomeUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.home_net_worth), style = MaterialTheme.typography.titleMedium)
            MoneyText(
                minorUnits = state.netWorth.minorUnits,
                currency = state.netWorth.currency,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
private fun AvailableAndCommittedRow(state: HomeUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.home_available), style = MaterialTheme.typography.labelLarge)
                MoneyText(state.available.minorUnits, state.available.currency, color = IncomeGreen)
            }
        }
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.home_committed), style = MaterialTheme.typography.labelLarge)
                MoneyText(state.committed.minorUnits, state.committed.currency, color = CommittedAmber)
            }
        }
    }
}

@Composable
private fun MonthSummaryCard(state: HomeUiState) {
    val summary = state.monthSummary ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.home_expense_this_month), style = MaterialTheme.typography.labelLarge)
                MoneyText(summary.totalExpense.minorUnits, summary.totalExpense.currency, color = ExpenseRed)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.home_income_this_month), style = MaterialTheme.typography.labelLarge)
                MoneyText(summary.totalIncome.minorUnits, summary.totalIncome.currency, color = IncomeGreen)
            }
            summary.savingsRate?.let { rate ->
                Text(
                    text = stringResource(R.string.home_savings_rate, (rate * 100).toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun UpcomingCommitmentRow(commitment: UpcomingCommitment) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(commitment.name, style = MaterialTheme.typography.bodyLarge)
                Text(commitment.dueDate.toString(), style = MaterialTheme.typography.bodyMedium)
            }
            Text(commitment.amount.minorUnits.formatAsMoney(commitment.amount.currency), color = CommittedAmber)
        }
    }
}

@Composable
private fun RecentTransactionRow(transaction: Transaction) {
    val color = if (transaction.direction == TransactionDirection.OUTFLOW) ExpenseRed else IncomeGreen
    val sign = if (transaction.direction == TransactionDirection.OUTFLOW) -1 else 1
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(transaction.merchant ?: transaction.type.label(), style = MaterialTheme.typography.bodyLarge)
            Text(transaction.date.toString(), style = MaterialTheme.typography.bodyMedium)
        }
        Text((sign * transaction.amount).formatAsMoney(transaction.currency), color = color)
    }
}
