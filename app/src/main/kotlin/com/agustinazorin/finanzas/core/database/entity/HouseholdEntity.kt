package com.agustinazorin.finanzas.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "households")
data class HouseholdEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val baseCurrency: String,
    val createdAt: Instant,
)
