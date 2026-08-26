package com.agustinazorin.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.agustinazorin.finanzas.core.database.entity.LiabilityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LiabilityDao {

    @Insert
    suspend fun insert(liability: LiabilityEntity): Long

    @Update
    suspend fun update(liability: LiabilityEntity)

    @Query("SELECT * FROM liabilities WHERE householdId = :householdId ORDER BY name")
    fun observeLiabilities(householdId: Long): Flow<List<LiabilityEntity>>

    @Query("SELECT * FROM liabilities WHERE householdId = :householdId AND isActive = 1 ORDER BY name")
    fun observeActiveLiabilities(householdId: Long): Flow<List<LiabilityEntity>>

    @Query("SELECT * FROM liabilities WHERE id = :id")
    suspend fun getById(id: Long): LiabilityEntity?
}
