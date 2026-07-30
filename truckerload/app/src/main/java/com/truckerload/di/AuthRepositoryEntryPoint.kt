package com.truckerload.di

import com.truckerload.data.repository.AuthRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Access [AuthRepository] from Compose helpers that are not `@HiltViewModel`s. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthRepositoryEntryPoint {
    fun authRepository(): AuthRepository
}
