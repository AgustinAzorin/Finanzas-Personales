package com.agustinazorin.finanzas.core.receipt

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import javax.inject.Inject

/**
 * Guarda las fotos de comprobantes en almacenamiento interno privado de la app (CLAUDE.md,
 * sección 45: nunca `WRITE_EXTERNAL_STORAGE`). Para que la cámara del sistema pueda escribir ahí
 * sin exponer una ruta de archivo real, se entrega un `content://` [Uri] vía [FileProvider] (ver
 * `res/xml/file_paths.xml` y el `<provider>` en el manifest).
 */
class ReceiptImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val receiptsDir: File
        get() = File(context.filesDir, "receipts").apply { mkdirs() }

    /** Crea un archivo nuevo vacío y devuelve el Uri de contenido para que la cámara escriba ahí. */
    fun createPendingImage(): ReceiptImageTarget {
        val file = File(receiptsDir, "receipt_${Instant.now().toEpochMilli()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return ReceiptImageTarget(path = file.absolutePath, uri = uri)
    }

    /**
     * Copia una imagen elegida de la galería a almacenamiento propio: el Uri que devuelve el
     * selector de fotos del sistema no es necesariamente legible a largo plazo, así que se
     * guarda una copia local igual que con las fotos sacadas con la cámara.
     */
    fun copyToReceiptsDir(sourceUri: Uri): String? {
        val file = File(receiptsDir, "receipt_${Instant.now().toEpochMilli()}.jpg")
        val copied = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
            true
        } ?: false
        return if (copied) file.absolutePath else null
    }
}

data class ReceiptImageTarget(val path: String, val uri: Uri)
