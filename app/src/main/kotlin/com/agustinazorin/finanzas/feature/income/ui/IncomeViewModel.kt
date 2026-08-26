package com.agustinazorin.finanzas.feature.income.ui

import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.core.ui.HouseholdScopedViewModel
import com.agustinazorin.finanzas.engine.model.RecurringType
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.feature.household.domain.HouseholdRepository
import com.agustinazorin.finanzas.feature.recurring.domain.RecurringTransactionRepository
import com.agustinazorin.finanzas.feature.transaction.domain.Transaction
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionFilter
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionRepository
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

data class IncomeUiState(
    val expectedMonthlyIncome: Money = Money.zero(BASE_CURRENCY),
    val realMonthlyIncome: Money = Money.zero(BASE_CURRENCY),
    val incomeTransactions: List<Transaction> = emptyList(),
)

/** Ingresos esperados vs reales del mes (CLAUDE.md, sección 31). */
@HiltViewModel
class IncomeViewModel @Inject constructor(
    householdRepository: HouseholdRepository,
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringTransactionRepository,
) : HouseholdScopedViewModel(householdRepository) {

    val uiState: StateFlow<IncomeUiState> = householdId.filterNotNull().flatMapLatest { id ->
        val monthStart = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
        val monthEnd = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth())

        combine(
            recurringRepository.observeActive(id),
            transactionRepository.observeFiltered(id, TransactionFilter(start = monthStart, end = monthEnd, type = TransactionType.INCOME)),
        ) { recurring, incomeTransactions ->
            val expected = Money.sum(
                recurring.filter { it.type == RecurringType.INCOME && it.currency == BASE_CURRENCY }.map { Money(it.estimatedAmount, it.currency) },
                BASE_CURRENCY,
            )
            val real = Money.sum(
                incomeTransactions.filter { it.currency == BASE_CURRENCY }.map { Money(it.amount, it.currency) },
                BASE_CURRENCY,
            )
            IncomeUiState(expected, real, incomeTransactions)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IncomeUiState())
}
