package com.agustinazorin.finanzas.engine.capture.parsers

import com.agustinazorin.finanzas.engine.capture.NotificationParser
import com.agustinazorin.finanzas.engine.capture.ParsedNotificationTransaction
import com.agustinazorin.finanzas.engine.capture.extractMerchant
import com.agustinazorin.finanzas.engine.capture.findAmountMatch
import com.agustinazorin.finanzas.engine.capture.foldForMatching
import com.agustinazorin.finanzas.engine.capture.parseArgentineAmountToMinorUnits
import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.engine.text.MerchantNormalizer

private val INFLOW_KEYWORDS = listOf("acreditaron", "se acredito", "recibiste", "depositaron", "te transfirieron")
private val OUTFLOW_KEYWORDS = listOf(
    "compra", "compraste", "pagaste", "debitaron", "se debito", "extraccion", "extrajiste", "transferiste",
)

/**
 * Parser de respaldo para apps bancarias no listadas explícitamente (CLAUDE.md, sección 37): los
 * bancos argentinos no comparten un formato único de notificación, así que en vez de hardcodear
 * cada packageName, este parser busca los patrones en español más frecuentes ("Compra por $X en
 * Y", "Se debitaron $X", "Se acreditó un pago por $X"). El usuario elige explícitamente qué apps
 * escuchar (ver pantalla de configuración de captura), así que sólo se invoca sobre apps que él
 * mismo habilitó.
 */
class GenericBankNotificationParser : NotificationParser {
    override val id: String = "generic_bank"
    override val version: Int = 1
    override val packageNames: Set<String> = emptySet()

    override fun parse(title: String?, text: String?): ParsedNotificationTransaction? {
        val combined = listOfNotNull(title, text).joinToString(" ").trim()
        if (combined.isBlank()) return null
        val folded = foldForMatching(combined)

        val direction = when {
            OUTFLOW_KEYWORDS.any { folded.contains(it) } -> TransactionDirection.OUTFLOW
            INFLOW_KEYWORDS.any { folded.contains(it) } -> TransactionDirection.INFLOW
            else -> return null
        }

        val amountMatch = findAmountMatch(combined) ?: return null
        val minorUnits = parseArgentineAmountToMinorUnits(amountMatch.groupValues[1]) ?: return null
        if (minorUnits <= 0) return null

        val merchant = extractMerchant(combined.substring(amountMatch.range.last + 1))

        return ParsedNotificationTransaction(
            amount = Money(minorUnits, "ARS"),
            direction = direction,
            merchant = merchant,
            merchantNormalized = merchant?.let { MerchantNormalizer.normalize(it) },
        )
    }
}
