package com.agustinazorin.finanzas.feature.patrimonio.data

import com.agustinazorin.finanzas.core.database.dao.AssetDao
import com.agustinazorin.finanzas.core.database.entity.AssetEntity
import com.agustinazorin.finanzas.engine.model.AssetCategory
import com.agustinazorin.finanzas.feature.patrimonio.domain.Asset
import com.agustinazorin.finanzas.feature.patrimonio.domain.AssetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class AssetRepositoryImpl @Inject constructor(
    private val dao: AssetDao,
) : AssetRepository {

    override fun observeAssets(householdId: Long): Flow<List<Asset>> =
        dao.observeAssets(householdId).map { list -> list.map { it.toDomain() } }

    override fun observeActiveAssets(householdId: Long): Flow<List<Asset>> =
        dao.observeActiveAssets(householdId).map { list -> list.map { it.toDomain() } }

    override suspend fun createAsset(
        householdId: Long,
        ownerMemberId: Long?,
        name: String,
        category: AssetCategory,
        currency: String,
        currentValue: Long,
        valuationDate: LocalDate,
    ): Long = dao.insert(
        AssetEntity(
            householdId = householdId,
            ownerMemberId = ownerMemberId,
            name = name,
            category = category,
            currency = currency,
            currentValue = currentValue,
            valuationDate = valuationDate,
            isActive = true,
        ),
    )

    override suspend fun setAssetActive(assetId: Long, isActive: Boolean) {
        val entity = dao.getById(assetId) ?: return
        dao.update(entity.copy(isActive = isActive))
    }
}

private fun AssetEntity.toDomain() = Asset(
    id = id, householdId = householdId, ownerMemberId = ownerMemberId, name = name, category = category,
    currency = currency, currentValue = currentValue, valuationDate = valuationDate, isActive = isActive,
)
