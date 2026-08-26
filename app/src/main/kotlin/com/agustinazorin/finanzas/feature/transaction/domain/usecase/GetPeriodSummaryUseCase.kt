package com.agustinazorin.finanzas.feature.transaction.domain.usecase

import com.agustinazorin.finanzas.core.engine.toEngineInstallment
import com.agustinazorin.finanzas.core.engine.toEngineTransaction
import com.agustinazorin.finanzas.engine.metrics.PeriodSummaryCalculator
import com.agustinazorin.finanzas.engine.model.PeriodSummary
import com.agustinazorin.finanzas.feature.installment.domain.InstallmentRepository
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionRepository
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Gasto/ingreso real de un período, por moneda (CLAUDE.md, secciones 6 y 32). Las compras en
 * cuotas nunca se cuentan por su fecha de compra: se reparten entre sus cuotas, cada una imputada
 * al período de su propio ciclo de facturación (Regla 3, CLAUDE.md sección 7).
 */
class GetPeriodSummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val installmentRepository: InstallmentRepository,
) {
    operator fun invoke(householdId: Long, start: LocalDate, end: LocalDate) =
        combine(
            transactionRepository.observeAllUpTo(householdId, end),
            installmentRepository.observeAllUpTo(householdId, end),
        ) { transactions, installments ->
            PeriodSummaryCalculator.summarize(
                transactions = transactions.map { it.toEngineTransaction() },
                start = start,
                end = end,
                installments = installments.map { it.toEngineInstallment() },
            )
        }
}

fun Map<String, PeriodSummary>.inCurrency(currency: String): PeriodSummary? = this[currency]
