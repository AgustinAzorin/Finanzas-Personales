package com.agustinazorin.finanzas.feature.currency.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agustinazorin.finanzas.R
import com.agustinazorin.finanzas.core.ui.format.label
import com.agustinazorin.finanzas.feature.currency.domain.ExchangeRate
import com.agustinazorin.finanzas.feature.currency.domain.InflationRate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CurrencyScreen(viewModel: CurrencyViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val rateRefresh by viewModel.rateRefresh.collectAsState()
    val inflationRefresh by viewModel.inflationRefresh.collectAsState()
    var showAddRateDialog by remember { mutableStateOf(false) }
    var showAddInflationDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionHeader(
                title = stringResource(R.string.currency_usd_rate_title),
                isLoading = rateRefresh.isLoading,
                onRefresh = viewModel::refreshExchangeRate,
                onAddManual = { showAddRateDialog = true },
            )
        }
        item {
            rateRefresh.errorMessage?.let { ErrorText(it) }
            val latest = state.latestUsdRate
            if (latest == null) {
                Text(stringResource(R.string.currency_no_rate), style = MaterialTheme.typography.bodyMedium)
            } else {
                RateRow(latest)
            }
        }
        items(state.rateHistory.drop(1), key = { "rate_${it.id}" }) { RateRow(it) }

        item {
            SectionHeader(
                title = stringResource(R.string.currency_inflation_title),
                isLoading = inflationRefresh.isLoading,
                onRefresh = viewModel::refreshInflation,
                onAddManual = { showAddInflationDialog = true },
            )
        }
        item { inflationRefresh.errorMessage?.let { ErrorText(it) } }
        if (state.inflationRates.isEmpty()) {
            item { Text(stringResource(R.string.currency_no_inflation), style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(state.inflationRates, key = { "inflation_${it.id}" }) { InflationRow(it) }
        }
    }

    if (showAddRateDialog) {
        AddRateDialog(
            onDismiss = { showAddRateDialog = false },
            onConfirm = { rate, date ->
                viewModel.addManualRate(rate, date)
                showAddRateDialog = false
            },
        )
    }

    if (showAddInflationDialog) {
        AddInflationDialog(
            onDismiss = { showAddInflationDialog = false },
            onConfirm = { month, percent ->
                viewModel.addManualInflation(month, percent)
                showAddInflationDialog = false
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String, isLoading: Boolean, onRefresh: () -> Unit, onAddManual: () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onRefresh, enabled = !isLoading) {
                Text(stringResource(if (isLoading) R.string.currency_refreshing else R.string.currency_refresh))
            }
            TextButton(onClick = onAddManual) { Text(stringResource(R.string.currency_add_manual)) }
        }
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(
        stringResource(R.string.currency_refresh_error, message),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun RateRow(rate: ExchangeRate) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(rate.date.toString(), style = MaterialTheme.typography.bodyLarge)
                Text(rate.source.label(), style = MaterialTheme.typography.bodySmall)
            }
            Text(stringResource(R.string.currency_rate_display, rate.rate.toString()), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun InflationRow(inflation: InflationRate) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(inflation.month.toString(), style = MaterialTheme.typography.bodyLarge)
                Text(inflation.source.label(), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                stringResource(R.string.currency_percent_display, inflation.percent.toString()),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun AddRateDialog(onDismiss: () -> Unit, onConfirm: (rate: BigDecimal, date: LocalDate) -> Unit) {
    var rateText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.currency_add_manual)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text(stringResource(R.string.currency_rate_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text(stringResource(R.string.currency_date_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error) {
                    Text(stringResource(R.string.currency_invalid_input), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val rate = rateText.replace(',', '.').toBigDecimalOrNull()
                    val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
                    if (rate != null && rate.signum() > 0 && date != null) {
                        onConfirm(rate, date)
                    } else {
                        error = true
                    }
                },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun AddInflationDialog(onDismiss: () -> Unit, onConfirm: (month: YearMonth, percent: BigDecimal) -> Unit) {
    var monthText by remember { mutableStateOf(YearMonth.now().toString()) }
    var percentText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.currency_add_manual_inflation)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = monthText,
                    onValueChange = { monthText = it },
                    label = { Text(stringResource(R.string.currency_month_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = percentText,
                    onValueChange = { percentText = it },
                    label = { Text(stringResource(R.string.currency_percent_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error) {
                    Text(stringResource(R.string.currency_invalid_input), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val month = runCatching { YearMonth.parse(monthText) }.getOrNull()
                    val percent = percentText.replace(',', '.').toBigDecimalOrNull()
                    if (month != null && percent != null) {
                        onConfirm(month, percent)
                    } else {
                        error = true
                    }
                },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
