package com.agustinazorin.finanzas.engine.household

import com.agustinazorin.finanzas.engine.model.EngineTransaction
import com.agustinazorin.finanzas.engine.model.EngineTransactionShare
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.engine.money.Money
import java.time.LocalDate

/** Clave interna de agregación: a quién y en qué moneda se le atribuye un monto. */
private data class AttributionKey(val memberId: Long, val currency: String)

/** Gasto/ingreso económico de un período atribuido a un miembro del hogar (CLAUDE.md, sección 30). */
data class MemberAttribution(
    val memberId: Long,
    val currency: String,
    val expense: Money,
    val income: Money,
)

/**
 * Reparte el gasto y el ingreso económico de un período entre los miembros del hogar, para
 * distinguir "reportes personales" de "reportes del hogar" (roadmap Fase 3).
 *
 * - Un gasto con beneficiarios reparte su monto entre ellos: cada uno "consume" su parte, más
 *   allá de quién haya pagado la cuenta realmente (gasto = consumo económico, CLAUDE.md sección 4).
 * - Un gasto o ingreso sin beneficiarios se atribuye completo a [EngineTransaction.ownerMemberId].
 * - Si tampoco tiene dueño, se atribuye a [UNASSIGNED]: gasto/ingreso del hogar, no de una persona.
 *
 * Excluye transacciones con cuotas (`hasInstallments`): repartir una compra en cuotas entre
 * miembros del hogar está fuera de alcance de esta fase.
 */
object MemberAttributionCalculator {

    /** Seudo-id usado cuando una transacción no tiene [EngineTransaction.ownerMemberId] ni beneficiarios. */
    const val UNASSIGNED: Long = -1L

    fun summarize(
        transactions: List<EngineTransaction>,
        sharesByTransactionId: Map<Long, List<EngineTransactionShare>>,
        start: LocalDate,
        end: LocalDate,
    ): List<MemberAttribution> {
        require(!end.isBefore(start)) { "end no puede ser anterior a start." }

        val inPeriod = transactions.filter {
            it.countsTowardsBalance && it.isEconomicFlow && !it.hasInstallments &&
                !it.date.isBefore(start) && !it.date.isAfter(end)
        }

        val expenseTotals = mutableMapOf<AttributionKey, Long>()
        val incomeTotals = mutableMapOf<AttributionKey, Long>()

        inPeriod.forEach { transaction ->
            val shares = sharesByTransactionId[transaction.id]
            if (transaction.type == TransactionType.EXPENSE && !shares.isNullOrEmpty()) {
                shares.forEach { share ->
                    val key = AttributionKey(share.memberId, share.shareAmount.currency)
                    expenseTotals[key] = (expenseTotals[key] ?: 0L) + share.shareAmount.minorUnits
                }
            } else {
                val key = AttributionKey(transaction.ownerMemberId ?: UNASSIGNED, transaction.amount.currency)
                val totals = if (transaction.type == TransactionType.EXPENSE) expenseTotals else incomeTotals
                totals[key] = (totals[key] ?: 0L) + transaction.amount.minorUnits
            }
        }

        return (expenseTotals.keys + incomeTotals.keys).map { key ->
            MemberAttribution(
                memberId = key.memberId,
                currency = key.currency,
                expense = Money(expenseTotals[key] ?: 0L, key.currency),
                income = Money(incomeTotals[key] ?: 0L, key.currency),
            )
        }
    }
}
