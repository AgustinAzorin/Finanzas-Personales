package com.agustinazorin.finanzas.engine.capture

import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.money.Money

/**
 * Resultado de interpretar el texto de una notificación (CLAUDE.md, sección 37). Es sólo una
 * candidata: nunca se persiste directamente como Transaction, siempre pasa primero por revisión
 * y confirmación del usuario (ver CapturedNotification en el módulo :app).
 */
data class ParsedNotificationTransaction(
    val amount: Money,
    val direction: TransactionDirection,
    val merchant: String?,
    val merchantNormalized: String?,
)

/**
 * Contrato común para todo parser de notificaciones (CLAUDE.md, sección 37). Cada parser sólo
 * sabe interpretar el texto de las apps declaradas en [packageNames]; nunca debe adivinar
 * información que el texto no contiene explícitamente.
 */
interface NotificationParser {
    /** Identificador estable del parser; se persiste junto a cada captura para trazabilidad. */
    val id: String

    /** Se incrementa cuando cambia la lógica de extracción, para poder re-parsear históricos. */
    val version: Int

    /** Paquetes de Android cuyas notificaciones este parser sabe interpretar. Vacío = ninguno en particular (parser de respaldo). */
    val packageNames: Set<String>

    /** Devuelve null si el texto no matchea ningún patrón conocido de gasto/ingreso. */
    fun parse(title: String?, text: String?): ParsedNotificationTransaction?
}

/** Resultado de rutear una notificación a través de un [NotificationParserRegistry]. */
data class ParsedCapture(
    val parserId: String,
    val parserVersion: Int,
    val transaction: ParsedNotificationTransaction,
)

/**
 * Enruta cada notificación al parser correcto según su packageName (CLAUDE.md, sección 37).
 * Los parsers específicos (ej. Mercado Pago) tienen prioridad sobre [fallbackParser], que se usa
 * únicamente para paquetes que el usuario habilitó explícitamente pero que ningún parser
 * específico reclama: los bancos argentinos no comparten un formato único de notificación, así
 * que el fallback intenta patrones genéricos en español en vez de listar apps a ciegas.
 */
class NotificationParserRegistry(
    private val specificParsers: List<NotificationParser>,
    private val fallbackParser: NotificationParser,
) {
    fun parse(packageName: String, title: String?, text: String?): ParsedCapture? {
        val specific = specificParsers.filter { packageName in it.packageNames }
        for (parser in specific) {
            parser.parse(title, text)?.let { return ParsedCapture(parser.id, parser.version, it) }
        }
        if (specific.isNotEmpty()) return null

        return fallbackParser.parse(title, text)
            ?.let { ParsedCapture(fallbackParser.id, fallbackParser.version, it) }
    }
}
