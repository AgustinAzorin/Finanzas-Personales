package com.agustinazorin.finanzas.core.network

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class DolarBlueQuote(val compra: BigDecimal, val venta: BigDecimal, val date: LocalDate)

/**
 * Cotización del dólar blue vía dolarapi.com (API pública, de solo lectura, sin autenticación).
 * Nunca se llama automáticamente: sólo cuando el usuario toca "Actualizar" (CLAUDE.md, sección 2
 * — las llamadas de red son opcionales, la app funciona entera en modo avión). Cualquier falla de
 * red se devuelve como [Result.failure] en vez de lanzar, para que la UI la muestre sin romper
 * nada (Definition of Done: "maneja errores", "funciona offline").
 */
class DolarApiClient @Inject constructor() {

    suspend fun fetchBlueQuote(): Result<DolarBlueQuote> = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONObject(httpGetJson("https://dolarapi.com/v1/dolares/blue"))
            val date = runCatching {
                Instant.parse(json.getString("fechaActualizacion")).atZone(ZoneOffset.UTC).toLocalDate()
            }.getOrDefault(LocalDate.now())
            DolarBlueQuote(
                compra = BigDecimal.valueOf(json.getDouble("compra")),
                venta = BigDecimal.valueOf(json.getDouble("venta")),
                date = date,
            )
        }
    }
}
