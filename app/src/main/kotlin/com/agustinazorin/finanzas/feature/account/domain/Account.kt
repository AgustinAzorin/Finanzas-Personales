package com.agustinazorin.finanzas.feature.account.domain

import com.agustinazorin.finanzas.engine.model.AccountType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class Account(
    val id: Long,
    val householdId: Long,
    val ownerMemberId: Long?,
    val name: String,
    val type: AccountType,
    val currency: String,
    val initialBalance: Long,
    val initialBalanceDate: LocalDate,
    val isActive: Boolean,
)

interface AccountRepository {
    fun observeAccounts(householdId: Long): Flow<List<Account>>
    fun observeActiveAccounts(householdId: Long): Flow<List<Account>>
    fun observeAccount(accountId: Long): Flow<Account?>
    suspend fun getAccount(accountId: Long): Account?

    suspend fun createAccount(
        householdId: Long,
        ownerMemberId: Long?,
        name: String,
        type: AccountType,
        currency: String,
        initialBalance: Long,
        initialBalanceDate: LocalDate,
    ): Long

    suspend fun updateAccount(account: Account)
    suspend fun setAccountActive(accountId: Long, isActive: Boolean)
}
