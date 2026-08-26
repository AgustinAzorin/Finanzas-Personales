package com.agustinazorin.finanzas.engine.text

import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantNormalizerTest {

    @Test
    fun `ignora mayusculas y acentos`() {
        assertEquals("CAFE MARTINEZ", MerchantNormalizer.normalize("Café Martínez"))
        assertEquals("CAFE MARTINEZ", MerchantNormalizer.normalize("  cafe martinez  "))
    }

    @Test
    fun `quita prefijos de pasarela de pago`() {
        assertEquals("CAFE MARTINEZ", MerchantNormalizer.normalize("MERCADOPAGO*CAFE MARTINEZ"))
        assertEquals("CAFE MARTINEZ", MerchantNormalizer.normalize("MP*CAFE MARTINEZ"))
        assertEquals("JUAN PEREZ", MerchantNormalizer.normalize("PAGO A JUAN PEREZ"))
    }

    @Test
    fun `quita codigo numerico de sucursal al final`() {
        assertEquals("CARREFOUR", MerchantNormalizer.normalize("Carrefour 4521"))
        assertEquals("CARREFOUR", MerchantNormalizer.normalize("CARREFOUR 001234"))
    }

    @Test
    fun `no quita numeros que forman parte del nombre`() {
        assertEquals("KFC 24", MerchantNormalizer.normalize("KFC 24"))
    }

    @Test
    fun `distintas variantes del mismo comercio normalizan igual`() {
        val a = MerchantNormalizer.normalize("MERCADOPAGO*McDonald's")
        val b = MerchantNormalizer.normalize("mcdonald's")
        assertEquals(a, b)
    }
}
