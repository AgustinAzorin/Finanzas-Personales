package com.agustinazorin.finanzas.engine.capture

import com.agustinazorin.finanzas.engine.capture.parsers.GenericBankNotificationParser
import com.agustinazorin.finanzas.engine.capture.parsers.MercadoPagoNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationParserRegistryTest {

    private val registry = NotificationParserRegistry(
        specificParsers = listOf(MercadoPagoNotificationParser()),
        fallbackParser = GenericBankNotificationParser(),
    )

    @Test
    fun `usa el parser especifico cuando el packageName matchea`() {
        val result = registry.parse(
            packageName = "com.mercadopago.wallet",
            title = "Compraste \$500,00",
            text = "en Kiosco",
        )
        checkNotNull(result)
        assertEquals("mercado_pago", result.parserId)
    }

    @Test
    fun `usa el fallback para apps sin parser especifico habilitadas por el usuario`() {
        val result = registry.parse(
            packageName = "com.bancogenerico.app",
            title = "Compra realizada",
            text = "Compra por \$300,00 en Farmacia",
        )
        checkNotNull(result)
        assertEquals("generic_bank", result.parserId)
    }

    @Test
    fun `no usa el fallback si el packageName es de un parser especifico pero el texto no matchea`() {
        val result = registry.parse(
            packageName = "com.mercadopago.wallet",
            title = "Novedades",
            text = "Mirá los nuevos beneficios",
        )
        assertNull(result)
    }

    @Test
    fun `devuelve null si ningun parser interpreta el texto`() {
        assertNull(registry.parse(packageName = "com.otraapp", title = "Hola", text = "como estas"))
    }
}
