package com.agustinazorin.finanzas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agustinazorin.finanzas.core.database.entity.CreditCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {

    /** Una tarjeta es 1 a 1 con su cuenta: reconfigurar (cambiar cierre/vencimiento/límite) reemplaza la fila existente. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(creditCard: CreditCardEntity)

    @Query("SELECT * FROM credit_cards WHERE accountId = :accountId")
    suspend fun getByAccountId(accountId: Long): CreditCardEntity?

    @Query("SELECT * FROM credit_cards WHERE accountId = :accountId")
    fun observeByAccountId(accountId: Long): Flow<CreditCardEntity?>

    @Query("SELECT * FROM credit_cards")
    fun observeAll(): Flow<List<CreditCardEntity>>
}
