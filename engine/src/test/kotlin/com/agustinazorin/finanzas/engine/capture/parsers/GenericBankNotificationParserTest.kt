package com.agustinazorin.finanzas.engine.capture.parsers

import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GenericBankNotificationParserTest {

    private val parser = GenericBankNotificationParser()

    @Test
    fun `interpreta una compra con tarjeta`() {
        val result = parser.parse(
            title = "Nueva compra",
            text = "Compra por \$1.234,56 en COMERCIO SA con tarjeta terminada en 1234",
        )
        checkNotNull(result)
        assertEquals(Money(123456, "ARS"), result.amount)
        assertEquals(TransactionDirection.OUTFLOW, result.direction)
        assertEquals("COMERCIO SA", result.merchant)
    }

    @Test
    fun `interpreta un debito`() {
        val result = parser.parse(title = null, text = "Se debitaron \$450,00 de tu cuenta")
        checkNotNull(result)
        assertEquals(TransactionDirection.OUTFLOW, result.direction)
    }

    @Test
    fun `interpreta un acreditamiento como ingreso`() {
        val result = parser.parse(title = "Acreditación", text = "Se acreditó un pago por \$2.000,00")
        checkNotNull(result)
        assertEquals(TransactionDirection.INFLOW, result.direction)
    }

    @Test
    fun `devuelve null para textos sin palabras clave conocidas`() {
        assertNull(parser.parse(title = "Tu resumen ya está disponible", text = "Consultalo en la app"))
    }

    @Test
    fun `no reclama ningun packageName especifico`() {
        assert(parser.packageNames.isEmpty())
    }
}
