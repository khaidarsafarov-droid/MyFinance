package com.truckerload.presentation.components

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.truckerload.R
import com.truckerload.data.local.LegacyDatabaseAbsorb
import com.truckerload.di.UserComponent
import com.truckerload.di.userComponentManager
import com.truckerload.presentation.di.LocalAuthStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Asks before absorbing a previous local/offline database into the active cloud account.
 *
 * @param onSessionRebuilt called on the main thread with the rebuilt [UserComponent]
 * after a successful absorb (Room pool was closed during copy).
 */
@Composable
fun LegacyAbsorbDialog(
    onSessionRebuilt: (UserComponent) -> Unit,
) {
    val context = LocalContext.current
    val authStore = LocalAuthStore.current
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val userId = authStore.currentUserIdOrNull()

    LaunchedEffect(userId) {
        visible = userId != null && LegacyDatabaseAbsorb.hasPendingPrompt(context, userId)
    }

    if (!visible || busy || userId == null) return

    AlertDialog(
        onDismissRequest = { /* require explicit Accept / Decline */ },
        title = { Text(stringResource(R.string.legacy_absorb_title)) },
        text = { Text(stringResource(R.string.legacy_absorb_message)) },
        confirmButton = {
            TlTextButton(
                onClick = {
                    busy = true
                    scope.launch {
                        val rebuilt = withContext(Dispatchers.IO) {
                            val ok = LegacyDatabaseAbsorb.acceptAndCopy(context, userId)
                            if (!ok) return@withContext null
                            context.userComponentManager().endSession()
                            context.userComponentManager().startSession(userId)
                        }
                        if (rebuilt != null) {
                            onSessionRebuilt(rebuilt)
                            Toast.makeText(
                                context,
                                context.getString(R.string.legacy_absorb_success),
                                Toast.LENGTH_LONG,
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.legacy_absorb_failed),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                        visible = false
                        busy = false
                    }
                },
            ) {
                Text(stringResource(R.string.legacy_absorb_confirm))
            }
        },
        dismissButton = {
            TlTextButton(
                onClick = {
                    LegacyDatabaseAbsorb.decline(context, userId)
                    visible = false
                },
            ) {
                Text(stringResource(R.string.legacy_absorb_dismiss))
            }
        },
    )
}
