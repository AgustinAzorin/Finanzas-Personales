package com.agustinazorin.finanzas.feature.household.domain

import com.agustinazorin.finanzas.engine.model.MemberType
import kotlinx.coroutines.flow.Flow

data class Household(
    val id: Long,
    val name: String,
    val baseCurrency: String,
)

data class HouseholdMember(
    val id: Long,
    val householdId: Long,
    val name: String,
    val type: MemberType,
    val isActive: Boolean,
)

/**
 * Fase 0 asume un único hogar por dispositivo (no hay onboarding multi-hogar todavía),
 * pero el modelo soporta más de uno a futuro. [requireHouseholdId] es el punto de entrada que
 * usan el resto de los repositorios/ViewModels para obtener el hogar por defecto, creándolo
 * si por algún motivo todavía no existe.
 */
interface HouseholdRepository {
    suspend fun requireHouseholdId(): Long
    fun observeDefaultHousehold(): Flow<Household?>
}

interface HouseholdMemberRepository {
    fun observeMembers(householdId: Long): Flow<List<HouseholdMember>>
    fun observeActiveMembers(householdId: Long): Flow<List<HouseholdMember>>
    suspend fun addMember(householdId: Long, name: String, type: MemberType): Long
    suspend fun updateMember(member: HouseholdMember)
    suspend fun setMemberActive(memberId: Long, isActive: Boolean)
}
