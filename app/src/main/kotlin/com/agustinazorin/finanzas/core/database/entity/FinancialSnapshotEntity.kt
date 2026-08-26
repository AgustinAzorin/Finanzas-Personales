package com.agustinazorin.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * Foto periódica del patrimonio de un hogar (CLAUDE.md, sección 22), guardada explícitamente en
 * vez de recalculada hacia atrás: a diferencia de las cuentas, los Asset/Liability independientes
 * no tienen historial de transacciones que reproducir (ver KDoc de
 * [com.agustinazorin.finanzas.engine.networth.NetWorthCalculator]). Esto permite reconstruir la
 * evolución histórica del patrimonio (sección 27) sin inventar valores pasados que no se guardaron.
 *
 * Una sola moneda por snapshot ([currency], en la práctica siempre la moneda base del hogar):
 * igual que el resto de las pantallas de este MVP, la conversión multi-moneda queda para Fase 6.
 *
 * El índice único (householdId, date, currency) hace que grabar un snapshot el mismo día sea
 * idempotente: ver [com.agustinazorin.finanzas.core.database.dao.FinancialSnapshotDao.upsert].
 */
@Entity(
    tableName = "financial_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = HouseholdEntity::class,
            parentColumns = ["id"],
            childColumns = ["householdId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("householdId"), Index(value = ["householdId", "date", "currency"], unique = true)],
)
data class FinancialSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val householdId: Long,
    val date: LocalDate,
    val currency: String,
    val netWorth: Long,
    val totalAssets: Long,
    val totalLiabilities: Long,
    val availableLiquidity: Long,
    val createdAt: Instant,
)
