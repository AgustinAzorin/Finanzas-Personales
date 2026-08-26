package com.agustinazorin.finanzas.engine.inflation

import com.agustinazorin.finanzas.engine.money.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/** Variación porcentual mensual del índice de inflación (ej: 4.2 significa +4.2% en ese mes). */
data class MonthlyInflationRate(val month: YearMonth, val percent: BigDecimal)

/**
 * Ajusta un monto nominal histórico a su equivalente en un momento posterior, componiendo las
 * variaciones mensuales de inflación conocidas (CLAUDE.md, sección 42).
 *
 * Esto es EXCLUSIVAMENTE para análisis: nunca debe usarse para sobrescribir el monto nominal
 * original de una transacción. El monto original y el ajustado deben mostrarse siempre por
 * separado (ej: "gasto original $100.000, valor equivalente $145.000").
 */
object InflationAdjuster {

    /**
     * @param amount monto nominal en el momento [from]. Nunca se modifica: se devuelve un nuevo
     *   [Money] equivalente en el momento [to].
     * @param rates variaciones mensuales conocidas. Deben cubrir cada mes estrictamente posterior
     *   a [from] hasta [to] inclusive; si falta algún mes, no se puede ajustar de forma confiable
     *   y se rechaza el cálculo en vez de aproximar con datos incompletos.
     */
    fun adjust(amount: Money, from: LocalDate, to: LocalDate, rates: List<MonthlyInflationRate>): Money {
        require(!to.isBefore(from)) { "No se puede ajustar hacia atrás en el tiempo (from=$from, to=$to)." }
        val fromMonth = YearMonth.from(from)
        val toMonth = YearMonth.from(to)
        if (fromMonth == toMonth) return amount

        val rateByMonth = rates.associateBy { it.month }
        var factor = BigDecimal.ONE
        var month = fromMonth.plusMonths(1)
        while (!month.isAfter(toMonth)) {
            val monthlyRate = rateByMonth[month]
                ?: throw IllegalArgumentException("Falta la tasa de inflación de $month para ajustar el monto.")
            factor = factor.multiply(BigDecimal.ONE.add(monthlyRate.percent.movePointLeft(2)))
            month = month.plusMonths(1)
        }
        return amount.convert(factor, amount.currency)
    }
}
