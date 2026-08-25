package com.agustinazorin.finanzas.feature.summary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
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
import com.agustinazorin.finanzas.core.ui.components.MoneyText
import com.agustinazorin.finanzas.core.ui.theme.ExpenseRed
import com.agustinazorin.finanzas.core.ui.theme.IncomeGreen

@Composable
fun SummaryScreen(viewModel: SummaryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SummaryCard(stringResource(R.string.summary_net_worth)) {
                MoneyText(state.netWorth.minorUnits, state.netWorth.currency, style = MaterialTheme.typography.headlineMedium)
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(stringResource(R.string.summary_assets), modifier = Modifier.weight(1f)) {
                    MoneyText(state.assets.minorUnits, state.assets.currency, color = IncomeGreen)
                }
                SummaryCard(stringResource(R.string.summary_liabilities), modifier = Modifier.weight(1f)) {
                    MoneyText(state.liabilities.minorUnits, state.liabilities.currency, color = ExpenseRed)
                }
            }
        }
        state.monthSummary?.let { summary ->
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(stringResource(R.string.summary_month_expense), modifier = Modifier.weight(1f)) {
                        MoneyText(summary.totalExpense.minorUnits, summary.totalExpense.currency, color = ExpenseRed)
                    }
                    SummaryCard(stringResource(R.string.summary_month_income), modifier = Modifier.weight(1f)) {
                        MoneyText(summary.totalIncome.minorUnits, summary.totalIncome.currency, color = IncomeGreen)
                    }
                }
            }
            item {
                SummaryCard(stringResource(R.string.summary_savings_rate)) {
                    val rateText = summary.savingsRate?.let { "${(it * 100).toInt()}%" } ?: stringResource(R.string.summary_not_available)
                    Text(rateText, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        item {
            SummaryCard(stringResource(R.string.summary_debt_to_income)) {
                val text = state.debtToIncomeRatio?.let { "${(it * 100).toInt()}%" } ?: stringResource(R.string.summary_not_available)
                Text(text, style = MaterialTheme.typography.titleMedium)
            }
        }
        item {
            SummaryCard(stringResource(R.string.summary_fixed_expenses_to_income)) {
                val text = state.fixedExpensesToIncomeRatio?.let { "${(it * 100).toInt()}%" } ?: stringResource(R.string.summary_not_available)
                Text(text, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            content()
        }
    }
}
