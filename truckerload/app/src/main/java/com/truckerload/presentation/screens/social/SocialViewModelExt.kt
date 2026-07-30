package com.truckerload.presentation.screens.social

internal fun Throwable.toUiMessage(): String =
    localizedMessage ?: message ?: javaClass.simpleName
