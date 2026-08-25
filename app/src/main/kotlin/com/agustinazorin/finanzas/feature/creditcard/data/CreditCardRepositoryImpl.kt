package com.agustinazorin.finanzas.feature.creditcard.data

import com.agustinazorin.finanzas.core.database.dao.CreditCardDao
import com.agustinazorin.finanzas.core.database.dao.CreditCardStatementDao
import com.agustinazorin.finanzas.core.database.entity.CreditCardEntity
import com.agustinazorin.finanzas.feature.creditcard.domain.CreditCard
import com.agustinazorin.finanzas.feature.creditcard.domain.CreditCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CreditCardRepositoryImpl @Inject constructor(
    private val dao: CreditCardDao,
    private val statementDao: CreditCardStatementDao,
) : CreditCardRepository {

    override fun observeByAccount(accountId: Long): Flow<CreditCard?> =
        dao.observeByAccountId(accountId).map { it?.toDomain() }

    override suspend fun getByAccount(accountId: Long): CreditCard? = dao.getByAccountId(accountId)?.toDomain()

    override suspend fun upsert(accountId: Long, closingDay: Int, dueDay: Int, creditLimit: Long) {
        dao.upsert(CreditCardEntity(accountId = accountId, closingDay = closingDay, dueDay = dueDay, creditLimit = creditLimit))
    }

    override suspend fun getOutstandingBalance(accountId: Long): Long = statementDao.getOutstandingBalance(accountId)
}

private fun CreditCardEntity.toDomain() =
    CreditCard(accountId = accountId, closingDay = closingDay, dueDay = dueDay, creditLimit = creditLimit)
