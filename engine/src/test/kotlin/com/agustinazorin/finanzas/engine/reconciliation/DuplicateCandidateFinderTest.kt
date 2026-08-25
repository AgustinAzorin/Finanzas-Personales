package com.agustinazorin.finanzas.engine.reconciliation

import com.agustinazorin.finanzas.engine.model.MatchConfidence
import com.agustinazorin.finanzas.engine.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DuplicateCandidateFinderTest {

    private val existing = listOf(
        ReconciliationRecord(
            transactionId = 1,
            accountId = 10,
            amount = Money(50000, "ARS"),
            date = LocalDate.of(2026, 8, 20),
            merchantNormalized = "CAFE MARTINEZ",
        ),
        ReconciliationRecord(
            transactionId = 2,
            accountId = 10,
            amount = Money(120000, "ARS"),
            date = LocalDate.of(2026, 8, 20),
            merchantNormalized = "FARMACIA",
        ),
        ReconciliationRecord(
            transactionId = 3,
            accountId = 20,
            amount = Money(50000, "ARS"),
            date = LocalDate.of(2026, 8, 21),
            merchantNormalized = "CAFE MARTINEZ",
        ),
    )

    @Test
    fun `mismo monto, misma fecha y mismo comercio es EXACT`() {
        val candidate = ReconciliationCandidate(
            accountId = 10,
            amount = Money(50000, "ARS"),
            date = LocalDate.of(2026, 8, 20),
            merchantNormalized = "CAFE MARTINEZ",
        )
        val matches = DuplicateCandidateFinder.findCandidates(candidate, existing)
        assertEquals(1, matches.size)
        assertEquals(MatchConfidence.EXACT, matches.first().confidence)
        assertEquals(1L, matches.first().record.transactionId)
    }

    @Test
    fun `mismo monto y fecha pero sin comercio en comun es LIKELY`() {
        val candidate = ReconciliationCandidate(
            accountId = 10,
            amount = Money(50000, "ARS"),
            date = LocalDate.of(2026, 8, 20),
            merchantNormalized = null,
        )
        val matches = DuplicateCandidateFinder.findCandidates(candidate, existing)
        assertEquals(1, matches.size)
        assertEquals(MatchConfidence.LIKELY, matches.first().confidence)
    }

    @Test
    fun `distinto monto nunca es candidato`() {
        val candidate = ReconciliationCandidate(
            accountId = 10,
            amount = Money(1, "ARS"),
            date = LocalDate.of(2026, 8, 20),
            merchantNormalized = "CAFE MARTINEZ",
        )
        assertTrue(DuplicateCandidateFinder.findCandidates(candidate, existing).isEmpty())
    }

    @Test
    fun `respeta la cuenta cuando se especifica`() {
        val candidate = ReconciliationCandidate(
            accountId = 10,
            amount = Money(50000, "ARS"),
            date = LocalDate.of(2026, 8, 21),
            merchantNormalized = "CAFE MARTINEZ",
        )
        // El registro id=3 tiene el mismo monto/comercio pero está en la cuenta 20.
        val matches = DuplicateCandidateFinder.findCandidates(candidate, existing)
        assertTrue(matches.none { it.record.transactionId == 3L })
    }

    @Test
    fun `fuera de la ventana de fechas no es candidato`() {
        val candidate = ReconciliationCandidate(
            accountId = 10,
            amount = Money(50000, "ARS"),
            date = LocalDate.of(2026, 8, 25),
            merchantNormalized = "CAFE MARTINEZ",
        )
        assertTrue(DuplicateCandidateFinder.findCandidates(candidate, existing).isEmpty())
    }

    @Test
    fun `sin cuenta especificada busca en todas`() {
        val candidate = ReconciliationCandidate(
            accountId = null,
            amount = Money(50000, "ARS"),
            date = LocalDate.of(2026, 8, 20),
            merchantNormalized = "CAFE MARTINEZ",
        )
        val matches = DuplicateCandidateFinder.findCandidates(candidate, existing)
        assertEquals(2, matches.size)
    }
}
