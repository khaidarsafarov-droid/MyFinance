package com.truckerload.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.di.LocalMaintenanceRepository
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.navigation.Routes
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.LocalReduceMotion
import com.truckerload.utils.FeedbackManager
import kotlinx.coroutines.flow.map

private data class PhoneTab(
    val route: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val labelRes: Int,
)

private val phoneTabs = listOf(
    PhoneTab(Routes.HOME, AppIcons.HomeOutlined, AppIcons.Home, R.string.nav_home),
    PhoneTab(Routes.STATS, AppIcons.InsightsOutlined, AppIcons.Insights, R.string.nav_stats),
    PhoneTab(Routes.PROFILE, AppIcons.PersonOutlined, AppIcons.Person, R.string.nav_profile),
)

/**
 * Floating pill for Home, Stats, and Profile. Add-load stays the home FAB.
 * [onQuickAction] stays on the scaffold contract; the tablet rail still calls it.
 */
@Composable
internal fun SoftBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onQuickAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maintenance = LocalMaintenanceRepository.current
    val overdue by remember(maintenance) {
        maintenance.watchActiveProgress().map { items -> items.count { it.isDue } }
    }.collectAsStateWithLifecycle(initialValue = 0)
    val brand = LocalBrandPalette.current
    val shape = RoundedCornerShape(32.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = brand.shadow,
                spotColor = brand.shadow,
            ),
        shape = shape,
        color = brand.card,
        border = BorderStroke(1.dp, brand.cardBorder),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            phoneTabs.forEach { tab ->
                PhoneNavTab(
                    tab = tab,
                    selected = isPhoneDestinationSelected(currentRoute, tab.route),
                    badgeCount = if (tab.route == Routes.PROFILE) overdue else 0,
                    onClick = {
                        FeedbackManager.onNavSelect()
                        onNavigate(tab.route)
                    },
                )
            }
        }
    }
}

@Composable
private fun PhoneNavTab(
    tab: PhoneTab,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
) {
    val brand = LocalBrandPalette.current
    val reduceMotion = LocalReduceMotion.current
    val label = stringResource(tab.labelRes)
    val spoken = if (badgeCount > 0) {
        stringResource(R.string.nav_profile_due_badge, badgeCount)
    } else {
        label
    }
    val motion = if (reduceMotion) {
        snap<Float>()
    } else {
        spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
    }
    val indicatorScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.72f,
        animationSpec = motion,
        label = "navIndicatorScale",
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = motion,
        label = "navIndicatorAlpha",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = motion,
        label = "navIconScale",
    )
    val tint by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) brand.primary else brand.textSecondary,
        animationSpec = if (reduceMotion) snap() else tween(180),
        label = "navTint",
    )
    val interaction = remember { MutableInteractionSource() }
    val fade = if (reduceMotion) snap<Float>() else tween<Float>(160)

    Column(
        modifier = Modifier
            .widthIn(min = 76.dp)
            .heightIn(min = 64.dp)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                role = Role.Tab
                contentDescription = spoken
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .graphicsLayer {
                        scaleX = indicatorScale
                        scaleY = indicatorScale
                        alpha = indicatorAlpha
                    }
                    .clip(CircleShape)
                    .background(brand.container),
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .indication(interaction, ripple(bounded = true, color = brand.primary)),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = selected,
                    transitionSpec = { fadeIn(fade) togetherWith fadeOut(fade) },
                    label = "navIcon",
                ) { on ->
                    Icon(
                        imageVector = if (on) tab.selectedIcon else tab.icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier
                            .size(28.dp)
                            .scale(iconScale),
                    )
                }
            }
            if (badgeCount > 0) {
                MaintenanceDueBadge(count = badgeCount)
            }
        }
        Text(
            text = label,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun BoxScope.MaintenanceDueBadge(count: Int) {
    val label = if (count > 9) "9+" else count.toString()
    Text(
        text = label,
        color = MaterialTheme.colorScheme.onError,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 8.dp, y = (-2).dp)
            .heightIn(min = 16.dp)
            .border(1.5.dp, LocalBrandPalette.current.card, CircleShape)
            .background(MaterialTheme.colorScheme.error, CircleShape)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}
