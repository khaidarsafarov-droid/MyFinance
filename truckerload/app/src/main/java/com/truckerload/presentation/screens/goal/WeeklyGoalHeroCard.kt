package com.truckerload.presentation.screens.goal

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckerload.R
import com.truckerload.domain.goal.WeeklyGoalProgress
import com.truckerload.presentation.components.LocalOpenDrawer
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.LocalReduceMotion
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.theme.motionDurationMs
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.presentation.utils.useNavigationRail

@Composable
internal fun GoalHeroCard(
    progress: WeeklyGoalProgress,
    isEditingGoal: Boolean,
    goalInput: String,
    goalError: String?,
    isSavingGoal: Boolean,
    suggestedGoals: List<Double>,
    onStartEdit: () -> Unit,
    onGoalInputChange: (String) -> Unit,
    onSaveGoal: () -> Unit,
    onCancelEdit: () -> Unit,
    onSuggestedGoal: (Double) -> Unit,
    motionSettled: Boolean,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current
    val openDrawer = LocalOpenDrawer.current
    val showMenu = !useNavigationRail()
    val onHero = Color.White
    val onHeroMuted = Color.White.copy(alpha = 0.86f)
    val amount = MoneyFormat.formatCurrency(progress.currentGross)
    val target = MoneyFormat.formatCurrency(progress.targetAmount)
    val remaining = MoneyFormat.formatCurrency(progress.remainingAmount)
    val perDay = MoneyFormat.formatCurrency(progress.dailyTargetNeeded)
    val expected = MoneyFormat.formatCurrency(progress.expectedGrossByNow, decimals = 0)
    val percent = progress.progressPercent.toInt()
    val expectedFraction = if (progress.targetAmount.isPositive()) {
        (progress.expectedGrossByNow.cents.toFloat() / progress.targetAmount.cents).coerceIn(0f, 1f)
    } else {
        0f
    }
    val barDescription = stringResource(R.string.goal_percent_caption, percent) +
        ", " + stringResource(R.string.goal_expected_caption, expected)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.horizontalGradient(listOf(brand.heroStart, brand.heroEnd)))
            .padding(bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 12.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showMenu) {
                IconButton(
                    onClick = openDrawer,
                    modifier = Modifier.size(UiDimens.ToolbarTouchTarget),
                ) {
                    Icon(
                        AppIcons.Menu,
                        contentDescription = stringResource(R.string.common_menu),
                        tint = onHero,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (!isEditingGoal) {
                Box(
                    modifier = Modifier
                        .size(UiDimens.ToolbarTouchTarget)
                        .clickable(role = Role.Button, onClick = onStartEdit),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            AppIcons.Edit,
                            contentDescription = stringResource(R.string.goal_edit_target),
                            tint = onHero,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.goal_screen_title),
            color = onHero,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp),
        )
        if (progress.weekLabel.isNotBlank()) {
            Text(
                text = progress.weekLabel,
                color = onHeroMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp),
            )
        }
        if (isEditingGoal) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(brand.card)
                    .padding(16.dp),
            ) {
                WeeklyGoalEditForm(
                    goalInput = goalInput,
                    goalError = goalError,
                    isSavingGoal = isSavingGoal,
                    suggestedGoals = suggestedGoals,
                    onGoalInputChange = onGoalInputChange,
                    onSaveGoal = onSaveGoal,
                    onCancelEdit = onCancelEdit,
                    onSuggestedGoal = onSuggestedGoal,
                )
            }
        } else {
            Text(
                text = stringResource(R.string.goal_earned_label),
                color = onHeroMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = amount,
                    color = onHero,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    lineHeight = 44.sp,
                    style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = stringResource(R.string.goal_of_amount, target),
                    color = onHeroMuted,
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
                    maxLines = 1,
                )
            }
            GoalHeroBar(
                progressFraction = (progress.progressPercent / 100f).coerceIn(0f, 1f),
                expectedFraction = expectedFraction,
                motionSettled = motionSettled,
                contentDescription = barDescription,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.goal_percent_caption, percent),
                    color = onHero,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(onHero.copy(alpha = 0.7f)),
                )
                Text(
                    text = stringResource(R.string.goal_expected_caption, expected),
                    color = onHero,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 1,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HeroTile(
                    label = stringResource(R.string.goal_tile_remaining),
                    value = remaining,
                    modifier = Modifier.weight(1f),
                )
                HeroTile(
                    label = stringResource(R.string.goal_tile_per_day),
                    value = perDay,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun GoalHeroBar(
    progressFraction: Float,
    expectedFraction: Float,
    motionSettled: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val reduce = LocalReduceMotion.current
    var play by remember { mutableStateOf(false) }
    LaunchedEffect(motionSettled) {
        if (motionSettled) play = true
    }
    val fill by animateFloatAsState(
        targetValue = if (play) progressFraction else 0f,
        animationSpec = if (reduce || !play) snap() else tween(motionDurationMs(reduce, 700)),
        label = "goalHeroFill",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.28f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(fill.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White),
        )
        if (play && expectedFraction > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(expectedFraction.coerceIn(0.04f, 0.96f)),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(3.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White),
                )
            }
        }
    }
}

@Composable
private fun HeroTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.86f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 1,
        )
    }
}
