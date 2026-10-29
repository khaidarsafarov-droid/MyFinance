package com.truckerload.presentation.screens.gallery

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.truckerload.presentation.ScreenPhase
import com.truckerload.presentation.theme.TruckerLoadTheme
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the gallery with fictional sample files. Not a golden-image test.
 * PNGs are written to /opt/cursor/artifacts.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class GalleryScreenshotTest {

    @Test
    @Config(qualifiers = "en-rUS-w414dp-h860dp-420dpi")
    fun captureEnglishGallery() {
        val sample = sample()
        capture("gallery-photos-en") {
            GalleryScreenContent(
                tab = GalleryKind.PHOTOS,
                filter = GalleryFilter.ALL,
                query = "",
                cards = sample.photoCards,
                phase = ScreenPhase.Content,
                filteredEmpty = false,
                onBack = {},
                onTab = {},
                onFilter = {},
                onQuery = {},
                onRetry = {},
                onOpenLoad = {},
                onCapture = {},
            )
        }
        capture("gallery-scans-en") {
            GalleryScreenContent(
                tab = GalleryKind.SCANS,
                filter = GalleryFilter.ALL,
                query = "",
                cards = sample.scanCards,
                phase = ScreenPhase.Content,
                filteredEmpty = false,
                onBack = {},
                onTab = {},
                onFilter = {},
                onQuery = {},
                onRetry = {},
                onOpenLoad = {},
                onCapture = {},
            )
        }
        capture("gallery-by-load-en") {
            GalleryScreenContent(
                tab = GalleryKind.PHOTOS,
                filter = GalleryFilter.BY_LOAD,
                query = "Garner",
                cards = sample.photoCards.filter { it.route.contains("Garner") },
                phase = ScreenPhase.Content,
                filteredEmpty = false,
                onBack = {},
                onTab = {},
                onFilter = {},
                onQuery = {},
                onRetry = {},
                onOpenLoad = {},
                onCapture = {},
            )
        }
        capture("gallery-grid-en") {
            GalleryLoadGridContent(
                title = "T-116KYL6KW",
                route = "Garner, NC → Dallas, TX",
                kind = GalleryKind.PHOTOS,
                items = sample.photoItems,
                phase = ScreenPhase.Content,
                selecting = true,
                selectedIds = setOf(sample.photoItems.first().id, sample.photoItems.last().id),
                onBack = {},
                onRetry = {},
                onStartSelect = {},
                onToggle = {},
                onOpen = {},
                onShare = {},
            )
        }
        capture("gallery-viewer-en") {
            GalleryViewerContent(
                title = "T-116KYL6KW",
                items = sample.photoItems,
                focusId = sample.photoItems.first().id,
                onBack = {},
                onShare = {},
                onDelete = {},
            )
        }
    }

    @Test
    @Config(qualifiers = "ru-rRU-w414dp-h860dp-420dpi")
    fun captureRussianGallery() {
        val sample = sample()
        capture("gallery-photos-ru") {
            GalleryScreenContent(
                tab = GalleryKind.SCANS,
                filter = GalleryFilter.THIS_WEEK,
                query = "",
                cards = sample.scanCards,
                phase = ScreenPhase.Content,
                filteredEmpty = false,
                onBack = {},
                onTab = {},
                onFilter = {},
                onQuery = {},
                onRetry = {},
                onOpenLoad = {},
                onCapture = {},
            )
        }
    }

    private fun capture(name: String, content: @Composable () -> Unit) {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            TruckerLoadTheme(darkTheme = true, dynamicColor = false, reduceMotion = true) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) { content() }
                }
            }
        }
        val metrics = activity.resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(1)
        val height = metrics.heightPixels.coerceAtLeast(1)
        val decor = activity.window.decorView
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        decor.layout(0, 0, width, height)
        val looper = shadowOf(Looper.getMainLooper())
        repeat(6) { looper.idle() }
        Robolectric.getForegroundThreadScheduler().advanceBy(300, TimeUnit.MILLISECONDS)
        repeat(20) {
            Thread.sleep(40)
            looper.idle()
        }
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        decor.layout(0, 0, width, height)
        repeat(4) { looper.idle() }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(android.graphics.Canvas(bitmap))
        val out = File("/opt/cursor/artifacts", "$name.png")
        out.parentFile?.mkdirs()
        FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue("$name should be a phone bitmap", bitmap.width >= 900 && bitmap.height >= 1400)
        assertTrue("$name should be written", out.length() > 10_000)
        activity.finish()
    }

    private fun sample(): Sample {
        val dir = File(System.getProperty("java.io.tmpdir"), "gallery-shots").apply { mkdirs() }
        val gold = jpeg(dir, "gold.jpg", Color.parseColor("#D6BC85"))
        val indigo = jpeg(dir, "indigo.jpg", Color.parseColor("#3F3D9E"))
        val cream = jpeg(dir, "cream.jpg", Color.parseColor("#EEEBE3"))
        val night = jpeg(dir, "night.jpg", Color.parseColor("#1E2150"))
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.OCTOBER, 20, 15, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val loads = listOf(
            GalleryLoadInfo("l1", "T-116KYL6KW", "Garner, NC", "Dallas, TX", "", "2026-10-20"),
            GalleryLoadInfo("l2", "T-88ATLANTA", "Chicago, IL", "Atlanta, GA", "", "2026-10-18"),
        )
        val photos = listOf(
            GalleryMediaRef("p1", "l1", now.timeInMillis, gold, "gold.jpg"),
            GalleryMediaRef("p2", "l1", now.timeInMillis - 3_600_000, indigo, "indigo.jpg"),
            GalleryMediaRef("p3", "l1", now.timeInMillis - 7_200_000, cream, "cream.jpg"),
            GalleryMediaRef("p4", "l2", now.timeInMillis - 86_400_000, night, "night.jpg"),
            GalleryMediaRef("p5", null, now.timeInMillis - 172_800_000, gold, "loose.jpg"),
        )
        val scans = listOf(
            GalleryMediaRef("s1", "l1", now.timeInMillis, cream, "rate.pdf"),
            GalleryMediaRef("s2", "l1", now.timeInMillis - 3_600_000, night, "bol.pdf"),
            GalleryMediaRef("s3", "l2", now.timeInMillis - 86_400_000, indigo, "pod.pdf"),
        )
        return Sample(
            photoItems = photos.filter { it.loadId == "l1" },
            photoCards = GalleryCatalog.buildCards(photos, loads, GalleryFilter.ALL, "", now, "Unlinked", Locale.US),
            scanCards = GalleryCatalog.buildCards(scans, loads, GalleryFilter.ALL, "", now, "Unlinked", Locale.US),
        )
    }

    private fun jpeg(dir: File, name: String, color: Int): String {
        val bitmap = Bitmap.createBitmap(160, 120, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        val file = File(dir, name)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return file.absolutePath
    }

    private data class Sample(
        val photoItems: List<GalleryMediaRef>,
        val photoCards: List<GalleryLoadCard>,
        val scanCards: List<GalleryLoadCard>,
    )
}
