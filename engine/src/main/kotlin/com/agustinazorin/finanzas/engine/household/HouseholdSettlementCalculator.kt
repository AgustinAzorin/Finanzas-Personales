package com.agustinazorin.finanzas.engine.household

import com.agustinazorin.finanzas.engine.model.EngineTransaction
import com.agustinazorin.finanzas.engine.model.EngineTransactionShare
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.engine.money.Money

/**
 * Cuánto le debe un miembro del hogar a otro por gastos compartidos que ese otro pagó
 * (CLAUDE.md, sección 30). Un solo sentido por par de miembros y moneda: ya neteado (si A le
 * debe a B $100 y B le debe a A $30 por otro gasto, el resultado es un único "A le debe a B $70").
 */
data class HouseholdDebt(
    val currency: String,
    val owedByMemberId: Long,
    val owedToMemberId: Long,
    val amount: Money,
)

object HouseholdSettlementCalculator {

    /**
     * @param sharesByTransactionId beneficiarios de cada transacción (ver [EngineTransactionShare]),
     * indexados por [EngineTransaction.id]. Sólo se consideran gastos confirmados con
     * [EngineTransaction.ownerMemberId] (el "Responsable" que pagó) y al menos un beneficiario.
     */
    fun calculate(
        transactions: List<EngineTransaction>,
        sharesByTransactionId: Map<Long, List<EngineTransactionShare>>,
    ): List<HouseholdDebt> {
        // amountOwed[currency][deudor a acreedor] = cuánto le debe el deudor al acreedor.
        val amountOwed = mutableMapOf<String, MutableMap<Pair<Long, Long>, Long>>()

        transactions.forEach { transaction ->
            val payer = transaction.ownerMemberId ?: return@forEach
            if (transaction.type != TransactionType.EXPENSE || !transaction.countsTowardsBalance) return@forEach
            val shares = sharesByTransactionId[transaction.id].orEmpty()

            shares.forEach { share ->
                require(share.shareAmount.currency == transaction.amount.currency) {
                    "El share de un beneficiario no puede estar en una moneda distinta a la de la transacción."
                }
                if (share.memberId == payer) return@forEach
                val perCurrency = amountOwed.getOrPut(transaction.amount.currency) { mutableMapOf() }
                val key = share.memberId to payer
                perCurrency[key] = (perCurrency[key] ?: 0L) + share.shareAmount.minorUnits
            }
        }

        val debts = mutableListOf<HouseholdDebt>()
        amountOwed.forEach { (currency, owedByPair) ->
            val resolvedPairs = mutableSetOf<Pair<Long, Long>>()
            owedByPair.keys.forEach { pair ->
                if (pair in resolvedPairs) return@forEach
                val reversePair = pair.second to pair.first
                resolvedPairs += pair
                resolvedPairs += reversePair

                val net = (owedByPair[pair] ?: 0L) - (owedByPair[reversePair] ?: 0L)
                when {
                    net > 0 -> debts += HouseholdDebt(currency, pair.first, pair.second, Money(net, currency))
                    net < 0 -> debts += HouseholdDebt(currency, pair.second, pair.first, Money(-net, currency))
                }
            }
        }
        return debts
    }
}
