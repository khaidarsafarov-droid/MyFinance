package com.truckerload.presentation.components

import com.truckerload.presentation.icons.AppIcons
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.truckerload.R
import com.truckerload.presentation.navigation.Routes
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

/** Full-width Material 3 bar: indicator on the selected icon, labels always visible. */
@Composable
internal fun SoftBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        phoneTabs.forEach { tab ->
            MaterialNavItem(
                route = tab.route,
                icon = tab.icon,
                labelRes = tab.labelRes,
                selected = isPhoneDestinationSelected(currentRoute, tab.route),
                onNavigate = onNavigate,
            )
        }
    }
}

@Composable
private fun RowScope.MaterialNavItem(
    route: String,
    icon: ImageVector,
    labelRes: Int,
    selected: Boolean,
    onNavigate: (String) -> Unit,
) {
    val label = stringResource(labelRes)
    NavigationBarItem(
        selected = selected,
        onClick = {
            FeedbackManager.onNavSelect()
            onNavigate(route)
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        alwaysShowLabel = true,
    )
}
