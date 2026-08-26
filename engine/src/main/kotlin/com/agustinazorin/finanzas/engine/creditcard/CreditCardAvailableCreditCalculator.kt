package com.agustinazorin.finanzas.engine.creditcard

import com.agustinazorin.finanzas.engine.money.Money

/**
 * Crédito disponible de una tarjeta (CLAUDE.md, sección 17): el límite menos toda la deuda
 * vigente (resúmenes sin pagar por completo, incluyendo cuotas futuras ya generadas). Nunca
 * negativo: un límite superado se muestra como cero disponible, no como disponible negativo.
 */
object CreditCardAvailableCreditCalculator {

    fun compute(creditLimit: Money, outstandingBalance: Money): Money {
        require(creditLimit.currency == outstandingBalance.currency) {
            "El límite y la deuda vigente deben estar en la misma moneda."
        }
        val available = creditLimit - outstandingBalance
        return if (available.isNegative) Money.zero(creditLimit.currency) else available
    }
}
