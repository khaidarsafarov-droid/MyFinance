package com.truckerload.presentation.screens.detail

import android.graphics.Bitmap
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.truckerload.data.backup.BackupTestFixtures
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.domain.money.Money
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.TruckerLoadTheme
import java.io.File
import java.io.FileOutputStream
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w390dp-h1600dp")
class LoadDetailScreenshotTest {

    @Test
    fun captureFilledDark() {
        capture(dark = true, name = "load-detail-after.png", filled = true)
    }

    @Test
    @Config(qualifiers = "ru-rRU-w390dp-h1600dp")
    fun captureFilledRussian() {
        capture(dark = true, name = "load-detail-after-ru.png", filled = true)
    }

    @Test
    fun captureEmptyLight() {
        capture(dark = false, name = "load-detail-after-empty.png", filled = false)
    }

    private fun capture(dark: Boolean, name: String, filled: Boolean) {
        val load = if (filled) {
            BackupTestFixtures.sampleLoad().copy(
                deadheadMiles = 45.0,
                isDispute = true,
                disputeResponseDate = "2026-07-25",
                disputeAmount = Money.fromDollars(120.0),
            ).withRouteMetrics()
        } else {
            BackupTestFixtures.sampleLoad().copy(
                totalMiles = 0.0,
                deadheadMiles = 0.0,
                actualFinishDate = null,
                isDispute = false,
                stops = emptyList(),
                puCount = 0,
                delCount = 0,
                stopCount = 0,
                durationDays = 0.0,
                pace = Money.ZERO,
                totalRate = Money.ZERO,
            ).withRouteMetrics()
        }
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            TruckerLoadTheme(darkTheme = dark, dynamicColor = false, reduceMotion = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BentoGlassTheme.ScreenBackground),
                ) {
                    LoadDetailLoadedContent(
                        load = load,
                        photos = emptyList(),
                        scans = if (filled) {
                            listOf(
                                ScanEntity(
                                    id = "scan-1",
                                    fileName = "rate-con.pdf",
                                    filePath = "/tmp/rate-con.pdf",
                                    timestamp = 0L,
                                    fileSizeBytes = 1200L,
                                    pageCount = 2,
                                ),
                            )
                        } else {
                            emptyList()
                        },
                        onEditMiles = {},
                        onEditFinish = {},
                        onSaveDeadhead = {},
                        onDisputeChanged = {},
                        onPhotoClick = {},
                        onCamera = {},
                        onScanner = {},
                        onScanClick = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        val decor = activity.window.decorView
        val width = decor.width.takeIf { it > 0 } ?: 390
        val height = decor.height.takeIf { it > 0 } ?: 1600
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        decor.layout(0, 0, width, height)
        repeat(6) { shadowOf(Looper.getMainLooper()).idle() }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(android.graphics.Canvas(bitmap))
        listOf(File("/opt/cursor/artifacts"), File("/tmp")).forEach { dir ->
            runCatching {
                dir.mkdirs()
                FileOutputStream(File(dir, name)).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        }
    }
}
