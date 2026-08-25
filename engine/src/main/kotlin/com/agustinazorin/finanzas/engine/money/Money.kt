package com.agustinazorin.finanzas.engine.money

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

    companion object {
        fun zero(currency: String): Money = Money(0, currency)

        fun sum(amounts: Collection<Money>, currency: String): Money =
            amounts.fold(zero(currency)) { acc, money -> acc + money }
    }
}

/** Agrupa y suma una lista de [Money] por moneda, sin mezclar montos entre monedas distintas. */
fun Iterable<Money>.sumByCurrency(): Map<String, Money> =
    groupBy { it.currency }.mapValues { (currency, amounts) -> Money.sum(amounts, currency) }
