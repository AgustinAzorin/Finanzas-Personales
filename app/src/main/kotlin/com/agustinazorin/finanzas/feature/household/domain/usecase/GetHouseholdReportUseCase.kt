package com.agustinazorin.finanzas.feature.household.domain.usecase

import com.agustinazorin.finanzas.core.engine.toEngineTransaction
import com.agustinazorin.finanzas.core.engine.toEngineTransactionShare
import com.agustinazorin.finanzas.engine.household.HouseholdDebt
import com.agustinazorin.finanzas.engine.household.HouseholdSettlementCalculator
import com.agustinazorin.finanzas.engine.household.MemberAttribution
import com.agustinazorin.finanzas.engine.household.MemberAttributionCalculator
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionFilter
import com.agustinazorin.finanzas.feature.transaction.domain.TransactionRepository
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/** Cuánto le corresponde a cada miembro del hogar en un período, y quién le debe a quién por gastos compartidos (CLAUDE.md, sección 30). */
data class HouseholdReport(
    val memberAttributions: List<MemberAttribution>,
    val debts: List<HouseholdDebt>,
)

/**
 * Reporte del hogar (roadmap Fase 3): reparte gasto/ingreso del período entre sus miembros y
 * calcula el saldo entre ellos por gastos compartidos.
 *
 * Las deudas sólo consideran los gastos compartidos del período recibido: no hay todavía un
 * concepto de "deuda saldada" que persista entre períodos (CLAUDE.md, sección 30, permite
 * empezar simple). Si un gasto compartido de un mes anterior sigue sin saldarse, no aparece acá.
 */
class GetHouseholdReportUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(householdId: Long, start: LocalDate, end: LocalDate) =
        combine(
            transactionRepository.observeFiltered(householdId, TransactionFilter(start = start, end = end)),
            transactionRepository.observeBeneficiariesForHousehold(householdId, start, end),
        ) { transactions, beneficiaries ->
            val currencyByTransactionId = transactions.associate { it.id to it.currency }
            val sharesByTransactionId = beneficiaries
                .mapNotNull { beneficiary ->
                    val currency = currencyByTransactionId[beneficiary.transactionId] ?: return@mapNotNull null
                    beneficiary.toEngineTransactionShare(currency)
                }
                .groupBy { it.transactionId }

            val engineTransactions = transactions.map { it.toEngineTransaction() }
            HouseholdReport(
                memberAttributions = MemberAttributionCalculator.summarize(engineTransactions, sharesByTransactionId, start, end),
                debts = HouseholdSettlementCalculator.calculate(engineTransactions, sharesByTransactionId),
            )
        }
}
