package com.agustinazorin.finanzas.feature.transaction.ui

import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.core.preferences.QuickAddPreferences
import com.agustinazorin.finanzas.core.ui.HouseholdScopedViewModel
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.engine.text.MerchantNormalizer
import com.agustinazorin.finanzas.feature.account.domain.Account
import com.agustinazorin.finanzas.feature.account.domain.AccountRepository
import com.agustinazorin.finanzas.feature.category.domain.Category
import com.agustinazorin.finanzas.feature.category.domain.CategoryRepository
import com.agustinazorin.finanzas.feature.category.domain.CategoryRuleRepository
import com.agustinazorin.finanzas.feature.household.domain.HouseholdRepository
import com.agustinazorin.finanzas.feature.transaction.domain.usecase.AddExpenseUseCase
import com.agustinazorin.finanzas.feature.transaction.domain.usecase.AddIncomeUseCase
import com.agustinazorin.finanzas.feature.transaction.domain.usecase.AddTransferUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class QuickAddOptions(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val defaultAccountId: Long? = null,
)

@HiltViewModel
class QuickAddViewModel @Inject constructor(
    householdRepository: HouseholdRepository,
    private val accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val addIncomeUseCase: AddIncomeUseCase,
    private val addTransferUseCase: AddTransferUseCase,
    private val quickAddPreferences: QuickAddPreferences,
    private val categoryRuleRepository: CategoryRuleRepository,
) : HouseholdScopedViewModel(householdRepository) {

    val options: StateFlow<QuickAddOptions> = householdId.filterNotNull().flatMapLatest { id ->
        combine(accountRepository.observeActiveAccounts(id), categoryRepository.observeAll()) { accounts, categories ->
            val defaultAccountId = quickAddPreferences.lastAccountId?.takeIf { last -> accounts.any { it.id == last } }
                ?: accounts.firstOrNull()?.id
            QuickAddOptions(accounts, categories, defaultAccountId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuickAddOptions())

    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved: SharedFlow<Unit> = _saved

    fun saveExpense(accountId: Long, amount: Long, currency: String, categoryId: Long?, merchant: String?, note: String?) {
        viewModelScope.launch {
            val id = householdId.value ?: return@launch
            addExpenseUseCase(id, accountId, ownerMemberId = null, amount = amount, currency = currency, date = LocalDate.now(), categoryId = categoryId, merchant = merchant, note = note)
            quickAddPreferences.lastAccountId = accountId
            _saved.tryEmit(Unit)
        }
    }

    fun saveIncome(accountId: Long, amount: Long, currency: String, categoryId: Long?, merchant: String?, note: String?) {
        viewModelScope.launch {
            val id = householdId.value ?: return@launch
            addIncomeUseCase(id, accountId, ownerMemberId = null, amount = amount, currency = currency, date = LocalDate.now(), categoryId = categoryId, merchant = merchant, note = note)
            quickAddPreferences.lastAccountId = accountId
            _saved.tryEmit(Unit)
        }
    }

    /** Sugerencia de categoría por comercio, aprendida de correcciones anteriores (CLAUDE.md, sección 39). */
    suspend fun suggestCategoryId(merchant: String): Long? =
        categoryRuleRepository.suggestCategory(MerchantNormalizer.normalize(merchant))

    fun saveTransfer(fromAccountId: Long, toAccountId: Long, amount: Long, note: String?) {
        viewModelScope.launch {
            val id = householdId.value ?: return@launch
            addTransferUseCase(id, fromAccountId, toAccountId, amount, LocalDate.now(), note)
            quickAddPreferences.lastAccountId = fromAccountId
            _saved.tryEmit(Unit)
        }
    }
}
