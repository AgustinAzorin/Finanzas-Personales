package com.agustinazorin.finanzas.feature.transaction.ui

import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.core.ui.HouseholdScopedViewModel
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.feature.account.domain.Account
import com.agustinazorin.finanzas.feature.account.domain.AccountRepository
import com.agustinazorin.finanzas.feature.category.domain.Category
import com.agustinazorin.finanzas.feature.category.domain.CategoryRepository
import com.agustinazorin.finanzas.feature.household.domain.HouseholdRepository
import com.agustinazorin.finanzas.feature.transaction.domain.Transaction
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionFilter
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class TransactionsFilterState(
    val type: TransactionType? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
)

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val filter: TransactionsFilterState = TransactionsFilterState(),
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    householdRepository: HouseholdRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
) : HouseholdScopedViewModel(householdRepository) {

    private val filterState = MutableStateFlow(TransactionsFilterState())

    val uiState: StateFlow<TransactionsUiState> = householdId.filterNotNull().flatMapLatest { id ->
        filterState.flatMapLatest { filter ->
            val monthStart = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
            val monthEnd = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth())
            combine(
                transactionRepository.observeFiltered(
                    id,
                    TransactionFilter(
                        start = monthStart,
                        end = monthEnd,
                        accountId = filter.accountId,
                        categoryId = filter.categoryId,
                        type = filter.type,
                    ),
                ),
                accountRepository.observeAccounts(id),
                categoryRepository.observeAll(),
            ) { transactions, accounts, categories ->
                TransactionsUiState(transactions, accounts, categories, filter)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun setTypeFilter(type: TransactionType?) {
        filterState.value = filterState.value.copy(type = type)
    }

    fun setAccountFilter(accountId: Long?) {
        filterState.value = filterState.value.copy(accountId = accountId)
    }

    fun setCategoryFilter(categoryId: Long?) {
        filterState.value = filterState.value.copy(categoryId = categoryId)
    }
}
