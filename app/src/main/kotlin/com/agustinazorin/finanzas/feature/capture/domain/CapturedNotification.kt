package com.agustinazorin.finanzas.feature.capture.domain

import com.agustinazorin.finanzas.engine.model.CaptureStatus
import com.agustinazorin.finanzas.engine.model.MatchConfidence
import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.feature.transaction.domain.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * Captura cruda de una notificación, pendiente de revisión (CLAUDE.md, sección 37). Nunca
 * representa dinero real por sí sola: sólo se vuelve una [Transaction] cuando el usuario la
 * confirma explícitamente (ver ConfirmCapturedNotificationUseCase).
 */
data class CapturedNotification(
    val id: Long,
    val packageName: String,
    val parserId: String?,
    val parserVersion: Int?,
    val postedAt: Instant,
    val rawTitle: String?,
    val rawText: String?,
    val parsedAmount: Long?,
    val parsedCurrency: String?,
    val parsedDirection: TransactionDirection?,
    val parsedMerchant: String?,
    val parsedMerchantNormalized: String?,
    val status: CaptureStatus,
    val linkedTransactionId: Long?,
    val createdAt: Instant,
) {
    /** Sin monto y dirección interpretados, el usuario tiene que cargarlos a mano al revisar. */
    val isParsed: Boolean get() = parsedAmount != null && parsedDirection != null
}

/** Transacción ya cargada que podría ser el mismo movimiento que una nueva captura (CLAUDE.md, sección 38). */
data class DuplicateCandidate(val transaction: Transaction, val confidence: MatchConfidence)

interface CapturedNotificationRepository {
    fun observePendingReview(): Flow<List<CapturedNotification>>
    fun observePendingReviewCount(): Flow<Int>
    suspend fun getById(id: Long): CapturedNotification?
    suspend fun insert(capture: CapturedNotification): Long
    suspend fun markConfirmed(id: Long, transactionId: Long)
    suspend fun markDiscarded(id: Long)
    suspend fun markDuplicate(id: Long, existingTransactionId: Long)

    /** Evita guardar dos veces la misma notificación si el listener la vuelve a entregar. */
    suspend fun existsExactDuplicate(packageName: String, rawText: String?, postedAt: Instant): Boolean

    /**
     * Busca transacciones ya cargadas que podrían ser el mismo gasto/ingreso que esta captura,
     * para que el usuario elija vincularla en vez de crear un movimiento duplicado (CLAUDE.md,
     * sección 38). Nunca decide sola: sólo devuelve candidatas ordenadas por confianza.
     */
    suspend fun findDuplicateCandidates(
        accountId: Long,
        amount: Long,
        currency: String,
        date: LocalDate,
        merchantNormalized: String?,
    ): List<DuplicateCandidate>
}
