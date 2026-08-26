package com.agustinazorin.finanzas.feature.transaction.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.agustinazorin.finanzas.core.ui.format.formatAsMoney
import com.agustinazorin.finanzas.core.ui.format.label
import com.agustinazorin.finanzas.core.ui.theme.ExpenseRed
import com.agustinazorin.finanzas.core.ui.theme.IncomeGreen
import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.feature.account.domain.Account
import com.agustinazorin.finanzas.feature.category.domain.Category
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMember
import com.agustinazorin.finanzas.feature.transaction.domain.Transaction

@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val typeOptions: List<TransactionType?> = listOf(null, TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)
            LabeledDropdown(
                label = stringResource(R.string.transactions_filter_type),
                options = typeOptions,
                selected = state.filter.type,
                optionLabel = { it?.label() ?: stringResource(R.string.common_all) },
                onSelected = { viewModel.setTypeFilter(it) },
                modifier = Modifier.width(160.dp),
            )
            val accountOptions: List<Account?> = listOf(null) + state.accounts
            LabeledDropdown(
                label = stringResource(R.string.transactions_filter_account),
                options = accountOptions,
                selected = state.accounts.firstOrNull { it.id == state.filter.accountId },
                optionLabel = { it?.name ?: stringResource(R.string.common_all) },
                onSelected = { viewModel.setAccountFilter(it?.id) },
                modifier = Modifier.width(160.dp),
            )
            val categoryOptions: List<Category?> = listOf(null) + state.categories
            LabeledDropdown(
                label = stringResource(R.string.transactions_filter_category),
                options = categoryOptions,
                selected = state.categories.firstOrNull { it.id == state.filter.categoryId },
                optionLabel = { it?.name ?: stringResource(R.string.common_all) },
                onSelected = { viewModel.setCategoryFilter(it?.id) },
                modifier = Modifier.width(160.dp),
            )
            if (state.members.isNotEmpty()) {
                val memberOptions: List<HouseholdMember?> = listOf(null) + state.members
                LabeledDropdown(
                    label = stringResource(R.string.transactions_filter_member),
                    options = memberOptions,
                    selected = state.members.firstOrNull { it.id == state.filter.memberId },
                    optionLabel = { it?.name ?: stringResource(R.string.common_all) },
                    onSelected = { viewModel.setMemberFilter(it?.id) },
                    modifier = Modifier.width(160.dp),
                )
            }
        }

        if (state.transactions.isEmpty()) {
            EmptyState(stringResource(R.string.transactions_empty))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.transactions, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        accountName = state.accounts.firstOrNull { it.id == transaction.accountId }?.name,
                        categoryName = state.categories.firstOrNull { it.id == transaction.categoryId }?.name,
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction, accountName: String?, categoryName: String?) {
    val color = if (transaction.direction == TransactionDirection.OUTFLOW) ExpenseRed else IncomeGreen
    val sign = if (transaction.direction == TransactionDirection.OUTFLOW) -1 else 1

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(transaction.merchant ?: transaction.type.label(), style = MaterialTheme.typography.bodyLarge)
                val subtitle = listOfNotNull(accountName, categoryName, transaction.date.toString()).joinToString(" · ")
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            Text((sign * transaction.amount).formatAsMoney(transaction.currency), color = color)
        }
    }
}
