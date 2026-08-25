package com.agustinazorin.finanzas.core.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.agustinazorin.finanzas.core.database.APP_MIGRATIONS
import com.agustinazorin.finanzas.core.database.AppDatabase
import com.agustinazorin.finanzas.core.database.DATABASE_NAME
import com.agustinazorin.finanzas.core.database.dao.AccountDao
import com.agustinazorin.finanzas.core.database.dao.CategoryDao
import com.agustinazorin.finanzas.core.database.dao.HouseholdDao
import com.agustinazorin.finanzas.core.database.dao.HouseholdMemberDao
import com.agustinazorin.finanzas.core.database.dao.RecurringTransactionDao
import com.agustinazorin.finanzas.core.database.dao.TransactionDao
import com.agustinazorin.finanzas.core.database.seedDefaultCategories
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

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
    ): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(*APP_MIGRATIONS)
            .addCallback(SeedDatabaseCallback(databaseProvider, applicationScope))
            .build()

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
    fun provideRecurringTransactionDao(db: AppDatabase): RecurringTransactionDao = db.recurringTransactionDao()
}
