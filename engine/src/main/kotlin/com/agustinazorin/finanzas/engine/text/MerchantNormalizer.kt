package com.agustinazorin.finanzas.engine.text

import java.text.Normalizer

private val DIACRITICS_REGEX = Regex("\\p{Mn}+")
private val NON_ALPHANUMERIC_REGEX = Regex("[^A-Z0-9 ]")
private val MULTIPLE_SPACES_REGEX = Regex(" +")
private val TRAILING_NUMERIC_CODE_REGEX = Regex(" [0-9]{3,}$")

/** Prefijos ruidosos que agregan pasarelas de pago y no forman parte del nombre del comercio. */
private val KNOWN_NOISE_PREFIXES = listOf(
    "MERCADOPAGO*",
    "MERCADOPAGO *",
    "MERCADO PAGO*",
    "MP*",
    "MP *",
    "PAGO A ",
    "PAGO EN ",
    "PAGO ",
    "COMPRA EN ",
    "COMPRA ",
)

/**
 * Normaliza un nombre de comercio para que capturas de distintas fuentes (notificación, manual,
 * importación, QR) puedan compararse entre sí y para que las reglas de categorización (CLAUDE.md,
 * sección 39) y la conciliación (sección 38) sean estables ante variaciones de mayúsculas,
 * acentos, o sufijos numéricos de sucursal/terminal.
 *
 * La normalización es intencionalmente conservadora: sólo colapsa variaciones tipográficas
 * conocidas, nunca intenta adivinar significado (ej. no traduce ni abrevia nombres de comercios).
 */
object MerchantNormalizer {

    fun normalize(raw: String): String {
        var value = raw.trim().uppercase()
        value = Normalizer.normalize(value, Normalizer.Form.NFD).replace(DIACRITICS_REGEX, "")

        for (prefix in KNOWN_NOISE_PREFIXES) {
            if (value.startsWith(prefix)) {
                value = value.removePrefix(prefix)
                break
            }
        }

        value = value.replace(NON_ALPHANUMERIC_REGEX, " ").replace(MULTIPLE_SPACES_REGEX, " ").trim()
        value = value.replace(TRAILING_NUMERIC_CODE_REGEX, "").trim()
        return value
    }
}
