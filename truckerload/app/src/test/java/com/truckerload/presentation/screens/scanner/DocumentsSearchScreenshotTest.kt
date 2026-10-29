package com.truckerload.presentation.screens.scanner

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.TruckerLoadTheme
import com.truckerload.presentation.theme.appFormField
import java.io.File
import java.io.FileOutputStream
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
 * Renders the Documents search before the contrast fix and after it.
 * The PNG is written for review; assertions check that the hint and typed
 * text actually paint lighter than the old navy-on-navy placeholder.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "ru-rRU-w400dp-h520dp-xxhdpi")
class DocumentsSearchScreenshotTest {

    @Test
    fun beforeAndAfterHintIsReadable() {
        val bitmap = capture()
        val bands = fieldBands(bitmap)
        assertTrue("expected three search fields, found ${bands.size}", bands.size >= 3)
        val before = lightGlyphs(bitmap, bands[0])
        val afterHint = lightGlyphs(bitmap, bands[1])
        val afterTyped = lightGlyphs(bitmap, bands[2])
        assertTrue(
            "hint glyphs should be much lighter after the fix (before=$before after=$afterHint)",
            afterHint > before * 3 && afterHint > 40,
        )
        assertTrue(
            "typed text should paint light glyphs (count=$afterTyped)",
            afterTyped > before * 3 && afterTyped > 40,
        )
        val icon = iconGlyphs(bitmap, bands[1])
        assertTrue("search icon should paint light pixels (count=$icon)", icon > 12)
    }

    private fun capture(): Bitmap {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            TruckerLoadTheme(darkTheme = true, dynamicColor = false, reduceMotion = true) {
                DocumentsSearchContrastPreview()
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
        repeat(3) { looper.idle() }
        // Cursor blink is visible for the first half-second. Stay inside that window.
        looper.idleFor(120, TimeUnit.MILLISECONDS)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(android.graphics.Canvas(bitmap))
        save(cropToContent(bitmap), "documents-search-before-after.png")
        assertTrue(bitmap.width >= 300)
        return bitmap
    }

    private fun save(bitmap: Bitmap, name: String) {
        listOf(File("/tmp"), File("/opt/cursor/artifacts")).forEach { dir ->
            dir.mkdirs()
            FileOutputStream(File(dir, name)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    }
}

@Composable
private fun DocumentsSearchContrastPreview() {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionLabel("Before")
        LegacyDocumentSearchField()
        SectionLabel("After")
        DocumentSearchField(value = "", onValueChange = {})
        SectionLabel("Typed")
        DocumentSearchField(
            value = "T-1165R4JBH",
            onValueChange = {},
            modifier = Modifier.focusRequester(focus),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = AppTypography.CardTitle,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

/** The old Documents field: placeholder and icon used the navy border token. */
@Composable
private fun LegacyDocumentSearchField() {
    val scheme = MaterialTheme.colorScheme
    OutlinedTextField(
        value = "",
        onValueChange = {},
        modifier = Modifier
            .fillMaxWidth()
            .appFormField(),
        placeholder = {
            Text(
                text = stringResource(R.string.scan_search_hint),
                color = scheme.outline,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = AppIcons.Search,
                contentDescription = null,
                tint = scheme.outline,
            )
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = scheme.onSurface,
            unfocusedTextColor = scheme.onSurface,
            cursorColor = scheme.primary,
            focusedBorderColor = scheme.primary,
            unfocusedBorderColor = scheme.primary,
            focusedPlaceholderColor = scheme.outline,
            unfocusedPlaceholderColor = scheme.outline,
            focusedLeadingIconColor = scheme.outline,
            unfocusedLeadingIconColor = scheme.outline,
            focusedContainerColor = scheme.surfaceVariant,
            unfocusedContainerColor = scheme.surfaceVariant,
        ),
    )
}

private fun cropToContent(bitmap: Bitmap): Bitmap {
    var last = bitmap.height - 1
    val page = Color.rgb(13, 15, 30)
    while (last > 0 && rowIsPage(bitmap, last, page)) last--
    val bottom = (last + 48).coerceAtMost(bitmap.height)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bottom)
}

private fun rowIsPage(bitmap: Bitmap, y: Int, page: Int): Boolean {
    val pr = Color.red(page)
    val pg = Color.green(page)
    val pb = Color.blue(page)
    var x = 0
    while (x < bitmap.width) {
        val pixel = bitmap.getPixel(x, y)
        if (
            kotlin.math.abs(Color.red(pixel) - pr) > 8 ||
            kotlin.math.abs(Color.green(pixel) - pg) > 8 ||
            kotlin.math.abs(Color.blue(pixel) - pb) > 8
        ) {
            return false
        }
        x += 12
    }
    return true
}

private fun fieldBands(bitmap: Bitmap): List<IntRange> {
    val rows = mutableListOf<Int>()
    val step = 6
    for (y in 0 until bitmap.height) {
        var hits = 0
        var samples = 0
        var x = bitmap.width / 5
        while (x < bitmap.width * 4 / 5) {
            if (isFieldFill(bitmap.getPixel(x, y))) hits++
            samples++
            x += step
        }
        if (samples > 0 && hits > samples * 0.45) rows.add(y)
    }
    if (rows.isEmpty()) return emptyList()
    val bands = mutableListOf<IntRange>()
    var start = rows.first()
    var prev = start
    for (y in rows.drop(1)) {
        if (y > prev + 4) {
            bands.add(start..prev)
            start = y
        }
        prev = y
    }
    bands.add(start..prev)
    return bands.filter { it.last - it.first > 20 }
}

private fun isFieldFill(pixel: Int): Boolean {
    val red = Color.red(pixel)
    val green = Color.green(pixel)
    val blue = Color.blue(pixel)
    return red in 24..70 && green in 26..74 && blue in 55..110 && blue > red && blue > green
}

private fun lightGlyphs(bitmap: Bitmap, band: IntRange): Int {
    val top = band.first + (band.last - band.first) / 5
    val bottom = band.last - (band.last - band.first) / 5
    val left = bitmap.width * 28 / 100
    val right = bitmap.width * 92 / 100
    return countLight(bitmap, left, top, right, bottom)
}

private fun iconGlyphs(bitmap: Bitmap, band: IntRange): Int {
    val top = band.first + (band.last - band.first) / 6
    val bottom = band.last - (band.last - band.first) / 6
    val left = bitmap.width * 4 / 100
    val right = bitmap.width * 22 / 100
    return countLight(bitmap, left, top, right, bottom)
}

private fun countLight(bitmap: Bitmap, left: Int, top: Int, right: Int, bottom: Int): Int {
    var count = 0
    var y = top
    while (y <= bottom) {
        var x = left
        while (x <= right) {
            val pixel = bitmap.getPixel(x, y)
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            val luma = (red * 3 + green * 6 + blue) / 10
            if (luma > 150 && red > 120) count++
            x += 2
        }
        y += 2
    }
    return count
}
