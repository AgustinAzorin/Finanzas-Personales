package com.agustinazorin.finanzas.engine.installments

import com.agustinazorin.finanzas.engine.creditcard.CreditCardCycleCalculator
import com.agustinazorin.finanzas.engine.money.Money
import java.time.LocalDate

/** Una cuota planificada, todavía sin persistir. */
data class PlannedInstallment(
    val installmentNumber: Int,
    val totalInstallments: Int,
    val amount: Money,
    val accountingDate: LocalDate,
    val dueDate: LocalDate,
)

/**
 * Genera el plan de cuotas de una compra con tarjeta (Regla 3, CLAUDE.md sección 7): 1 compra
 * se traduce en N cuotas, cada una imputada al ciclo de facturación de la tarjeta que le
 * corresponde (la primera, al ciclo vigente al momento de la compra; las siguientes, una por
 * cada ciclo posterior). Nunca pierde ni gana un centavo: la suma de las cuotas siempre es
 * exactamente el monto total de la compra — el resto de la división entera, si lo hay, se ajusta
 * en la última cuota.
 */
object InstallmentPlanner {

    fun plan(
        purchaseDate: LocalDate,
        totalAmount: Money,
        totalInstallments: Int,
        closingDay: Int,
        dueDay: Int,
    ): List<PlannedInstallment> {
        require(totalInstallments >= 1) { "Una compra debe tener al menos 1 cuota." }
        require(totalAmount.minorUnits > 0) { "El monto de la compra debe ser mayor a cero." }

        val baseAmount = totalAmount.minorUnits / totalInstallments
        val remainder = totalAmount.minorUnits % totalInstallments

        var cycle = CreditCardCycleCalculator.cycleContaining(purchaseDate, closingDay, dueDay)
        val plans = mutableListOf<PlannedInstallment>()
        for (number in 1..totalInstallments) {
            val isLast = number == totalInstallments
            val amount = baseAmount + if (isLast) remainder else 0
            plans += PlannedInstallment(
                installmentNumber = number,
                totalInstallments = totalInstallments,
                amount = Money(amount, totalAmount.currency),
                accountingDate = cycle.closingDate,
                dueDate = cycle.dueDate,
            )
            if (!isLast) cycle = CreditCardCycleCalculator.nextCycle(cycle, closingDay, dueDay)
        }
        return plans
    }
}
