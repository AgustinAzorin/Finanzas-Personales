package com.agustinazorin.finanzas.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.agustinazorin.finanzas.core.database.dao.AccountDao
import com.agustinazorin.finanzas.core.database.dao.CapturedNotificationDao
import com.agustinazorin.finanzas.core.database.dao.CategoryDao
import com.agustinazorin.finanzas.core.database.dao.CategoryRuleDao
import com.agustinazorin.finanzas.core.database.dao.CreditCardDao
import com.agustinazorin.finanzas.core.database.dao.CreditCardStatementDao
import com.agustinazorin.finanzas.core.database.dao.HouseholdDao
import com.agustinazorin.finanzas.core.database.dao.HouseholdMemberDao
import com.agustinazorin.finanzas.core.database.dao.InstallmentDao
import com.agustinazorin.finanzas.core.database.dao.RecurringTransactionDao
import com.agustinazorin.finanzas.core.database.dao.TransactionBeneficiaryDao
import com.agustinazorin.finanzas.core.database.dao.TransactionDao
import com.agustinazorin.finanzas.core.database.entity.AccountEntity
import com.agustinazorin.finanzas.core.database.entity.CapturedNotificationEntity
import com.agustinazorin.finanzas.core.database.entity.CategoryEntity
import com.agustinazorin.finanzas.core.database.entity.CategoryRuleEntity
import com.agustinazorin.finanzas.core.database.entity.CreditCardEntity
import com.agustinazorin.finanzas.core.database.entity.CreditCardStatementEntity
import com.agustinazorin.finanzas.core.database.entity.HouseholdEntity
import com.agustinazorin.finanzas.core.database.entity.HouseholdMemberEntity
import com.agustinazorin.finanzas.core.database.entity.InstallmentEntity
import com.agustinazorin.finanzas.core.database.entity.RecurringTransactionEntity
import com.agustinazorin.finanzas.core.database.entity.TransactionBeneficiaryEntity
import com.agustinazorin.finanzas.core.database.entity.TransactionEntity

const val DATABASE_NAME = "finanzas.db"

/**
 * La versión 1 todavía no salió de este repositorio (CLAUDE.md, sección 0: el sandbox de Claude
 * Code on the web nunca pudo compilar :app, así que Fase 0 nunca llegó a instalarse en un
 * dispositivo real ni exportó `schemas/1.json`). Por eso las entidades de Fase 1, Fase 2 y Fase 3
 * ([CapturedNotificationEntity], [CategoryRuleEntity], [CreditCardEntity],
 * [CreditCardStatementEntity], [InstallmentEntity], [TransactionBeneficiaryEntity]) se agregan
 * directamente a la versión 1 en vez de vía [Migration][androidx.room.migration.Migration]: no
 * hay datos reales de un schema anterior que proteger todavía. A partir de la primera build real
 * del usuario, este esquema pasa a ser la base congelada: cualquier cambio futuro sí deberá ir
 * por una Migration explícita (ver [APP_MIGRATIONS]).
 */
@Database(
    entities = [
        HouseholdEntity::class,
        HouseholdMemberEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        TransactionBeneficiaryEntity::class,
        RecurringTransactionEntity::class,
        CapturedNotificationEntity::class,
        CategoryRuleEntity::class,
        CreditCardEntity::class,
        CreditCardStatementEntity::class,
        InstallmentEntity::class,
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
    abstract fun transactionBeneficiaryDao(): TransactionBeneficiaryDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun capturedNotificationDao(): CapturedNotificationDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun creditCardStatementDao(): CreditCardStatementDao
    abstract fun installmentDao(): InstallmentDao
}
