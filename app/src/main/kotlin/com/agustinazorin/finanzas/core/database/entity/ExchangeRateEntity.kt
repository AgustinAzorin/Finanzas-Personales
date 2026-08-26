package com.agustinazorin.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.agustinazorin.finanzas.engine.model.RateSource
import java.time.Instant
import java.time.LocalDate

/**
 * Cotización de [currency] en [baseCurrency] para un día dado (CLAUDE.md, sección 41): cuántas
 * unidades de [baseCurrency] equivalen a 1 unidad de [currency].
 *
 * No pertenece a ningún hogar: es información pública compartida (como una tasa de cambio o un
 * índice de inflación), no un dato financiero personal. El índice único (currency, baseCurrency,
 * date) hace que cargar/actualizar la cotización de un mismo día sea idempotente (mismo patrón
 * que [FinancialSnapshotEntity]).
 */
@Entity(
    tableName = "exchange_rates",
    indices = [Index(value = ["currency", "baseCurrency", "date"], unique = true)],
)
data class ExchangeRateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val currency: String,
    val baseCurrency: String,
    val rate: Double,
    val date: LocalDate,
    val source: RateSource,
    val fetchedAt: Instant,
)
