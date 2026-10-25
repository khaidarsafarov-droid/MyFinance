package com.truckerload.presentation.premium

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.truckerload.domain.premium.PremiumFeature
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

/**
 * Renders the paywall tall enough to show the feature list. Not a golden-image test.
 * PNGs are written for review; the assertions only check that the hero and page painted.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "en-rUS-w390dp-h1800dp")
class PaywallScreenshotTest {

    @Test
    fun lightPaywallShowsMidnightHero() {
        val bitmap = capture(dark = false, name = "paywall_light.png")
        assertIndigoHero(bitmap)
        assertPage(bitmap, dark = false)
    }

    @Test
    fun darkPaywallShowsMidnightHero() {
        val bitmap = capture(dark = true, name = "paywall_dark.png")
        assertIndigoHero(bitmap)
        assertPage(bitmap, dark = true)
    }

    private fun assertIndigoHero(bitmap: Bitmap) {
        val pixel = bitmap.getPixel(bitmap.width / 2, 72)
        assertTrue(
            "hero should be midnight navy, was ${Integer.toHexString(pixel)}",
            Color.blue(pixel) >= 48 &&
                Color.blue(pixel) > Color.red(pixel) &&
                Color.blue(pixel) > Color.green(pixel) &&
                Color.red(pixel) < 80 &&
                Color.green(pixel) < 80,
        )
    }

    private fun assertPage(bitmap: Bitmap, dark: Boolean) {
        val pixel = bitmap.getPixel(bitmap.width / 2, (bitmap.height * 0.42f).toInt())
        val red = Color.red(pixel)
        assertTrue(
            "page at 42% height should match the ${if (dark) "dark" else "light"} canvas, was ${Integer.toHexString(pixel)}",
            if (dark) red < 80 else red > 180,
        )
    }

    private fun capture(dark: Boolean, name: String): Bitmap {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            TruckerLoadTheme(darkTheme = dark, dynamicColor = false, reduceMotion = true) {
                PaywallScreen(feature = PremiumFeature.ANALYTICS, onBack = {})
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
        repeat(8) { looper.idle() }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(android.graphics.Canvas(bitmap))
        save(bitmap, name)
        assertTrue("screenshot $name should be a phone-width bitmap", bitmap.width >= 300)
        return bitmap
    }

    private fun save(bitmap: Bitmap, name: String) {
        listOf(File("/tmp"), File("/opt/cursor/artifacts")).forEach { dir ->
            runCatching {
                dir.mkdirs()
                FileOutputStream(File(dir, name)).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        }
    }
}
