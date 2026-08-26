package com.agustinazorin.finanzas.engine.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Monto monetario expresado en unidades mínimas de la moneda (ej: centavos).
 * Nunca usar Float/Double para dinero (ver CLAUDE.md, sección 6).
 *
 * Dos [Money] de distinta moneda nunca se suman ni comparan implícitamente:
 * mezclar monedas silenciosamente rompería la Regla de multi-moneda (CLAUDE.md, sección 41).
 */
data class Money(val minorUnits: Long, val currency: String) : Comparable<Money> {

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(minorUnits + other.minorUnits, currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(minorUnits - other.minorUnits, currency)
    }

    operator fun unaryMinus(): Money = Money(-minorUnits, currency)

    val isNegative: Boolean get() = minorUnits < 0
    val isPositive: Boolean get() = minorUnits > 0

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minorUnits.compareTo(other.minorUnits)
    }

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "No se pueden combinar montos de distinta moneda ($currency vs ${other.currency}) " +
                "sin pasar por una conversión explícita."
        }
    }

    /**
     * Convierte este monto a [toCurrency] usando [rate] (unidades de [toCurrency] por 1 unidad
     * de [currency]). Es la única forma de pasar dinero de una moneda a otra: nunca implícita,
     * siempre con una tasa explícita (CLAUDE.md, sección 41 — "las conversiones son una capa de
     * presentación/análisis", nunca sobrescriben el monto nominal original guardado aparte).
     */
    fun convert(rate: BigDecimal, toCurrency: String): Money {
        require(rate.signum() > 0) { "La tasa de conversión debe ser positiva ($rate)." }
        val convertedMinorUnits = BigDecimal(minorUnits).multiply(rate)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
        return Money(convertedMinorUnits, toCurrency)
    }

    companion object {
        fun zero(currency: String): Money = Money(0, currency)

        fun sum(amounts: Collection<Money>, currency: String): Money =
            amounts.fold(zero(currency)) { acc, money -> acc + money }
    }
}

/** Agrupa y suma una lista de [Money] por moneda, sin mezclar montos entre monedas distintas. */
fun Iterable<Money>.sumByCurrency(): Map<String, Money> =
    groupBy { it.currency }.mapValues { (currency, amounts) -> Money.sum(amounts, currency) }
