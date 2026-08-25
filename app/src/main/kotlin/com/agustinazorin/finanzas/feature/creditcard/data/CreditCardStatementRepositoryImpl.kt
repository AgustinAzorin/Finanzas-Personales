package com.agustinazorin.finanzas.feature.creditcard.data

import com.agustinazorin.finanzas.core.database.dao.CreditCardStatementDao
import com.agustinazorin.finanzas.core.database.dao.InstallmentDao
import com.agustinazorin.finanzas.core.database.entity.CreditCardStatementEntity
import com.agustinazorin.finanzas.engine.creditcard.CreditCardStatementStatusCalculator
import com.agustinazorin.finanzas.engine.model.CreditCardStatementStatus
import com.agustinazorin.finanzas.feature.creditcard.domain.CreditCardStatement
import com.agustinazorin.finanzas.feature.creditcard.domain.CreditCardStatementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class CreditCardStatementRepositoryImpl @Inject constructor(
    private val dao: CreditCardStatementDao,
    private val installmentDao: InstallmentDao,
) : CreditCardStatementRepository {

    override fun observeByAccount(creditCardAccountId: Long): Flow<List<CreditCardStatement>> =
        dao.observeByAccount(creditCardAccountId).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): CreditCardStatement? = dao.getById(id)?.toDomain()

    override suspend fun recomputeStatement(
        creditCardAccountId: Long,
        periodStart: LocalDate,
        closingDate: LocalDate,
        dueDate: LocalDate,
    ) {
        val total = dao.sumInstallmentsForCycle(creditCardAccountId, closingDate)
        val existing = dao.findByCycle(creditCardAccountId, closingDate)
        val status = CreditCardStatementStatusCalculator.effectiveStatus(
            closingDate = closingDate,
            totalAmount = total,
            paidAmount = existing?.paidAmount ?: 0,
            asOf = LocalDate.now(),
        )
        if (existing != null) {
            dao.update(existing.copy(totalAmount = total, status = status))
        } else {
            dao.insert(
                CreditCardStatementEntity(
                    creditCardAccountId = creditCardAccountId,
                    periodStart = periodStart,
                    closingDate = closingDate,
                    dueDate = dueDate,
                    totalAmount = total,
                    paidAmount = 0,
                    status = status,
                ),
            )
        }
    }

    override suspend fun registerPayment(statementId: Long, amount: Long) {
        val statement = dao.getById(statementId) ?: return
        val newPaidAmount = statement.paidAmount + amount
        val newStatus = CreditCardStatementStatusCalculator.effectiveStatus(
            closingDate = statement.closingDate,
            totalAmount = statement.totalAmount,
            paidAmount = newPaidAmount,
            asOf = LocalDate.now(),
        )
        dao.update(statement.copy(paidAmount = newPaidAmount, status = newStatus))
        if (newStatus == CreditCardStatementStatus.PAID) {
            installmentDao.markPaidForCycle(statement.creditCardAccountId, statement.closingDate)
        }
    }
}

private fun CreditCardStatementEntity.toDomain() = CreditCardStatement(
    id = id, creditCardAccountId = creditCardAccountId, periodStart = periodStart, closingDate = closingDate,
    dueDate = dueDate, totalAmount = totalAmount, paidAmount = paidAmount, status = status,
)
