package com.agustinazorin.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.agustinazorin.finanzas.engine.model.ReceiptSource
import java.time.Instant
import java.time.LocalDate

/**
 * Un comprobante fotografiado (CLAUDE.md, sección 40): la foto siempre se guarda ([imagePath],
 * en almacenamiento interno privado de la app — nunca externo, sección 45), y opcionalmente los
 * datos que se pudieron extraer automáticamente: estructurados desde el QR AFIP/ARCA
 * ([qrRawContent] + los campos `afip*`) o, a falta de QR, texto plano por OCR ([ocrText]).
 *
 * [transactionId] es nullable a propósito: un comprobante puede fotografiarse y guardarse antes
 * de vincularlo a un gasto (o sin vincularlo nunca). `onDelete = SET_NULL` porque borrar el gasto
 * no debe borrar la foto del comprobante.
 */
@Entity(
    tableName = "receipts",
    foreignKeys = [
        ForeignKey(
            entity = HouseholdEntity::class,
            parentColumns = ["id"],
            childColumns = ["householdId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("householdId"), Index("transactionId")],
)
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val householdId: Long,
    val transactionId: Long?,
    val imagePath: String,
    val source: ReceiptSource,
    val qrRawContent: String?,
    val ocrText: String?,
    val afipCuitEmisor: Long?,
    val afipPointOfSale: Int?,
    val afipInvoiceType: Int?,
    val afipInvoiceNumber: Long?,
    val afipAmount: Long?,
    val afipCurrency: String?,
    val afipDate: LocalDate?,
    val afipAuthorizationCode: Long?,
    val capturedAt: Instant,
)
