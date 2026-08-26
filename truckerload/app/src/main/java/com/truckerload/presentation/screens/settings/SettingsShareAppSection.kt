package com.truckerload.presentation.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.BentoGlassSection
import com.truckerload.utils.AppApkShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun SettingsShareAppSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    BentoGlassSection(
        title = stringResource(R.string.settings_share_app_title),
        subtitle = stringResource(R.string.settings_share_app_body),
    ) {
        OutlinedButton(
            onClick = {
                if (busy) return@OutlinedButton
                scope.launch {
                    busy = true
                    val ok = withContext(Dispatchers.IO) {
                        AppApkShare.shareInstalledApk(context)
                    }
                    busy = false
                    if (!ok) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_share_app_unavailable),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(stringResource(R.string.settings_share_app_button))
        }
    }
}
