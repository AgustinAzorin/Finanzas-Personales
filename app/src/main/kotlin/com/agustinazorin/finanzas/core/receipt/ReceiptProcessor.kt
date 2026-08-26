package com.agustinazorin.finanzas.core.receipt

import android.content.Context
import android.net.Uri
import com.agustinazorin.finanzas.engine.receipt.AfipQrParser
import com.agustinazorin.finanzas.engine.receipt.AfipReceiptData
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Lo que se pudo extraer automáticamente de la foto de un comprobante (CLAUDE.md, sección 40). */
sealed interface ReceiptProcessingResult {
    data class Afip(val rawQrContent: String, val data: AfipReceiptData) : ReceiptProcessingResult
    data class Ocr(val text: String) : ReceiptProcessingResult
    data object None : ReceiptProcessingResult
}

/**
 * Procesa la foto de un comprobante: primero intenta leer un código QR y, si es el de una
 * factura AFIP/ARCA, lo parsea con [AfipQrParser] (la fuente más confiable, CLAUDE.md sección
 * 40). Si no hay QR reconocible, cae a OCR como respaldo. Ambos modelos de ML Kit corren
 * on-device: ni el escaneo de QR ni el OCR llaman a ningún servidor.
 */
class ReceiptProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun process(imagePath: String): ReceiptProcessingResult {
        val image = InputImage.fromFilePath(context, Uri.fromFile(File(imagePath)))

        val barcodes = BarcodeScanning.getClient().use { scanner ->
            runCatching { scanner.process(image).awaitResult() }.getOrNull()
        }
        val afipMatch = barcodes?.firstNotNullOfOrNull { barcode ->
            barcode.rawValue?.let { raw -> AfipQrParser.parse(raw)?.let { data -> raw to data } }
        }
        if (afipMatch != null) {
            return ReceiptProcessingResult.Afip(rawQrContent = afipMatch.first, data = afipMatch.second)
        }

        val ocrText = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).use { recognizer ->
            runCatching { recognizer.process(image).awaitResult().text }.getOrNull()
        }
        return if (!ocrText.isNullOrBlank()) ReceiptProcessingResult.Ocr(ocrText) else ReceiptProcessingResult.None
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}
