package com.agustinazorin.finanzas.feature.household.ui

import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.core.ui.HouseholdScopedViewModel
import com.agustinazorin.finanzas.engine.household.HouseholdDebt
import com.agustinazorin.finanzas.engine.household.MemberAttribution
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMember
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMemberRepository
import com.agustinazorin.finanzas.feature.household.domain.HouseholdRepository
import com.agustinazorin.finanzas.feature.household.domain.usecase.GetHouseholdReportUseCase
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

data class HouseholdReportUiState(
    val start: LocalDate = LocalDate.now(),
    val end: LocalDate = LocalDate.now(),
    val members: List<HouseholdMember> = emptyList(),
    val memberAttributions: List<MemberAttribution> = emptyList(),
    val debts: List<HouseholdDebt> = emptyList(),
)

/** Reporte del hogar del mes (roadmap Fase 3): reparto de gasto/ingreso por persona y saldos entre miembros por gastos compartidos. */
@HiltViewModel
class HouseholdReportViewModel @Inject constructor(
    householdRepository: HouseholdRepository,
    private val householdMemberRepository: HouseholdMemberRepository,
    private val getHouseholdReportUseCase: GetHouseholdReportUseCase,
) : HouseholdScopedViewModel(householdRepository) {

    val uiState: StateFlow<HouseholdReportUiState> = householdId.filterNotNull().flatMapLatest { id ->
        val monthStart = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth())
        val monthEnd = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth())

        combine(
            getHouseholdReportUseCase(id, monthStart, monthEnd),
            householdMemberRepository.observeMembers(id),
        ) { report, members ->
            HouseholdReportUiState(
                start = monthStart,
                end = monthEnd,
                members = members,
                memberAttributions = report.memberAttributions.filter { it.currency == BASE_CURRENCY },
                debts = report.debts.filter { it.currency == BASE_CURRENCY },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HouseholdReportUiState())
}
