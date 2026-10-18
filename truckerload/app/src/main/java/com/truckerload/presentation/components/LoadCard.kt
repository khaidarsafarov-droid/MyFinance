package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.preferences.RpmThresholds
import com.truckerload.domain.model.Load
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.theme.loadSharedBounds
import com.truckerload.presentation.theme.pressScale
import java.util.Locale

@Composable
fun LoadCard(
    load: Load,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    solidBackground: Boolean = false,
    wrapInCard: Boolean = true,
    rpmThresholds: RpmThresholds? = null,
    onCameraClick: (() -> Unit)? = null,
    onScanClick: (() -> Unit)? = null,
) {
    // Разные ветки — единственный корректный способ избежать collectAsState в каждой карточке списка.
    if (rpmThresholds != null) {
        LoadCardContent(
            load = load,
            onClick = onClick,
            modifier = modifier,
            solidBackground = solidBackground,
            wrapInCard = wrapInCard,
            rpmThresholds = rpmThresholds,
            onCameraClick = onCameraClick,
            onScanClick = onScanClick,
        )
    } else {
        val thresholds by LocalRpmThresholdsStore.current.thresholds.collectAsStateWithLifecycle()
        LoadCardContent(
            load = load,
            onClick = onClick,
            modifier = modifier,
            solidBackground = solidBackground,
            wrapInCard = wrapInCard,
            rpmThresholds = thresholds,
            onCameraClick = onCameraClick,
            onScanClick = onScanClick,
        )
    }
}

@Composable
private fun LoadCardContent(
    load: Load,
    onClick: () -> Unit,
    modifier: Modifier,
    @Suppress("UNUSED_PARAMETER") solidBackground: Boolean,
    wrapInCard: Boolean,
    rpmThresholds: RpmThresholds,
    onCameraClick: (() -> Unit)?,
    onScanClick: (() -> Unit)?,
) {
    val tc = LocalTruckColors.current
    val disputeStain = if (load.isDispute) tc.Danger else null
    val disputeWash = if (load.isActiveDispute) 0.16f else 0.10f

    val cardContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (disputeStain != null) {
                        Modifier.drawBehind {
                            drawRect(disputeStain.copy(alpha = disputeWash))
                            drawRect(
                                color = disputeStain,
                                size = Size(6.dp.toPx(), size.height),
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            LoadCardOptionC(
                load = load,
                rpmThresholds = rpmThresholds,
                onCameraClick = onCameraClick,
                onScanClick = onScanClick,
            )
        }
    }

    // Shared bounds only when this composable owns the outer card chrome.
    // SwipeableLoadCard applies [loadSharedBounds] on its Surface instead.
    val cardModifier = if (wrapInCard) {
        modifier.loadSharedBounds(load.id)
    } else {
        modifier
    }

    if (wrapInCard) {
        LoadCardSurface(
            onClick = onClick,
            modifier = cardModifier
                .fillMaxWidth()
                .heightIn(min = UiDimens.LoadCardMinHeight),
            content = { cardContent() },
        )
    } else {
        androidx.compose.foundation.layout.Box(
            modifier = cardModifier
                .fillMaxWidth()
                .heightIn(min = UiDimens.LoadCardMinHeight)
                .minimumInteractiveComponentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
        ) {
            cardContent()
        }
    }
}

/** Computes RPM. Returns null when miles are zero. */
fun computeRpm(totalRate: Double, totalMiles: Double): Double? =
    if (totalMiles > 0) totalRate / totalMiles else null

fun computeRpm(totalRate: com.truckerload.domain.money.Money, totalMiles: Double): Double? =
    computeRpm(totalRate.cents / 100.0, totalMiles)

/** RPM = Total Amount / Total Miles. */
fun formatRpm(totalRate: com.truckerload.domain.money.Money, totalMiles: Double, unitFormat: String): String =
    formatRpm(totalRate.cents / 100.0, totalMiles, unitFormat)

fun formatRpm(totalRate: Double, totalMiles: Double, unitFormat: String): String {
    return if (totalMiles > 0) {
        String.format(Locale.US, unitFormat, totalRate / totalMiles)
    } else {
        "—"
    }
}

@Composable
internal fun LoadCardSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val brand = LocalBrandPalette.current
    val shape = RoundedCornerShape(22.dp)
    Surface(
        onClick = onClick,
        modifier = modifier
            .pressScale()
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = brand.shadow,
                spotColor = brand.shadow,
            ),
        shape = shape,
        color = brand.card,
        border = BorderStroke(1.dp, brand.cardBorder),
        shadowElevation = 0.dp,
    ) {
        content()
    }
}

@Composable
internal fun DisputeCardChip(
    load: Load,
    modifier: Modifier = Modifier,
) {
    val label = if (load.hadDispute) {
        stringResource(R.string.dispute_was_dispute)
    } else {
        stringResource(R.string.dispute_active)
    }
    val color = LocalTruckColors.current.Danger
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = AppTypography.Caption.copy(color = color),
        )
    }
}
