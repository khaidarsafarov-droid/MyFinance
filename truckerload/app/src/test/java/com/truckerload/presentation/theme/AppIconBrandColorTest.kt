package com.truckerload.presentation.theme

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertTrue
import org.junit.Test

/** Launcher + in-app mark must use kit purple, not legacy navy/forest green. */
class AppIconBrandColorTest {

    @Test
    fun launcherBackground_isKitPrimary() {
        val xml = readRes("drawable/ic_launcher_background.xml")
        assertTrue(xml.contains("#FF5B54E6"))
        assertTrue(!xml.contains("#FF143882"))
        assertTrue(!xml.contains("#FF2F4F3E"))
    }

    @Test
    fun launcherForegroundFallback_isKitPrimary() {
        val xml = readRes("drawable/ic_launcher_foreground.xml")
        assertTrue(xml.contains("#FF5B54E6"))
        assertTrue(!xml.contains("#FF143882"))
        assertTrue(!xml.contains("#FF2F4F3E"))
    }

    @Test
    fun inAppLogoPlate_usesForestPrimary() {
        val src = readSource("presentation/components/AppBrandLogo.kt")
        assertTrue(src.contains("SoftUiColors.ForestPrimary"))
        assertTrue(!src.contains("0xFF143882"))
    }

    @Test
    fun adaptiveIconForeground_hasSafeZoneInset() {
        val layer = readRes("drawable/ic_launcher_foreground_layer.xml")
        assertTrue(layer.contains("inset"))
        assertTrue(layer.contains("@drawable/ic_launcher_image"))
        assertTrue(layer.contains("21dp"))

        val adaptive = readRes("mipmap-anydpi-v26/ic_launcher.xml")
        assertTrue(adaptive.contains("@drawable/ic_launcher_foreground_layer"))
    }

    @Test
    fun legacyLauncherMipmap_hasSafeZoneMargins() {
        val image = readResImage("mipmap-xxxhdpi/ic_launcher.png")
        val margin = transparentMarginPx(image)
        // ~17% of 192px ≈ 33px per side for 66/108 safe zone.
        assertTrue("Expected transparent launcher margins, got $margin", margin >= 28)
    }

    private fun transparentMarginPx(image: BufferedImage): Int {
        val w = image.width
        val h = image.height
        fun rowTransparent(y: Int): Boolean {
            for (x in 0 until w) {
                if (image.getRGB(x, y) ushr 24 and 0xFF >= 16) return false
            }
            return true
        }
        fun colTransparent(x: Int): Boolean {
            for (y in 0 until h) {
                if (image.getRGB(x, y) ushr 24 and 0xFF >= 16) return false
            }
            return true
        }
        var top = 0
        while (top < h && rowTransparent(top)) top++
        var bottom = 0
        var y = h - 1
        while (y >= 0 && rowTransparent(y)) {
            bottom++
            y--
        }
        var left = 0
        while (left < w && colTransparent(left)) left++
        var right = 0
        var x = w - 1
        while (x >= 0 && colTransparent(x)) {
            right++
            x--
        }
        return minOf(top, bottom, left, right)
    }

    @Test
    fun launcherForegroundPng_usesKitPalette_notLegacyForestGreen() {
        val image = readResImage("drawable-nodpi/ic_launcher_image.png")
        assertTrue(image.width > 32 && image.height > 32)
        // Legacy Mindwell forest green plate (#2F4F3E / #30513F) must not remain.
        assertTrue(!containsRgb(image, 0x2F, 0x4F, 0x3E))
        assertTrue(!containsRgb(image, 0x30, 0x51, 0x3F))
        assertTrue(!containsRgb(image, 0x14, 0x38, 0x82))
        // Kit purple plate should be present.
        assertTrue(containsRgb(image, 0x5B, 0x54, 0xE6) || containsRgb(image, 0x20, 0x1D, 0x51))
    }

    private fun readResImage(relativePath: String): BufferedImage {
        val file = listOf(
            File("src/main/res/$relativePath"),
            File("app/src/main/res/$relativePath"),
            File("../app/src/main/res/$relativePath"),
        ).firstOrNull(File::isFile) ?: error("Resource not found: $relativePath")
        return ImageIO.read(file) ?: error("Failed to decode PNG: $relativePath")
    }

    /** Allow ±10 per channel for resample/compression drift. */
    private fun containsRgb(image: BufferedImage, r: Int, g: Int, b: Int, tolerance: Int = 10): Boolean {
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val rgb = image.getRGB(x, y)
                val alpha = rgb ushr 24 and 0xFF
                if (alpha < 16) continue
                val pr = rgb shr 16 and 0xFF
                val pg = rgb shr 8 and 0xFF
                val pb = rgb and 0xFF
                if (kotlin.math.abs(pr - r) <= tolerance &&
                    kotlin.math.abs(pg - g) <= tolerance &&
                    kotlin.math.abs(pb - b) <= tolerance
                ) {
                    return true
                }
            }
        }
        return false
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/com/truckerload/$relativePath"),
            File("app/src/main/java/com/truckerload/$relativePath"),
            File("../app/src/main/java/com/truckerload/$relativePath"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Source not found: $relativePath")
    }

    private fun readRes(relativePath: String): String {
        val candidates = listOf(
            File("src/main/res/$relativePath"),
            File("app/src/main/res/$relativePath"),
            File("../app/src/main/res/$relativePath"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Resource not found: $relativePath")
    }
}
