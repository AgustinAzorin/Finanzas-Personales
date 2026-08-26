package com.agustinazorin.finanzas.feature.receipt.data

import com.agustinazorin.finanzas.core.database.dao.ReceiptDao
import com.agustinazorin.finanzas.core.database.entity.ReceiptEntity
import com.agustinazorin.finanzas.engine.model.ReceiptSource
import com.agustinazorin.finanzas.feature.receipt.domain.AfipReceiptInfo
import com.agustinazorin.finanzas.feature.receipt.domain.Receipt
import com.agustinazorin.finanzas.feature.receipt.domain.ReceiptRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReceiptRepositoryImpl @Inject constructor(
    private val dao: ReceiptDao,
) : ReceiptRepository {

    override fun observeAll(householdId: Long): Flow<List<Receipt>> =
        dao.observeAll(householdId).map { list -> list.map { it.toDomain() } }

    override fun observeUnlinked(householdId: Long): Flow<List<Receipt>> =
        dao.observeUnlinked(householdId).map { list -> list.map { it.toDomain() } }

    override fun observeByTransaction(transactionId: Long): Flow<List<Receipt>> =
        dao.observeByTransaction(transactionId).map { list -> list.map { it.toDomain() } }

    override suspend fun createReceipt(
        householdId: Long,
        imagePath: String,
        source: ReceiptSource,
        qrRawContent: String?,
        ocrText: String?,
        afip: AfipReceiptInfo?,
    ): Long = dao.insert(
        ReceiptEntity(
            householdId = householdId,
            transactionId = null,
            imagePath = imagePath,
            source = source,
            qrRawContent = qrRawContent,
            ocrText = ocrText,
            afipCuitEmisor = afip?.cuitEmisor,
            afipPointOfSale = afip?.pointOfSale,
            afipInvoiceType = afip?.invoiceType,
            afipInvoiceNumber = afip?.invoiceNumber,
            afipAmount = afip?.amount,
            afipCurrency = afip?.currency,
            afipDate = afip?.date,
            afipAuthorizationCode = afip?.authorizationCode,
            capturedAt = Instant.now(),
        ),
    )

    override suspend fun linkToTransaction(receiptId: Long, transactionId: Long) {
        dao.linkToTransaction(receiptId, transactionId)
    }
}

private fun ReceiptEntity.toDomain(): Receipt {
    val afip = if (afipCuitEmisor != null && afipPointOfSale != null && afipInvoiceType != null &&
        afipInvoiceNumber != null && afipAmount != null && afipCurrency != null && afipDate != null && afipAuthorizationCode != null
    ) {
        AfipReceiptInfo(
            cuitEmisor = afipCuitEmisor,
            pointOfSale = afipPointOfSale,
            invoiceType = afipInvoiceType,
            invoiceNumber = afipInvoiceNumber,
            amount = afipAmount,
            currency = afipCurrency,
            date = afipDate,
            authorizationCode = afipAuthorizationCode,
        )
    } else {
        null
    }
    return Receipt(
        id = id,
        householdId = householdId,
        transactionId = transactionId,
        imagePath = imagePath,
        source = source,
        qrRawContent = qrRawContent,
        ocrText = ocrText,
        afip = afip,
        capturedAt = capturedAt,
    )
}
