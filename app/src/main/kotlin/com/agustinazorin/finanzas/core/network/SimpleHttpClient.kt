package com.agustinazorin.finanzas.core.network

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente HTTP mínimo para las únicas llamadas de red que la app hace (CLAUDE.md, sección 2):
 * APIs públicas de solo lectura para cotizaciones e inflación. No se agrega Retrofit/OkHttp ni un
 * parser JSON externo (Gson/Moshi/kotlinx-serialization): `HttpURLConnection` y `org.json` ya
 * vienen con el SDK de Android, así que esto evita dependencias nuevas para un uso tan acotado
 * (Definition of Done: "no introduce dependencias innecesarias").
 */
internal fun httpGetJson(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 10_000
    connection.readTimeout = 10_000
    connection.setRequestProperty("Accept", "application/json")
    try {
        val code = connection.responseCode
        if (code !in 200..299) {
            throw IOException("HTTP $code al consultar $url")
        }
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
