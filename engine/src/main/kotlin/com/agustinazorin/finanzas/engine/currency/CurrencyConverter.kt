package com.agustinazorin.finanzas.engine.currency

import com.agustinazorin.finanzas.engine.money.Money
import java.math.BigDecimal

/**
 * Resultado de convertir un total multi-moneda (ej: el `Map<String, Money>` que devuelve
 * [com.agustinazorin.finanzas.engine.networth.NetWorthCalculator.netWorthByCurrency]) a una
 * única moneda base.
 *
 * [total] es la suma en [Money.currency] == moneda base de todas las monedas que sí tenían tasa
 * conocida. [missingRates] lista las monedas que no se pudieron convertir por falta de tasa: la
 * UI debe mostrarlas aparte en vez de fingir que ya están incluidas en [total] (CLAUDE.md,
 * sección 34 — "no inventar información").
 */
data class CurrencyConversionResult(
    val total: Money,
    val missingRates: Set<String>,
)

/**
 * Convierte montos en distintas monedas a una única moneda base para reportes (CLAUDE.md,
 * sección 41). Nunca modifica los montos nominales originales: opera sobre una copia (el
 * `Map<String, Money>` de entrada), la conversión es exclusivamente para presentación/análisis.
 */
object CurrencyConverter {

    /**
     * @param amountsByCurrency montos ya agrupados por moneda (nunca mezclados entre sí).
     * @param baseCurrency moneda a la que se convierte todo.
     * @param ratesToBase mapa moneda -> unidades de [baseCurrency] por 1 unidad de esa moneda.
     *   No necesita incluir una entrada para [baseCurrency] misma.
     */
    fun toBaseCurrency(
        amountsByCurrency: Map<String, Money>,
        baseCurrency: String,
        ratesToBase: Map<String, BigDecimal>,
    ): CurrencyConversionResult {
        var total = Money.zero(baseCurrency)
        val missingRates = mutableSetOf<String>()
        for ((currency, amount) in amountsByCurrency) {
            when {
                currency == baseCurrency -> total += amount
                ratesToBase.containsKey(currency) -> total += amount.convert(ratesToBase.getValue(currency), baseCurrency)
                else -> missingRates += currency
            }
        }
        return CurrencyConversionResult(total, missingRates)
    }
}
