package com.agustinazorin.finanzas.feature.creditcard.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.engine.model.AccountType
import com.agustinazorin.finanzas.feature.account.domain.Account
import com.agustinazorin.finanzas.feature.account.domain.AccountRepository
import com.agustinazorin.finanzas.feature.category.domain.Category
import com.agustinazorin.finanzas.feature.category.domain.CategoryRepository
import com.agustinazorin.finanzas.feature.creditcard.domain.CreditCardRepository
import com.agustinazorin.finanzas.feature.creditcard.domain.usecase.AddCreditCardPurchaseUseCase
import com.agustinazorin.finanzas.feature.creditcard.domain.usecase.CreditCardOverview
import com.agustinazorin.finanzas.feature.creditcard.domain.usecase.GetCreditCardOverviewUseCase
import com.agustinazorin.finanzas.feature.creditcard.domain.usecase.PayCreditCardStatementUseCase
import com.agustinazorin.finanzas.feature.household.domain.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CreditCardDetailUiState(
    val account: Account? = null,
    val isConfigured: Boolean = false,
    val overview: CreditCardOverview? = null,
    val categories: List<Category> = emptyList(),
    val payFromAccounts: List<Account> = emptyList(),
)

@HiltViewModel
class CreditCardDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val householdRepository: HouseholdRepository,
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val getCreditCardOverviewUseCase: GetCreditCardOverviewUseCase,
    private val addCreditCardPurchaseUseCase: AddCreditCardPurchaseUseCase,
    private val payCreditCardStatementUseCase: PayCreditCardStatementUseCase,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val accountId: Long = checkNotNull(savedStateHandle["accountId"]) { "Falta accountId en la navegación." }

    private val householdId = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch { householdId.value = householdRepository.requireHouseholdId() }
    }

    val uiState: StateFlow<CreditCardDetailUiState> = combine(
        accountRepository.observeAccount(accountId),
        creditCardRepository.observeByAccount(accountId),
        categoryRepository.observeAll(),
        householdId.filterNotNull().flatMapLatest { id -> accountRepository.observeActiveAccounts(id) },
    ) { account, creditCard, categories, accounts ->
        CreditCardConfigState(
            account = account,
            isConfigured = creditCard != null,
            categories = categories,
            payFromAccounts = accounts.filter { it.id != accountId && it.type != AccountType.CREDIT_CARD },
        )
    }.flatMapLatest { config ->
        if (!config.isConfigured) {
            flowOf(config.toUiState(overview = null))
        } else {
            getCreditCardOverviewUseCase(accountId).map { overview -> config.toUiState(overview) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CreditCardDetailUiState())

    fun configure(closingDay: Int, dueDay: Int, creditLimit: Long) {
        viewModelScope.launch { creditCardRepository.upsert(accountId, closingDay, dueDay, creditLimit) }
    }

    fun addPurchase(
        amount: Long,
        currency: String,
        date: LocalDate,
        installments: Int,
        categoryId: Long?,
        merchant: String?,
        note: String?,
    ) {
        viewModelScope.launch {
            val id = householdId.value ?: return@launch
            addCreditCardPurchaseUseCase(
                householdId = id,
                creditCardAccountId = accountId,
                ownerMemberId = null,
                totalAmount = amount,
                currency = currency,
                purchaseDate = date,
                totalInstallments = installments,
                categoryId = categoryId,
                merchant = merchant,
                note = note,
            )
        }
    }

    fun payStatement(statementId: Long, fromAccountId: Long, amount: Long) {
        viewModelScope.launch {
            val id = householdId.value ?: return@launch
            payCreditCardStatementUseCase(
                householdId = id,
                statementId = statementId,
                fromAccountId = fromAccountId,
                amount = amount,
                date = LocalDate.now(),
            )
        }
    }
}

private data class CreditCardConfigState(
    val account: Account?,
    val isConfigured: Boolean,
    val categories: List<Category>,
    val payFromAccounts: List<Account>,
) {
    fun toUiState(overview: CreditCardOverview?) = CreditCardDetailUiState(
        account = account,
        isConfigured = isConfigured,
        overview = overview,
        categories = categories,
        payFromAccounts = payFromAccounts,
    )
}
