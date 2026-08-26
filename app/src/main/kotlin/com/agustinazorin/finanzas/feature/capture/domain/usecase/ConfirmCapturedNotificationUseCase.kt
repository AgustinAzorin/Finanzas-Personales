package com.agustinazorin.finanzas.feature.capture.domain.usecase

import com.agustinazorin.finanzas.engine.model.CaptureStatus
import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.model.TransactionSource
import com.agustinazorin.finanzas.engine.model.TransactionStatus
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.engine.text.MerchantNormalizer
import com.agustinazorin.finanzas.feature.capture.domain.CapturedNotificationRepository
import com.agustinazorin.finanzas.feature.category.domain.CategoryRuleRepository
import com.agustinazorin.finanzas.feature.transaction.domain.Transaction
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionRepository
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * Confirma una notificación capturada, recién ahí transformándola en una Transaction real
 * (CLAUDE.md, sección 37: "los gastos capturados automáticamente deben quedar PENDING_REVIEW
 * hasta confirmación"). El usuario ya tuvo que elegir cuenta y categoría en la pantalla de
 * revisión: acá sólo se persiste esa decisión y, si corrigió o confirmó una categoría para un
 * comercio, se aprende la regla correspondiente (CLAUDE.md, sección 39).
 */
class ConfirmCapturedNotificationUseCase @Inject constructor(
    private val capturedNotificationRepository: CapturedNotificationRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRuleRepository: CategoryRuleRepository,
) {
    suspend operator fun invoke(
        captureId: Long,
        householdId: Long,
        accountId: Long,
        ownerMemberId: Long?,
        amount: Long,
        currency: String,
        direction: TransactionDirection,
        date: LocalDate,
        merchant: String?,
        categoryId: Long?,
        note: String?,
    ): Long {
        require(amount > 0) { "El monto debe ser mayor a cero." }
        val capture = requireNotNull(capturedNotificationRepository.getById(captureId)) { "La captura no existe." }
        require(capture.status == CaptureStatus.PENDING_REVIEW) { "La captura ya fue resuelta." }

        val type = if (direction == TransactionDirection.OUTFLOW) TransactionType.EXPENSE else TransactionType.INCOME
        val now = Instant.now()
        val transactionId = transactionRepository.createTransaction(
            Transaction(
                id = 0,
                householdId = householdId,
                accountId = accountId,
                ownerMemberId = ownerMemberId,
                amount = amount,
                currency = currency,
                direction = direction,
                date = date,
                merchant = merchant,
                categoryId = categoryId,
                type = type,
                source = TransactionSource.NOTIFICATION,
                note = note,
                reconciliationHash = null,
                linkedTransactionId = null,
                status = TransactionStatus.CONFIRMED,
                createdAt = now,
                updatedAt = now,
            ),
        )

        capturedNotificationRepository.markConfirmed(captureId, transactionId)

        if (!merchant.isNullOrBlank() && categoryId != null) {
            categoryRuleRepository.learn(MerchantNormalizer.normalize(merchant), categoryId)
        }

        return transactionId
    }
}
