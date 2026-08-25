package com.agustinazorin.finanzas.feature.patrimonio.data

import com.agustinazorin.finanzas.core.database.dao.LiabilityDao
import com.agustinazorin.finanzas.core.database.entity.LiabilityEntity
import com.agustinazorin.finanzas.engine.model.LiabilityType
import com.agustinazorin.finanzas.feature.patrimonio.domain.Liability
import com.agustinazorin.finanzas.feature.patrimonio.domain.LiabilityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class LiabilityRepositoryImpl @Inject constructor(
    private val dao: LiabilityDao,
) : LiabilityRepository {

    override fun observeLiabilities(householdId: Long): Flow<List<Liability>> =
        dao.observeLiabilities(householdId).map { list -> list.map { it.toDomain() } }

    override fun observeActiveLiabilities(householdId: Long): Flow<List<Liability>> =
        dao.observeActiveLiabilities(householdId).map { list -> list.map { it.toDomain() } }

    override suspend fun createLiability(
        householdId: Long,
        ownerMemberId: Long?,
        name: String,
        type: LiabilityType,
        principal: Long,
        outstandingAmount: Long,
        currency: String,
        dueDate: LocalDate?,
        interestRate: Double?,
    ): Long = dao.insert(
        LiabilityEntity(
            householdId = householdId,
            ownerMemberId = ownerMemberId,
            name = name,
            type = type,
            principal = principal,
            outstandingAmount = outstandingAmount,
            currency = currency,
            dueDate = dueDate,
            interestRate = interestRate,
            isActive = true,
        ),
    )

    override suspend fun setLiabilityActive(liabilityId: Long, isActive: Boolean) {
        val entity = dao.getById(liabilityId) ?: return
        dao.update(entity.copy(isActive = isActive))
    }
}

private fun LiabilityEntity.toDomain() = Liability(
    id = id, householdId = householdId, ownerMemberId = ownerMemberId, name = name, type = type,
    principal = principal, outstandingAmount = outstandingAmount, currency = currency,
    dueDate = dueDate, interestRate = interestRate, isActive = isActive,
)
