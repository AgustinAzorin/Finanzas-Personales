package com.agustinazorin.finanzas.engine.model

/** Tipo de miembro del hogar (CLAUDE.md, sección 8). No implica cuenta de usuario. */
enum class MemberType { OWNER, MEMBER, DEPENDENT, OTHER }

/** Tipo de cuenta (CLAUDE.md, sección 9). */
enum class AccountType {
    CASH,
    BANK_ACCOUNT,
    SAVINGS_ACCOUNT,
    MERCADO_PAGO,
    CREDIT_CARD,
    INVESTMENT,
    DIGITAL_WALLET,
    OTHER_ASSET,
    LOAN,
    OTHER_LIABILITY,
}

/** Naturaleza económica de una transacción (CLAUDE.md, sección 14). */
enum class TransactionType { EXPENSE, INCOME, TRANSFER, ADJUSTMENT }

/**
 * Dirección del movimiento sobre la cuenta a la que pertenece la transacción.
 *
 * Es independiente de [TransactionType]: un EXPENSE siempre es OUTFLOW, un INCOME siempre
 * es INFLOW, pero un TRANSFER genera dos filas —una OUTFLOW en la cuenta origen y una
 * INFLOW en la cuenta destino— y un ADJUSTMENT puede ir en cualquier sentido.
 *
 * El saldo de una cuenta es siempre `initialBalance + Σ INFLOW - Σ OUTFLOW`, sin
 * excepciones por tipo de cuenta: en una cuenta de pasivo (ej. tarjeta de crédito) un saldo
 * negativo representa deuda, y un OUTFLOW (una compra) profundiza esa deuda.
 */
enum class TransactionDirection { INFLOW, OUTFLOW }

/** Origen de captura de una transacción (CLAUDE.md, sección 14). */
enum class TransactionSource { MANUAL, NOTIFICATION, QR, EMAIL, IMPORT, SYSTEM }

/** Estado de revisión/conciliación de una transacción (CLAUDE.md, sección 14). */
enum class TransactionStatus { CONFIRMED, PENDING_REVIEW, IGNORED, DUPLICATE }

/** Tipo de movimiento recurrente (CLAUDE.md, sección 15). */
enum class RecurringType { EXPENSE, INCOME }

/** Periodicidad de un movimiento recurrente (CLAUDE.md, sección 15). */
enum class Periodicity { WEEKLY, BIWEEKLY, MONTHLY, ANNUAL }

/**
 * Nivel de certeza de un evento de cash flow (CLAUDE.md, secciones 19 y 36).
 * Nunca se debe mezclar dinero real (ACTUAL/CONFIRMED) con estimaciones (COMMITTED/ESTIMATED).
 */
enum class Certainty { ACTUAL, CONFIRMED, COMMITTED, ESTIMATED }

/**
 * Estado de revisión de una notificación capturada automáticamente (CLAUDE.md, sección 37).
 * Es independiente de [TransactionStatus]: una captura recién llegada nunca es todavía una
 * Transaction real, sólo lo pasa a ser cuando el usuario la confirma.
 */
enum class CaptureStatus { PENDING_REVIEW, CONFIRMED, DISCARDED, DUPLICATE }

/**
 * Qué tan segura es una coincidencia entre una nueva captura y una transacción ya existente
 * durante la conciliación (CLAUDE.md, sección 38). Declarado en orden ascendente de confianza
 * para poder ordenar candidatos por [Enum.ordinal] de mayor a menor certeza.
 */
enum class MatchConfidence { POSSIBLE, LIKELY, EXACT }

/** Estado de una cuota de una compra en cuotas (CLAUDE.md, sección 16). */
enum class InstallmentStatus { PENDING, PAID, CANCELLED }

/** Estado de un resumen de tarjeta de crédito (CLAUDE.md, sección 18). */
enum class CreditCardStatementStatus { OPEN, CLOSED, PARTIALLY_PAID, PAID }

/**
 * Categoría de un [com.agustinazorin.finanzas.engine.model.EngineAsset] (CLAUDE.md, sección 10):
 * un activo cuyo valor no corresponde a una cuenta corriente (Account). No incluye efectivo o
 * inversiones ya modeladas como Account (CASH, INVESTMENT): esta categoría CASH es para dinero
 * físico fuera de cualquier cuenta/billetera (ej. dólares guardados en casa).
 */
enum class AssetCategory { CASH, VEHICLE, REAL_ESTATE, INVESTMENT, OTHER }

/**
 * Tipo de una Liability independiente de una cuenta (CLAUDE.md, sección 11). No incluye deuda de
 * tarjeta ni cuotas futuras: esas ya se modelan vía Account(CREDIT_CARD)/CreditCardStatement/
 * Installment (Fase 2). Esta entidad es para obligaciones que no tienen una cuenta propia detrás,
 * como un préstamo personal o una deuda informal.
 */
enum class LiabilityType { LOAN, PERSONAL_DEBT, OTHER }
