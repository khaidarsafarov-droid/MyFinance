package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.LocalSemanticColors

enum class RpmTier {
    Good,
    Ok,
    Low,
}

fun rpmTier(rpm: Double?, minThreshold: Double, targetThreshold: Double): RpmTier? = when {
    rpm == null -> null
    rpm < minThreshold -> RpmTier.Low
    rpm < targetThreshold -> RpmTier.Ok
    else -> RpmTier.Good
}

@Composable
fun rpmTierWord(tier: RpmTier): String = stringResource(
    when (tier) {
        RpmTier.Good -> R.string.rpm_badge_good
        RpmTier.Ok -> R.string.rpm_badge_ok
        RpmTier.Low -> R.string.rpm_badge_low
    },
)

@Composable
fun RpmValueBadge(
    rpmText: String,
    tier: RpmTier,
    modifier: Modifier = Modifier,
) {
    val word = rpmTierWord(tier)
    val (bg, fg) = rpmTierColors(tier)
    Text(
        text = rpmText,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .semantics { contentDescription = "$word, $rpmText" }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = AppTypography.Caption.copy(
            color = fg,
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum",
        ),
    )
}

@Composable
fun RpmTierPill(
    tier: RpmTier,
    modifier: Modifier = Modifier,
) {
    val word = rpmTierWord(tier)
    val (bg, fg) = rpmTierColors(tier)
    Text(
        text = stringResource(R.string.rpm_badge_tier_rpm, word),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium.copy(
            color = fg,
            fontWeight = FontWeight.SemiBold,
        ),
    )
}

@Composable
private fun rpmTierColors(tier: RpmTier) = when (tier) {
    RpmTier.Good -> {
        val semantic = LocalSemanticColors.current
        semantic.successContainer to semantic.success
    }
    RpmTier.Ok -> {
        val semantic = LocalSemanticColors.current
        semantic.warningContainer to semantic.warning
    }
    RpmTier.Low -> {
        val semantic = LocalSemanticColors.current
        semantic.dangerContainer to semantic.danger
    }
}
