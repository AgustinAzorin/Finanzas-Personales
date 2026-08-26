package com.agustinazorin.finanzas.feature.transaction.domain.usecase

import com.agustinazorin.finanzas.engine.text.MerchantNormalizer
import com.agustinazorin.finanzas.feature.category.domain.CategoryRuleRepository
import com.agustinazorin.finanzas.feature.transaction.domain.Transaction
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionRepository
import java.time.Instant
import javax.inject.Inject

/**
 * Edita los campos "seguros" de una transacción ya existente (monto, fecha, categoría,
 * comercio, nota, estado). No permite cambiar de cuenta ni de tipo: eso rompería el vínculo
 * de una transferencia o el significado económico del movimiento (Regla 1, CLAUDE.md sección 7).
 */
class EditTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRuleRepository: CategoryRuleRepository,
) {
    suspend operator fun invoke(
        transactionId: Long,
        amount: Long,
        categoryId: Long?,
        merchant: String?,
        note: String?,
    ) {
        require(amount > 0) { "El monto debe ser mayor a cero." }
        val existing = requireNotNull(transactionRepository.getById(transactionId)) { "La transacción no existe." }
        val updated = existing.copy(
            amount = amount,
            categoryId = categoryId,
            merchant = merchant,
            note = note,
            updatedAt = Instant.now(),
        )
        transactionRepository.updateTransaction(updated)
        // Una corrección manual de categoría es exactamente la señal de aprendizaje de CLAUDE.md sección 39.
        if (!merchant.isNullOrBlank() && categoryId != null) {
            categoryRuleRepository.learn(MerchantNormalizer.normalize(merchant), categoryId)
        }
    }
}
