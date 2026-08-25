package com.agustinazorin.finanzas.core.di

import com.agustinazorin.finanzas.feature.account.data.AccountRepositoryImpl
import com.agustinazorin.finanzas.feature.account.domain.AccountRepository
import com.agustinazorin.finanzas.feature.category.data.CategoryRepositoryImpl
import com.agustinazorin.finanzas.feature.category.domain.CategoryRepository
import com.agustinazorin.finanzas.feature.household.data.HouseholdMemberRepositoryImpl
import com.agustinazorin.finanzas.feature.household.data.HouseholdRepositoryImpl
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMemberRepository
import com.agustinazorin.finanzas.feature.household.domain.HouseholdRepository
import com.agustinazorin.finanzas.feature.recurring.data.RecurringTransactionRepositoryImpl
import com.agustinazorin.finanzas.feature.recurring.domain.RecurringTransactionRepository
import com.agustinazorin.finanzas.feature.transaction.data.TransactionRepositoryImpl
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindHouseholdRepository(impl: HouseholdRepositoryImpl): HouseholdRepository

    @Binds
    @Singleton
    abstract fun bindHouseholdMemberRepository(impl: HouseholdMemberRepositoryImpl): HouseholdMemberRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindRecurringTransactionRepository(impl: RecurringTransactionRepositoryImpl): RecurringTransactionRepository
}
