package com.agustinazorin.finanzas.core.ui.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

private val ARGENTINE_LOCALE = Locale.Builder().setLanguage("es").setRegion("AR").build()

/**
 * Convierte un monto en unidades mínimas (centavos) a texto legible, ej: 150000 -> "$1.500,00".
 * Nunca se usa Float/Double para el monto en sí: sólo BigDecimal, exclusivamente para formateo.
 */
fun Long.formatAsMoney(currency: String): String {
    val symbol = when (currency) {
        "ARS" -> "$"
        "USD" -> "US$"
        else -> "$currency "
    }
    val format = NumberFormat.getNumberInstance(ARGENTINE_LOCALE).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    val major = BigDecimal(this).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
    val text = format.format(major)
    return if (this < 0) "-$symbol${text.removePrefix("-")}" else "$symbol$text"
}

/** Parsea el texto ingresado por el usuario (con coma decimal, ej: "1.500,50") a unidades mínimas. */
fun parseMoneyInput(text: String): Long? {
    if (text.isBlank()) return null
    val symbols = DecimalFormatSymbols(ARGENTINE_LOCALE)
    val normalized = text
        .trim()
        .replace(symbols.groupingSeparator.toString(), "")
        .replace(symbols.decimalSeparator, '.')
    val value = normalized.toBigDecimalOrNull() ?: return null
    return value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
}
