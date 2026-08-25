package com.agustinazorin.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.agustinazorin.finanzas.core.database.entity.HouseholdEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdDao {

    @Insert
    suspend fun insert(household: HouseholdEntity): Long

    @Query("SELECT * FROM households LIMIT 1")
    fun observeDefaultHousehold(): Flow<HouseholdEntity?>

    @Query("SELECT * FROM households LIMIT 1")
    suspend fun getDefaultHousehold(): HouseholdEntity?
}
