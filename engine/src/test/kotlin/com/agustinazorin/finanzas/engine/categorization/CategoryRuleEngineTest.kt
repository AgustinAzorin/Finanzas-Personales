package com.agustinazorin.finanzas.engine.categorization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryRuleEngineTest {

    private val rules = mapOf("MCDONALDS" to 7L, "CAFE MARTINEZ" to 3L)

    @Test
    fun `sugiere la categoria de una regla exacta`() {
        assertEquals(7L, CategoryRuleEngine.suggestCategory("MCDONALDS", rules))
    }

    @Test
    fun `no sugiere nada si no hay regla para ese comercio`() {
        assertNull(CategoryRuleEngine.suggestCategory("KIOSCO", rules))
    }

    @Test
    fun `no sugiere nada si el comercio es null o vacio`() {
        assertNull(CategoryRuleEngine.suggestCategory(null, rules))
        assertNull(CategoryRuleEngine.suggestCategory("", rules))
    }

    @Test
    fun `no matchea parcialmente, solo exacto`() {
        assertNull(CategoryRuleEngine.suggestCategory("MCDONALDS 24", rules))
    }
}
