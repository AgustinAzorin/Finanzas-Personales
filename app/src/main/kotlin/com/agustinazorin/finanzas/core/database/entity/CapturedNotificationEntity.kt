package com.agustinazorin.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.agustinazorin.finanzas.engine.model.CaptureStatus
import com.agustinazorin.finanzas.engine.model.TransactionDirection
import java.time.Instant

/**
 * Captura cruda de una notificación (CLAUDE.md, sección 37). Nunca es directamente una
 * Transaction: es sólo una candidata que el usuario tiene que revisar y confirmar, eligiendo la
 * cuenta y (opcionalmente corrigiendo) la categoría, antes de que se genere el movimiento real.
 * Siempre se guardan [packageName], [postedAt], [rawTitle]/[rawText] y [parserVersion] tal cual
 * llegaron, incluso si el parseo falla, para poder depurar o re-parsear en el futuro.
 */
@Entity(
    tableName = "captured_notifications",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedTransactionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("status"),
        Index("postedAt"),
        Index("linkedTransactionId"),
    ],
)
data class CapturedNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    /** Null si ningún parser supo interpretar el texto; igual queda para revisión manual. */
    val parserId: String?,
    val parserVersion: Int?,
    val postedAt: Instant,
    val rawTitle: String?,
    val rawText: String?,
    /** Siempre positivo, en unidades mínimas de la moneda; ver [TransactionEntity.amount]. */
    val parsedAmount: Long?,
    val parsedCurrency: String?,
    val parsedDirection: TransactionDirection?,
    val parsedMerchant: String?,
    val parsedMerchantNormalized: String?,
    val status: CaptureStatus,
    val linkedTransactionId: Long?,
    val createdAt: Instant,
)
