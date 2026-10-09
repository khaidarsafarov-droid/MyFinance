package com.truckerload.presentation.premium

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.premium.PremiumStore

/** Shown once after install: the download is free and the first month is a trial. */
@Composable
fun TrialWelcomeDialog() {
    val context = LocalContext.current
    val store = remember { PremiumStore(context) }
    val status by rememberPremiumAccess().status.collectAsStateWithLifecycle()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(status.inTrial) {
        visible = status.inTrial && store.shouldShowTrialWelcome()
    }

    if (!visible) return
    AlertDialog(
        onDismissRequest = {
            store.markTrialWelcomeShown()
            visible = false
        },
        title = { Text(stringResource(R.string.premium_welcome_title)) },
        text = {
            Text(
                stringResource(
                    R.string.premium_welcome_body,
                    status.trialDaysLeft(System.currentTimeMillis()).coerceAtLeast(1),
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                store.markTrialWelcomeShown()
                visible = false
            }) {
                Text(stringResource(R.string.premium_welcome_start))
            }
        },
    )
}
