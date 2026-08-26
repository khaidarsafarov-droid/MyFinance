package com.truckerload.presentation.theme

import androidx.compose.foundation.focusable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.android.awaitFrame

/**
 * Requests focus once after this destination becomes visible so TalkBack /
 * keyboard users land on a meaningful heading instead of the previous screen.
 *
 * No-op when [enabled] is false (e.g. still showing a skeleton).
 */
fun Modifier.focusAfterNavigate(
    key: Any? = Unit,
    enabled: Boolean = true,
): Modifier = composed {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(key, enabled) {
        if (!enabled) return@LaunchedEffect
        awaitFrame()
        runCatching { focusRequester.requestFocus() }
    }
    this
        .focusRequester(focusRequester)
        .focusable()
}
