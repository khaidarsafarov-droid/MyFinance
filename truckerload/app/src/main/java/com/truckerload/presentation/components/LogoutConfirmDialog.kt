package com.truckerload.presentation.components

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.truckerload.R
import com.truckerload.di.userComponentManager
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.sync.SessionTeardown
import kotlinx.coroutines.launch

@Composable
fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onSignedOut: () -> Unit = {},
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val authStore = LocalAuthStore.current
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.settings_logout_confirm_title),
                color = tc.TextPrimary,
            )
        },
        text = {
            Text(
                stringResource(R.string.settings_logout_confirm_message),
                color = tc.TextSecondary,
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        SessionTeardown.signOut(
                            context = context,
                            authStore = authStore,
                            endSession = { context.userComponentManager().endSession() },
                        )
                        onDismiss()
                        onSignedOut()
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_logout_success),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
            ) {
                Text(stringResource(R.string.settings_logout_button))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
