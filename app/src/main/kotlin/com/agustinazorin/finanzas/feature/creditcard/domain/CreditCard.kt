package com.agustinazorin.finanzas.feature.creditcard.domain

import kotlinx.coroutines.flow.Flow

/** Configuración de una tarjeta de crédito, 1 a 1 con su Account (CLAUDE.md, sección 17). */
data class CreditCard(
    val accountId: Long,
    val closingDay: Int,
    val dueDay: Int,
    val creditLimit: Long,
)

interface CreditCardRepository {
    fun observeByAccount(accountId: Long): Flow<CreditCard?>
    suspend fun getByAccount(accountId: Long): CreditCard?
    suspend fun upsert(accountId: Long, closingDay: Int, dueDay: Int, creditLimit: Long)

    /** Deuda vigente: la suma de lo que falta pagar en todos los resúmenes de la tarjeta. */
    suspend fun getOutstandingBalance(accountId: Long): Long
}
