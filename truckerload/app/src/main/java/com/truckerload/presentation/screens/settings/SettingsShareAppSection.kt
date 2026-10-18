package com.truckerload.presentation.screens.settings

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.utils.AppStoreLinkShare

@Composable
internal fun SettingsShareAppSection() {
    val context = LocalContext.current
    val resources = LocalResources.current
    SettingsNavRow(
        icon = AppIcons.Share,
        title = stringResource(R.string.settings_share_app_title),
        subtitle = stringResource(R.string.settings_row_share_subtitle),
        onClick = {
            val ok = AppStoreLinkShare.share(context)
            if (!ok) {
                Toast.makeText(
                    context,
                    resources.getString(R.string.settings_share_app_unavailable),
                    Toast.LENGTH_LONG,
                ).show()
            }
        },
    )
}
