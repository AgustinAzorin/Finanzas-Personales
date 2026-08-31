package com.agustinazorin.finanzas.core.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.agustinazorin.finanzas.core.database.APP_MIGRATIONS
import com.agustinazorin.finanzas.core.database.AppDatabase
import com.agustinazorin.finanzas.core.database.DATABASE_NAME
import com.agustinazorin.finanzas.core.database.dao.AccountDao
import com.agustinazorin.finanzas.core.database.dao.AssetDao
import com.agustinazorin.finanzas.core.database.dao.CapturedNotificationDao
import com.agustinazorin.finanzas.core.database.dao.CategoryDao
import com.agustinazorin.finanzas.core.database.dao.CategoryRuleDao
import com.agustinazorin.finanzas.core.database.dao.CreditCardDao
import com.agustinazorin.finanzas.core.database.dao.CreditCardStatementDao
import com.agustinazorin.finanzas.core.database.dao.ExchangeRateDao
import com.agustinazorin.finanzas.core.database.dao.FinancialSnapshotDao
import com.agustinazorin.finanzas.core.database.dao.HouseholdDao
import com.agustinazorin.finanzas.core.database.dao.HouseholdMemberDao
import com.agustinazorin.finanzas.core.database.dao.InflationRateDao
import com.agustinazorin.finanzas.core.database.dao.InstallmentDao
import com.agustinazorin.finanzas.core.database.dao.LiabilityDao
import com.agustinazorin.finanzas.core.database.dao.ReceiptDao
import com.agustinazorin.finanzas.core.database.dao.RecurringTransactionDao
import com.agustinazorin.finanzas.core.database.dao.TransactionBeneficiaryDao
import com.agustinazorin.finanzas.core.database.dao.TransactionDao
import com.agustinazorin.finanzas.core.database.seedDefaultCategories
import com.agustinazorin.finanzas.core.diagnostics.CrashDiagnostics
import com.agustinazorin.finanzas.core.security.DatabasePassphraseProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Puebla la base de datos recién creada con el árbol de categorías estándar (CLAUDE.md,
 * sección 12). Recibe [database] como [Provider] para romper el ciclo Database -> Callback ->
 * Database (patrón recomendado por Room).
 *
 * El hogar y el miembro por defecto NO se crean acá: [HouseholdRepositoryImpl.requireHouseholdId]
 * los crea perezosamente con protección contra condiciones de carrera (varios ViewModels
 * pueden pedirlos casi al mismo tiempo apenas arranca la app); crearlos también acá podría
 * terminar en dos hogares si esa carrera pasa antes de que este callback corra.
 */
class SeedDatabaseCallback(
    private val database: Provider<AppDatabase>,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : androidx.room.RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        applicationScope.launch {
            seedDefaultCategories(database.get().categoryDao())
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        databaseProvider: Provider<AppDatabase>,
        @ApplicationScope applicationScope: CoroutineScope,
        passphraseProvider: DatabasePassphraseProvider,
    ): AppDatabase {
        // SQLCipher cifra `finanzas.db` en reposo (CLAUDE.md, sección 43) con una passphrase
        // generada localmente y protegida por Android Keystore (ver DatabasePassphraseProvider),
        // nunca hardcodeada. `System.loadLibrary("sqlcipher")` ya corrió en
        // FinanzasApplication.onCreate antes de que este Provider pueda ser invocado.
        //
        // Se usa `net.zetetic:sqlcipher-android` (no `net.zetetic:android-database-sqlcipher`: ese
        // artefacto está discontinuado desde 2023 y su librería nativa no soporta el tamaño de
        // página de 16 KB que exigen los dispositivos nuevos con Android 15+ — abrir la base con
        // esa librería vieja crasheaba nativamente, sin excepción de Kotlin que atrapar, en
        // dispositivos como el que reportó este crash).
        //
        // Este Provider se ejecuta la primera vez que algún ViewModel necesita un DAO — no
        // necesariamente durante Application.onCreate — así que un fallo acá (passphrase, Room,
        // SQLCipher) queda fuera del rastro que deja FinanzasApplication. Se registra por
        // separado para que CrashDiagnostics siga siendo útil sin importar en qué paso del
        // arranque pasó.
        try {
            val passphrase = passphraseProvider.getOrCreatePassphrase()
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .addMigrations(*APP_MIGRATIONS)
                .addCallback(SeedDatabaseCallback(databaseProvider, applicationScope))
                .build()
                .also { CrashDiagnostics.recordStep(context, "DatabaseModule: AppDatabase creada") }
        } catch (error: Throwable) {
            CrashDiagnostics.recordCaught(context, "DatabaseModule: AppDatabase.build", error)
            throw error
        }
    }

    @Provides
    fun provideHouseholdDao(db: AppDatabase): HouseholdDao = db.householdDao()

    @Provides
    fun provideHouseholdMemberDao(db: AppDatabase): HouseholdMemberDao = db.householdMemberDao()

    @Provides
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideTransactionBeneficiaryDao(db: AppDatabase): TransactionBeneficiaryDao = db.transactionBeneficiaryDao()

    @Provides
    fun provideRecurringTransactionDao(db: AppDatabase): RecurringTransactionDao = db.recurringTransactionDao()

    @Provides
    fun provideCapturedNotificationDao(db: AppDatabase): CapturedNotificationDao = db.capturedNotificationDao()

    @Provides
    fun provideCategoryRuleDao(db: AppDatabase): CategoryRuleDao = db.categoryRuleDao()

    @Provides
    fun provideCreditCardDao(db: AppDatabase): CreditCardDao = db.creditCardDao()

    @Provides
    fun provideCreditCardStatementDao(db: AppDatabase): CreditCardStatementDao = db.creditCardStatementDao()

    @Provides
    fun provideInstallmentDao(db: AppDatabase): InstallmentDao = db.installmentDao()

    @Provides
    fun provideAssetDao(db: AppDatabase): AssetDao = db.assetDao()

    @Provides
    fun provideLiabilityDao(db: AppDatabase): LiabilityDao = db.liabilityDao()

    @Provides
    fun provideFinancialSnapshotDao(db: AppDatabase): FinancialSnapshotDao = db.financialSnapshotDao()

    @Provides
    fun provideExchangeRateDao(db: AppDatabase): ExchangeRateDao = db.exchangeRateDao()

    @Provides
    fun provideInflationRateDao(db: AppDatabase): InflationRateDao = db.inflationRateDao()

    @Provides
    fun provideReceiptDao(db: AppDatabase): ReceiptDao = db.receiptDao()
}
