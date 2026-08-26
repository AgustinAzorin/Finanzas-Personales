package com.agustinazorin.finanzas.engine.creditcard

import java.time.LocalDate

/** Un ciclo de facturación de tarjeta: lo que se compra en `(periodStart, closingDate]` se factura con vencimiento [dueDate]. */
data class CreditCardCycle(
    val periodStart: LocalDate,
    val closingDate: LocalDate,
    val dueDate: LocalDate,
)

/**
 * Calcula ciclos de facturación de tarjeta a partir del día de cierre y de vencimiento
 * (CLAUDE.md, sección 17). [closingDay]/[dueDay] son días calendario (1..31), recortados al
 * último día del mes cuando corresponda (ej: cierre día 31 en febrero cae el 28 o 29).
 *
 * El vencimiento de pago se busca en el primer día [dueDay] que no sea anterior al cierre: si
 * cae antes dentro del mismo mes del cierre, se interpreta en el mes siguiente (patrón habitual
 * de tarjetas argentinas: cierra a fin de mes, vence a principios del próximo).
 */
object CreditCardCycleCalculator {

    /** El ciclo cuyo cierre es el primer [closingDay] que no es anterior a [date] (una compra el día del cierre queda en ese ciclo). */
    fun cycleContaining(date: LocalDate, closingDay: Int, dueDay: Int): CreditCardCycle {
        val closingThisMonth = clampToMonthLength(date.year, date.monthValue, closingDay)
        val closingDate = if (!closingThisMonth.isBefore(date)) {
            closingThisMonth
        } else {
            val next = date.plusMonths(1)
            clampToMonthLength(next.year, next.monthValue, closingDay)
        }
        val previousClosing = clampToMonthLength(
            closingDate.minusMonths(1).year,
            closingDate.minusMonths(1).monthValue,
            closingDay,
        )
        return CreditCardCycle(
            periodStart = previousClosing.plusDays(1),
            closingDate = closingDate,
            dueDate = dueDateFor(closingDate, dueDay),
        )
    }

    /** El ciclo inmediatamente posterior a [cycle]. */
    fun nextCycle(cycle: CreditCardCycle, closingDay: Int, dueDay: Int): CreditCardCycle =
        cycleContaining(cycle.closingDate.plusDays(1), closingDay, dueDay)

    private fun dueDateFor(closingDate: LocalDate, dueDay: Int): LocalDate {
        val sameMonth = clampToMonthLength(closingDate.year, closingDate.monthValue, dueDay)
        return if (!sameMonth.isBefore(closingDate)) {
            sameMonth
        } else {
            val next = closingDate.plusMonths(1)
            clampToMonthLength(next.year, next.monthValue, dueDay)
        }
    }

    private fun clampToMonthLength(year: Int, month: Int, day: Int): LocalDate {
        val length = LocalDate.of(year, month, 1).lengthOfMonth()
        return LocalDate.of(year, month, day.coerceIn(1, length))
    }
}
