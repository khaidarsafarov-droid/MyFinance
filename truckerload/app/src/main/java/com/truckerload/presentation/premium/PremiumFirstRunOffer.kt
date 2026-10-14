package com.truckerload.presentation.premium

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.premium.PremiumOfferStore
import com.truckerload.domain.premium.PremiumPolicy
import com.truckerload.utils.findActivity

/**
 * Once, after the journal is on screen. The free month starts only if the user
 * subscribes in Google Play. Skipping keeps the free version and does not start a trial.
 */
@Composable
fun PremiumFirstRunOffer() {
    val context = LocalContext.current
    val resources = LocalResources.current
    val access = rememberPremiumAccess()
    val store = remember { PremiumOfferStore(context) }
    val subscribed by access.isSubscribed.collectAsStateWithLifecycle()
    var visible by remember {
        mutableStateOf(
            PremiumPolicy.shouldPromptForSubscription(
                subscribed = access.isSubscribed.value,
                offerAlreadyHandled = store.wasHandled(),
            ),
        )
    }

    LaunchedEffect(subscribed) {
        if (subscribed) {
            store.markHandled()
            visible = false
        }
    }

    if (!visible) return

    AlertDialog(
        onDismissRequest = {
            store.markHandled()
            visible = false
        },
        title = { Text(stringResource(R.string.premium_offer_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.premium_offer_body))
                Text(stringResource(R.string.premium_terms))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val activity = context.findActivity() ?: return@TextButton
                    access.purchase(activity) { error ->
                        if (error != null) {
                            Toast.makeText(
                                context,
                                resources.getString(R.string.premium_purchase_unavailable),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                },
            ) {
                Text(stringResource(R.string.premium_subscribe))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    store.markHandled()
                    visible = false
                },
            ) {
                Text(stringResource(R.string.premium_offer_continue_free))
            }
        },
    )
}
