package com.agustinazorin.finanzas.feature.home.ui

import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.core.engine.toEngineTransaction
import com.agustinazorin.finanzas.core.ui.HouseholdScopedViewModel
import com.agustinazorin.finanzas.engine.metrics.PeriodSummaryCalculator
import com.agustinazorin.finanzas.engine.model.PeriodSummary
import com.agustinazorin.finanzas.engine.model.UpcomingCommitment
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.feature.account.domain.usecase.GetAvailableLiquidityUseCase
import com.agustinazorin.finanzas.feature.account.domain.usecase.GetNetWorthUseCase
import com.agustinazorin.finanzas.feature.account.domain.usecase.inCurrency
import com.agustinazorin.finanzas.feature.household.domain.HouseholdRepository
import com.agustinazorin.finanzas.feature.recurring.domain.usecase.GetUpcomingCommitmentsUseCase
import com.agustinazorin.finanzas.feature.transaction.domain.Transaction
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionRepository
import com.agustinazorin.finanzas.feature.transaction.domain.usecase.inCurrency
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

private const val BASE_CURRENCY = "ARS"
private const val COMMITMENT_HORIZON_DAYS = 30L
private const val RECENT_TRANSACTIONS_LIMIT = 5
private const val UPCOMING_PREVIEW_LIMIT = 5

data class HomeUiState(
    val isLoading: Boolean = true,
    val netWorth: Money = Money.zero(BASE_CURRENCY),
    val available: Money = Money.zero(BASE_CURRENCY),
    val committed: Money = Money.zero(BASE_CURRENCY),
    val monthSummary: PeriodSummary? = null,
    val upcomingCommitments: List<UpcomingCommitment> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    householdRepository: HouseholdRepository,
    private val transactionRepository: TransactionRepository,
    private val getNetWorthUseCase: GetNetWorthUseCase,
    private val getAvailableLiquidityUseCase: GetAvailableLiquidityUseCase,
    private val getUpcomingCommitmentsUseCase: GetUpcomingCommitmentsUseCase,
) : HouseholdScopedViewModel(householdRepository) {

    val uiState: StateFlow<HomeUiState> = householdId.filterNotNull().flatMapLatest { id ->
        val today = LocalDate.now()
        val monthStart = today.with(TemporalAdjusters.firstDayOfMonth())
        val monthEnd = today.with(TemporalAdjusters.lastDayOfMonth())

        combine(
            getNetWorthUseCase(id, today),
            getAvailableLiquidityUseCase(id, BASE_CURRENCY, today),
            getUpcomingCommitmentsUseCase(id, today, COMMITMENT_HORIZON_DAYS),
            transactionRepository.observeAllUpTo(id, monthEnd),
            transactionRepository.observeRecent(id, RECENT_TRANSACTIONS_LIMIT),
        ) { netWorthByCurrency, available, commitments, monthTransactions, recent ->
            val committed = Money.sum(
                commitments.map { it.amount }.filter { it.currency == BASE_CURRENCY },
                BASE_CURRENCY,
            )
            val monthSummary = PeriodSummaryCalculator.summarize(
                transactions = monthTransactions.map { it.toEngineTransaction() },
                start = monthStart,
                end = monthEnd,
            ).inCurrency(BASE_CURRENCY)

            HomeUiState(
                isLoading = false,
                netWorth = netWorthByCurrency.inCurrency(BASE_CURRENCY),
                available = available,
                committed = committed,
                monthSummary = monthSummary,
                upcomingCommitments = commitments.take(UPCOMING_PREVIEW_LIMIT),
                recentTransactions = recent,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
