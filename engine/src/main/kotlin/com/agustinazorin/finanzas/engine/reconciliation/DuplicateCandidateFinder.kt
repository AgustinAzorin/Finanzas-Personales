package com.agustinazorin.finanzas.engine.reconciliation

import com.agustinazorin.finanzas.engine.model.MatchConfidence
import com.agustinazorin.finanzas.engine.money.Money
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** Transacción ya cargada, candidata a coincidir con una nueva captura (CLAUDE.md, sección 38). */
data class ReconciliationRecord(
    val transactionId: Long,
    val accountId: Long,
    val amount: Money,
    val date: LocalDate,
    val merchantNormalized: String?,
)

/** Nueva captura (notificación, QR, importación) buscando posibles duplicados ya cargados. */
data class ReconciliationCandidate(
    val accountId: Long?,
    val amount: Money,
    val date: LocalDate,
    val merchantNormalized: String?,
)

data class DuplicateMatch(val record: ReconciliationRecord, val confidence: MatchConfidence)

/**
 * Encuentra transacciones ya existentes que podrían ser el mismo movimiento económico que una
 * nueva captura, para pedirle confirmación al usuario en vez de mergear automáticamente (CLAUDE.md,
 * sección 38: "Nunca hacer merge automático solamente porque coincidan monto y fecha"). Esta
 * función sólo genera candidatos ordenados por confianza; la decisión final siempre es del usuario.
 */
object DuplicateCandidateFinder {

    private const val DATE_WINDOW_DAYS = 2L

    fun findCandidates(
        candidate: ReconciliationCandidate,
        existing: List<ReconciliationRecord>,
    ): List<DuplicateMatch> =
        existing
            .asSequence()
            .filter { it.amount == candidate.amount }
            .filter { candidate.accountId == null || it.accountId == candidate.accountId }
            .filter { abs(ChronoUnit.DAYS.between(it.date, candidate.date)) <= DATE_WINDOW_DAYS }
            .map { record -> DuplicateMatch(record, confidenceFor(candidate, record)) }
            .sortedByDescending { it.confidence.ordinal }
            .toList()

    private fun confidenceFor(candidate: ReconciliationCandidate, record: ReconciliationRecord): MatchConfidence {
        val sameDate = candidate.date == record.date
        val merchantMatch = candidate.merchantNormalized != null &&
            candidate.merchantNormalized == record.merchantNormalized
        return when {
            sameDate && merchantMatch -> MatchConfidence.EXACT
            sameDate || merchantMatch -> MatchConfidence.LIKELY
            else -> MatchConfidence.POSSIBLE
        }
    }
}
