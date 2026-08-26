package com.agustinazorin.finanzas.core.di

import com.agustinazorin.finanzas.feature.account.data.AccountRepositoryImpl
import com.agustinazorin.finanzas.feature.account.domain.AccountRepository
import com.agustinazorin.finanzas.feature.capture.data.CapturedNotificationRepositoryImpl
import com.agustinazorin.finanzas.feature.capture.domain.CapturedNotificationRepository
import com.agustinazorin.finanzas.feature.category.data.CategoryRepositoryImpl
import com.agustinazorin.finanzas.feature.category.data.CategoryRuleRepositoryImpl
import com.agustinazorin.finanzas.feature.category.domain.CategoryRepository
import com.agustinazorin.finanzas.feature.category.domain.CategoryRuleRepository
import com.agustinazorin.finanzas.feature.creditcard.data.CreditCardRepositoryImpl
import com.agustinazorin.finanzas.feature.creditcard.data.CreditCardStatementRepositoryImpl
import com.agustinazorin.finanzas.feature.creditcard.domain.CreditCardRepository
import com.agustinazorin.finanzas.feature.creditcard.domain.CreditCardStatementRepository
import com.agustinazorin.finanzas.feature.currency.data.ExchangeRateRepositoryImpl
import com.agustinazorin.finanzas.feature.currency.data.InflationRateRepositoryImpl
import com.agustinazorin.finanzas.feature.currency.domain.ExchangeRateRepository
import com.agustinazorin.finanzas.feature.currency.domain.InflationRateRepository
import com.agustinazorin.finanzas.feature.household.data.HouseholdMemberRepositoryImpl
import com.agustinazorin.finanzas.feature.household.data.HouseholdRepositoryImpl
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMemberRepository
import com.agustinazorin.finanzas.feature.household.domain.HouseholdRepository
import com.agustinazorin.finanzas.feature.installment.data.InstallmentRepositoryImpl
import com.agustinazorin.finanzas.feature.installment.domain.InstallmentRepository
import com.agustinazorin.finanzas.feature.patrimonio.data.AssetRepositoryImpl
import com.agustinazorin.finanzas.feature.patrimonio.data.FinancialSnapshotRepositoryImpl
import com.agustinazorin.finanzas.feature.patrimonio.data.LiabilityRepositoryImpl
import com.agustinazorin.finanzas.feature.patrimonio.domain.AssetRepository
import com.agustinazorin.finanzas.feature.patrimonio.domain.FinancialSnapshotRepository
import com.agustinazorin.finanzas.feature.patrimonio.domain.LiabilityRepository
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

    @Binds
    @Singleton
    abstract fun bindCapturedNotificationRepository(impl: CapturedNotificationRepositoryImpl): CapturedNotificationRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRuleRepository(impl: CategoryRuleRepositoryImpl): CategoryRuleRepository

    @Binds
    @Singleton
    abstract fun bindCreditCardRepository(impl: CreditCardRepositoryImpl): CreditCardRepository

    @Binds
    @Singleton
    abstract fun bindCreditCardStatementRepository(impl: CreditCardStatementRepositoryImpl): CreditCardStatementRepository

    @Binds
    @Singleton
    abstract fun bindInstallmentRepository(impl: InstallmentRepositoryImpl): InstallmentRepository

    @Binds
    @Singleton
    abstract fun bindAssetRepository(impl: AssetRepositoryImpl): AssetRepository

    @Binds
    @Singleton
    abstract fun bindLiabilityRepository(impl: LiabilityRepositoryImpl): LiabilityRepository

    @Binds
    @Singleton
    abstract fun bindFinancialSnapshotRepository(impl: FinancialSnapshotRepositoryImpl): FinancialSnapshotRepository

    @Binds
    @Singleton
    abstract fun bindExchangeRateRepository(impl: ExchangeRateRepositoryImpl): ExchangeRateRepository

    @Binds
    @Singleton
    abstract fun bindInflationRateRepository(impl: InflationRateRepositoryImpl): InflationRateRepository
}
