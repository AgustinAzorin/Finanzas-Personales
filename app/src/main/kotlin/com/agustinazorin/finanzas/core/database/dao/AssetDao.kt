package com.agustinazorin.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.agustinazorin.finanzas.core.database.entity.AssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {

    @Insert
    suspend fun insert(asset: AssetEntity): Long

    @Update
    suspend fun update(asset: AssetEntity)

    @Query("SELECT * FROM assets WHERE householdId = :householdId ORDER BY name")
    fun observeAssets(householdId: Long): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE householdId = :householdId AND isActive = 1 ORDER BY name")
    fun observeActiveAssets(householdId: Long): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun getById(id: Long): AssetEntity?
}
