package com.agustinazorin.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agustinazorin.finanzas.core.database.entity.CategoryRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryRuleDao {

    /** Reemplaza la regla existente para ese comercio: sólo puede haber una categoría por comercio normalizado. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: CategoryRuleEntity)

    @Query("SELECT * FROM category_rules ORDER BY merchantNormalized")
    fun observeAll(): Flow<List<CategoryRuleEntity>>

    @Query("SELECT * FROM category_rules WHERE merchantNormalized = :merchantNormalized")
    suspend fun findByMerchant(merchantNormalized: String): CategoryRuleEntity?

    @Query("DELETE FROM category_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
