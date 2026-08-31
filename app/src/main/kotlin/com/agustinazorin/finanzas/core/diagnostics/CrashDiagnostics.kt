package com.agustinazorin.finanzas.core.diagnostics

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

/**
 * Diagnóstico de arranque, exclusivamente local (CLAUDE.md, sección 2: nada de telemetría ni
 * llamadas de red propias). Registra en un archivo del almacenamiento privado de la app cada paso
 * del arranque y cualquier excepción no manejada, para poder diagnosticar un crash en el arranque
 * sin depender de `adb logcat` — que la gran mayoría de quienes usan la app no tienen forma de
 * correr. La app nunca envía este archivo a ningún lado; es el usuario quien decide compartirlo
 * (ver [com.agustinazorin.finanzas.core.diagnostics.ui.StartupDiagnosticsScreen]).
 */
object CrashDiagnostics {

    private const val FILE_NAME = "startup_diagnostics.log"

    @Synchronized
    fun recordStep(context: Context, step: String) {
        appendLine(context, "PASO OK: $step")
    }

    @Synchronized
    fun recordCaught(context: Context, step: String, error: Throwable) {
        appendLine(context, "FALLÓ: $step\n${error.stackTraceToText()}")
    }

    /**
     * Instala un [Thread.UncaughtExceptionHandler] que registra la excepción antes de delegar al
     * handler anterior (preservando el comportamiento normal de Android ante un crash). Hay que
     * llamarlo lo antes posible en el arranque del proceso — desde `attachBaseContext`, no desde
     * `onCreate` — para que cubra incluso una excepción durante la inyección de campos de Hilt.
     */
    fun install(context: Context) {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                appendLine(
                    context,
                    "EXCEPCIÓN NO MANEJADA en hilo '${thread.name}':\n${throwable.stackTraceToText()}",
                )
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    fun readLog(context: Context): String? =
        logFile(context).takeIf { it.exists() }?.readText()?.ifBlank { null }

    fun clear(context: Context) {
        logFile(context).delete()
    }

    private fun appendLine(context: Context, message: String) {
        runCatching {
            logFile(context).appendText(
                "[${Instant.now()}] [${Build.MANUFACTURER} ${Build.MODEL}, API ${Build.VERSION.SDK_INT}] " +
                    "$message\n\n",
            )
        }
    }

    private fun logFile(context: Context): File = File(context.filesDir, FILE_NAME)

    private fun Throwable.stackTraceToText(): String {
        val writer = StringWriter()
        printStackTrace(PrintWriter(writer))
        return writer.toString()
    }
}
