package com.agustinazorin.finanzas.engine.capture.parsers

import com.agustinazorin.finanzas.engine.model.TransactionDirection
import com.agustinazorin.finanzas.engine.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MercadoPagoNotificationParserTest {

    private val parser = MercadoPagoNotificationParser()

    @Test
    fun `interpreta una compra como egreso`() {
        val result = parser.parse(title = "Compraste $1.234,56", text = "en Café Martínez")
        checkNotNull(result)
        assertEquals(Money(123456, "ARS"), result.amount)
        assertEquals(TransactionDirection.OUTFLOW, result.direction)
        assertEquals("Café Martínez", result.merchant)
        assertEquals("CAFE MARTINEZ", result.merchantNormalized)
    }

    @Test
    fun `interpreta un pago recibido como ingreso`() {
        val result = parser.parse(title = "Recibiste un pago", text = "Recibiste un pago de \$500,00 de Juan Pérez")
        checkNotNull(result)
        assertEquals(Money(50000, "ARS"), result.amount)
        assertEquals(TransactionDirection.INFLOW, result.direction)
        assertEquals("Juan Pérez", result.merchant)
    }

    @Test
    fun `devuelve null si no hay monto`() {
        assertNull(parser.parse(title = "Compraste algo", text = "en un lugar"))
    }

    @Test
    fun `devuelve null para notificaciones que no son de pago`() {
        assertNull(parser.parse(title = "Novedades", text = "Descubrí los nuevos beneficios de Mercado Pago"))
    }

    @Test
    fun `funciona sin comercio explicito`() {
        val result = parser.parse(title = "Compraste \$500,00", text = null)
        checkNotNull(result)
        assertEquals(TransactionDirection.OUTFLOW, result.direction)
        assertNull(result.merchant)
    }
}
