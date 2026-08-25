package com.agustinazorin.finanzas.feature.capture.data

import com.agustinazorin.finanzas.core.database.dao.CapturedNotificationDao
import com.agustinazorin.finanzas.core.database.entity.CapturedNotificationEntity
import com.agustinazorin.finanzas.core.database.entity.TransactionEntity
import com.agustinazorin.finanzas.engine.model.CaptureStatus
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.engine.reconciliation.DuplicateCandidateFinder
import com.agustinazorin.finanzas.engine.reconciliation.ReconciliationCandidate
import com.agustinazorin.finanzas.engine.reconciliation.ReconciliationRecord
import com.agustinazorin.finanzas.engine.text.MerchantNormalizer
import com.agustinazorin.finanzas.feature.capture.domain.CapturedNotification
import com.agustinazorin.finanzas.feature.capture.domain.CapturedNotificationRepository
import com.agustinazorin.finanzas.feature.capture.domain.DuplicateCandidate
import com.agustinazorin.finanzas.feature.transaction.domain.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

private const val RECONCILIATION_WINDOW_DAYS = 2L

class CapturedNotificationRepositoryImpl @Inject constructor(
    private val dao: CapturedNotificationDao,
) : CapturedNotificationRepository {

    override fun observePendingReview(): Flow<List<CapturedNotification>> =
        dao.observePendingReview().map { list -> list.map { it.toDomain() } }

    override fun observePendingReviewCount(): Flow<Int> = dao.observePendingReviewCount()

    override suspend fun getById(id: Long): CapturedNotification? = dao.getById(id)?.toDomain()

    override suspend fun insert(capture: CapturedNotification): Long = dao.insert(capture.toEntity())

    override suspend fun markConfirmed(id: Long, transactionId: Long) {
        dao.setResolution(id, CaptureStatus.CONFIRMED, transactionId)
    }

    override suspend fun markDiscarded(id: Long) {
        dao.setResolution(id, CaptureStatus.DISCARDED, null)
    }

    override suspend fun markDuplicate(id: Long, existingTransactionId: Long) {
        dao.setResolution(id, CaptureStatus.DUPLICATE, existingTransactionId)
    }

    override suspend fun existsExactDuplicate(packageName: String, rawText: String?, postedAt: Instant): Boolean =
        dao.existsExactDuplicate(packageName, rawText, postedAt)

    override suspend fun findDuplicateCandidates(
        accountId: Long,
        amount: Long,
        currency: String,
        date: LocalDate,
        merchantNormalized: String?,
    ): List<DuplicateCandidate> {
        val window = dao.getTransactionsBetween(
            date.minusDays(RECONCILIATION_WINDOW_DAYS),
            date.plusDays(RECONCILIATION_WINDOW_DAYS),
        )
        val entitiesById = window.associateBy { it.id }
        val records = window.map { entity ->
            ReconciliationRecord(
                transactionId = entity.id,
                accountId = entity.accountId,
                amount = Money(entity.amount, entity.currency),
                date = entity.date,
                merchantNormalized = entity.merchant?.let(MerchantNormalizer::normalize),
            )
        }
        val candidate = ReconciliationCandidate(
            accountId = accountId,
            amount = Money(amount, currency),
            date = date,
            merchantNormalized = merchantNormalized,
        )

        return DuplicateCandidateFinder.findCandidates(candidate, records).mapNotNull { match ->
            entitiesById[match.record.transactionId]?.let { entity ->
                DuplicateCandidate(entity.toTransactionDomain(), match.confidence)
            }
        }
    }
}

private fun CapturedNotificationEntity.toDomain() = CapturedNotification(
    id = id, packageName = packageName, parserId = parserId, parserVersion = parserVersion,
    postedAt = postedAt, rawTitle = rawTitle, rawText = rawText, parsedAmount = parsedAmount,
    parsedCurrency = parsedCurrency, parsedDirection = parsedDirection, parsedMerchant = parsedMerchant,
    parsedMerchantNormalized = parsedMerchantNormalized, status = status,
    linkedTransactionId = linkedTransactionId, createdAt = createdAt,
)

private fun CapturedNotification.toEntity() = CapturedNotificationEntity(
    id = id, packageName = packageName, parserId = parserId, parserVersion = parserVersion,
    postedAt = postedAt, rawTitle = rawTitle, rawText = rawText, parsedAmount = parsedAmount,
    parsedCurrency = parsedCurrency, parsedDirection = parsedDirection, parsedMerchant = parsedMerchant,
    parsedMerchantNormalized = parsedMerchantNormalized, status = status,
    linkedTransactionId = linkedTransactionId, createdAt = createdAt,
)

private fun TransactionEntity.toTransactionDomain() = Transaction(
    id = id, householdId = householdId, accountId = accountId, ownerMemberId = ownerMemberId,
    amount = amount, currency = currency, direction = direction, date = date, merchant = merchant,
    categoryId = categoryId, type = type, source = source, note = note,
    reconciliationHash = reconciliationHash, linkedTransactionId = linkedTransactionId,
    status = status, createdAt = createdAt, updatedAt = updatedAt,
)
