package com.agustinazorin.finanzas.feature.receipt.domain

import com.agustinazorin.finanzas.engine.model.ReceiptSource
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Datos estructurados del QR AFIP/ARCA de un comprobante, ya guardados (CLAUDE.md, sección 40). */
data class AfipReceiptInfo(
    val cuitEmisor: Long,
    val pointOfSale: Int,
    val invoiceType: Int,
    val invoiceNumber: Long,
    /** Unidades mínimas de la moneda (ej: centavos). */
    val amount: Long,
    val currency: String,
    val date: LocalDate,
    val authorizationCode: Long,
)

data class Receipt(
    val id: Long,
    val householdId: Long,
    val transactionId: Long?,
    val imagePath: String,
    val source: ReceiptSource,
    val qrRawContent: String?,
    val ocrText: String?,
    val afip: AfipReceiptInfo?,
    val capturedAt: Instant,
)

interface ReceiptRepository {
    fun observeAll(householdId: Long): Flow<List<Receipt>>
    fun observeUnlinked(householdId: Long): Flow<List<Receipt>>
    fun observeByTransaction(transactionId: Long): Flow<List<Receipt>>

    suspend fun createReceipt(
        householdId: Long,
        imagePath: String,
        source: ReceiptSource,
        qrRawContent: String?,
        ocrText: String?,
        afip: AfipReceiptInfo?,
    ): Long

    suspend fun linkToTransaction(receiptId: Long, transactionId: Long)
}
