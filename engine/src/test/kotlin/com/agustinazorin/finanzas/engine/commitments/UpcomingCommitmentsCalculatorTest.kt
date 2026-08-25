package com.agustinazorin.finanzas.engine.commitments

import com.agustinazorin.finanzas.engine.model.Certainty
import com.agustinazorin.finanzas.engine.model.EngineRecurringTransaction
import com.agustinazorin.finanzas.engine.model.Periodicity
import com.agustinazorin.finanzas.engine.model.RecurringType
import com.agustinazorin.finanzas.engine.money.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class UpcomingCommitmentsCalculatorTest {

    private fun recurring(
        id: Long = 1,
        name: String = "Alquiler",
        amount: Long = 450_000,
        periodicity: Periodicity = Periodicity.MONTHLY,
        dueDay: Int = 10,
        isActive: Boolean = true,
    ) = EngineRecurringTransaction(
        id = id,
        type = RecurringType.EXPENSE,
        name = name,
        estimatedAmount = Money(amount, "ARS"),
        periodicity = periodicity,
        dueDay = dueDay,
        isActive = isActive,
    )

    @Test
    fun `proyecta la proxima ocurrencia mensual dentro de la ventana`() {
        val result = UpcomingCommitmentsCalculator.upcoming(
            recurring = listOf(recurring(dueDay = 10)),
            from = LocalDate.of(2026, 8, 25),
            days = 30,
        )
        assertEquals(listOf(LocalDate.of(2026, 9, 10)), result.map { it.dueDate })
        assertEquals(Certainty.COMMITTED, result.single().certainty)
    }

    @Test
    fun `si el dia de vencimiento ya paso este mes, usa el mes siguiente`() {
        val result = UpcomingCommitmentsCalculator.upcoming(
            recurring = listOf(recurring(dueDay = 5)),
            from = LocalDate.of(2026, 8, 25),
            days = 30,
        )
        assertEquals(listOf(LocalDate.of(2026, 9, 5)), result.map { it.dueDate })
    }

    @Test
    fun `recorta el dia de vencimiento a la duracion real del mes`() {
        val result = UpcomingCommitmentsCalculator.upcoming(
            recurring = listOf(recurring(dueDay = 31)),
            from = LocalDate.of(2026, 1, 25),
            days = 10,
        )
        // Enero tiene 31 días, cae dentro de la ventana.
        assertEquals(listOf(LocalDate.of(2026, 1, 31)), result.map { it.dueDate })
    }

    @Test
    fun `una ventana de 90 dias puede capturar mas de una ocurrencia mensual`() {
        val result = UpcomingCommitmentsCalculator.upcoming(
            recurring = listOf(recurring(dueDay = 10)),
            from = LocalDate.of(2026, 8, 1),
            days = 90,
        )
        assertEquals(
            listOf(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 9, 10), LocalDate.of(2026, 10, 10)),
            result.map { it.dueDate },
        )
    }

    @Test
    fun `ignora recurrentes inactivos`() {
        val result = UpcomingCommitmentsCalculator.upcoming(
            recurring = listOf(recurring(isActive = false)),
            from = LocalDate.of(2026, 8, 25),
            days = 30,
        )
        assertEquals(emptyList<LocalDate>(), result.map { it.dueDate })
    }

    @Test
    fun `cuotas cruzando el fin de ano se proyectan correctamente`() {
        val result = UpcomingCommitmentsCalculator.upcoming(
            recurring = listOf(recurring(dueDay = 15)),
            from = LocalDate.of(2026, 12, 20),
            days = 30,
        )
        assertEquals(listOf(LocalDate.of(2027, 1, 15)), result.map { it.dueDate })
    }
}
