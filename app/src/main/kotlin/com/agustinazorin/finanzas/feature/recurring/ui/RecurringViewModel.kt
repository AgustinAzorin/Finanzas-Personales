package com.agustinazorin.finanzas.feature.recurring.ui

import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.core.ui.HouseholdScopedViewModel
import com.agustinazorin.finanzas.engine.model.Periodicity
import com.agustinazorin.finanzas.engine.model.RecurringType
import com.agustinazorin.finanzas.feature.account.domain.Account
import com.agustinazorin.finanzas.feature.account.domain.AccountRepository
import com.agustinazorin.finanzas.feature.category.domain.Category
import com.agustinazorin.finanzas.feature.category.domain.CategoryRepository
import com.agustinazorin.finanzas.feature.household.domain.HouseholdRepository
import com.agustinazorin.finanzas.feature.recurring.domain.RecurringTransaction
import com.agustinazorin.finanzas.feature.recurring.domain.RecurringTransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecurringUiState(
    val recurring: List<RecurringTransaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
)

@HiltViewModel
class RecurringViewModel @Inject constructor(
    householdRepository: HouseholdRepository,
    private val recurringRepository: RecurringTransactionRepository,
    private val accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) : HouseholdScopedViewModel(householdRepository) {

    val uiState: StateFlow<RecurringUiState> = householdId.filterNotNull().flatMapLatest { id ->
        combine(
            recurringRepository.observeAll(id),
            accountRepository.observeActiveAccounts(id),
            categoryRepository.observeAll(),
        ) { recurring, accounts, categories ->
            RecurringUiState(recurring, accounts, categories)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringUiState())

    fun addRecurring(
        type: RecurringType,
        name: String,
        estimatedAmount: Long,
        currency: String,
        periodicity: Periodicity,
        dueDay: Int,
        categoryId: Long?,
        accountId: Long?,
        memberId: Long?,
    ) {
        if (name.isBlank() || estimatedAmount <= 0) return
        viewModelScope.launch {
            val id = householdId.value ?: return@launch
            recurringRepository.create(id, type, name, estimatedAmount, currency, periodicity, dueDay, categoryId, accountId, memberId)
        }
    }

    fun setActive(id: Long, isActive: Boolean) {
        viewModelScope.launch { recurringRepository.setActive(id, isActive) }
    }
}
