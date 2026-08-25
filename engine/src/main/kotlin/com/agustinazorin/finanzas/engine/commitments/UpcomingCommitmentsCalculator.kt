package com.agustinazorin.finanzas.engine.commitments

import com.agustinazorin.finanzas.engine.model.Certainty
import com.agustinazorin.finanzas.engine.model.EngineRecurringTransaction
import com.agustinazorin.finanzas.engine.model.Periodicity
import com.agustinazorin.finanzas.engine.model.UpcomingCommitment
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Proyecta la(s) próxima(s) ocurrencia(s) de cada [EngineRecurringTransaction] activo dentro
 * de una ventana [from, from + days].
 *
 * Nunca asume que el movimiento ya ocurrió (CLAUDE.md, sección 15): esto sólo genera eventos
 * futuros con certeza [Certainty.COMMITTED], no transacciones reales. Confirmar que
 * efectivamente ocurrieron es responsabilidad del usuario o de la captura automática (Fase 1).
 */
object UpcomingCommitmentsCalculator {

    fun upcoming(
        recurring: List<EngineRecurringTransaction>,
        from: LocalDate,
        days: Long,
    ): List<UpcomingCommitment> {
        require(days >= 0) { "days no puede ser negativo." }
        val to = from.plusDays(days)

        return recurring
            .filter { it.isActive }
            .flatMap { r -> occurrencesInWindow(r, from, to).map { date -> r.toCommitment(date) } }
            .sortedBy { it.dueDate }
    }

    private fun occurrencesInWindow(r: EngineRecurringTransaction, from: LocalDate, to: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var candidate = firstOccurrenceOnOrAfter(r, from)
        while (!candidate.isAfter(to)) {
            dates += candidate
            candidate = nextOccurrenceAfter(r, candidate)
        }
        return dates
    }

    private fun firstOccurrenceOnOrAfter(r: EngineRecurringTransaction, date: LocalDate): LocalDate = when (r.periodicity) {
        Periodicity.MONTHLY -> monthlyOccurrenceOnOrAfter(date, r.dueDay)
        Periodicity.ANNUAL -> annualOccurrenceOnOrAfter(date, r.dueDay)
        Periodicity.WEEKLY, Periodicity.BIWEEKLY -> weeklyOccurrenceOnOrAfter(date, r.dueDay)
    }

    private fun nextOccurrenceAfter(r: EngineRecurringTransaction, previous: LocalDate): LocalDate = when (r.periodicity) {
        Periodicity.MONTHLY -> monthlyOccurrenceOnOrAfter(previous.plusDays(1), r.dueDay)
        Periodicity.ANNUAL -> annualOccurrenceOnOrAfter(previous.plusDays(1), r.dueDay)
        Periodicity.WEEKLY -> previous.plusWeeks(1)
        Periodicity.BIWEEKLY -> previous.plusWeeks(2)
    }

    /** [dayOfMonth] se interpreta como día calendario (1..31), recortado al último día del mes si excede su longitud. */
    private fun monthlyOccurrenceOnOrAfter(date: LocalDate, dayOfMonth: Int): LocalDate {
        val thisMonth = clampToMonthLength(date.year, date.monthValue, dayOfMonth)
        return if (!thisMonth.isBefore(date)) {
            thisMonth
        } else {
            val next = date.plusMonths(1)
            clampToMonthLength(next.year, next.monthValue, dayOfMonth)
        }
    }

    private fun annualOccurrenceOnOrAfter(date: LocalDate, dayOfYear: Int): LocalDate {
        // dayOfYear se interpreta como día-del-año (1..366); en años no bisiestos se recorta a 365.
        val clampedThisYear = clampToYearLength(date.year, dayOfYear)
        return if (!clampedThisYear.isBefore(date)) clampedThisYear else clampToYearLength(date.year + 1, dayOfYear)
    }

    /** [isoDayOfWeek] 1 = lunes .. 7 = domingo (ISO-8601). */
    private fun weeklyOccurrenceOnOrAfter(date: LocalDate, isoDayOfWeek: Int): LocalDate {
        val target = DayOfWeek.of(isoDayOfWeek.coerceIn(1, 7))
        return if (date.dayOfWeek == target) date else date.with(TemporalAdjusters.nextOrSame(target))
    }

    private fun clampToMonthLength(year: Int, month: Int, day: Int): LocalDate {
        val length = LocalDate.of(year, month, 1).lengthOfMonth()
        return LocalDate.of(year, month, day.coerceIn(1, length))
    }

    private fun clampToYearLength(year: Int, dayOfYear: Int): LocalDate {
        val length = LocalDate.of(year, 1, 1).lengthOfYear()
        return LocalDate.of(year, 1, 1).plusDays((dayOfYear.coerceIn(1, length) - 1).toLong())
    }

    private fun EngineRecurringTransaction.toCommitment(date: LocalDate) = UpcomingCommitment(
        recurringTransactionId = id,
        name = name,
        type = type,
        amount = estimatedAmount,
        dueDate = date,
        categoryId = categoryId,
        certainty = Certainty.COMMITTED,
    )
}
