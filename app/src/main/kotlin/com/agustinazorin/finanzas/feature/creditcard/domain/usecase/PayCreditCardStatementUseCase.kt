package com.agustinazorin.finanzas.feature.creditcard.domain.usecase

import com.agustinazorin.finanzas.feature.creditcard.domain.CreditCardStatementRepository
import com.agustinazorin.finanzas.feature.transaction.domain.usecase.AddTransferUseCase
import java.time.LocalDate
import javax.inject.Inject

/**
 * Pagar el resumen de una tarjeta es una Transferencia, nunca un gasto adicional (Regla 2,
 * CLAUDE.md sección 7): el gasto real ya se contó al momento de cada compra. Acá sólo se mueve
 * dinero de la cuenta que paga hacia la tarjeta, y se registra cuánto de ese resumen ya está pago.
 */
class PayCreditCardStatementUseCase @Inject constructor(
    private val addTransferUseCase: AddTransferUseCase,
    private val creditCardStatementRepository: CreditCardStatementRepository,
) {
    suspend operator fun invoke(
        householdId: Long,
        statementId: Long,
        fromAccountId: Long,
        amount: Long,
        date: LocalDate,
        note: String? = null,
    ) {
        require(amount > 0) { "El monto del pago debe ser mayor a cero." }
        val statement = requireNotNull(creditCardStatementRepository.getById(statementId)) { "El resumen no existe." }

        addTransferUseCase(
            householdId = householdId,
            fromAccountId = fromAccountId,
            toAccountId = statement.creditCardAccountId,
            amount = amount,
            date = date,
            note = note,
        )
        creditCardStatementRepository.registerPayment(statementId, amount)
    }
}
