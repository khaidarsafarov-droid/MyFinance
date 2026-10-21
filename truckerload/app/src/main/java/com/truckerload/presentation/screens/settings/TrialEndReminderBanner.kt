package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.premium.TrialReminderStore
import com.truckerload.domain.premium.PremiumOfferPrice
import com.truckerload.domain.premium.TrialReminderPolicy
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.premium.rememberPremiumAccess
import com.truckerload.presentation.theme.LocalBrandPalette
import java.time.ZoneId

/**
 * Shown on the settings premium hero while Premium is active and the free month
 * ends within three days. Dismissing hides it for this purchase token only.
 */
@Composable
internal fun TrialEndReminderBanner(onManage: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val access = rememberPremiumAccess()
    val subscribed by access.isSubscribed.collectAsStateWithLifecycle()
    val store = remember { TrialReminderStore(context) }
    var revision by remember { mutableIntStateOf(0) }
    val state = remember(subscribed, revision) { store.read().state }
    val decision = TrialReminderPolicy.decide(
        premiumActive = subscribed,
        state = state,
        nowMillis = System.currentTimeMillis(),
    )
    if (!decision.showBanner) return

    val brand = LocalBrandPalette.current
    val price = PremiumOfferPrice.display(state.formattedPrice)
    val date = TrialReminderPolicy.endDate(state.trialEndMillis, locale, ZoneId.systemDefault())
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = stringResource(R.string.trial_reminder_body, date, price),
                color = brand.heroText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 8.dp),
            )
            IconButton(
                onClick = {
                    store.dismissBanner(state.purchaseToken)
                    revision += 1
                },
            ) {
                Icon(
                    AppIcons.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = brand.heroText,
                )
            }
        }
        Text(
            text = stringResource(R.string.settings_premium_manage_short),
            color = brand.goldOnGold,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(end = 8.dp)
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(listOf(brand.goldStart, brand.goldEnd)))
                .clickable(role = Role.Button, onClick = onManage)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
