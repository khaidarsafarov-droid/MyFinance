package com.truckerload.presentation.screens.social

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
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

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "ru-rRU-w390dp-h844dp")
class ProfileScreenshotTest {

    @Test
    fun lightHeroIsIndigo() {
        assertIndigo(capture(subscribed = true, empty = false, dark = false, name = "profile-light.png"))
    }

    @Test
    fun emptyHeroIsIndigo() {
        assertIndigo(capture(subscribed = false, empty = true, dark = false, name = "profile-empty.png"))
    }

    @Test
    fun darkHeroIsIndigo() {
        assertIndigo(capture(subscribed = true, empty = false, dark = true, name = "profile-dark.png"))
    }

    private fun assertIndigo(bitmap: Bitmap) {
        val pixel = bitmap.getPixel(bitmap.width / 2, (bitmap.height * 0.08f).toInt())
        assertTrue(
            "hero should be midnight navy, was ${Integer.toHexString(pixel)}",
            Color.blue(pixel) >= 48 &&
                Color.blue(pixel) > Color.red(pixel) &&
                Color.blue(pixel) > Color.green(pixel) &&
                Color.red(pixel) < 60 &&
                Color.green(pixel) < 60,
        )
    }

    private fun capture(subscribed: Boolean, empty: Boolean, dark: Boolean, name: String): Bitmap {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            ProfileStylePreview(
                subscribed = subscribed,
                empty = empty,
                dark = dark,
                modifier = Modifier.fillMaxSize(),
            )
        }
        val decor = activity.window.decorView
        val width = decor.width.takeIf { it > 0 } ?: 1080
        val height = decor.height.takeIf { it > 0 } ?: 1920
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        decor.layout(0, 0, width, height)
        repeat(4) { shadowOf(Looper.getMainLooper()).idle() }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(android.graphics.Canvas(bitmap))
        listOf(File("/tmp"), File("/opt/cursor/artifacts")).forEach { dir ->
            runCatching {
                dir.mkdirs()
                FileOutputStream(File(dir, name)).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        }
        return bitmap
    }
}
