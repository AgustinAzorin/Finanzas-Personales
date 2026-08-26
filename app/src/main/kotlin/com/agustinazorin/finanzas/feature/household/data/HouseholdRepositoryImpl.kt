package com.agustinazorin.finanzas.feature.household.data

import com.agustinazorin.finanzas.core.database.dao.HouseholdDao
import com.agustinazorin.finanzas.core.database.dao.HouseholdMemberDao
import com.agustinazorin.finanzas.core.database.entity.HouseholdEntity
import com.agustinazorin.finanzas.core.database.entity.HouseholdMemberEntity
import com.agustinazorin.finanzas.engine.model.MemberType
import com.agustinazorin.finanzas.feature.household.domain.Household
import com.agustinazorin.finanzas.feature.household.domain.HouseholdRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fase 0 asume un único hogar por dispositivo, creado perezosamente la primera vez que algo
 * lo necesita (no hay onboarding). [requireHouseholdId] puede ser llamado concurrentemente por
 * varios ViewModels apenas arranca la app: el [mutex] con doble chequeo evita crear dos hogares
 * por una carrera entre esas llamadas.
 */
@Singleton
class HouseholdRepositoryImpl @Inject constructor(
    private val householdDao: HouseholdDao,
    private val householdMemberDao: HouseholdMemberDao,
) : HouseholdRepository {

    private val mutex = Mutex()

    override suspend fun requireHouseholdId(): Long {
        householdDao.getDefaultHousehold()?.let { return it.id }
        return mutex.withLock {
            householdDao.getDefaultHousehold()?.let { return@withLock it.id }
            val householdId = householdDao.insert(
                HouseholdEntity(name = "Mi hogar", baseCurrency = "ARS", createdAt = Instant.now()),
            )
            householdMemberDao.insert(
                HouseholdMemberEntity(householdId = householdId, name = "Yo", type = MemberType.OWNER, isActive = true),
            )
            householdId
        }
    }

    override fun observeDefaultHousehold(): Flow<Household?> =
        householdDao.observeDefaultHousehold().map { it?.toDomain() }
}

private fun HouseholdEntity.toDomain() = Household(id = id, name = name, baseCurrency = baseCurrency)
