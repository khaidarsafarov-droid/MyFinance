package com.truckerload.presentation.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

fun formKeyboard(type: KeyboardType, done: Boolean = false): KeyboardOptions =
    KeyboardOptions(
        keyboardType = type,
        imeAction = if (done) ImeAction.Done else ImeAction.Next,
    )

@Composable
fun formKeyboardActions(onDone: (() -> Unit)? = null): KeyboardActions {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return KeyboardActions(
        onNext = { focusManager.moveFocus(FocusDirection.Next) },
        onDone = {
            keyboard?.hide()
            onDone?.invoke()
        },
    )
}
