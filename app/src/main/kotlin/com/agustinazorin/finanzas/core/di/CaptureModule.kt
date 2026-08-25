package com.agustinazorin.finanzas.core.di

import com.agustinazorin.finanzas.engine.capture.NotificationParser
import com.agustinazorin.finanzas.engine.capture.NotificationParserRegistry
import com.agustinazorin.finanzas.engine.capture.parsers.GenericBankNotificationParser
import com.agustinazorin.finanzas.engine.capture.parsers.MercadoPagoNotificationParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Cablea el motor de parsers de notificaciones (CLAUDE.md, sección 37). Agregar un nuevo banco
 * soportado explícitamente sólo requiere sumarlo a [specificParsers]; cualquier otra app la cubre
 * el [GenericBankNotificationParser] si el usuario la habilita en la pantalla de captura.
 */
@Module
@InstallIn(SingletonComponent::class)
object CaptureModule {

    @Provides
    @Singleton
    fun provideNotificationParserRegistry(): NotificationParserRegistry {
        val specificParsers: List<NotificationParser> = listOf(MercadoPagoNotificationParser())
        return NotificationParserRegistry(
            specificParsers = specificParsers,
            fallbackParser = GenericBankNotificationParser(),
        )
    }
}
