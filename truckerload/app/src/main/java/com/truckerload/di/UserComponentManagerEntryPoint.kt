package com.truckerload.di

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UserComponentManagerEntryPoint {
    fun userComponentManager(): UserComponentManager
}

fun Context.userComponentManager(): UserComponentManager =
    EntryPointAccessors.fromApplication(
        applicationContext,
        UserComponentManagerEntryPoint::class.java,
    ).userComponentManager()
