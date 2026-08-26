package com.agustinazorin.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.agustinazorin.finanzas.core.database.entity.HouseholdMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdMemberDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(member: HouseholdMemberEntity): Long

    @Update
    suspend fun update(member: HouseholdMemberEntity)

    @Query("SELECT * FROM household_members WHERE householdId = :householdId ORDER BY name")
    fun observeMembers(householdId: Long): Flow<List<HouseholdMemberEntity>>

    @Query("SELECT * FROM household_members WHERE householdId = :householdId AND isActive = 1 ORDER BY name")
    fun observeActiveMembers(householdId: Long): Flow<List<HouseholdMemberEntity>>

    @Query("SELECT * FROM household_members WHERE id = :id")
    suspend fun getById(id: Long): HouseholdMemberEntity?
}
