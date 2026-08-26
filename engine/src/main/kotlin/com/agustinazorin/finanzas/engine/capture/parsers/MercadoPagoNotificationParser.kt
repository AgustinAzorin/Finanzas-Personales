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

private val INFLOW_KEYWORDS = listOf("recibiste", "te transfirieron", "te enviaron", "cobraste")
private val OUTFLOW_KEYWORDS = listOf("compraste", "pagaste", "hiciste un pago", "transferiste", "enviaste")

/**
 * Parser para notificaciones de Mercado Pago Argentina (CLAUDE.md, sección 37). Sólo interpreta
 * los textos de pago/cobro más comunes ("Compraste $X en Y", "Recibiste un pago de $X de Y");
 * cualquier otro texto (promociones, recordatorios, novedades) se ignora devolviendo null en vez
 * de arriesgar un dato incorrecto.
 */
class MercadoPagoNotificationParser : NotificationParser {
    override val id: String = "mercado_pago"
    override val version: Int = 1
    override val packageNames: Set<String> = setOf("com.mercadopago.wallet")

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
