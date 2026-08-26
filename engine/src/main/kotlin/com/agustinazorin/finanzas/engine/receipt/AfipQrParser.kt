package com.agustinazorin.finanzas.engine.receipt

import java.math.BigDecimal
import java.net.URLDecoder
import java.time.LocalDate
import java.util.Base64

/**
 * Datos estructurados de un comprobante electrónico argentino (Factura AFIP/ARCA), extraídos del
 * QR que la normativa (RG 4892/2020) exige imprimir en cada comprobante (CLAUDE.md, sección 40).
 */
data class AfipReceiptData(
    val date: LocalDate,
    val cuitEmisor: Long,
    val pointOfSale: Int,
    val invoiceType: Int,
    val invoiceNumber: Long,
    val amount: BigDecimal,
    val currency: String,
    val exchangeRate: BigDecimal,
    val authorizationCode: Long,
)

/**
 * Parsea el QR de un comprobante AFIP/ARCA: una URL con la forma
 * `https://www.afip.gob.ar/fe/qr/?p=<JSON codificado en Base64>` (CLAUDE.md, sección 40 — "nunca
 * depender exclusivamente del OCR si existe información estructurada en el QR").
 *
 * El JSON del payload es siempre un objeto plano (sin anidamiento) con un conjunto fijo de
 * claves, así que en vez de sumar una dependencia de JSON sólo para esto, se extraen los campos
 * con una expresión regular acotada a esa forma conocida.
 */
object AfipQrParser {
    private const val AFIP_HOST_MARKER = "afip.gob.ar"
    private val FLAT_JSON_FIELD = Regex(""""([a-zA-Z0-9_]+)"\s*:\s*("([^"]*)"|-?[0-9]+(?:\.[0-9]+)?)""")

    /** Devuelve `null` si [rawQrContent] no tiene la forma de un QR de AFIP/ARCA o le faltan campos. */
    fun parse(rawQrContent: String): AfipReceiptData? {
        if (!rawQrContent.contains(AFIP_HOST_MARKER, ignoreCase = true)) return null
        val payload = extractQueryParam(rawQrContent, "p") ?: return null
        val json = decodeBase64(payload) ?: return null
        val fields = extractFlatJsonFields(json)
        return runCatching {
            AfipReceiptData(
                date = LocalDate.parse(fields.getValue("fecha")),
                cuitEmisor = fields.getValue("cuit").toLong(),
                pointOfSale = fields.getValue("ptoVta").toInt(),
                invoiceType = fields.getValue("tipoCmp").toInt(),
                invoiceNumber = fields.getValue("nroCmp").toLong(),
                amount = fields.getValue("importe").toBigDecimal(),
                currency = fields["moneda"] ?: "PES",
                exchangeRate = fields["ctz"]?.toBigDecimal() ?: BigDecimal.ONE,
                authorizationCode = fields.getValue("codAut").toLong(),
            )
        }.getOrNull()
    }

    private fun decodeBase64(payload: String): String? =
        runCatching { String(Base64.getDecoder().decode(payload), Charsets.UTF_8) }
            .recoverCatching { String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8) }
            .getOrNull()

    private fun extractQueryParam(url: String, name: String): String? {
        val queryStart = url.indexOf('?')
        if (queryStart == -1) return null
        return url.substring(queryStart + 1)
            .split('&')
            .map { it.split('=', limit = 2) }
            .firstOrNull { it.size == 2 && it[0] == name }
            ?.get(1)
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
    }

    private fun extractFlatJsonFields(json: String): Map<String, String> =
        FLAT_JSON_FIELD.findAll(json).associate { match ->
            val key = match.groupValues[1]
            val rawValue = match.groupValues[2]
            val value = if (rawValue.startsWith("\"")) match.groupValues[3] else rawValue
            key to value
        }
}
