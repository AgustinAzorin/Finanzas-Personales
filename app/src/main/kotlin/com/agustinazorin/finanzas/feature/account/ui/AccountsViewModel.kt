package com.agustinazorin.finanzas.feature.account.ui

import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.core.ui.HouseholdScopedViewModel
import com.agustinazorin.finanzas.engine.model.AccountType
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.feature.account.domain.Account
import com.agustinazorin.finanzas.feature.account.domain.AccountRepository
import com.agustinazorin.finanzas.feature.account.domain.usecase.GetAccountBalancesUseCase
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMember
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMemberRepository
import com.agustinazorin.finanzas.feature.household.domain.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AccountUi(val account: Account, val balance: Money)

data class AccountsUiState(
    val isLoading: Boolean = true,
    val accounts: List<AccountUi> = emptyList(),
    val members: List<HouseholdMember> = emptyList(),
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    householdRepository: HouseholdRepository,
    private val accountRepository: AccountRepository,
    private val householdMemberRepository: HouseholdMemberRepository,
    private val getAccountBalancesUseCase: GetAccountBalancesUseCase,
) : HouseholdScopedViewModel(householdRepository) {

    val uiState: StateFlow<AccountsUiState> = householdId.filterNotNull().flatMapLatest { id ->
        combine(
            accountRepository.observeAccounts(id),
            getAccountBalancesUseCase(id),
            householdMemberRepository.observeActiveMembers(id),
        ) { accounts, balances, members ->
            val balanceByAccount = balances.associateBy { it.accountId }
            AccountsUiState(
                isLoading = false,
                accounts = accounts.map { account ->
                    AccountUi(account, balanceByAccount[account.id]?.balance ?: Money.zero(account.currency))
                },
                members = members,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    fun addAccount(
        name: String,
        type: AccountType,
        currency: String,
        initialBalance: Long,
        ownerMemberId: Long?,
    ) {
        viewModelScope.launch {
            val id = householdId.value ?: return@launch
            accountRepository.createAccount(
                householdId = id,
                ownerMemberId = ownerMemberId,
                name = name,
                type = type,
                currency = currency,
                initialBalance = initialBalance,
                initialBalanceDate = LocalDate.now(),
            )
        }
    }

    fun setAccountActive(accountId: Long, isActive: Boolean) {
        viewModelScope.launch { accountRepository.setAccountActive(accountId, isActive) }
    }
}
