package com.agustinazorin.finanzas.feature.account.data

import com.agustinazorin.finanzas.core.database.dao.AccountDao
import com.agustinazorin.finanzas.core.database.entity.AccountEntity
import com.agustinazorin.finanzas.engine.model.AccountType
import com.agustinazorin.finanzas.feature.account.domain.Account
import com.agustinazorin.finanzas.feature.account.domain.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val dao: AccountDao,
) : AccountRepository {

    override fun observeAccounts(householdId: Long): Flow<List<Account>> =
        dao.observeAccounts(householdId).map { list -> list.map { it.toDomain() } }

    override fun observeActiveAccounts(householdId: Long): Flow<List<Account>> =
        dao.observeActiveAccounts(householdId).map { list -> list.map { it.toDomain() } }

    override fun observeAccount(accountId: Long): Flow<Account?> =
        dao.observeById(accountId).map { it?.toDomain() }

    override suspend fun getAccount(accountId: Long): Account? = dao.getById(accountId)?.toDomain()

    override suspend fun createAccount(
        householdId: Long,
        ownerMemberId: Long?,
        name: String,
        type: AccountType,
        currency: String,
        initialBalance: Long,
        initialBalanceDate: LocalDate,
    ): Long = dao.insert(
        AccountEntity(
            householdId = householdId,
            ownerMemberId = ownerMemberId,
            name = name,
            type = type,
            currency = currency,
            initialBalance = initialBalance,
            initialBalanceDate = initialBalanceDate,
            isActive = true,
        ),
    )

    override suspend fun updateAccount(account: Account) {
        dao.update(account.toEntity())
    }

    override suspend fun setAccountActive(accountId: Long, isActive: Boolean) {
        val entity = dao.getById(accountId) ?: return
        dao.update(entity.copy(isActive = isActive))
    }
}

private fun AccountEntity.toDomain() = Account(
    id = id, householdId = householdId, ownerMemberId = ownerMemberId, name = name, type = type,
    currency = currency, initialBalance = initialBalance, initialBalanceDate = initialBalanceDate,
    isActive = isActive,
)

private fun Account.toEntity() = AccountEntity(
    id = id, householdId = householdId, ownerMemberId = ownerMemberId, name = name, type = type,
    currency = currency, initialBalance = initialBalance, initialBalanceDate = initialBalanceDate,
    isActive = isActive,
)
