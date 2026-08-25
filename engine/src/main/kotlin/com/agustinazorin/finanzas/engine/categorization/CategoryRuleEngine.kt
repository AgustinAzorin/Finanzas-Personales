package com.agustinazorin.finanzas.engine.categorization

/**
 * Sugiere una categoría para un comercio a partir de reglas aprendidas de correcciones previas
 * del usuario (CLAUDE.md, sección 39: "merchantNormalized = 'MCDONALDS' -> category = RESTAURANTES").
 * Sólo hace matching exacto sobre el nombre ya normalizado: nada de heurísticas difusas que
 * puedan categorizar mal en silencio. Las reglas son transparentes y editables (ver
 * CategoryRuleRepository en :app); esta función es pura para poder testearla sin Room.
 */
object CategoryRuleEngine {
    fun suggestCategory(merchantNormalized: String?, rulesByMerchant: Map<String, Long>): Long? {
        if (merchantNormalized.isNullOrBlank()) return null
        return rulesByMerchant[merchantNormalized]
    }
}
