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
    /**
     * true cuando esta es la compra "padre" de una compra en cuotas (Regla 3, CLAUDE.md
     * sección 7): su gasto económico ya no se cuenta acá, sino a través de sus
     * [EngineInstallment] correspondientes, cada una imputada a su propio período. Ver
     * [com.agustinazorin.finanzas.engine.metrics.PeriodSummaryCalculator]. No afecta el saldo
     * de la cuenta (Regla 4): la deuda se reconoce igual, de una sola vez, al momento de compra.
     */
    val hasInstallments: Boolean = false,
    /** "Responsable" de la transacción (CLAUDE.md, sección 30): quién la pagó. Null = sin atribuir a una persona. */
    val ownerMemberId: Long? = null,
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

/**
 * Proyección de una cuota (Installment) para el motor financiero (CLAUDE.md, sección 16).
 * [type] se hereda de la Transaction "padre" (siempre EXPENSE o INCOME: una cuota nunca es una
 * transferencia ni un ajuste).
 */
data class EngineInstallment(
    val id: Long,
    val transactionId: Long,
    val type: TransactionType,
    val amount: Money,
    val accountingDate: LocalDate,
    val status: InstallmentStatus,
)

/**
 * Cuánto de una [EngineTransaction] le corresponde económicamente a un "Beneficiario" (CLAUDE.md,
 * sección 30), más allá de quién la pagó. La suma de los shares de una transacción siempre es
 * igual a su monto total: se calcula una única vez al cargar el gasto compartido (ver
 * [com.agustinazorin.finanzas.engine.split.ExpenseSplitCalculator]) y se persiste, nunca se
 * recalcula a partir de porcentajes en cada lectura.
 */
data class EngineTransactionShare(
    val transactionId: Long,
    val memberId: Long,
    val shareAmount: Money,
)

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

/**
 * Proyección de un Asset para el motor financiero (CLAUDE.md, sección 10): un activo sin cuenta
 * corriente propia (vehículo, inmueble, efectivo físico, etc.). A diferencia de [EngineAccount],
 * no tiene historial de transacciones: [currentValue] es la última valuación conocida tal cual,
 * sin importar la fecha en la que se consulte el patrimonio (ver [com.agustinazorin.finanzas.engine.networth.NetWorthCalculator]).
 */
data class EngineAsset(
    val id: Long,
    val currentValue: Money,
    val valuationDate: LocalDate,
    val isActive: Boolean,
)

/**
 * Proyección de una Liability para el motor financiero (CLAUDE.md, sección 11): una obligación sin
 * cuenta propia detrás (préstamo personal, deuda informal). [outstandingAmount] siempre es
 * positivo; su contribución al patrimonio es negativa (ver [com.agustinazorin.finanzas.engine.networth.NetWorthCalculator]).
 */
data class EngineLiability(
    val id: Long,
    val outstandingAmount: Money,
    val isActive: Boolean,
) {
    init {
        require(outstandingAmount.minorUnits >= 0) { "El saldo pendiente de una deuda siempre es positivo." }
    }
}

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

/**
 * Un evento de flujo de caja futuro (CLAUDE.md, sección 36): una entrada o salida de dinero
 * todavía no ocurrida, con su nivel de certeza. Fuente-agnóstico a propósito: puede originarse
 * en un movimiento recurrente, una cuota pendiente, etc.
 *
 * @param amount con signo: negativo = salida de dinero, positivo = entrada.
 */
data class CashFlowEvent(
    val date: LocalDate,
    val label: String,
    val amount: Money,
    val certainty: Certainty,
)

/**
 * Un punto de la línea de tiempo de flujo de caja proyectado (CLAUDE.md, sección 26): el saldo
 * resultante después de aplicar un evento, en orden cronológico.
 *
 * @param delta null únicamente en el primer punto (el saldo actual, sin evento asociado).
 */
data class CashFlowPoint(
    val date: LocalDate,
    val label: String,
    val delta: Money?,
    val balance: Money,
    val certainty: Certainty,
)
