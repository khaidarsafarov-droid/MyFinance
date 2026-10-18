package com.truckerload.presentation.premium

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.premium.PremiumAccess
import com.truckerload.domain.premium.PremiumFeature
import com.truckerload.domain.premium.PremiumOfferQuote
import com.truckerload.presentation.components.TlButton
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.findActivity

@Composable
fun PremiumGate(
    feature: PremiumFeature,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val access = rememberPremiumAccess()
    val status by access.status.collectAsStateWithLifecycle()
    if (status.allows(feature)) {
        content()
    } else {
        PaywallScreen(feature = feature, onBack = onBack)
    }
}

@Composable
fun rememberPremiumAccess(): PremiumAccess {
    val context = LocalContext.current
    return remember { PremiumAccess.get(context) }
}

@Composable
fun PaywallScreen(
    feature: PremiumFeature,
    onBack: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val access = rememberPremiumAccess()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.premium_title),
            style = MaterialTheme.typography.headlineSmall,
            color = tc.TextPrimary,
        )
        Text(
            stringResource(feature.lockedMessage()),
            style = MaterialTheme.typography.bodyLarge,
            color = tc.TextPrimary,
        )
        Text(
            stringResource(R.string.premium_body),
            style = MaterialTheme.typography.bodyMedium,
            color = tc.TextSecondary,
        )
        Text(
            stringResource(R.string.premium_terms),
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
        )
        PremiumPurchaseButton(modifier = Modifier.fillMaxWidth())
        TextButton(
            onClick = { access.refresh() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.premium_restore))
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.common_back))
        }
    }
}

@Composable
fun PremiumSettingsBanner() {
    val tc = LocalTruckColors.current
    val access = rememberPremiumAccess()
    val status by access.status.collectAsStateWithLifecycle()
    val label = if (status.subscribed) {
        stringResource(R.string.premium_active)
    } else {
        stringResource(R.string.premium_play_trial)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.premium_title), style = MaterialTheme.typography.titleMedium, color = tc.TextPrimary)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = tc.TextSecondary)
        Text(stringResource(R.string.premium_body), style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary)
        if (status.subscribed) {
            ManageSubscriptionButton()
        } else {
            Text(
                stringResource(R.string.premium_terms),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
            TextButton(onClick = { access.refresh() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.premium_restore))
            }
            PremiumPurchaseButton(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun PremiumLockedCard(feature: PremiumFeature) {
    val tc = LocalTruckColors.current
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(feature.lockedMessage()),
            style = MaterialTheme.typography.bodyMedium,
            color = tc.TextSecondary,
        )
        TlButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.premium_subscribe))
        }
    }
    if (open) {
        PremiumPaywallDialog(onDismiss = { open = false })
    }
}

/** Same purchase dialog the locked-feature card uses. */
@Composable
fun PremiumPaywallDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.premium_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.premium_body))
                Text(stringResource(R.string.premium_terms))
            }
        },
        confirmButton = {
            PremiumPurchaseButton(
                filled = false,
                onFinished = { error -> if (error == null) onDismiss() },
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun PremiumPurchaseButton(
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    onFinished: (String?) -> Unit = {},
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val access = rememberPremiumAccess()
    var buying by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var quote by remember { mutableStateOf<PremiumOfferQuote?>(null) }
    LaunchedEffect(access) {
        quote = access.loadOffer()
        loaded = true
    }
    val label = subscribeLabel(loaded, quote)
    fun startPurchase() {
        val activity = context.findActivity() ?: return
        buying = true
        access.purchase(activity) { error ->
            buying = false
            if (error != null) {
                Toast.makeText(
                    context,
                    resources.getString(R.string.premium_purchase_unavailable),
                    Toast.LENGTH_LONG,
                ).show()
            }
            onFinished(error)
        }
    }
    if (filled) {
        TlButton(onClick = ::startPurchase, enabled = !buying, modifier = modifier) { Text(label) }
    } else {
        TextButton(onClick = ::startPurchase, enabled = !buying, modifier = modifier) { Text(label) }
    }
}

@Composable
private fun ManageSubscriptionButton() {
    val context = LocalContext.current
    val access = rememberPremiumAccess()
    TextButton(
        onClick = {
            val activity = context.findActivity() ?: return@TextButton
            access.openSubscriptionManagement(activity)
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.premium_manage))
    }
}

@Composable
private fun subscribeLabel(loaded: Boolean, quote: PremiumOfferQuote?): String {
    if (!loaded) return stringResource(R.string.premium_price_loading)
    val price = quote?.formattedPrice
    return when {
        price != null && quote.includesFreeMonth -> stringResource(R.string.premium_subscribe_price, price)
        price != null -> stringResource(R.string.premium_subscribe_paid, price)
        else -> stringResource(R.string.premium_subscribe)
    }
}

private fun PremiumFeature.lockedMessage(): Int = when (this) {
    PremiumFeature.TELEGRAM -> R.string.premium_locked_telegram
    PremiumFeature.ANALYTICS -> R.string.premium_locked_analytics
    PremiumFeature.WEEKLY_GOAL -> R.string.premium_locked_goal
    PremiumFeature.MAP -> R.string.premium_locked_map
    PremiumFeature.TAX -> R.string.premium_locked_tax
    PremiumFeature.SCANNER -> R.string.premium_locked_scanner
    PremiumFeature.CAMERA -> R.string.premium_locked_camera
    PremiumFeature.EXPORT -> R.string.premium_locked_export
    PremiumFeature.DRIVE -> R.string.premium_locked_drive
}
