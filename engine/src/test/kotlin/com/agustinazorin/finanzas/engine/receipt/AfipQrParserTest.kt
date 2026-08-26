package com.agustinazorin.finanzas.engine.receipt

import java.math.BigDecimal
import java.time.LocalDate
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AfipQrParserTest {

    private fun buildQrUrl(json: String): String {
        val encoded = Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
        return "https://www.afip.gob.ar/fe/qr/?p=$encoded"
    }

    @Test
    fun `parsea un QR valido de AFIP`() {
        val json = """
            {"ver":1,"fecha":"2024-03-15","cuit":30712345678,"ptoVta":5,"tipoCmp":6,"nroCmp":123,
            "importe":15000.50,"moneda":"PES","ctz":1,"tipoDocRec":80,"nroDocRec":20123456789,
            "tipoCodAut":"E","codAut":70123456789012}
        """.trimIndent().replace("\n", "")

        val result = AfipQrParser.parse(buildQrUrl(json))

        assertEquals(
            AfipReceiptData(
                date = LocalDate.of(2024, 3, 15),
                cuitEmisor = 30712345678L,
                pointOfSale = 5,
                invoiceType = 6,
                invoiceNumber = 123L,
                amount = BigDecimal("15000.50"),
                currency = "PES",
                exchangeRate = BigDecimal("1"),
                authorizationCode = 70123456789012L,
            ),
            result,
        )
    }

    @Test
    fun `usa moneda PES y cotizacion 1 por defecto si no vienen en el payload`() {
        val json = """{"fecha":"2024-01-01","cuit":1,"ptoVta":1,"tipoCmp":1,"nroCmp":1,"importe":100,"codAut":1}"""

        val result = AfipQrParser.parse(buildQrUrl(json))

        assertEquals("PES", result?.currency)
        assertEquals(BigDecimal.ONE, result?.exchangeRate)
    }

    @Test
    fun `devuelve null si la url no es de afip`() {
        assertNull(AfipQrParser.parse("https://example.com/no-es-afip"))
    }

    @Test
    fun `devuelve null si falta el parametro p`() {
        assertNull(AfipQrParser.parse("https://www.afip.gob.ar/fe/qr/?otra=cosa"))
    }

    @Test
    fun `devuelve null si al payload le faltan campos requeridos`() {
        val json = """{"fecha":"2024-01-01","cuit":1}"""
        assertNull(AfipQrParser.parse(buildQrUrl(json)))
    }

    @Test
    fun `devuelve null para un qr que no tiene forma de url de afip`() {
        assertNull(AfipQrParser.parse("cualquier texto que no sea una url"))
    }
}
