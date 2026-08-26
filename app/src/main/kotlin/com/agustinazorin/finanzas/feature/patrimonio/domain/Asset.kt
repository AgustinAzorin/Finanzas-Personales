package com.agustinazorin.finanzas.feature.patrimonio.domain

import com.agustinazorin.finanzas.engine.model.AssetCategory
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class Asset(
    val id: Long,
    val householdId: Long,
    val ownerMemberId: Long?,
    val name: String,
    val category: AssetCategory,
    val currency: String,
    val currentValue: Long,
    val valuationDate: LocalDate,
    val isActive: Boolean,
)

interface AssetRepository {
    fun observeAssets(householdId: Long): Flow<List<Asset>>
    fun observeActiveAssets(householdId: Long): Flow<List<Asset>>

    suspend fun createAsset(
        householdId: Long,
        ownerMemberId: Long?,
        name: String,
        category: AssetCategory,
        currency: String,
        currentValue: Long,
        valuationDate: LocalDate,
    ): Long

    suspend fun setAssetActive(assetId: Long, isActive: Boolean)
}
