package com.truckerload.presentation.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.premium.Day7SurveyStore
import com.truckerload.domain.premium.Day7SurveyPolicy
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.SoftUiShapes
import com.truckerload.sync.Day7SurveyScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Soft Home card. Claiming the display once keeps the card up for this visit
 * without writing [Day7SurveyPolicy.Record.lastShownAt] again on recomposition.
 */
@Composable
fun Day7SurveyBanner(
    onWantMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val store = remember { Day7SurveyStore(context) }
    val access = rememberPremiumAccess()
    val premium by access.isSubscribed.collectAsStateWithLifecycle()
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val brand = LocalBrandPalette.current

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val claimed = withContext(Dispatchers.IO) {
            store.ensureFirstLaunch(now)
            if (access.isUnlocked()) return@withContext false
            val won = store.claimDisplay(now, premiumActive = false)
            if (won) {
                Day7SurveyScheduler.enqueue(context, Day7SurveyPolicy.CADENCE_MILLIS)
            }
            won && !access.isUnlocked()
        }
        if (claimed) visible = true
    }

    LaunchedEffect(premium) {
        if (premium) visible = false
    }

    if (!visible || premium) return

    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(shape)
            .background(brand.card)
            .border(1.dp, brand.goldEnd, shape)
            .padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = stringResource(R.string.day7_survey_question),
                style = MaterialTheme.typography.titleMedium,
                color = brand.textPrimary,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 10.dp),
            )
            IconButton(
                onClick = { visible = false },
            ) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = brand.textSecondary,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = {
                    visible = false
                    scope.launch(Dispatchers.IO) {
                        store.optOut()
                        Day7SurveyScheduler.cancel(context)
                    }
                },
                modifier = Modifier.weight(1f),
                shape = SoftUiShapes.Button,
            ) {
                Text(stringResource(R.string.day7_survey_enough))
            }
            Button(
                onClick = {
                    visible = false
                    onWantMore()
                },
                modifier = Modifier.weight(1f),
                shape = SoftUiShapes.Button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = brand.goldEnd,
                    contentColor = brand.goldOnGold,
                ),
            ) {
                Text(stringResource(R.string.day7_survey_more))
            }
        }
    }
}
