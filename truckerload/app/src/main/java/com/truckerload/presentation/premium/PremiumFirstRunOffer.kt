package com.truckerload.presentation.premium

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.premium.PremiumOfferStore
import com.truckerload.domain.premium.PremiumOfferPrice
import com.truckerload.domain.premium.PremiumPolicy
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalBrandPalette
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
    var playPrice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(subscribed) {
        if (subscribed) {
            store.markHandled()
            visible = false
        }
    }
    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        playPrice = access.loadOffer()?.formattedPrice
    }

    if (!visible) return

    val brand = LocalBrandPalette.current
    val price = PremiumOfferPrice.display(playPrice)
    AlertDialog(
        onDismissRequest = {
            store.markHandled()
            visible = false
        },
        containerColor = brand.card,
        title = {
            Text(
                text = stringResource(R.string.premium_offer_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = brand.goldTagText,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.premium_offer_price_line, price),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = brand.textPrimary,
                )
                Text(
                    text = stringResource(R.string.premium_offer_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = brand.textSecondary,
                )
                OfferFeature(stringResource(R.string.premium_offer_feature_telegram))
                OfferFeature(stringResource(R.string.premium_offer_feature_insights))
                OfferFeature(stringResource(R.string.premium_offer_feature_tools))
                Text(
                    text = stringResource(R.string.premium_terms),
                    style = MaterialTheme.typography.bodySmall,
                    color = brand.textTertiary,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val activity = context.findActivity() ?: return@Button
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = brand.goldEnd,
                    contentColor = brand.goldOnGold,
                ),
            ) {
                Text(
                    text = stringResource(R.string.premium_subscribe),
                    fontWeight = FontWeight.Bold,
                )
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

@Composable
private fun OfferFeature(text: String) {
    val brand = LocalBrandPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            AppIcons.Check,
            contentDescription = null,
            tint = brand.goldEnd,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = brand.textPrimary,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
