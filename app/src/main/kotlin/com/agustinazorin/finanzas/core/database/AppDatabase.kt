package com.agustinazorin.finanzas.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.agustinazorin.finanzas.core.database.dao.AccountDao
import com.agustinazorin.finanzas.core.database.dao.CategoryDao
import com.agustinazorin.finanzas.core.database.dao.HouseholdDao
import com.agustinazorin.finanzas.core.database.dao.HouseholdMemberDao
import com.agustinazorin.finanzas.core.database.dao.RecurringTransactionDao
import com.agustinazorin.finanzas.core.database.dao.TransactionDao
import com.agustinazorin.finanzas.core.database.entity.AccountEntity
import com.agustinazorin.finanzas.core.database.entity.CategoryEntity
import com.agustinazorin.finanzas.core.database.entity.HouseholdEntity
import com.agustinazorin.finanzas.core.database.entity.HouseholdMemberEntity
import com.agustinazorin.finanzas.core.database.entity.RecurringTransactionEntity
import com.agustinazorin.finanzas.core.database.entity.TransactionEntity

const val DATABASE_NAME = "finanzas.db"

@Database(
    entities = [
        HouseholdEntity::class,
        HouseholdMemberEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        RecurringTransactionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun householdDao(): HouseholdDao
    abstract fun householdMemberDao(): HouseholdMemberDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
}
