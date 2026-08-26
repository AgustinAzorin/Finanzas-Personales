package com.agustinazorin.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.agustinazorin.finanzas.engine.model.RateSource
import java.time.Instant
import java.time.LocalDate

/**
 * Variación porcentual mensual de inflación (CLAUDE.md, sección 42), guardada exclusivamente
 * para análisis histórico: nunca modifica el monto nominal de ninguna transacción.
 *
 * [yearMonth] siempre guarda el día 1 del mes al que corresponde la variación, para poder
 * reutilizar el mismo [com.agustinazorin.finanzas.core.database.Converters] de [LocalDate] sin
 * agregar un tipo nuevo. No pertenece a ningún hogar: es un índice público compartido por toda
 * la app, igual que [ExchangeRateEntity].
 */
@Entity(
    tableName = "inflation_rates",
    indices = [Index(value = ["yearMonth"], unique = true)],
)
data class InflationRateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val yearMonth: LocalDate,
    val monthlyRatePercent: Double,
    val source: RateSource,
    val fetchedAt: Instant,
)
