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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.premium.PremiumAccess
import com.truckerload.domain.premium.PremiumFeature
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
    val context = LocalContext.current
    val access = rememberPremiumAccess()
    var buying by remember { mutableStateOf(false) }
    var price by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(access) { price = access.formattedPrice() }
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
        TlButton(
            onClick = {
                val activity = context.findActivity() ?: return@TlButton
                buying = true
                access.purchase(activity) { error ->
                    buying = false
                    if (error != null) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.premium_purchase_unavailable),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            },
            enabled = !buying,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                price?.let { stringResource(R.string.premium_subscribe_price, it) }
                    ?: stringResource(R.string.premium_subscribe),
            )
        }
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
    val label = when {
        status.subscribed -> stringResource(R.string.premium_active)
        status.inTrial -> stringResource(
            R.string.premium_trial_days,
            status.trialDaysLeft(System.currentTimeMillis()),
        )
        else -> stringResource(R.string.premium_trial_ended)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.premium_title), style = MaterialTheme.typography.titleMedium, color = tc.TextPrimary)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = tc.TextSecondary)
        Text(stringResource(R.string.premium_body), style = MaterialTheme.typography.bodySmall, color = tc.TextSecondary)
        if (!status.subscribed) {
            val context = LocalContext.current
            TextButton(onClick = { access.refresh() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.premium_restore))
            }
            TlButton(onClick = {
                val activity = context.findActivity() ?: return@TlButton
                access.purchase(activity) { error ->
                    if (error != null) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.premium_purchase_unavailable),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.premium_subscribe))
            }
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
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.premium_title)) },
            text = { Text(stringResource(R.string.premium_body)) },
            confirmButton = {
                val context = LocalContext.current
                val access = rememberPremiumAccess()
                TextButton(onClick = {
                    val activity = context.findActivity() ?: return@TextButton
                    access.purchase(activity) { error ->
                        if (error == null) open = false
                        else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.premium_purchase_unavailable),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                }) { Text(stringResource(R.string.premium_subscribe)) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
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
    PremiumFeature.DEADHEAD -> R.string.premium_locked_deadhead
}
