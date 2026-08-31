package com.agustinazorin.finanzas.core.backup

import android.content.Context
import android.net.Uri
import com.agustinazorin.finanzas.BuildConfig
import com.agustinazorin.finanzas.core.database.AppDatabase
import com.agustinazorin.finanzas.core.database.DATABASE_NAME
import com.agustinazorin.finanzas.core.security.DatabasePassphraseProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SQLiteDatabase as SqlCipherDatabase
import org.json.JSONObject

private const val RECEIPTS_DIR_NAME = "receipts"
private const val DB_ENTRY_NAME = "database.db"
private const val METADATA_ENTRY_NAME = "metadata.json"

/**
 * Backup cifrado completo (CLAUDE.md, sección 44): base de datos + fotos de comprobantes +
 * metadata, en un único archivo que el usuario guarda donde quiera (Drive, disco, etc. — vía SAF,
 * nunca `WRITE_EXTERNAL_STORAGE`) y que se puede restaurar en otro dispositivo con la contraseña
 * que eligió al exportarlo.
 *
 * La base de datos vive cifrada con SQLCipher usando la passphrase local del dispositivo (ver
 * [DatabasePassphraseProvider]), que no viaja entre dispositivos. Para el backup se genera una
 * copia plana (sin cifrar) de la base usando el mecanismo de export nativo de SQLCipher
 * (`sqlcipher_export`), se empaqueta junto con los comprobantes, y recién ese paquete se cifra
 * con la contraseña del usuario ([PasswordEncryption]). Al importar se hace el camino inverso:
 * se vuelve a cifrar la base plana con la passphrase local del dispositivo que restaura.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val passphraseProvider: DatabasePassphraseProvider,
) {

    suspend fun export(destination: Uri, password: CharArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val stagingDir = createStagingDir("export")
            try {
                val plainDbFile = File(stagingDir, DB_ENTRY_NAME)
                exportPlainDatabaseCopy(plainDbFile)

                val zipFile = File(stagingDir, "backup.zip")
                ZipOutputStream(zipFile.outputStream()).use { zip ->
                    addFileEntry(zip, plainDbFile, DB_ENTRY_NAME)
                    receiptsDir().listFiles()?.forEach { file ->
                        addFileEntry(zip, file, "$RECEIPTS_DIR_NAME/${file.name}")
                    }
                    addBytesEntry(zip, buildMetadataJson().toByteArray(Charsets.UTF_8), METADATA_ENTRY_NAME)
                }

                val encrypted = PasswordEncryption.encrypt(zipFile.readBytes(), password)
                val output = context.contentResolver.openOutputStream(destination)
                    ?: error("No se pudo abrir el destino del backup.")
                output.use { it.write(encrypted) }
            } finally {
                stagingDir.deleteRecursively()
            }
        }
    }

    /**
     * Reemplaza la base de datos y los comprobantes actuales por los del backup. Cierra la
     * conexión de Room a propósito: quien llame a esta función debe reiniciar el proceso de la
     * app inmediatamente después de un resultado exitoso, porque a partir de acá cualquier acceso
     * a través del [AppDatabase] inyectado ya no es válido.
     */
    suspend fun import(source: Uri, password: CharArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val encryptedBytes = context.contentResolver.openInputStream(source)?.use { it.readBytes() }
                ?: error("No se pudo leer el archivo de backup.")
            val zipBytes = PasswordEncryption.decrypt(encryptedBytes, password)

            val stagingDir = createStagingDir("import")
            try {
                extractZip(zipBytes, stagingDir)
                val extractedDbFile = File(stagingDir, DB_ENTRY_NAME)
                require(extractedDbFile.exists()) { "El backup no contiene una base de datos válida." }

                appDatabase.close()

                val reencryptedDbFile = reencryptImportedDatabase(extractedDbFile, stagingDir)
                replaceLiveDatabaseFile(reencryptedDbFile)
                replaceReceiptsDirectory(File(stagingDir, RECEIPTS_DIR_NAME))
            } finally {
                stagingDir.deleteRecursively()
            }
        }
    }

    private fun createStagingDir(prefix: String): File =
        File(context.cacheDir, "backup_${prefix}_${System.currentTimeMillis()}").apply { mkdirs() }

    private fun receiptsDir(): File = File(context.filesDir, RECEIPTS_DIR_NAME).apply { mkdirs() }

    private fun buildMetadataJson(): String =
        JSONObject()
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("exportedAt", Instant.now().toString())
            .toString()

    /** Recipe estándar de SQLCipher para volcar una base cifrada a un archivo plano sin tocar el original. */
    private fun exportPlainDatabaseCopy(target: File) {
        if (target.exists()) target.delete()
        val currentDbPath = context.getDatabasePath(DATABASE_NAME).absolutePath
        val passphrase = passphraseProvider.getOrCreatePassphrase()
        // openDatabase sólo tiene overloads de password String/CharArray, no ByteArray: se pasa
        // la clave cruda con la sintaxis hexadecimal x'...' que SQLCipher reconoce para saltear
        // la derivación PBKDF2 (misma convención que reencryptImportedDatabase más abajo).
        val encryptedDb = SqlCipherDatabase.openDatabase(
            currentDbPath, passphrase.toSqlCipherHexKey(), null, SqlCipherDatabase.OPEN_READONLY,
        )
        try {
            encryptedDb.execSQL("ATTACH DATABASE '${target.absolutePath}' AS plaintext KEY ''")
            encryptedDb.execSQL("SELECT sqlcipher_export('plaintext')")
            encryptedDb.execSQL("DETACH DATABASE plaintext")
        } finally {
            encryptedDb.close()
        }
    }

    /** Recipe inversa: vuelve a cifrar una base plana con la passphrase local de este dispositivo. */
    private fun reencryptImportedDatabase(plainDbFile: File, stagingDir: File): File {
        val passphrase = passphraseProvider.getOrCreatePassphrase()
        val reencryptedFile = File(stagingDir, "database_reencrypted.db")
        if (reencryptedFile.exists()) reencryptedFile.delete()

        val plainDb = SqlCipherDatabase.openDatabase(
            plainDbFile.absolutePath, "", null, SqlCipherDatabase.OPEN_READWRITE,
        )
        try {
            plainDb.execSQL("ATTACH DATABASE '${reencryptedFile.absolutePath}' AS encrypted KEY \"${passphrase.toSqlCipherHexKey()}\"")
            plainDb.execSQL("SELECT sqlcipher_export('encrypted')")
            plainDb.execSQL("DETACH DATABASE encrypted")
        } finally {
            plainDb.close()
        }
        return reencryptedFile
    }

    private fun replaceLiveDatabaseFile(newDbFile: File) {
        val target = context.getDatabasePath(DATABASE_NAME)
        listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
            File(target.parentFile, target.name + suffix).delete()
        }
        target.parentFile?.mkdirs()
        newDbFile.copyTo(target, overwrite = true)
    }

    private fun replaceReceiptsDirectory(sourceDir: File) {
        val target = receiptsDir()
        target.deleteRecursively()
        target.mkdirs()
        if (sourceDir.exists()) {
            sourceDir.listFiles()?.forEach { file -> file.copyTo(File(target, file.name), overwrite = true) }
        }
    }

    private fun addFileEntry(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun addBytesEntry(zip: ZipOutputStream, bytes: ByteArray, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        zip.write(bytes)
        zip.closeEntry()
    }

    /** Descomprime rechazando cualquier entrada que intente escribir fuera de [targetDir] ("zip slip"). */
    private fun extractZip(zipBytes: ByteArray, targetDir: File) {
        val targetDirCanonical = targetDir.canonicalPath
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val target = File(targetDir, entry.name)
                    require(target.canonicalPath.startsWith(targetDirCanonical + File.separator)) {
                        "El backup contiene una ruta inválida: ${entry.name}"
                    }
                    target.parentFile?.mkdirs()
                    target.outputStream().use { output -> zip.copyTo(output) }
                }
                entry = zip.nextEntry
            }
        }
    }
}

/** Formato de clave cruda de SQLCipher (`x'...'`): salta la derivación PBKDF2 de una passphrase de texto. */
private fun ByteArray.toSqlCipherHexKey(): String =
    "x'" + joinToString("") { "%02x".format(it) } + "'"
