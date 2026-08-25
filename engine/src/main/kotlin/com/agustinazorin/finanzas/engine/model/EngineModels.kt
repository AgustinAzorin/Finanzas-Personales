package com.agustinazorin.finanzas.engine.model

import com.agustinazorin.finanzas.engine.money.Money
import java.time.LocalDate

/**
 * Proyección de una Account para el motor financiero: sólo los campos necesarios para
 * calcular saldos y patrimonio, sin depender de Room ni de la capa de hogar/UI.
 */
data class EngineAccount(
    val id: Long,
    val type: AccountType,
    val currency: String,
    val initialBalance: Money,
    val initialBalanceDate: LocalDate,
    val isActive: Boolean,
)

/**
 * Proyección de una Transaction para el motor financiero.
 *
 * @param amount siempre positivo; [direction] determina si suma o resta del saldo de [accountId].
 */
data class EngineTransaction(
    val id: Long,
    val accountId: Long,
    val amount: Money,
    val direction: TransactionDirection,
    val date: LocalDate,
    val type: TransactionType,
    val status: TransactionStatus,
    val categoryId: Long? = null,
    val linkedTransactionId: Long? = null,
) {
    init {
        require(amount.minorUnits >= 0) { "El monto de una transacción siempre es positivo; el signo lo da 'direction'." }
    }

    /** Contribución con signo al saldo de la cuenta: positiva si INFLOW, negativa si OUTFLOW. */
    val signedAmount: Money
        get() = if (direction == TransactionDirection.INFLOW) amount else -amount

    /** Excluye transacciones que no representan dinero real en la cuenta. */
    val countsTowardsBalance: Boolean
        get() = status == TransactionStatus.CONFIRMED || status == TransactionStatus.PENDING_REVIEW

    /** Transferencias y ajustes no son gasto ni ingreso económico (Regla 1, CLAUDE.md sección 7). */
    val isEconomicFlow: Boolean
        get() = type == TransactionType.EXPENSE || type == TransactionType.INCOME
}

data class EngineRecurringTransaction(
    val id: Long,
    val type: RecurringType,
    val name: String,
    val estimatedAmount: Money,
    val periodicity: Periodicity,
    val dueDay: Int,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val isActive: Boolean = true,
)

/** Saldo de una cuenta a una fecha determinada. */
data class AccountBalance(
    val accountId: Long,
    val asOf: LocalDate,
    val balance: Money,
)

/** Resumen de ingresos/gastos reales de un período, en una única moneda. */
data class PeriodSummary(
    val start: LocalDate,
    val end: LocalDate,
    val currency: String,
    val totalExpense: Money,
    val totalIncome: Money,
) {
    /** (ingresos - gastos) / ingresos. Null si no hubo ingresos en el período (evita división por cero). */
    val savingsRate: Double?
        get() = if (totalIncome.minorUnits == 0L) {
            null
        } else {
            (totalIncome.minorUnits - totalExpense.minorUnits).toDouble() / totalIncome.minorUnits.toDouble()
        }
}

/** Un compromiso futuro proyectado a partir de un movimiento recurrente. */
data class UpcomingCommitment(
    val recurringTransactionId: Long,
    val name: String,
    val type: RecurringType,
    val amount: Money,
    val dueDate: LocalDate,
    val categoryId: Long?,
    val certainty: Certainty,
)
