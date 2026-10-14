package com.truckerload.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.di.LocalMaintenanceRepository
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.navigation.Routes
import com.truckerload.presentation.theme.LocalReduceMotion
import com.truckerload.utils.FeedbackManager
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

private data class PhoneTab(
    val route: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val labelRes: Int,
)

private val phoneTabsLeft = listOf(
    PhoneTab(Routes.HOME, AppIcons.LocalShippingOutlined, AppIcons.LocalShipping, R.string.nav_logbook),
    PhoneTab(Routes.STATS, AppIcons.FlagOutlined, AppIcons.Flag, R.string.nav_weekly_goal),
)

private val phoneTabProfile = PhoneTab(
    Routes.PROFILE,
    AppIcons.PersonOutlined,
    AppIcons.Person,
    R.string.nav_profile,
)

/**
 * Phone bar: Loads, Goal, a centered add button, Profile.
 * Diesel stays in the menu. The bar is a neutral surface with a hairline,
 * and the selected tab uses a filled icon, brand color, and a heavier label.
 */
@Composable
internal fun SoftBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onQuickAction: () -> Unit,
) {
    val overdue by LocalMaintenanceRepository.current
        .watchActiveProgress()
        .map { items -> items.count { it.isDue } }
        .collectAsStateWithLifecycle(initialValue = 0)
    val reduceMotion = LocalReduceMotion.current
    val barColor = MaterialTheme.colorScheme.surfaceVariant
    val indicator = remember { Animatable(0f) }
    var targetCenter by remember { mutableFloatStateOf(-1f) }
    var barLeft by remember { mutableFloatStateOf(-1f) }
    var seenIndicator by remember { mutableIntStateOf(0) }
    val selectedRoute = listOf(phoneTabsLeft[0], phoneTabsLeft[1], phoneTabProfile)
        .firstOrNull { isPhoneDestinationSelected(currentRoute, it.route) }
        ?.route
    val showIndicator = selectedRoute != null && targetCenter >= 0f && barLeft >= 0f
    val indicatorHalfPx = with(LocalDensity.current) { 12.dp.toPx() }

    LaunchedEffect(targetCenter, barLeft, showIndicator, reduceMotion) {
        if (!showIndicator) return@LaunchedEffect
        val center = targetCenter - barLeft
        if (seenIndicator == 0 || reduceMotion) {
            indicator.snapTo(center)
            seenIndicator = 1
        } else {
            indicator.animateTo(center, tween(durationMillis = 200, easing = EaseOutCubic))
        }
    }

    Surface(color = barColor, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                    .onGloballyPositioned { barLeft = it.positionInRoot().x },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        phoneTabsLeft.forEach { tab ->
                            PhoneNavTab(
                                tab = tab,
                                selected = isPhoneDestinationSelected(currentRoute, tab.route),
                                badgeCount = 0,
                                onCenter = { targetCenter = it },
                                onClick = {
                                    FeedbackManager.onNavSelect()
                                    onNavigate(tab.route)
                                },
                            )
                        }
                    }
                    QuickAddButton(onClick = onQuickAction)
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PhoneNavTab(
                            tab = phoneTabProfile,
                            selected = isPhoneDestinationSelected(currentRoute, phoneTabProfile.route),
                            badgeCount = overdue,
                            onCenter = { targetCenter = it },
                            onClick = {
                                FeedbackManager.onNavSelect()
                                onNavigate(phoneTabProfile.route)
                            },
                        )
                    }
                }
                if (showIndicator) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset {
                                IntOffset((indicator.value - indicatorHalfPx).roundToInt(), 0)
                            }
                            .padding(bottom = 3.dp)
                            .width(24.dp)
                            .height(2.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(1.dp),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneNavTab(
    tab: PhoneTab,
    selected: Boolean,
    badgeCount: Int,
    onCenter: (Float) -> Unit,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val label = stringResource(tab.labelRes)
    val spoken = if (badgeCount > 0) {
        stringResource(R.string.nav_profile_due_badge, badgeCount)
    } else {
        label
    }
    val tint = if (selected) cs.primary else cs.onSurfaceVariant
    Column(
        modifier = Modifier
            .widthIn(min = 64.dp)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .onGloballyPositioned { coords ->
                if (selected) onCenter(coords.positionInRoot().x + coords.size.width / 2f)
            }
            .semantics(mergeDescendants = true) { contentDescription = spoken },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            Icon(
                imageVector = if (selected) tab.selectedIcon else tab.icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
            if (badgeCount > 0) {
                MaintenanceDueBadge(count = badgeCount)
            }
        }
        Text(
            text = label,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
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
            .offset(x = 10.dp, y = (-6).dp)
            .heightIn(min = 16.dp)
            .border(1.5.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .background(MaterialTheme.colorScheme.error, CircleShape)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}

@Composable
private fun QuickAddButton(onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .size(48.dp)
            .shadow(2.dp, CircleShape)
            .clip(CircleShape)
            .background(cs.primary)
            .clickable(role = Role.Button, onClick = {
                FeedbackManager.onNavSelect()
                onClick()
            }),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.Add,
            contentDescription = stringResource(R.string.quick_actions_title),
            tint = cs.onPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}
