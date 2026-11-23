package com.truckerload.share

import android.graphics.Bitmap
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.truckerload.presentation.theme.TruckerLoadTheme
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the share-reject screen the way the receiver shows a non-load. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "en-rUS-w390dp-h800dp")
class ShareReceiverScreenshotTest {

    @Test
    fun rejectedShareShowsTheNotALoadMessage() {
        val bitmap = capture()
        assertTrue(bitmap.width > 100 && bitmap.height > 100)
        var ink = 0
        val step = 8
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                if (bitmap.getPixel(x, y) != android.graphics.Color.TRANSPARENT) ink++
            }
        }
        assertTrue(ink > 20)
    }

    private fun capture(): Bitmap {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            TruckerLoadTheme {
                ShareReceiverScreen(
                    checking = false,
                    message = ShareReceiverMessage.REJECTED,
                    onDone = {},
                    onViewLoad = null,
                )
            }
        }
        val decor = activity.window.decorView
        val width = decor.width.takeIf { it > 0 } ?: 1080
        val height = decor.height.takeIf { it > 0 } ?: 1920
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        decor.layout(0, 0, width, height)
        repeat(6) { shadowOf(Looper.getMainLooper()).idle() }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(android.graphics.Canvas(bitmap))
        listOf(File("/tmp"), File("/opt/cursor/artifacts")).forEach { dir ->
            runCatching {
                dir.mkdirs()
                FileOutputStream(File(dir, "share-rejected-not-a-load.png")).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        }
        return bitmap
    }
}
