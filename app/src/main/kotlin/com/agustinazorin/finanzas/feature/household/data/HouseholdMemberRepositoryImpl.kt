package com.agustinazorin.finanzas.feature.household.data

import com.agustinazorin.finanzas.core.database.dao.HouseholdMemberDao
import com.agustinazorin.finanzas.core.database.entity.HouseholdMemberEntity
import com.agustinazorin.finanzas.engine.model.MemberType
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMember
import com.agustinazorin.finanzas.feature.household.domain.HouseholdMemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HouseholdMemberRepositoryImpl @Inject constructor(
    private val dao: HouseholdMemberDao,
) : HouseholdMemberRepository {

    override fun observeMembers(householdId: Long): Flow<List<HouseholdMember>> =
        dao.observeMembers(householdId).map { list -> list.map { it.toDomain() } }

    override fun observeActiveMembers(householdId: Long): Flow<List<HouseholdMember>> =
        dao.observeActiveMembers(householdId).map { list -> list.map { it.toDomain() } }

    override suspend fun addMember(householdId: Long, name: String, type: MemberType): Long =
        dao.insert(HouseholdMemberEntity(householdId = householdId, name = name, type = type, isActive = true))

    override suspend fun updateMember(member: HouseholdMember) {
        dao.update(member.toEntity())
    }

    override suspend fun setMemberActive(memberId: Long, isActive: Boolean) {
        val entity = dao.getById(memberId) ?: return
        dao.update(entity.copy(isActive = isActive))
    }
}

private fun HouseholdMemberEntity.toDomain() = HouseholdMember(
    id = id, householdId = householdId, name = name, type = type, isActive = isActive,
)

private fun HouseholdMember.toEntity() = HouseholdMemberEntity(
    id = id, householdId = householdId, name = name, type = type, isActive = isActive,
)
