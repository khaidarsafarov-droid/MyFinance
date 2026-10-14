package com.truckerload.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource

/**
 * Shows [errorRes] after the field loses focus, or when Save has already failed.
 * The first keystrokes stay quiet.
 */
class BlurGate {
    var blurred by mutableStateOf(false)
        private set
    private var focusedOnce = false

    fun onFocusChange(isFocused: Boolean) {
        if (isFocused) focusedOnce = true
        else if (focusedOnce) blurred = true
    }

    fun visibleRes(errorRes: Int?, reveal: Boolean): Int? =
        errorRes?.takeIf { reveal || blurred }
}

@Composable
fun rememberBlurGate(): BlurGate = remember { BlurGate() }

fun Modifier.blurGate(gate: BlurGate): Modifier =
    onFocusChanged { gate.onFocusChange(it.isFocused) }

@Composable
fun errorSupport(res: Int?): (@Composable () -> Unit)? =
    res?.let { id -> { Text(stringResource(id)) } }
