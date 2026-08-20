package com.truckerload.presentation.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.domain.friends.FriendsLocationSharePolicy
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.sync.FriendsLocationShareScheduler
import kotlinx.coroutines.launch

@Composable
fun FriendsLocationSettingsSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val tc = LocalTruckColors.current
    val settings = LocalSettingsDataStore.current
    val scope = rememberCoroutineScope()
    val sharePath by settings.sharePathWithFriends.collectAsStateWithLifecycle(false)
    val interval by settings.friendsLocationIntervalMinutes.collectAsStateWithLifecycle(
        FriendsLocationSharePolicy.DEFAULT_INTERVAL_MINUTES,
    )
    val liveMode by settings.friendsLiveMode.collectAsStateWithLifecycle(false)

    val activityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { FriendsLocationShareScheduler.sync(context) }
    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { FriendsLocationShareScheduler.sync(context) }

    BentoGlassSection(
        title = stringResource(R.string.settings_friends_location_title),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.settings_friends_location_interval),
            color = tc.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
        )
        FriendsLocationSharePolicy.allowedIntervalsMinutes.forEach { minutes ->
            val label = stringResource(R.string.settings_friends_location_interval_option, minutes)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = interval == minutes,
                    onClick = {
                        scope.launch {
                            settings.saveFriendsLocationIntervalMinutes(minutes)
                            FriendsLocationShareScheduler.sync(context)
                        }
                    },
                )
                Text(label, color = tc.TextPrimary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(
            text = stringResource(R.string.settings_friends_location_interval_hint),
            color = tc.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_friends_live_mode),
                    color = tc.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.settings_friends_live_mode_desc),
                    color = tc.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = liveMode,
                onCheckedChange = { enabled ->
                    if (enabled && !sharePath) {
                        scope.launch { settings.saveSharePathWithFriends(true) }
                    }
                    requestOptionalBackgroundPermissions(
                        context = context,
                        activityLauncher = { activityLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                        backgroundLauncher = {
                            if (Build.VERSION.SDK_INT >= 29) {
                                backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            }
                        },
                    )
                    if (enabled) {
                        FriendsLocationShareScheduler.startLiveSession(context, fromUserToggle = true)
                    } else {
                        FriendsLocationShareScheduler.stopLiveSession(context)
                    }
                },
                colors = AppSwitchDefaults.colors(),
            )
        }
    }
}

private fun requestOptionalBackgroundPermissions(
    context: android.content.Context,
    activityLauncher: () -> Unit,
    backgroundLauncher: () -> Unit,
) {
    if (Build.VERSION.SDK_INT >= 29 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        activityLauncher()
    }
    if (Build.VERSION.SDK_INT >= 29 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        backgroundLauncher()
    }
}
