package com.agustinazorin.finanzas.feature.household.ui

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
import com.agustinazorin.finanzas.engine.household.HouseholdDebt
import com.agustinazorin.finanzas.engine.household.MemberAttribution
import com.agustinazorin.finanzas.engine.household.MemberAttributionCalculator
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMember

@Composable
fun HouseholdReportScreen(viewModel: HouseholdReportViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    if (state.memberAttributions.isEmpty() && state.debts.isEmpty()) {
        EmptyState(stringResource(R.string.household_report_empty))
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.memberAttributions, key = { it.memberId }) { attribution ->
            MemberAttributionCard(attribution, memberDisplayName(attribution.memberId, state.members))
        }

        item {
            Text(stringResource(R.string.household_report_debts_title), style = MaterialTheme.typography.titleMedium)
        }
        if (state.debts.isEmpty()) {
            item {
                Text(stringResource(R.string.household_report_debts_empty), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(state.debts) { debt ->
                DebtCard(debt, state.members)
            }
        }
    }
}

@Composable
private fun MemberAttributionCard(attribution: MemberAttribution, memberName: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(memberName, style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.household_report_expense), style = MaterialTheme.typography.bodyMedium)
                MoneyText(attribution.expense.minorUnits, attribution.expense.currency)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.household_report_income), style = MaterialTheme.typography.bodyMedium)
                MoneyText(attribution.income.minorUnits, attribution.income.currency)
            }
        }
    }
}

@Composable
private fun DebtCard(debt: HouseholdDebt, members: List<HouseholdMember>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(
                    R.string.household_report_owes,
                    memberDisplayName(debt.owedByMemberId, members),
                    memberDisplayName(debt.owedToMemberId, members),
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            MoneyText(debt.amount.minorUnits, debt.amount.currency)
        }
    }
}

@Composable
private fun memberDisplayName(memberId: Long, members: List<HouseholdMember>): String =
    if (memberId == MemberAttributionCalculator.UNASSIGNED) {
        stringResource(R.string.household_report_unassigned)
    } else {
        members.firstOrNull { it.id == memberId }?.name ?: stringResource(R.string.household_report_unassigned)
    }
