package com.agustinazorin.finanzas.feature.transaction.domain.usecase

import com.agustinazorin.finanzas.core.engine.toEngineTransaction
import com.agustinazorin.finanzas.engine.metrics.PeriodSummaryCalculator
import com.agustinazorin.finanzas.engine.model.PeriodSummary
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionRepository
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/** Gasto/ingreso real de un período, por moneda (CLAUDE.md, secciones 6 y 32). */
class GetPeriodSummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(householdId: Long, start: LocalDate, end: LocalDate) =
        transactionRepository.observeAllUpTo(householdId, end).map { transactions ->
            PeriodSummaryCalculator.summarize(
                transactions = transactions.map { it.toEngineTransaction() },
                start = start,
                end = end,
            )
        }
}

fun Map<String, PeriodSummary>.inCurrency(currency: String): PeriodSummary? = this[currency]
