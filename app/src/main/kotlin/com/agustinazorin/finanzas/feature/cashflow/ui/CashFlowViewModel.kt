package com.agustinazorin.finanzas.feature.cashflow.ui

import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.core.ui.HouseholdScopedViewModel
import com.agustinazorin.finanzas.engine.cashflow.CashFlowProjectionCalculator
import com.agustinazorin.finanzas.engine.model.CashFlowPoint
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

/** Horizontes fijos de la pantalla "Flujo" (CLAUDE.md, sección 23). */
val CASH_FLOW_HORIZON_OPTIONS = listOf(7L, 30L, 60L, 90L)

data class CashFlowUiState(
    val selectedHorizon: Long = 30L,
    val points: List<CashFlowPoint> = emptyList(),
    /** Primera fecha en la que la liquidez proyectada cae por debajo de $0 dentro del horizonte elegido. Null si no ocurre. */
    val liquidityAlertDate: LocalDate? = null,
)

/**
 * ViewModel de la pantalla "Flujo" (CLAUDE.md, sección 26): línea de tiempo de saldo proyectado.
 */
@HiltViewModel
class CashFlowViewModel @Inject constructor(
    householdRepository: HouseholdRepository,
    private val getCashFlowProjectionUseCase: GetCashFlowProjectionUseCase,
) : HouseholdScopedViewModel(householdRepository) {

    private val selectedHorizon = MutableStateFlow(30L)

    val uiState: StateFlow<CashFlowUiState> = householdId.filterNotNull().flatMapLatest { id ->
        combine(selectedHorizon, getCashFlowProjectionUseCase(id, BASE_CURRENCY)) { horizon, projection ->
            val today = LocalDate.now()
            val limit = today.plusDays(horizon)
            val eventsInWindow = projection.events.filter { !it.date.isAfter(limit) }
            val points = CashFlowProjectionCalculator.project(today, projection.startingBalance, eventsInWindow)
            CashFlowUiState(
                selectedHorizon = horizon,
                points = points,
                liquidityAlertDate = CashFlowProjectionCalculator.firstDateBelow(points.drop(1), Money.zero(BASE_CURRENCY)),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CashFlowUiState())

    fun selectHorizon(days: Long) {
        selectedHorizon.value = days
    }
}
