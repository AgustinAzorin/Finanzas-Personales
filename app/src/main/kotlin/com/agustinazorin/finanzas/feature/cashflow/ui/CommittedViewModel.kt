package com.agustinazorin.finanzas.feature.cashflow.ui

import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.core.ui.HouseholdScopedViewModel
import com.agustinazorin.finanzas.engine.cashflow.CashFlowProjectionCalculator
import com.agustinazorin.finanzas.engine.model.CashFlowEvent
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.feature.cashflow.domain.usecase.GetCashFlowProjectionUseCase
import com.agustinazorin.finanzas.feature.household.domain.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

private const val BASE_CURRENCY = "ARS"

/** Horizontes fijos de la pantalla "Comprometido" (CLAUDE.md, sección 25). */
val COMMITTED_HORIZON_OPTIONS = listOf(7L, 30L, 60L, 90L)

data class CommittedUiState(
    val selectedHorizon: Long = 30L,
    val totalsByHorizon: Map<Long, Money> = emptyMap(),
    val items: List<CashFlowEvent> = emptyList(),
)

/** ViewModel de la pantalla "Comprometido" (CLAUDE.md, sección 25): cuánta plata futura ya está comprometida. */
@HiltViewModel
class CommittedViewModel @Inject constructor(
    householdRepository: HouseholdRepository,
    private val getCashFlowProjectionUseCase: GetCashFlowProjectionUseCase,
) : HouseholdScopedViewModel(householdRepository) {

    private val selectedHorizon = MutableStateFlow(30L)

    val uiState: StateFlow<CommittedUiState> = householdId.filterNotNull().flatMapLatest { id ->
        combine(selectedHorizon, getCashFlowProjectionUseCase(id, BASE_CURRENCY)) { horizon, projection ->
            val today = LocalDate.now()
            val totals = COMMITTED_HORIZON_OPTIONS.associateWith { days ->
                CashFlowProjectionCalculator.totalCommitted(BASE_CURRENCY, today, days, projection.events)
            }
            val limit = today.plusDays(horizon)
            CommittedUiState(
                selectedHorizon = horizon,
                totalsByHorizon = totals,
                items = projection.events
                    .filter { it.amount.isNegative && !it.date.isBefore(today) && !it.date.isAfter(limit) }
                    .sortedBy { it.date },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CommittedUiState())

    fun selectHorizon(days: Long) {
        selectedHorizon.value = days
    }
}
