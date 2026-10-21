package com.truckerload.presentation.screens.settings

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.domain.premium.PremiumOfferQuote
import com.truckerload.presentation.premium.rememberPremiumAccess
import com.truckerload.utils.findActivity

@Composable
internal fun SettingsPremiumHeroHost(
    showBack: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val access = rememberPremiumAccess()
    val subscribed by access.isSubscribed.collectAsStateWithLifecycle()
    var loaded by remember { mutableStateOf(false) }
    var quote by remember { mutableStateOf<PremiumOfferQuote?>(null) }
    var buying by remember { mutableStateOf(false) }
    LaunchedEffect(access) {
        quote = access.loadOffer()
        loaded = true
    }
    val model = SettingsPremiumState.hero(subscribed, loaded, quote)
    SettingsPremiumHero(
        model = model,
        showBack = showBack,
        onBack = onBack,
        purchasing = buying,
        onManage = {
            val activity = context.findActivity() ?: return@SettingsPremiumHero
            access.openSubscriptionManagement(activity)
        },
        onPurchase = {
            val activity = context.findActivity() ?: return@SettingsPremiumHero
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
            }
        },
        onRestore = { access.refresh() },
        modifier = modifier,
        footer = {
            TrialEndReminderBanner(
                onManage = {
                    val activity = context.findActivity() ?: return@TrialEndReminderBanner
                    access.openSubscriptionManagement(activity)
                },
            )
        },
    )
}
