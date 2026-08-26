package com.agustinazorin.finanzas.feature.patrimonio.domain

import com.agustinazorin.finanzas.engine.model.LiabilityType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class Liability(
    val id: Long,
    val householdId: Long,
    val ownerMemberId: Long?,
    val name: String,
    val type: LiabilityType,
    val principal: Long,
    val outstandingAmount: Long,
    val currency: String,
    val dueDate: LocalDate?,
    val interestRate: Double?,
    val isActive: Boolean,
)

interface LiabilityRepository {
    fun observeLiabilities(householdId: Long): Flow<List<Liability>>
    fun observeActiveLiabilities(householdId: Long): Flow<List<Liability>>

    suspend fun createLiability(
        householdId: Long,
        ownerMemberId: Long?,
        name: String,
        type: LiabilityType,
        principal: Long,
        outstandingAmount: Long,
        currency: String,
        dueDate: LocalDate?,
        interestRate: Double?,
    ): Long

    suspend fun setLiabilityActive(liabilityId: Long, isActive: Boolean)
}
