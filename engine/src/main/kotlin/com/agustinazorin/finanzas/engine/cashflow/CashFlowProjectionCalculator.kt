package com.agustinazorin.finanzas.engine.cashflow

import com.agustinazorin.finanzas.engine.model.CashFlowEvent
import com.agustinazorin.finanzas.engine.model.CashFlowPoint
import com.agustinazorin.finanzas.engine.model.Certainty
import com.agustinazorin.finanzas.engine.money.Money
import java.time.LocalDate

/**
 * Proyecta el flujo de caja (CLAUDE.md, secciones 5, 23, 25, 26 y 36): a partir de un saldo
 * líquido actual y una lista de eventos futuros (recurrentes, cuotas, etc.), arma la línea de
 * tiempo de saldo proyectado y responde "¿cuánto voy a tener disponible en N días?" y "¿cuánto
 * tengo comprometido en N días?".
 *
 * Nunca mezcla certeza: cada [CashFlowPoint] conserva el [Certainty] de su evento de origen, así
 * la UI puede distinguir dinero real de una estimación (CLAUDE.md, sección 36).
 */
object CashFlowProjectionCalculator {

    /**
     * Línea de tiempo completa: el primer punto es siempre el saldo actual ([Certainty.ACTUAL],
     * sin evento asociado), seguido de un punto por cada [events] en orden cronológico con el
     * saldo acumulado hasta ese momento.
     */
    fun project(asOf: LocalDate, startingBalance: Money, events: List<CashFlowEvent>): List<CashFlowPoint> {
        require(events.all { it.amount.currency == startingBalance.currency }) {
            "Todos los eventos deben estar en la misma moneda que el saldo inicial (${startingBalance.currency})."
        }
        var running = startingBalance
        val points = mutableListOf(
            CashFlowPoint(date = asOf, label = "Saldo actual", delta = null, balance = running, certainty = Certainty.ACTUAL),
        )
        events.sortedBy { it.date }.forEach { event ->
            running += event.amount
            points += CashFlowPoint(date = event.date, label = event.label, delta = event.amount, balance = running, certainty = event.certainty)
        }
        return points
    }

    /**
     * Total comprometido (sólo salidas de dinero, CLAUDE.md sección 25) dentro de
     * [from, from + horizonDays], en [currency]. Ignora eventos de otras monedas en vez de
     * fallar: la ventana "Comprometido" siempre pide un total por moneda a la vez.
     */
    fun totalCommitted(currency: String, from: LocalDate, horizonDays: Long, events: List<CashFlowEvent>): Money {
        require(horizonDays >= 0) { "horizonDays no puede ser negativo." }
        val limit = from.plusDays(horizonDays)
        val outflows = events.filter {
            it.amount.currency == currency && it.amount.isNegative && !it.date.isBefore(from) && !it.date.isAfter(limit)
        }
        return -Money.sum(outflows.map { it.amount }, currency)
    }

    /**
     * Primera fecha, dentro de [points] ya proyectados, en la que el saldo cae por debajo de
     * [threshold]. Null si nunca ocurre. Toda alerta de liquidez debe originarse acá, nunca de
     * un umbral arbitrario sin datos detrás (CLAUDE.md, sección 23).
     */
    fun firstDateBelow(points: List<CashFlowPoint>, threshold: Money): LocalDate? =
        points.firstOrNull { it.balance.currency == threshold.currency && it.balance < threshold }?.date
}
