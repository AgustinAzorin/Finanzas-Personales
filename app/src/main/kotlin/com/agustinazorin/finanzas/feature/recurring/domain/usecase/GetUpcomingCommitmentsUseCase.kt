package com.agustinazorin.finanzas.feature.recurring.domain.usecase

import com.agustinazorin.finanzas.core.engine.toEngineRecurringTransaction
import com.agustinazorin.finanzas.engine.commitments.UpcomingCommitmentsCalculator
import com.agustinazorin.finanzas.feature.recurring.domain.RecurringTransactionRepository
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Próximos compromisos proyectados a partir de los movimientos recurrentes activos
 * (CLAUDE.md, sección 25 — pantalla "Comprometido"). Nunca asume que ya ocurrieron.
 */
class GetUpcomingCommitmentsUseCase @Inject constructor(
    private val recurringTransactionRepository: RecurringTransactionRepository,
) {
    operator fun invoke(householdId: Long, from: LocalDate = LocalDate.now(), days: Long) =
        recurringTransactionRepository.observeActive(householdId).map { recurring ->
            UpcomingCommitmentsCalculator.upcoming(
                recurring = recurring.map { it.toEngineRecurringTransaction() },
                from = from,
                days = days,
            )
        }
}
