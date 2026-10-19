package com.truckerload.presentation.theme

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertTrue
import org.junit.Test

/** Launcher mark is Midnight & Gold. In-app logo plate stays on the theme primary. */
class AppIconBrandColorTest {

    @Test
    fun launcherBackground_isMidnightGold() {
        val xml = readRes("drawable/ic_launcher_background.xml")
        assertTrue(xml.contains("#FF1E2150"))
        assertTrue(xml.contains("#FF0F1230"))
        assertTrue(xml.contains("#FFC8A867"))
        assertTrue(!xml.contains("#FF5B54E6"))
        assertTrue(!xml.contains("#FF143882"))
        assertTrue(!xml.contains("#FF2F4F3E"))
    }

    @Test
    fun launcherForegroundFallback_isMidnight() {
        val xml = readRes("drawable/ic_launcher_foreground.xml")
        assertTrue(xml.contains("#FF1E2150"))
        assertTrue(!xml.contains("#FF5B54E6"))
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
    fun legacyLauncherMipmap_isMidnightGoldNotKitPurple() {
        val image = readResImage("mipmap-xxxhdpi/ic_launcher.png")
        val round = readResImage("mipmap-xxxhdpi/ic_launcher_round.png")
        assertTrue(image.width == 192 && round.width == 192)
        assertTrue(!containsRgb(image, 0x5B, 0x54, 0xE6))
        assertTrue(!containsRgb(round, 0x5B, 0x54, 0xE6))
        assertTrue(containsRgb(image, 0x1E, 0x21, 0x50, tolerance = 18))
        assertTrue(containsRgb(image, 0xC8, 0xA8, 0x67, tolerance = 22))
        assertTrue(containsRgb(round, 0xC8, 0xA8, 0x67, tolerance = 22))
        // Circle mask keeps the corners clear; the ring sits inside the disc.
        val corner = image.getRGB(0, 0)
        assertTrue((corner ushr 24 and 0xFF) < 16)
        val roundCorner = round.getRGB(0, 0)
        assertTrue((roundCorner ushr 24 and 0xFF) < 16)
    }

    @Test
    fun launcherForegroundPng_isTransparentGlyph_notKitPurple() {
        val image = readResImage("drawable-nodpi/ic_launcher_image.png")
        assertTrue(image.width > 32 && image.height > 32)
        assertTrue(!containsRgb(image, 0x2F, 0x4F, 0x3E))
        assertTrue(!containsRgb(image, 0x30, 0x51, 0x3F))
        assertTrue(!containsRgb(image, 0x14, 0x38, 0x82))
        assertTrue(!containsRgb(image, 0x5B, 0x54, 0xE6))
        assertTrue(containsRgb(image, 0x38, 0xA0, 0x88, tolerance = 24))
        assertTrue(hasTransparentPixel(image))
    }

    private fun hasTransparentPixel(image: BufferedImage): Boolean {
        for (y in 0 until image.height step 4) {
            for (x in 0 until image.width step 4) {
                if (image.getRGB(x, y) ushr 24 and 0xFF < 16) return true
            }
        }
        return false
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
