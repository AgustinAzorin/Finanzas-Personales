package com.agustinazorin.finanzas.core.network

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class InflacionMensual(val month: YearMonth, val percent: BigDecimal)

/**
 * Serie de inflación mensual (INDEC/IPC) vía api.argentinadatos.com (API pública, de solo
 * lectura, sin autenticación). Mismo criterio que [DolarApiClient]: solo se llama por acción
 * explícita del usuario, nunca automáticamente, y una falla de red nunca debe romper la app.
 */
class InflacionApiClient @Inject constructor() {

    suspend fun fetchMonthlyInflation(): Result<List<InflacionMensual>> = withContext(Dispatchers.IO) {
        runCatching {
            val array = JSONArray(httpGetJson("https://api.argentinadatos.com/v1/finanzas/indices/inflacion"))
            (0 until array.length()).map { index ->
                val entry = array.getJSONObject(index)
                InflacionMensual(
                    month = YearMonth.from(LocalDate.parse(entry.getString("fecha"))),
                    percent = BigDecimal.valueOf(entry.getDouble("valor")),
                )
            }
        }
    }
}
