package com.truckerload.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(RESULT_CANCELED)
        val existing = WidgetPrefsStore.load(applicationContext, appWidgetId)

        setContent {
            TruckerLoadTheme {
                var sizeMode by rememberSaveable { mutableStateOf(existing.sizeMode) }
                var showGross by rememberSaveable { mutableStateOf(existing.showGross) }
                var showPace by rememberSaveable { mutableStateOf(existing.showPace) }
                var showGoal by rememberSaveable { mutableStateOf(existing.showGoal) }
                var themeMode by rememberSaveable { mutableStateOf(existing.themeMode) }

                WidgetConfigureScreen(
                    sizeMode = sizeMode,
                    showGross = showGross,
                    showPace = showPace,
                    showGoal = showGoal,
                    themeMode = themeMode,
                    onSizeModeChange = { sizeMode = it },
                    onShowGrossChange = { showGross = it },
                    onShowPaceChange = { showPace = it },
                    onShowGoalChange = { showGoal = it },
                    onThemeModeChange = { themeMode = it },
                    onSave = {
                        WidgetPrefsStore.save(
                            applicationContext,
                            appWidgetId,
                            WidgetPrefs(
                                sizeMode = sizeMode,
                                showGross = showGross,
                                showPace = showPace,
                                showGoal = showGoal,
                                themeMode = themeMode,
                            ),
                        )
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                        )
                        WidgetRefresh.paintCached(applicationContext, intArrayOf(appWidgetId))
                        WidgetRefresh.refreshAndUpdateAsync(applicationContext)
                        finish()
                    },
                )
            }
        }
    }
}
