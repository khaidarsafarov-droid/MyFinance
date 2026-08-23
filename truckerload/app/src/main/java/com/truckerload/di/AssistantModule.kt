package com.truckerload.di

import android.content.Context
import com.truckerload.data.assistant.AndroidSpeechToText
import com.truckerload.data.assistant.SpeechToText
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AssistantModule {
    @Provides
    fun provideSpeechToText(@ApplicationContext context: Context): SpeechToText =
        AndroidSpeechToText(context.applicationContext)
}
