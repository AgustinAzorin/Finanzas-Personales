package com.agustinazorin.finanzas.engine.metrics

import com.agustinazorin.finanzas.engine.model.EngineTransaction
import com.agustinazorin.finanzas.engine.model.PeriodSummary
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.engine.money.Money
import java.time.LocalDate

/**
 * Gasto e ingreso real de un período. Excluye TRANSFER y ADJUSTMENT (Regla 1, CLAUDE.md
 * sección 7) e IGNORED/DUPLICATE (no representan dinero real).
 *
 * Devuelve un resumen por moneda: nunca mezcla montos nominales de monedas distintas.
 */
object PeriodSummaryCalculator {

    fun summarize(
        transactions: List<EngineTransaction>,
        start: LocalDate,
        end: LocalDate,
    ): Map<String, PeriodSummary> {
        require(!end.isBefore(start)) { "end no puede ser anterior a start." }

        val inPeriod = transactions.filter {
            it.countsTowardsBalance && it.isEconomicFlow &&
                !it.date.isBefore(start) && !it.date.isAfter(end)
        }

        return inPeriod.groupBy { it.amount.currency }.mapValues { (currency, txns) ->
            val expense = Money.sum(
                txns.filter { it.type == TransactionType.EXPENSE }.map { it.amount },
                currency,
            )
            val income = Money.sum(
                txns.filter { it.type == TransactionType.INCOME }.map { it.amount },
                currency,
            )
            PeriodSummary(start = start, end = end, currency = currency, totalExpense = expense, totalIncome = income)
        }
    }
}
