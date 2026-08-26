package com.truckerload.presentation.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.utils.AppStoreLinkShare
import com.truckerload.utils.StoreListings

@Composable
internal fun SettingsShareAppSection() {
    val context = LocalContext.current
    val hasIosListing = StoreListings.appStoreHttpsUrl() != null
    val bodyRes = if (hasIosListing) {
        R.string.settings_share_app_body
    } else {
        R.string.settings_share_app_body_play_only
    }

    BentoGlassSection(
        title = stringResource(R.string.settings_share_app_title),
        subtitle = stringResource(bodyRes),
    ) {
        OutlinedButton(
            onClick = {
                val ok = AppStoreLinkShare.share(context)
                if (!ok) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_share_app_unavailable),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(stringResource(R.string.settings_share_app_button))
        }
    }
}
