package com.agustinazorin.finanzas.engine.metrics

import com.agustinazorin.finanzas.engine.model.EngineInstallment
import com.agustinazorin.finanzas.engine.model.EngineTransaction
import com.agustinazorin.finanzas.engine.model.InstallmentStatus
import com.agustinazorin.finanzas.engine.model.PeriodSummary
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.engine.money.Money
import java.time.LocalDate

/**
 * Gasto e ingreso real de un período. Excluye TRANSFER y ADJUSTMENT (Regla 1, CLAUDE.md
 * sección 7) e IGNORED/DUPLICATE (no representan dinero real).
 *
 * Una compra en cuotas (`hasInstallments = true`) nunca se cuenta por su propia fecha: su gasto
 * se reparte entre [installments], cada una imputada al período de su propio `accountingDate`
 * (Regla 3, CLAUDE.md sección 7). Así, comprar $120.000 en 12 cuotas nunca aparece como
 * "gastaste $120.000 este mes"; aparece como $10.000 en cada uno de los 12 meses.
 *
 * Devuelve un resumen por moneda: nunca mezcla montos nominales de monedas distintas.
 */
object PeriodSummaryCalculator {

    fun summarize(
        transactions: List<EngineTransaction>,
        start: LocalDate,
        end: LocalDate,
        installments: List<EngineInstallment> = emptyList(),
    ): Map<String, PeriodSummary> {
        require(!end.isBefore(start)) { "end no puede ser anterior a start." }

        val plainFlows = transactions.filter {
            it.countsTowardsBalance && it.isEconomicFlow && !it.hasInstallments &&
                !it.date.isBefore(start) && !it.date.isAfter(end)
        }
        val installmentFlows = installments.filter {
            it.status != InstallmentStatus.CANCELLED &&
                !it.accountingDate.isBefore(start) && !it.accountingDate.isAfter(end)
        }

        val currencies = plainFlows.map { it.amount.currency }.toSet() + installmentFlows.map { it.amount.currency }.toSet()

        return currencies.associateWith { currency ->
            val expense = Money.sum(
                plainFlows.filter { it.type == TransactionType.EXPENSE && it.amount.currency == currency }.map { it.amount } +
                    installmentFlows.filter { it.type == TransactionType.EXPENSE && it.amount.currency == currency }.map { it.amount },
                currency,
            )
            val income = Money.sum(
                plainFlows.filter { it.type == TransactionType.INCOME && it.amount.currency == currency }.map { it.amount } +
                    installmentFlows.filter { it.type == TransactionType.INCOME && it.amount.currency == currency }.map { it.amount },
                currency,
            )
            PeriodSummary(start = start, end = end, currency = currency, totalExpense = expense, totalIncome = income)
        }
    }
}
