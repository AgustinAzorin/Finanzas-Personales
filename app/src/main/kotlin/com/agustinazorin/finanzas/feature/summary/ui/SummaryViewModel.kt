package com.agustinazorin.finanzas.feature.summary.ui

import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.core.engine.toEngineTransaction
import com.agustinazorin.finanzas.core.ui.HouseholdScopedViewModel
import com.agustinazorin.finanzas.engine.metrics.PeriodSummaryCalculator
import com.agustinazorin.finanzas.engine.model.AccountType
import com.agustinazorin.finanzas.engine.model.Periodicity
import com.agustinazorin.finanzas.engine.model.PeriodSummary
import com.agustinazorin.finanzas.engine.model.RecurringType
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.feature.account.domain.AccountRepository
import com.agustinazorin.finanzas.feature.account.domain.usecase.GetAccountBalancesUseCase
import com.agustinazorin.finanzas.feature.account.domain.usecase.GetNetWorthUseCase
import com.agustinazorin.finanzas.feature.account.domain.usecase.inCurrency
import com.agustinazorin.finanzas.feature.household.domain.HouseholdRepository
import com.agustinazorin.finanzas.feature.recurring.domain.RecurringTransactionRepository
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionRepository
import com.agustinazorin.finanzas.feature.transaction.domain.usecase.inCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

private const val BASE_CURRENCY = "ARS"
private val LIABILITY_TYPES = setOf(AccountType.CREDIT_CARD, AccountType.LOAN, AccountType.OTHER_LIABILITY)

data class SummaryUiState(
    val netWorth: Money = Money.zero(BASE_CURRENCY),
    val assets: Money = Money.zero(BASE_CURRENCY),
    val liabilities: Money = Money.zero(BASE_CURRENCY),
    val monthSummary: PeriodSummary? = null,
    val debtToIncomeRatio: Double? = null,
    val fixedExpensesToIncomeRatio: Double? = null,
)

/** Resumen financiero (CLAUDE.md, secciones 27 y 32): una foto consolidada, no sólo "cuánto gastaste". */
@HiltViewModel
class SummaryViewModel @Inject constructor(
    householdRepository: HouseholdRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringTransactionRepository,
    private val getAccountBalancesUseCase: GetAccountBalancesUseCase,
    private val getNetWorthUseCase: GetNetWorthUseCase,
) : HouseholdScopedViewModel(householdRepository) {

    val uiState: StateFlow<SummaryUiState> = householdId.filterNotNull().flatMapLatest { id ->
        val today = LocalDate.now()
        val monthStart = today.with(TemporalAdjusters.firstDayOfMonth())
        val monthEnd = today.with(TemporalAdjusters.lastDayOfMonth())

        combine(
            getNetWorthUseCase(id, today),
            accountRepository.observeActiveAccounts(id),
            getAccountBalancesUseCase(id, today),
            transactionRepository.observeAllUpTo(id, monthEnd),
            recurringRepository.observeActive(id),
        ) { netWorthByCurrency, accounts, balances, monthTransactions, recurring ->
            val balanceByAccount = balances.associateBy { it.accountId }.mapValues { it.value.balance }
            val assets = Money.sum(
                accounts.filter { it.type !in LIABILITY_TYPES && it.currency == BASE_CURRENCY }
                    .mapNotNull { balanceByAccount[it.id] }.filter { it.isPositive },
                BASE_CURRENCY,
            )
            val liabilities = Money.sum(
                accounts.filter { it.type in LIABILITY_TYPES && it.currency == BASE_CURRENCY }
                    .mapNotNull { balanceByAccount[it.id] }.filter { it.isNegative }.map { -it },
                BASE_CURRENCY,
            )
            val monthSummary = PeriodSummaryCalculator.summarize(
                monthTransactions.map { it.toEngineTransaction() }, monthStart, monthEnd,
            ).inCurrency(BASE_CURRENCY)

            val monthlyIncome = monthSummary?.totalIncome?.minorUnits ?: 0L
            val debtToIncome = if (monthlyIncome > 0) liabilities.minorUnits.toDouble() / monthlyIncome else null

            val fixedExpenses = recurring
                .filter { it.type == RecurringType.EXPENSE && it.periodicity == Periodicity.MONTHLY && it.currency == BASE_CURRENCY }
                .sumOf { it.estimatedAmount }
            val fixedToIncome = if (monthlyIncome > 0) fixedExpenses.toDouble() / monthlyIncome else null

            SummaryUiState(
                netWorth = netWorthByCurrency.inCurrency(BASE_CURRENCY),
                assets = assets,
                liabilities = liabilities,
                monthSummary = monthSummary,
                debtToIncomeRatio = debtToIncome,
                fixedExpensesToIncomeRatio = fixedToIncome,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryUiState())
}
