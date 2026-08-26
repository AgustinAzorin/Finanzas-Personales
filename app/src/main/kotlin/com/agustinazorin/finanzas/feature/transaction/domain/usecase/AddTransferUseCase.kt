package com.agustinazorin.finanzas.feature.transaction.domain.usecase

import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.model.TransactionSource
import com.agustinazorin.finanzas.engine.model.TransactionStatus
import com.agustinazorin.finanzas.engine.model.TransactionType
import com.agustinazorin.finanzas.feature.account.domain.AccountRepository
import com.agustinazorin.finanzas.feature.transaction.domain.Transaction
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionRepository
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * Única forma soportada de mover dinero entre dos cuentas propias (Regla 1, CLAUDE.md
 * sección 7): nunca es un gasto, siempre genera un par OUTFLOW/INFLOW vinculado.
 */
class AddTransferUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(
        householdId: Long,
        fromAccountId: Long,
        toAccountId: Long,
        amount: Long,
        date: LocalDate,
        note: String? = null,
    ) {
        require(amount > 0) { "El monto de una transferencia debe ser mayor a cero." }
        require(fromAccountId != toAccountId) { "No se puede transferir de una cuenta a sí misma." }

        val fromAccount = requireNotNull(accountRepository.getAccount(fromAccountId)) { "Cuenta origen inexistente." }
        val toAccount = requireNotNull(accountRepository.getAccount(toAccountId)) { "Cuenta destino inexistente." }
        require(fromAccount.currency == toAccount.currency) {
            "No se puede transferir entre cuentas de distinta moneda sin conversión explícita (disponible en Fase 6)."
        }

        val now = Instant.now()
        val outflow = Transaction(
            id = 0, householdId = householdId, accountId = fromAccountId, ownerMemberId = fromAccount.ownerMemberId,
            amount = amount, currency = fromAccount.currency, direction = TransactionDirection.OUTFLOW, date = date,
            merchant = null, categoryId = null, type = TransactionType.TRANSFER, source = TransactionSource.MANUAL,
            note = note, reconciliationHash = null, linkedTransactionId = null, status = TransactionStatus.CONFIRMED,
            createdAt = now, updatedAt = now,
        )
        val inflow = outflow.copy(accountId = toAccountId, ownerMemberId = toAccount.ownerMemberId, direction = TransactionDirection.INFLOW)

        transactionRepository.createTransfer(outflow, inflow)
    }
}
