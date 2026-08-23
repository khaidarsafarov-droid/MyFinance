package com.truckerload.presentation.components

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.navigation.Routes
import com.truckerload.presentation.theme.OneUiTokens
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.theme.rememberReduceMotion
import com.truckerload.utils.FeedbackManager

private data class PhoneTab(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int,
)

private val phoneTabs = listOf(
    PhoneTab(Routes.HOME, AppIcons.LocalShipping, R.string.nav_logbook),
    PhoneTab(Routes.STATS, AppIcons.Flag, R.string.nav_weekly_goal),
    PhoneTab(Routes.PROFILE, AppIcons.Person, R.string.nav_profile),
)

@Composable
internal fun SoftBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(
                start = OneUiTokens.BottomBarHorizontalInset,
                end = OneUiTokens.BottomBarHorizontalInset,
                bottom = OneUiTokens.BottomBarBottomInset,
            ),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            color = cs.surface,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(UiDimens.NavBarHeight)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                phoneTabs.forEach { tab ->
                    ExpandingPillNavItem(
                        route = tab.route,
                        icon = tab.icon,
                        labelRes = tab.labelRes,
                        selected = isPhoneDestinationSelected(currentRoute, tab.route),
                        onNavigate = onNavigate,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandingPillNavItem(
    route: String,
    icon: ImageVector,
    labelRes: Int,
    selected: Boolean,
    onNavigate: (String) -> Unit,
) {
    val label = stringResource(labelRes)
    val cs = MaterialTheme.colorScheme
    val reduceMotion = rememberReduceMotion()
    val colorSpec = OneUiTokens.motionSpec<Color>(reduceMotion, OneUiTokens.MotionShortMs)
    val fadeSpec = OneUiTokens.motionSpec<Float>(reduceMotion, OneUiTokens.MotionShortMs)
    val sizeSpec = OneUiTokens.motionSpec<IntSize>(reduceMotion, OneUiTokens.MotionShortMs)
    val pillColor by animateColorAsState(
        targetValue = if (selected) cs.onSurface else Color.Transparent,
        animationSpec = colorSpec,
        label = "navPillBg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) cs.surface else cs.onSurface.copy(alpha = 0.78f),
        animationSpec = colorSpec,
        label = "navPillFg",
    )
    val horizontalPad by animateDpAsState(
        targetValue = if (selected) 14.dp else 12.dp,
        animationSpec = OneUiTokens.motionSpec(reduceMotion, OneUiTokens.MotionShortMs),
        label = "navPillPad",
    )
    val enter = fadeIn(fadeSpec) + expandHorizontally(
        animationSpec = sizeSpec,
        expandFrom = Alignment.Start,
    )
    val exit = fadeOut(fadeSpec) + shrinkHorizontally(
        animationSpec = sizeSpec,
        shrinkTowards = Alignment.Start,
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(CircleShape)
            .background(pillColor)
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = selected
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    FeedbackManager.onNavSelect()
                    onNavigate(route)
                },
            )
            .touchTarget()
            .padding(horizontal = horizontalPad, vertical = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(UiDimens.IconNavCompact),
            tint = contentColor,
        )
        AnimatedVisibility(
            visible = selected,
            enter = enter,
            exit = exit,
        ) {
            Text(
                text = label,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
