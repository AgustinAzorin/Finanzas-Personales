package com.agustinazorin.finanzas.engine.split

import com.agustinazorin.finanzas.engine.money.Money

/**
 * Reparte un gasto compartido entre sus "Beneficiarios" (CLAUDE.md, sección 30). El resultado se
 * persiste tal cual (ver [com.agustinazorin.finanzas.engine.model.EngineTransactionShare]): nunca
 * se recalculan porcentajes en cada lectura.
 */
object ExpenseSplitCalculator {

    /**
     * Reparte [total] en partes iguales entre [memberIds]. El monto en unidades mínimas no
     * siempre es divisible exactamente por la cantidad de miembros; el resto se reparte de a 1
     * unidad mínima empezando por los primeros miembros de la lista, así la suma de las partes
     * es siempre exactamente igual a [total] (nunca se pierden ni se inventan centavos).
     */
    fun splitEqually(total: Money, memberIds: List<Long>): Map<Long, Money> {
        require(memberIds.isNotEmpty()) { "Un gasto compartido necesita al menos un beneficiario." }
        require(memberIds.toSet().size == memberIds.size) { "No se puede repetir un beneficiario." }
        require(total.minorUnits > 0) { "El monto a repartir debe ser mayor a cero." }

        val count = memberIds.size
        val base = total.minorUnits / count
        val remainder = total.minorUnits % count
        return memberIds.mapIndexed { index, memberId ->
            val share = base + if (index < remainder) 1 else 0
            memberId to Money(share, total.currency)
        }.toMap()
    }
}
