package com.agustinazorin.finanzas.feature.cashflow.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.core.ui.components.LabeledDropdown
import com.agustinazorin.finanzas.core.ui.components.MoneyText
import com.agustinazorin.finanzas.core.ui.theme.CommittedAmber
import com.agustinazorin.finanzas.core.ui.theme.ExpenseRed
import com.agustinazorin.finanzas.core.ui.theme.IncomeGreen
import com.agustinazorin.finanzas.engine.model.CashFlowPoint
import java.time.LocalDate

@Composable
fun CashFlowScreen(viewModel: CashFlowViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            LabeledDropdown(
                label = stringResource(R.string.cashflow_horizon_label),
                options = CASH_FLOW_HORIZON_OPTIONS,
                selected = state.selectedHorizon,
                optionLabel = { stringResource(R.string.committed_horizon_days, it) },
                onSelected = { viewModel.selectHorizon(it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.liquidityAlertDate?.let { date ->
            item { LiquidityAlertCard(date) }
        }

        items(state.points) { CashFlowPointRow(it) }
    }
}

@Composable
private fun LiquidityAlertCard(date: LocalDate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Text(
            text = stringResource(R.string.cashflow_alert_negative, date.toString()),
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun CashFlowPointRow(point: CashFlowPoint) {
    val delta = point.delta
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(point.date.toString(), style = MaterialTheme.typography.labelLarge)
                Text(point.label, style = MaterialTheme.typography.bodyLarge)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (delta != null) {
                    val color = if (delta.isNegative) ExpenseRed else IncomeGreen
                    MoneyText(delta.minorUnits, delta.currency, style = MaterialTheme.typography.bodyMedium, color = color)
                }
                MoneyText(point.balance.minorUnits, point.balance.currency, color = CommittedAmber)
            }
        }
    }
}
