package com.agustinazorin.finanzas.feature.patrimonio.ui

import androidx.lifecycle.viewModelScope
import com.agustinazorin.finanzas.core.ui.HouseholdScopedViewModel
import com.agustinazorin.finanzas.engine.currency.CurrencyConversionResult
import com.agustinazorin.finanzas.engine.inflation.InflationAdjuster
import com.agustinazorin.finanzas.engine.inflation.MonthlyInflationRate
import com.agustinazorin.finanzas.engine.model.AccountType
import com.agustinazorin.finanzas.engine.model.AssetCategory
import com.agustinazorin.finanzas.engine.model.LiabilityType
import com.agustinazorin.finanzas.engine.money.Money
import com.agustinazorin.finanzas.feature.account.domain.AccountRepository
import com.agustinazorin.finanzas.feature.account.domain.usecase.GetAccountBalancesUseCase
import com.agustinazorin.finanzas.feature.account.domain.usecase.GetNetWorthUseCase
import com.agustinazorin.finanzas.feature.account.domain.usecase.inCurrency
import com.agustinazorin.finanzas.feature.currency.domain.InflationRateRepository
import com.agustinazorin.finanzas.feature.currency.domain.usecase.ConvertToBaseCurrencyUseCase
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMember
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMemberRepository
import com.agustinazorin.finanzas.feature.household.domain.HouseholdRepository
import com.agustinazorin.finanzas.feature.patrimonio.domain.Asset
import com.agustinazorin.finanzas.feature.patrimonio.domain.AssetRepository
import com.agustinazorin.finanzas.feature.patrimonio.domain.FinancialSnapshot
import com.agustinazorin.finanzas.feature.patrimonio.domain.FinancialSnapshotRepository
import com.agustinazorin.finanzas.feature.patrimonio.domain.Liability
import com.agustinazorin.finanzas.feature.patrimonio.domain.LiabilityRepository
import com.agustinazorin.finanzas.feature.patrimonio.domain.usecase.RecordSnapshotIfNeededUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val BASE_CURRENCY = "ARS"
private val LIABILITY_ACCOUNT_TYPES = setOf(AccountType.CREDIT_CARD, AccountType.LOAN, AccountType.OTHER_LIABILITY)

data class PatrimonioBreakdownItem(val label: String, val amount: Money)

/**
 * Un snapshot histórico junto a su equivalente ajustado por inflación a hoy (CLAUDE.md, sección
 * 42). [adjustedToToday] es null cuando falta algún mes de inflación entre el snapshot y hoy: en
 * ese caso no se inventa un valor aproximado, se muestra sólo el nominal.
 */
data class SnapshotEvolutionItem(val snapshot: FinancialSnapshot, val adjustedToToday: Money?)

data class PatrimonioUiState(
    val netWorth: Money = Money.zero(BASE_CURRENCY),
    val totalAssets: Money = Money.zero(BASE_CURRENCY),
    val totalLiabilities: Money = Money.zero(BASE_CURRENCY),
    val accountAssetItems: List<PatrimonioBreakdownItem> = emptyList(),
    val accountLiabilityItems: List<PatrimonioBreakdownItem> = emptyList(),
    val customAssets: List<Asset> = emptyList(),
    val customLiabilities: List<Liability> = emptyList(),
)

/**
 * Patrimonio (CLAUDE.md, sección 27): patrimonio neto, su descomposición en activos/pasivos —
 * tanto los que ya viven en una Account (Fase 0) como los Asset/Liability independientes
 * (Fase 5) — y su evolución histórica a partir de [FinancialSnapshot]s.
 */
