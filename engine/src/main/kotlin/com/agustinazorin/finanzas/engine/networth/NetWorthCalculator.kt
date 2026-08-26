package com.agustinazorin.finanzas.engine.networth

import com.agustinazorin.finanzas.engine.balance.BalanceCalculator
import com.agustinazorin.finanzas.engine.model.EngineAccount
import com.agustinazorin.finanzas.engine.model.EngineAsset
import com.agustinazorin.finanzas.engine.model.EngineLiability
import com.agustinazorin.finanzas.engine.model.EngineTransaction
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.engine.money.sumByCurrency
import java.time.LocalDate

/**
 * Patrimonio neto = Activos - Pasivos (CLAUDE.md, Regla 4).
 *
 * El patrimonio se deriva de tres fuentes: el saldo de cada [EngineAccount] (que ya incorpora si
 * es activo o pasivo gracias a la convención de signo de [BalanceCalculator]), más el valor de
 * cada [EngineAsset] independiente, menos el saldo pendiente de cada [EngineLiability]
 * independiente (CLAUDE.md, sección 5: Asset/Liability no atados a una cuenta corriente). Una
 * transferencia entre cuentas propias no cambia el patrimonio porque el OUTFLOW de una cuenta
 * compensa exactamente el INFLOW de la otra.
 *
 * A diferencia de las cuentas, [EngineAsset]/[EngineLiability] no tienen historial de
 * transacciones para reproducir hacia atrás: su valor se usa tal cual está guardado sin importar
 * [asOf]. Por eso una consulta con [asOf] en el pasado sigue mezclando saldos de cuenta
 * históricamente correctos con la última valuación conocida de Asset/Liability — para una foto
 * histórica realmente precisa del patrimonio hay que usar un `FinancialSnapshot` guardado en esa
 * fecha (CLAUDE.md, sección 22), no recalcular hacia atrás.
 *
 * No convierte entre monedas: si hay cuentas/activos/pasivos en más de una moneda, el resultado
 * queda separado por moneda (ver [sumByCurrency]) en lugar de mezclar montos nominales distintos.
 */
object NetWorthCalculator {

    fun netWorthByCurrency(
        accounts: List<EngineAccount>,
        transactions: List<EngineTransaction>,
        asOf: LocalDate = LocalDate.now(),
        assets: List<EngineAsset> = emptyList(),
        liabilities: List<EngineLiability> = emptyList(),
    ): Map<String, Money> {
        val accountBalances = BalanceCalculator.allAccountBalances(
            accounts = accounts.filter { it.isActive },
            transactions = transactions,
            asOf = asOf,
        ).map { it.balance }
        val assetValues = assets.filter { it.isActive }.map { it.currentValue }
        val liabilityValues = liabilities.filter { it.isActive }.map { -it.outstandingAmount }
        return (accountBalances + assetValues + liabilityValues).sumByCurrency()
    }
}
