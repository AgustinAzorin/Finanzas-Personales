package com.agustinazorin.finanzas.engine.capture

import java.text.Normalizer

private val DIACRITICS_REGEX = Regex("\\p{Mn}+")
private val AMOUNT_REGEX = Regex("\\$\\s*([0-9][0-9.,]*)")
private val CARD_SUFFIX_REGEX = Regex("\\bcon tarjeta\\b.*", RegexOption.IGNORE_CASE)
private val MERCHANT_REGEX = Regex("(?:\\ben\\b|\\ba\\b|\\bde\\b)\\s+(.+)$", RegexOption.IGNORE_CASE)

/** Minúsculas y sin acentos, para hacer matching de palabras clave insensible a tildes. */
internal fun foldForMatching(text: String): String =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(DIACRITICS_REGEX, "")

/**
 * Interpreta un monto en formato argentino ("$1.234,56" -> 123456 centavos) sin pasar por
 * Float/Double en ningún momento (CLAUDE.md, sección 6): separa entera/decimal a mano y opera
 * sobre enteros.
 */
internal fun parseArgentineAmountToMinorUnits(raw: String): Long? {
    val cleaned = raw.trim().removePrefix("$").trim()
    if (cleaned.isEmpty()) return null

    val commaIndex = cleaned.lastIndexOf(',')
    val integerPart = if (commaIndex >= 0) cleaned.substring(0, commaIndex) else cleaned
    val decimalPart = if (commaIndex >= 0) cleaned.substring(commaIndex + 1) else ""

    val integerDigits = integerPart.filter { it.isDigit() }
    if (integerDigits.isEmpty()) return null
    val units = integerDigits.toLongOrNull() ?: return null

    val decimalDigits = decimalPart.filter { it.isDigit() }.padEnd(2, '0').take(2)
    val cents = decimalDigits.toLongOrNull() ?: 0L

    return units * 100 + cents
}

/** Encuentra el primer monto en pesos dentro de [text] junto con el rango que ocupa. */
internal fun findAmountMatch(text: String): MatchResult? = AMOUNT_REGEX.find(text)

/**
 * Extrae el nombre de comercio de lo que sigue al monto, asumiendo el patrón habitual
 * "... en/a/de NOMBRE [con tarjeta ...]" de las notificaciones de pago en Argentina.
 */
internal fun extractMerchant(afterAmount: String): String? {
    val withoutCardSuffix = afterAmount.replace(CARD_SUFFIX_REGEX, "").trim()
    return MERCHANT_REGEX.find(withoutCardSuffix)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
}
