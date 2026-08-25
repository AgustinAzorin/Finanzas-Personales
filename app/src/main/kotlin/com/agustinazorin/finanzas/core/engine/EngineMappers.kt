package com.agustinazorin.finanzas.core.engine

import com.agustinazorin.finanzas.engine.model.EngineAccount
import com.agustinazorin.finanzas.engine.model.EngineRecurringTransaction
import com.agustinazorin.finanzas.engine.model.EngineTransaction
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.feature.account.domain.Account
import com.agustinazorin.finanzas.feature.recurring.domain.RecurringTransaction
import com.agustinazorin.finanzas.feature.transaction.domain.Transaction

/**
 * Traducción entre los modelos persistidos por feature (Room-friendly, con campos de UI/hogar)
 * y los modelos puros que consume el Financial Engine (:engine), que no conoce Room ni Android.
 */

fun Account.toEngineAccount(): EngineAccount = EngineAccount(
    id = id,
    type = type,
    currency = currency,
    initialBalance = Money(initialBalance, currency),
    initialBalanceDate = initialBalanceDate,
    isActive = isActive,
)

fun Transaction.toEngineTransaction(): EngineTransaction = EngineTransaction(
    id = id,
    accountId = accountId,
    amount = Money(amount, currency),
    direction = direction,
    date = date,
    type = type,
    status = status,
    categoryId = categoryId,
    linkedTransactionId = linkedTransactionId,
)

fun RecurringTransaction.toEngineRecurringTransaction(): EngineRecurringTransaction = EngineRecurringTransaction(
    id = id,
    type = type,
    name = name,
    estimatedAmount = Money(estimatedAmount, currency),
    periodicity = periodicity,
    dueDay = dueDay,
    categoryId = categoryId,
    accountId = accountId,
    isActive = isActive,
)
