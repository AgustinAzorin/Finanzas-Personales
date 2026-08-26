package com.agustinazorin.finanzas.feature.income.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.agustinazorin.finanzas.core.ui.components.EmptyState
import com.agustinazorin.finanzas.core.ui.components.MoneyText
import com.agustinazorin.finanzas.core.ui.format.formatAsMoney
import com.agustinazorin.finanzas.core.ui.theme.IncomeGreen
import com.agustinazorin.finanzas.feature.transaction.domain.Transaction

@Composable
fun IncomeScreen(viewModel: IncomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.income_expected), style = MaterialTheme.typography.labelLarge)
                        MoneyText(state.expectedMonthlyIncome.minorUnits, state.expectedMonthlyIncome.currency, color = IncomeGreen)
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.income_real), style = MaterialTheme.typography.labelLarge)
                        MoneyText(state.realMonthlyIncome.minorUnits, state.realMonthlyIncome.currency, color = IncomeGreen)
                    }
                }
            }
        }

        if (state.incomeTransactions.isEmpty()) {
            item { EmptyState(stringResource(R.string.income_empty)) }
        } else {
            items(state.incomeTransactions, key = { it.id }) { IncomeRow(it) }
        }
    }
}

@Composable
private fun IncomeRow(transaction: Transaction) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(transaction.merchant ?: transaction.date.toString(), style = MaterialTheme.typography.bodyLarge)
                Text(transaction.date.toString(), style = MaterialTheme.typography.bodyMedium)
            }
            Text(transaction.amount.formatAsMoney(transaction.currency), color = IncomeGreen)
        }
    }
}