@HiltViewModel
class PatrimonioViewModel @Inject constructor(
    householdRepository: HouseholdRepository,
    private val accountRepository: AccountRepository,
    private val householdMemberRepository: HouseholdMemberRepository,
    private val assetRepository: AssetRepository,
    private val liabilityRepository: LiabilityRepository,
    private val financialSnapshotRepository: FinancialSnapshotRepository,
    private val inflationRateRepository: InflationRateRepository,
    private val getAccountBalancesUseCase: GetAccountBalancesUseCase,
    private val getNetWorthUseCase: GetNetWorthUseCase,
    private val recordSnapshotIfNeededUseCase: RecordSnapshotIfNeededUseCase,
    private val convertToBaseCurrencyUseCase: ConvertToBaseCurrencyUseCase,
) : HouseholdScopedViewModel(householdRepository) {

    val uiState: StateFlow<PatrimonioUiState> = householdId.filterNotNull().flatMapLatest { id ->
        val today = LocalDate.now()
        combine(
            getNetWorthUseCase(id, today),
            accountRepository.observeActiveAccounts(id),
            getAccountBalancesUseCase(id, today),
            assetRepository.observeActiveAssets(id),
            liabilityRepository.observeActiveLiabilities(id),
        ) { netWorthByCurrency, accounts, balances, assets, liabilities ->
            val balanceByAccount = balances.associateBy { it.accountId }.mapValues { it.value.balance }

            val accountAssetItems = accounts
                .filter { it.type !in LIABILITY_ACCOUNT_TYPES && it.currency == BASE_CURRENCY }
                .mapNotNull { account ->
                    balanceByAccount[account.id]?.takeIf { it.isPositive }?.let { PatrimonioBreakdownItem(account.name, it) }
                }
            val accountLiabilityItems = accounts
                .filter { it.type in LIABILITY_ACCOUNT_TYPES && it.currency == BASE_CURRENCY }
                .mapNotNull { account ->
                    balanceByAccount[account.id]?.takeIf { it.isNegative }?.let { PatrimonioBreakdownItem(account.name, -it) }
                }

            val customAssetsInBase = assets.filter { it.currency == BASE_CURRENCY }.map { Money(it.currentValue, BASE_CURRENCY) }
            val customLiabilitiesInBase =
                liabilities.filter { it.currency == BASE_CURRENCY }.map { Money(it.outstandingAmount, BASE_CURRENCY) }

            PatrimonioUiState(
                netWorth = netWorthByCurrency.inCurrency(BASE_CURRENCY),
                totalAssets = Money.sum(accountAssetItems.map { it.amount } + customAssetsInBase, BASE_CURRENCY),
                totalLiabilities = Money.sum(accountLiabilityItems.map { it.amount } + customLiabilitiesInBase, BASE_CURRENCY),
                accountAssetItems = accountAssetItems,
                accountLiabilityItems = accountLiabilityItems,
                customAssets = assets,
                customLiabilities = liabilities,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PatrimonioUiState())

    val evolution: StateFlow<List<SnapshotEvolutionItem>> = householdId.filterNotNull().flatMapLatest { id ->
        combine(
            financialSnapshotRepository.observeHistory(id, BASE_CURRENCY),
            inflationRateRepository.observeAll(),
        ) { snapshots, inflationRates ->
            val today = LocalDate.now()
            val monthlyRates = inflationRates.map { MonthlyInflationRate(it.month, it.percent) }
            snapshots.map { snapshot ->
                val adjusted = runCatching {
                    InflationAdjuster.adjust(Money(snapshot.netWorth, snapshot.currency), snapshot.date, today, monthlyRates)
                }.getOrNull()
                SnapshotEvolutionItem(snapshot, adjusted)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Patrimonio total convertido a [BASE_CURRENCY] (CLAUDE.md, sección 41): además del
     * desglose ARS-only de [uiState] (sin cambios, para no arriesgar esa lógica ya probada de
     * Fase 5), esto suma lo que haya en otras monedas (ej: cuentas o activos en USD) usando la
     * última cotización cargada. Es `null` cuando todo ya está en [BASE_CURRENCY] (no hay nada
     * que convertir, no tiene sentido mostrar la tarjeta).
     */
    val convertedNetWorth: StateFlow<CurrencyConversionResult?> = householdId.filterNotNull().flatMapLatest { id ->
        getNetWorthUseCase(id, LocalDate.now()).flatMapLatest { byCurrency ->
            if (byCurrency.keys.all { it == BASE_CURRENCY }) {
                flowOf(null)
            } else {
                convertToBaseCurrencyUseCase(byCurrency, BASE_CURRENCY)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val members: StateFlow<List<HouseholdMember>> = householdId.filterNotNull().flatMapLatest { id ->
        householdMemberRepository.observeActiveMembers(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val id = householdId.filterNotNull().first()
            recordSnapshotIfNeededUseCase(id, BASE_CURRENCY)
        }
    }

    fun addAsset(name: String, category: AssetCategory, currency: String, currentValue: Long, ownerMemberId: Long?) {
        viewModelScope.launch {
            val id = householdId.value ?: return@launch
            assetRepository.createAsset(id, ownerMemberId, name, category, currency, currentValue, LocalDate.now())
        }
    }

    fun addLiability(name: String, type: LiabilityType, currency: String, outstandingAmount: Long, ownerMemberId: Long?) {
        viewModelScope.launch {
            val id = householdId.value ?: return@launch
            liabilityRepository.createLiability(
                householdId = id,
                ownerMemberId = ownerMemberId,
                name = name,
                type = type,
                principal = outstandingAmount,
                outstandingAmount = outstandingAmount,
                currency = currency,
                dueDate = null,
                interestRate = null,
            )
        }
    }

    fun removeAsset(assetId: Long) {
        viewModelScope.launch { assetRepository.setAssetActive(assetId, false) }
    }

    fun removeLiability(liabilityId: Long) {
        viewModelScope.launch { liabilityRepository.setLiabilityActive(liabilityId, false) }
    }
}
