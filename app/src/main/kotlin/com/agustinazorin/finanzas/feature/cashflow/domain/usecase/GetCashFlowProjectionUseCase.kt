package com.agustinazorin.finanzas.feature.cashflow.domain.usecase

import com.agustinazorin.finanzas.core.engine.toEngineRecurringTransaction
import com.agustinazorin.finanzas.engine.commitments.UpcomingCommitmentsCalculator
import com.agustinazorin.finanzas.engine.model.CashFlowEvent
import com.agustinazorin.finanzas.engine.model.Certainty
import com.agustinazorin.finanzas.engine.model.RecurringType
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.engine.model.UpcomingCommitment
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.feature.account.domain.usecase.GetAvailableLiquidityUseCase
import com.agustinazorin.finanzas.feature.installment.domain.InstallmentForSummary
import com.agustinazorin.finanzas.feature.installment.domain.InstallmentRepository
import com.agustinazorin.finanzas.feature.recurring.domain.RecurringTransactionRepository
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/** Hasta dónde se proyecta el flujo de caja de una sola vez; las pantallas recortan una ventana menor sobre esto (CLAUDE.md, sección 23: horizontes de 7/30/60/90 días). */
const val CASH_FLOW_MAX_HORIZON_DAYS = 90L

/** Todo lo que necesita el motor financiero para proyectar el flujo de caja: el saldo líquido actual y los eventos futuros conocidos. */
data class CashFlowProjection(val startingBalance: Money, val events: List<CashFlowEvent>)

/**
 * Arma la proyección de flujo de caja (CLAUDE.md, sección 36) combinando el saldo líquido actual
 * con dos fuentes de eventos futuros ya persistidas: movimientos recurrentes (ingresos y gastos)
 * y cuotas pendientes de tarjetas. Deliberadamente no incluye deudas/préstamos (Liability,
 * sección 11): esa entidad todavía no está implementada (queda para Fase 5, Patrimonio).
 *
 * No existe una entidad `FinancialCommitment` persistida: mantenerla sincronizada con
 * recurrentes/cuotas sería una segunda fuente de verdad para el mismo dato, violando la regla de
 * integridad de datos. En su lugar, cada [CashFlowEvent] se deriva en el momento a partir de las
 * fuentes ya existentes, igual que ya hacía [UpcomingCommitmentsCalculator] antes de esta fase.
 */
class GetCashFlowProjectionUseCase @Inject constructor(
    private val getAvailableLiquidityUseCase: GetAvailableLiquidityUseCase,
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val installmentRepository: InstallmentRepository,
) {
    operator fun invoke(householdId: Long, currency: String, from: LocalDate = LocalDate.now()) =
        combine(
            getAvailableLiquidityUseCase(householdId, currency, from),
            recurringTransactionRepository.observeActive(householdId),
            installmentRepository.observeUpcomingForHousehold(householdId),
        ) { startingBalance, recurring, installments ->
            val limit = from.plusDays(CASH_FLOW_MAX_HORIZON_DAYS)

            val recurringEvents = UpcomingCommitmentsCalculator.upcoming(
                recurring = recurring.map { it.toEngineRecurringTransaction() },
                from = from,
                days = CASH_FLOW_MAX_HORIZON_DAYS,
            ).filter { it.amount.currency == currency }.map { it.toCashFlowEvent() }

            val installmentEvents = installments
                .filter {
                    it.currency == currency && !it.installment.dueDate.isBefore(from) && !it.installment.dueDate.isAfter(limit)
                }
                .map { it.toCashFlowEvent() }

            CashFlowProjection(startingBalance, recurringEvents + installmentEvents)
        }
}

private fun UpcomingCommitment.toCashFlowEvent(): CashFlowEvent {
    val signedAmount = if (type == RecurringType.EXPENSE) -amount else amount
    return CashFlowEvent(date = dueDate, label = name, amount = signedAmount, certainty = certainty)
}

private fun InstallmentForSummary.toCashFlowEvent(): CashFlowEvent {
    val unsigned = Money(installment.amount, currency)
    val signedAmount = if (type == TransactionType.EXPENSE) -unsigned else unsigned
    val label = "Cuota ${installment.installmentNumber}/${installment.totalInstallments}"
    return CashFlowEvent(date = installment.dueDate, label = label, amount = signedAmount, certainty = Certainty.COMMITTED)
}
