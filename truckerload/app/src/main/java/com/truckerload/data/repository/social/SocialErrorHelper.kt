package com.truckerload.data.repository.social

import android.content.Context
import androidx.annotation.StringRes

internal fun socialError(context: Context, @StringRes fallbackRes: Int, throwable: Throwable): String =
    throwable.message?.takeIf { it.isNotBlank() } ?: context.getString(fallbackRes)
