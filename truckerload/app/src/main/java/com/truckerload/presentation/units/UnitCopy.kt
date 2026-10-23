package com.truckerload.presentation.units

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.truckerload.domain.units.DisplayUnits

@Composable
fun unitString(@StringRes miles: Int, @StringRes kilometers: Int): String =
    stringResource(if (DisplayUnits.isMetric) kilometers else miles)

fun Context.stringForUnits(@StringRes miles: Int, @StringRes kilometers: Int): String =
    getString(if (DisplayUnits.isMetric) kilometers else miles)
