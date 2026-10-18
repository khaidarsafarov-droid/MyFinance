package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import com.truckerload.R
import com.truckerload.presentation.di.LocalMaintenanceRepository
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.navigation.Routes
import com.truckerload.presentation.theme.pressScale
import com.truckerload.utils.FeedbackManager

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
 * Phone bar: Home, Stats, Profile. Add-load is the home extended FAB.
 * [onQuickAction] stays on the scaffold contract; the tablet rail still calls it.
 */
@Composable
internal fun SoftBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onQuickAction: () -> Unit,
) {
    val maintenance = LocalMaintenanceRepository.current
    val overdue by remember(maintenance) {
        maintenance.watchActiveProgress().map { items -> items.count { it.isDue } }
    }.collectAsStateWithLifecycle(initialValue = 0)
    val barColor = MaterialTheme.colorScheme.surface

    Surface(color = barColor, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
}

@Composable
private fun PhoneNavTab(
    tab: PhoneTab,
    selected: Boolean,
    badgeCount: Int,
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
            .widthIn(min = 72.dp)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) cs.primaryContainer else Color.Transparent)
            .pressScale()
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
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
            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            .background(MaterialTheme.colorScheme.error, CircleShape)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}
