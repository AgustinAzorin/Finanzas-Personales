package com.agustinazorin.finanzas.engine.networth

import com.agustinazorin.finanzas.engine.balance.BalanceCalculator
import com.agustinazorin.finanzas.engine.model.EngineAccount
import com.agustinazorin.finanzas.engine.model.EngineTransaction
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.engine.money.sumByCurrency
import java.time.LocalDate

/**
 * Patrimonio neto = Activos - Pasivos (CLAUDE.md, Regla 4).
 *
 * En Fase 0 el patrimonio se deriva únicamente de [EngineAccount] (todavía no existen
 * Asset/Liability independientes, eso es Fase 5): el saldo de cada cuenta ya incorpora si es
 * activo o pasivo gracias a la convención de signo de [BalanceCalculator]. Una transferencia
 * entre cuentas propias no cambia el patrimonio porque el OUTFLOW de una cuenta compensa
 * exactamente el INFLOW de la otra.
 *
 * No convierte entre monedas: si hay cuentas en más de una moneda, el resultado queda
 * separado por moneda (ver [sumByCurrency]) en lugar de mezclar montos nominales distintos.
 */
object NetWorthCalculator {

    fun netWorthByCurrency(
        accounts: List<EngineAccount>,
        transactions: List<EngineTransaction>,
        asOf: LocalDate = LocalDate.now(),
    ): Map<String, Money> {
        val balances = BalanceCalculator.allAccountBalances(
            accounts = accounts.filter { it.isActive },
            transactions = transactions,
            asOf = asOf,
        )
        return balances.map { it.balance }.sumByCurrency()
    }
}
