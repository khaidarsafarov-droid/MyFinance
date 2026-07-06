package com.truckerload.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.truckerload.presentation.components.TlButton as Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.TruckerLoadTheme
import com.truckerload.utils.AppLocale

class WidgetConfigureActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(RESULT_CANCELED)
        val existing = WidgetPrefsStore.load(applicationContext, appWidgetId)

        setContent {
            TruckerLoadTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var sizeMode by rememberSaveable { mutableStateOf(existing.sizeMode) }
                    var showGross by rememberSaveable { mutableStateOf(existing.showGross) }
                    var showPace by rememberSaveable { mutableStateOf(existing.showPace) }
                    var showGoal by rememberSaveable { mutableStateOf(existing.showGoal) }
                    var themeMode by rememberSaveable { mutableStateOf(existing.themeMode) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.widget_configure_title),
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Text(stringResource(R.string.widget_configure_size))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                WidgetSizeMode.AUTO to R.string.widget_configure_size_auto,
                                WidgetSizeMode.SMALL to R.string.widget_configure_size_small,
                                WidgetSizeMode.MEDIUM to R.string.widget_configure_size_medium,
                                WidgetSizeMode.LARGE to R.string.widget_configure_size_large
                            ).forEach { (mode, labelRes) ->
                                FilterChip(
                                    selected = sizeMode == mode,
                                    onClick = { sizeMode = mode },
                                    label = { Text(stringResource(labelRes)) }
                                )
                            }
                        }

                        Text(stringResource(R.string.widget_configure_metrics))
                        MetricToggle(
                            label = stringResource(R.string.widget_configure_show_gross),
                            checked = showGross,
                            onCheckedChange = { showGross = it }
                        )
                        MetricToggle(
                            label = stringResource(R.string.widget_configure_show_pace),
                            checked = showPace,
                            onCheckedChange = { showPace = it }
                        )
                        MetricToggle(
                            label = stringResource(R.string.widget_configure_show_goal),
                            checked = showGoal,
                            onCheckedChange = { showGoal = it }
                        )

                        Text(stringResource(R.string.widget_configure_theme))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                WidgetThemeMode.SYSTEM to R.string.widget_configure_theme_system,
                                WidgetThemeMode.LIGHT to R.string.widget_configure_theme_light,
                                WidgetThemeMode.DARK to R.string.widget_configure_theme_dark
                            ).forEach { (mode, labelRes) ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { themeMode = mode },
                                    label = { Text(stringResource(labelRes)) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                WidgetPrefsStore.save(
                                    applicationContext,
                                    appWidgetId,
                                    WidgetPrefs(
                                        sizeMode = sizeMode,
                                        showGross = showGross,
                                        showPace = showPace,
                                        showGoal = showGoal,
                                        themeMode = themeMode
                                    )
                                )
                                val result = Intent().putExtra(
                                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                                    appWidgetId
                                )
                                setResult(RESULT_OK, result)
                                WidgetRefresh.paintCached(applicationContext, intArrayOf(appWidgetId))
                                WidgetRefresh.refreshAndUpdateAsync(applicationContext)
                                finish()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.widget_configure_save))
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MetricToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = AppSwitchDefaults.colors(),
        )
    }
}
