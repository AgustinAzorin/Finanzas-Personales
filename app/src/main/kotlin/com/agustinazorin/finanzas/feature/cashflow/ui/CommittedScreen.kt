package com.agustinazorin.finanzas.feature.cashflow.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.agustinazorin.finanzas.core.ui.components.LabeledDropdown
import com.agustinazorin.finanzas.core.ui.components.MoneyText
import com.agustinazorin.finanzas.core.ui.theme.CommittedAmber
import com.agustinazorin.finanzas.engine.model.CashFlowEvent
import com.agustinazorin.finanzas.engine.money.Money

@Composable
fun CommittedScreen(viewModel: CommittedViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(COMMITTED_HORIZON_OPTIONS) { horizon ->
                    HorizonTotalCard(horizon, state.totalsByHorizon[horizon])
                }
            }
        }

        item {
            LabeledDropdown(
                label = stringResource(R.string.committed_horizon_label),
                options = COMMITTED_HORIZON_OPTIONS,
                selected = state.selectedHorizon,
                optionLabel = { stringResource(R.string.committed_horizon_days, it) },
                onSelected = { viewModel.selectHorizon(it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (state.items.isEmpty()) {
            item { EmptyState(stringResource(R.string.committed_empty)) }
        } else {
            items(state.items) { CommittedItemRow(it) }
        }
    }
}

@Composable
private fun HorizonTotalCard(horizonDays: Long, total: Money?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(stringResource(R.string.committed_horizon_days, horizonDays), style = MaterialTheme.typography.labelLarge)
            if (total != null) {
                MoneyText(total.minorUnits, total.currency, color = CommittedAmber)
            }
        }
    }
}

@Composable
private fun CommittedItemRow(event: CashFlowEvent) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(event.label, style = MaterialTheme.typography.bodyLarge)
                Text(event.date.toString(), style = MaterialTheme.typography.bodyMedium)
            }
            MoneyText((-event.amount).minorUnits, event.amount.currency, color = CommittedAmber)
        }
    }
}
