package com.truckerload.data.repository.account

import com.truckerload.data.local.dao.UserAccountDao
import com.truckerload.data.local.entities.UserAccountEntity
import com.truckerload.domain.account.UserAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountIdentityRepository(
    private val dao: UserAccountDao,
) {
    suspend fun get(userId: String): UserAccount? = dao.get(userId)?.toDomain()

    fun watch(userId: String): Flow<UserAccount?> = dao.watch(userId).map { it?.toDomain() }

    suspend fun upsert(account: UserAccount) {
        dao.upsert(account.toEntity())
    }

    suspend fun delete(userId: String) {
        dao.delete(userId)
    }
}

internal fun UserAccountEntity.toDomain(): UserAccount = UserAccount(
    id = id,
    phone = phone,
    email = email,
    authProvider = authProvider,
    displayName = displayName,
    createdAt = createdAt,
    isVerified = isVerified,
    ageConfirmed = ageConfirmed,
    acceptedTosAt = acceptedTosAt,
    analyticsConsentAt = analyticsConsentAt,
)

internal fun UserAccount.toEntity(): UserAccountEntity = UserAccountEntity(
    id = id,
    phone = phone,
    email = email,
    authProvider = authProvider,
    displayName = displayName,
    createdAt = createdAt,
    isVerified = isVerified,
    ageConfirmed = ageConfirmed,
    acceptedTosAt = acceptedTosAt,
    analyticsConsentAt = analyticsConsentAt,
)
