package com.truckerload.presentation.store

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import com.truckerload.R
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.repository.MaintenanceRepository
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.MaintenanceProgress
import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask
import com.truckerload.domain.money.Money
import com.truckerload.presentation.components.HomePeriodFilterDropdown
import com.truckerload.presentation.components.LoadCard
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.components.SoftBottomBar
import com.truckerload.presentation.di.LocalMaintenanceRepository
import com.truckerload.presentation.navigation.Routes
import com.truckerload.presentation.screens.home.HomeQuickActionRow
import com.truckerload.presentation.screens.home.HomeScreenTopBar
import com.truckerload.presentation.screens.home.HomeWeekHero
import com.truckerload.presentation.screens.maintenance.MaintenanceTasksSection
import com.truckerload.presentation.screens.scanner.PendingScan
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.utils.PhotoManager
import android.content.Context
import java.io.File
import java.time.LocalDate

/** 2026-10-20 09:41 UTC — the same instant on the photo stamp, the scan, and the home screen. */
internal const val PLAY_STORY_AT = 1_792_489_260_000L

/** In-memory Room so the real bottom bar can read maintenance state. */
internal fun playMaintenanceRepository(context: Context): Pair<AppDatabase, MaintenanceRepository> {
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    return db to MaintenanceRepository(db)
}

@Composable
internal fun PlayDashboard(loads: List<Load>, maintenance: MaintenanceRepository) {
    val shown = loads.take(2)
    CompositionLocalProvider(LocalMaintenanceRepository provides maintenance) {
        BentoGlassScreenBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 108.dp),
                ) {
                    HomeScreenTopBar(
                        welcomeName = "Alex",
                        weekLabel = "",
                        filter = LoadFilter.THIS_WEEK,
                        openDrawer = {},
                        onSearchToggle = {},
                        onAvatar = {},
                    )
                    HomeWeekHero(
                        periodLabel = "",
                        filter = LoadFilter.THIS_WEEK,
                        gross = Money.fromDollars(loads.sumOf { it.totalRate.cents } / 100.0),
                        miles = loads.sumOf { it.drivenMiles },
                        loadCount = loads.size,
                        avgRpm = loads.sumOf { it.totalRate.cents } / 100.0 /
                            loads.sumOf { it.drivenMiles }.coerceAtLeast(1.0),
                        weeklyGoal = 13_000.0,
                        summaryContentDescription = "",
                        onOpenWeeklyGoal = {},
                    )
                    HomeQuickActionRow(
                        onDiesel = {},
                        onPaycheck = {},
                        onExpense = {},
                        onScanner = {},
                    )
                    Text(
                        text = stringResource(R.string.home_recent_loads),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    HomePeriodFilterDropdown(
                        currentFilter = LoadFilter.THIS_WEEK,
                        selectedYear = 2026,
                        selectedDateLabel = "",
                        selectedWeekLabel = "",
                        selectedMonthLabel = "",
                        onFilterSelected = {},
                        onOpenCalendar = {},
                        onOpenMonthPicker = {},
                    )
                    shown.forEach { load ->
                        LoadCard(
                            load = load,
                            onClick = {},
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            rpmThresholds = PlayThresholds,
                            onCameraClick = {},
                            onScanClick = {},
                        )
                    }
                }
                SoftBottomBar(
                    currentRoute = Routes.HOME,
                    onNavigate = {},
                    onQuickAction = {},
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

/** Launcher page inset shared by the widget, icon grid, search pill, and dock. */
private val LauncherInset = 16.dp

/**
 * Pixel-style launcher. One full-width 4×3 widget under the status bar, then
 * an icon grid, a search pill, a dock, and the gesture bar. The wallpaper stays
 * visible through a scrim so the widget plate stays readable.
 */
@Composable
internal fun PlayWidgetHome(wallpaper: Bitmap, widget: Bitmap) {
    val launcher = rememberLauncherIcon()
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = wallpaper.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.28f),
                        0.42f to Color.Black.copy(alpha = 0.36f),
                        1f to Color.Black.copy(alpha = 0.48f),
                    ),
                ),
        )
        Column(modifier = Modifier.fillMaxSize()) {
            DemoStatusBar(onDark = true)
            Spacer(Modifier.height(LauncherInset))
            WidgetPlate(widget, horizontal = LauncherInset)
            Spacer(Modifier.height(18.dp))
            HomeIconGrid(launcher)
            Spacer(Modifier.weight(1f))
            HomeSearchPill()
            Spacer(Modifier.height(10.dp))
            HomeDock()
            Spacer(Modifier.height(8.dp))
            HomeGestureBar()
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun rememberLauncherIcon(): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    return remember(context) {
        adaptiveLauncherIcon(context).asImageBitmap()
    }
}

/** Icon B: midnight gradient, gold ring, circular, same 56dp as the other plates. */
private fun adaptiveLauncherIcon(context: Context): Bitmap {
    val decoded = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_round)
    if (decoded != null && isMidnightPlate(decoded)) return decoded
    return paintAdaptiveLauncherIcon(context)
}

private fun isMidnightPlate(bitmap: Bitmap): Boolean {
    val center = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
    val alpha = (center ushr 24) and 0xFF
    val lum = (((center shr 16) and 0xFF) + ((center shr 8) and 0xFF) + (center and 0xFF)) / 3
    return alpha > 200 && lum < 80
}

private fun paintAdaptiveLauncherIcon(context: Context): Bitmap {
    val size = 192
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.shader = android.graphics.LinearGradient(
        size * 0.89f,
        size * 0.81f,
        size * 0.11f,
        size * 0.19f,
        0xFF1E2150.toInt(),
        0xFF0F1230.toInt(),
        android.graphics.Shader.TileMode.CLAMP,
    )
    val radius = size / 2f
    canvas.drawCircle(radius, radius, radius, paint)
    paint.shader = null
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = size * (1.6f / 108f)
    paint.color = 0xFFC8A867.toInt()
    canvas.drawCircle(radius, radius, size * (29.8f / 72f), paint)
    val mark = BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_image)
    if (mark != null) {
        val inset = size * (21f / 108f)
        paint.style = Paint.Style.FILL
        canvas.drawBitmap(
            mark,
            null,
            android.graphics.RectF(inset, inset, size - inset, size - inset),
            paint,
        )
    }
    return bitmap
}

private enum class HomeGlyph { Phone, Chat, Camera, Photo, Calendar, Clock, Folder, Gear, Mail, Note, Cloud }

private data class HomeApp(val label: String, val glyph: HomeGlyph, val plate: Color)

private val HomeApps = listOf(
    HomeApp("Phone", HomeGlyph.Phone, Color(0xFF3E4C5E)),
    HomeApp("Messages", HomeGlyph.Chat, Color(0xFF4A5568)),
    HomeApp("Camera", HomeGlyph.Camera, Color(0xFF2F3C4A)),
    HomeApp("Photos", HomeGlyph.Photo, Color(0xFF5C5348)),
    HomeApp("Calendar", HomeGlyph.Calendar, Color(0xFF3F4A44)),
    HomeApp("Clock", HomeGlyph.Clock, Color(0xFF4E463C)),
    HomeApp("Files", HomeGlyph.Folder, Color(0xFF3A4454)),
    HomeApp("Mail", HomeGlyph.Mail, Color(0xFF4A4038)),
    HomeApp("Notes", HomeGlyph.Note, Color(0xFF3D4A42)),
    HomeApp("Weather", HomeGlyph.Cloud, Color(0xFF3A4A55)),
    HomeApp("Settings", HomeGlyph.Gear, Color(0xFF454550)),
)

@Composable
private fun HomeIconGrid(launcher: androidx.compose.ui.graphics.ImageBitmap?) {
    val columns = 4
    val cells = listOf<(@Composable () -> Unit)>(
        { LauncherAppIcon(launcher) },
    ) + HomeApps.map { app ->
        { GlyphAppIcon(app) }
    }
    Column(
        modifier = Modifier.padding(horizontal = LauncherInset),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        cells.chunked(columns).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { cell ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { cell() }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun LauncherAppIcon(launcher: androidx.compose.ui.graphics.ImageBitmap?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (launcher != null) {
            Image(
                bitmap = launcher,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(CircleShape),
            )
        } else {
            GlyphPlate(Color(0xFF1E2150)) { /* mark is the bitmap */ }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "TruckoRig",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun GlyphAppIcon(app: HomeApp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GlyphPlate(app.plate) { HomeGlyphMark(app.glyph) }
        Spacer(Modifier.height(4.dp))
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun GlyphPlate(plate: Color, mark: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(plate),
        contentAlignment = Alignment.Center,
    ) {
        mark()
    }
}

@Composable
private fun HomeGlyphMark(glyph: HomeGlyph) {
    val ink = Color.White
    Canvas(modifier = Modifier.size(26.dp)) {
        val s = size.minDimension
        val stroke = Stroke(width = s * 0.08f, cap = StrokeCap.Round)
        when (glyph) {
            HomeGlyph.Phone -> {
                drawRoundRect(
                    ink,
                    topLeft = Offset(s * 0.28f, s * 0.08f),
                    size = Size(s * 0.44f, s * 0.84f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.1f, s * 0.1f),
                    style = stroke,
                )
            }
            HomeGlyph.Chat -> {
                drawRoundRect(
                    ink,
                    topLeft = Offset(s * 0.08f, s * 0.12f),
                    size = Size(s * 0.84f, s * 0.58f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.12f, s * 0.12f),
                    style = stroke,
                )
                drawLine(ink, Offset(s * 0.28f, s * 0.7f), Offset(s * 0.22f, s * 0.9f), stroke.width, StrokeCap.Round)
            }
            HomeGlyph.Camera -> {
                drawRoundRect(
                    ink,
                    topLeft = Offset(s * 0.08f, s * 0.28f),
                    size = Size(s * 0.84f, s * 0.56f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.1f, s * 0.1f),
                    style = stroke,
                )
                drawCircle(ink, radius = s * 0.16f, center = Offset(s * 0.5f, s * 0.56f), style = stroke)
            }
            HomeGlyph.Photo -> {
                drawRoundRect(
                    ink,
                    topLeft = Offset(s * 0.1f, s * 0.16f),
                    size = Size(s * 0.8f, s * 0.68f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.08f, s * 0.08f),
                    style = stroke,
                )
                drawCircle(ink, radius = s * 0.08f, center = Offset(s * 0.32f, s * 0.36f))
            }
            HomeGlyph.Calendar -> {
                drawRoundRect(
                    ink,
                    topLeft = Offset(s * 0.12f, s * 0.2f),
                    size = Size(s * 0.76f, s * 0.68f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.08f, s * 0.08f),
                    style = stroke,
                )
                drawLine(ink, Offset(s * 0.12f, s * 0.4f), Offset(s * 0.88f, s * 0.4f), stroke.width, StrokeCap.Round)
            }
            HomeGlyph.Clock -> {
                drawCircle(ink, radius = s * 0.38f, center = Offset(s * 0.5f, s * 0.5f), style = stroke)
                drawLine(ink, Offset(s * 0.5f, s * 0.5f), Offset(s * 0.5f, s * 0.28f), stroke.width, StrokeCap.Round)
                drawLine(ink, Offset(s * 0.5f, s * 0.5f), Offset(s * 0.68f, s * 0.58f), stroke.width, StrokeCap.Round)
            }
            HomeGlyph.Folder -> {
                drawRoundRect(
                    ink,
                    topLeft = Offset(s * 0.1f, s * 0.32f),
                    size = Size(s * 0.8f, s * 0.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.08f, s * 0.08f),
                    style = stroke,
                )
                drawLine(ink, Offset(s * 0.1f, s * 0.4f), Offset(s * 0.42f, s * 0.4f), stroke.width, StrokeCap.Round)
            }
            HomeGlyph.Gear -> {
                val center = Offset(s * 0.5f, s * 0.5f)
                drawCircle(ink, radius = s * 0.16f, center = center, style = stroke)
                for (i in 0 until 8) {
                    val angle = i * (Math.PI / 4.0)
                    val inner = s * 0.26f
                    val outer = s * 0.44f
                    drawLine(
                        ink,
                        Offset(center.x + kotlin.math.cos(angle).toFloat() * inner, center.y + kotlin.math.sin(angle).toFloat() * inner),
                        Offset(center.x + kotlin.math.cos(angle).toFloat() * outer, center.y + kotlin.math.sin(angle).toFloat() * outer),
                        stroke.width,
                        StrokeCap.Round,
                    )
                }
            }
            HomeGlyph.Mail -> {
                drawRoundRect(
                    ink,
                    topLeft = Offset(s * 0.08f, s * 0.22f),
                    size = Size(s * 0.84f, s * 0.56f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.08f, s * 0.08f),
                    style = stroke,
                )
                drawLine(ink, Offset(s * 0.12f, s * 0.28f), Offset(s * 0.5f, s * 0.52f), stroke.width, StrokeCap.Round)
                drawLine(ink, Offset(s * 0.88f, s * 0.28f), Offset(s * 0.5f, s * 0.52f), stroke.width, StrokeCap.Round)
            }
            HomeGlyph.Note -> {
                drawRoundRect(
                    ink,
                    topLeft = Offset(s * 0.22f, s * 0.1f),
                    size = Size(s * 0.56f, s * 0.8f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.06f, s * 0.06f),
                    style = stroke,
                )
                drawLine(ink, Offset(s * 0.34f, s * 0.36f), Offset(s * 0.66f, s * 0.36f), stroke.width, StrokeCap.Round)
                drawLine(ink, Offset(s * 0.34f, s * 0.52f), Offset(s * 0.66f, s * 0.52f), stroke.width, StrokeCap.Round)
            }
            HomeGlyph.Cloud -> {
                drawCircle(ink, radius = s * 0.2f, center = Offset(s * 0.38f, s * 0.48f), style = stroke)
                drawCircle(ink, radius = s * 0.26f, center = Offset(s * 0.6f, s * 0.44f), style = stroke)
            }
        }
    }
}

@Composable
private fun HomeSearchPill() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LauncherInset)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.94f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val ink = Color(0xFF5C6370)
            drawCircle(ink, radius = size.minDimension * 0.32f, center = Offset(size.width * 0.42f, size.height * 0.42f), style = Stroke(width = size.minDimension * 0.12f))
            drawLine(
                ink,
                Offset(size.width * 0.64f, size.height * 0.64f),
                Offset(size.width * 0.88f, size.height * 0.88f),
                strokeWidth = size.minDimension * 0.12f,
                cap = StrokeCap.Round,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(text = "Search", color = Color(0xFF5C6370), fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HomeDock() {
    val dock = listOf(
        HomeApp("Phone", HomeGlyph.Phone, Color(0xFF3E4C5E)),
        HomeApp("Messages", HomeGlyph.Chat, Color(0xFF4A5568)),
        HomeApp("Camera", HomeGlyph.Camera, Color(0xFF2F3C4A)),
        HomeApp("Settings", HomeGlyph.Gear, Color(0xFF454550)),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LauncherInset)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        dock.forEach { app -> GlyphPlate(app.plate) { HomeGlyphMark(app.glyph) } }
    }
}

@Composable
private fun HomeGestureBar() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .width(108.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.88f)),
        )
    }
}

/**
 * The production week header is day-first and lowercase for every locale
 * ("18 – 24 oct"). The English listing wants a US month-first label. This
 * repaints only that header line, starting at the same left edge as the
 * gross amount so the rounded header corner cannot clip the first letter.
 */
internal fun withEnglishWeekRange(source: Bitmap): Bitmap {
    val bitmap = source.copy(Bitmap.Config.ARGB_8888, true)
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    fun lum(color: Int): Int {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return (r + g + b) / 3
    }
    fun bright(x: Int, y: Int): Boolean = lum(pixels[y * width + x]) > 200
    val scanBottom = (height * 0.48f).toInt().coerceAtMost(height)
    // Skip the rounded corner. A light pixel there is the plate, not a glyph.
    val inset = (width * 0.07f).toInt()
    val mid = (width * 0.58f).toInt()
    val rowCount = IntArray(scanBottom)
    for (y in 0 until scanBottom) {
        var count = 0
        for (x in inset until mid) {
            if (bright(x, y)) count++
        }
        rowCount[y] = count
    }
    var amountTop = -1
    var amountBottom = -1
    var bestSpan = 0
    var y = 0
    while (y < scanBottom) {
        if (rowCount[y] > 8) {
            val bandTop = y
            while (y < scanBottom && rowCount[y] > 8) y++
            val bandBottom = y - 1
            val span = bandBottom - bandTop
            if (span > bestSpan) {
                bestSpan = span
                amountTop = bandTop
                amountBottom = bandBottom
            }
        } else {
            y++
        }
    }
    check(amountTop >= 0) { "widget amount was not found on the rendered bitmap" }
    var amountLeft = width
    for (row in amountTop..amountBottom) {
        for (x in inset until mid) {
            if (bright(x, row)) {
                if (x < amountLeft) amountLeft = x
                break
            }
        }
    }
    check(amountLeft < width) { "widget amount has no left edge" }
    var cursor = amountTop - 1
    while (cursor >= 0 && rowCount[cursor] > 4) cursor--
    while (cursor >= 0 && rowCount[cursor] <= 4) cursor--
    var dateTop = -1
    var dateBottom = if (cursor >= 0) cursor else -1
    var dateRight = amountLeft
    while (cursor >= 0 && rowCount[cursor] > 4) {
        dateTop = cursor
        cursor--
    }
    check(dateTop >= 0 && dateBottom >= dateTop) { "widget week label was not found on the rendered bitmap" }
    val minX = (amountLeft - 8).coerceAtLeast(0)
    for (row in dateTop..dateBottom) {
        for (x in minX until (width * 0.75f).toInt()) {
            if (bright(x, row) && x > dateRight) dateRight = x
        }
    }
    val density = width / 400f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 12f * density
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    val label = "Oct 18 – 24"
    val bounds = android.graphics.Rect()
    paint.getTextBounds(label, 0, label.length, bounds)
    val bandHeight = (dateBottom - dateTop).toFloat().coerceAtLeast(1f)
    if (bounds.height() > 0) {
        val fitted = paint.textSize * (bandHeight / bounds.height())
        paint.textSize = fitted.coerceIn(12f * density * 0.9f, 12f * density * 1.45f)
        paint.getTextBounds(label, 0, label.length, bounds)
    }
    val textWidth = paint.measureText(label)
    var gapY = (dateBottom + 1).coerceAtMost(height - 1)
    for (row in (dateBottom + 1) until amountTop) {
        if (rowCount[row] == 0) {
            gapY = row
            break
        }
    }
    val clearLeft = (amountLeft - 4).coerceAtLeast(0)
    val clearRight = maxOf(dateRight + 8, (amountLeft + textWidth + 4).toInt()).coerceAtMost(width - 1)
    val clearTop = (dateTop - 2).coerceAtLeast(0)
    val clearBottom = (dateBottom + 2).coerceAtMost(height - 1)
    for (row in clearTop..clearBottom) {
        for (x in clearLeft..clearRight) {
            pixels[row * width + x] = pixels[gapY * width + x]
        }
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    val baseline = dateTop + (bandHeight - bounds.height()) / 2f - bounds.top
    val drawX = (amountLeft - bounds.left).toFloat()
    AndroidCanvas(bitmap).drawText(label, drawX, baseline, paint)
    return bitmap
}

/**
 * The 4×3 compose size is taller than the FULL layout, so the plate keeps an
 * empty band under the buttons. Keep the rendered content and a short pad.
 */
internal fun croppedToWidgetContent(source: Bitmap): Bitmap {
    val width = source.width
    val height = source.height
    if (width < 8 || height < 8) return source
    val density = width / 400f
    val pad = (10f * density).toInt().coerceAtLeast(1)
    val probeY = (height - (6f * density).toInt()).coerceIn(0, height - 1)
    val plate = source.getPixel(width / 2, probeY)
    fun differs(color: Int): Boolean {
        val alpha = (color ushr 24) and 0xFF
        if (alpha < 200) return true
        val dr = kotlin.math.abs(((color shr 16) and 0xFF) - ((plate shr 16) and 0xFF))
        val dg = kotlin.math.abs(((color shr 8) and 0xFF) - ((plate shr 8) and 0xFF))
        val db = kotlin.math.abs((color and 0xFF) - (plate and 0xFF))
        return dr + dg + db > 36
    }
    val x0 = (width * 0.14f).toInt()
    val x1 = (width * 0.86f).toInt()
    val step = ((x1 - x0) / 48).coerceAtLeast(1)
    var contentBottom = -1
    for (y in height - 1 downTo (height * 0.4f).toInt()) {
        var hits = 0
        var x = x0
        while (x < x1) {
            if (differs(source.getPixel(x, y))) {
                hits++
                if (hits >= 3) break
            }
            x += step
        }
        if (hits >= 3) {
            contentBottom = y
            break
        }
    }
    if (contentBottom < 0) return source
    val keep = (contentBottom + 1 + pad).coerceAtMost(height)
    if (keep >= height - 2) return source
    return Bitmap.createBitmap(source, 0, 0, width, keep)
}

@Composable
private fun WidgetPlate(bitmap: Bitmap, horizontal: androidx.compose.ui.unit.Dp) {
    val image = bitmap.asImageBitmap()
    val ratio = image.width.toFloat() / image.height.toFloat().coerceAtLeast(1f)
    Image(
        bitmap = image,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .padding(horizontal = horizontal)
            .fillMaxWidth()
            .aspectRatio(ratio)
            .shadow(10.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp)),
    )
}

@Composable
internal fun PlayScanFrame(pending: PendingScan, page: Bitmap) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                .height(268.dp)
                .clip(RoundedCornerShape(18.dp)),
        ) {
            Image(
                bitmap = page.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
            ScanCornerBrackets()
        }
        Box(modifier = Modifier.weight(1f)) {
            PlayScanner(pending, attached = false)
        }
    }
}

/** Crisp detection corners on the visible page. Drawn in vectors so they stay sharp. */
@Composable
private fun ScanCornerBrackets() {
    val ink = androidx.compose.ui.graphics.Color(0xFF1A140C)
    val gold = androidx.compose.ui.graphics.Color(0xFFE4C98A)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val inset = 18.dp.toPx()
        val arm = 34.dp.toPx()
        val under = Stroke(width = 6.5.dp.toPx(), cap = StrokeCap.Round)
        val stroke = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
        val right = size.width - inset
        val bottom = size.height - inset
        fun corner(color: androidx.compose.ui.graphics.Color, width: Float, x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(color, Offset(x, y), Offset(x + dx, y), width, StrokeCap.Round)
            drawLine(color, Offset(x, y), Offset(x, y + dy), width, StrokeCap.Round)
        }
        val corners = listOf(
            floatArrayOf(inset, inset, arm, arm),
            floatArrayOf(right, inset, -arm, arm),
            floatArrayOf(inset, bottom, arm, -arm),
            floatArrayOf(right, bottom, -arm, -arm),
        )
        corners.forEach { (x, y, dx, dy) -> corner(ink, under.width, x, y, dx, dy) }
        corners.forEach { (x, y, dx, dy) -> corner(gold, stroke.width, x, y, dx, dy) }
    }
}

@Composable
internal fun PlayMaintenance() {
    SoftAppPageScaffold(
        title = stringResource(R.string.maintenance_title),
        subtitle = stringResource(R.string.maintenance_active_section),
        useDashboardHero = true,
        showBack = true,
        onBack = {},
        showPhoneMenu = false,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MaintenanceTasksSection(
                activeProgress = playMaintenanceProgress(),
                completedTasks = playCompletedMaintenance(),
                onAddTask = {},
                onCompleteTask = {},
                onDeleteTask = {},
            )
        }
    }
}

internal fun playAsset(name: String): Bitmap {
    val stream = PlayListingFeatureScreens::class.java.classLoader
        ?.getResourceAsStream("play/$name")
        ?: error("missing test resource play/$name")
    return stream.use { BitmapFactory.decodeStream(it) ?: error("undecodable play/$name") }
}

/** Class token so the test resource loader has a stable Class object. */
private object PlayListingFeatureScreens

internal fun stampLocationPhoto(context: Context, source: Bitmap): File {
    // Watermark the decoded pixels and store them losslessly. A second JPEG pass
    // and an upscale both soften the truck and the location stamp.
    val stamped = PhotoManager(context).addWatermark(
        bitmap = source,
        locationData = com.truckerload.utils.LocationData(
            latitude = 35.711,
            longitude = -78.614,
            city = "Garner",
            state = "NC",
            zipCode = "27529",
        ),
        timestamp = PLAY_STORY_AT,
        title = "T-116KYL6KW",
    )
    val file = File(File(System.getProperty("java.io.tmpdir"), "play-photo").apply { mkdirs() }, "garner-stop.png")
    file.outputStream().use { stamped.compress(Bitmap.CompressFormat.PNG, 100, it) }
    if (stamped !== source) stamped.recycle()
    return file
}

internal fun playScanPending(pageFile: File): PendingScan = PendingScan(
    file = pageFile,
    pageCount = 1,
    ocrText = """
        NORTHLINE FREIGHT
        RATE CONFIRMATION

        Load: T-116KYL6KW
        Pickup: SWF2, Garner, NC
        Delivery: DAL3, Dallas, TX
        Miles: 850
        Rate: ${'$'}2,500.00
        Equipment: Dry van
        Date: October 20, 2026
    """.trimIndent(),
    timestamp = PLAY_STORY_AT,
    category = com.truckerload.domain.model.ScanDocumentCategory.LOAD,
)

private fun playMaintenanceProgress(): List<MaintenanceProgress> = listOf(
    MaintenanceProgress(
        task = MaintenanceTask(
            id = 1,
            title = "Oil and filter",
            startDate = "2026-06-12",
            reminderType = MaintenanceReminderType.MILES,
            intervalMiles = 15_000.0,
            odometerAtStart = 412_200.0,
        ),
        milesDrivenSinceStart = 14_880.0,
        estimatedOdometer = 427_080.0,
        targetOdometer = 427_200.0,
        milesRemaining = 120.0,
        daysRemaining = null,
        isDue = true,
        loadsCounted = 18,
        progressFraction = 0.99f,
    ),
    MaintenanceProgress(
        task = MaintenanceTask(
            id = 2,
            title = "Annual inspection",
            startDate = "2025-11-02",
            reminderType = MaintenanceReminderType.DATE,
            dueDate = "2026-11-02",
        ),
        milesDrivenSinceStart = 0.0,
        estimatedOdometer = null,
        targetOdometer = null,
        milesRemaining = null,
        daysRemaining = 13,
        isDue = false,
        loadsCounted = 0,
        progressFraction = 0.6f,
    ),
    MaintenanceProgress(
        task = MaintenanceTask(
            id = 4,
            title = "Drive tires",
            startDate = "2026-04-02",
            reminderType = MaintenanceReminderType.MILES,
            intervalMiles = 40_000.0,
            odometerAtStart = 390_000.0,
        ),
        milesDrivenSinceStart = 18_400.0,
        estimatedOdometer = 408_400.0,
        targetOdometer = 430_000.0,
        milesRemaining = 21_600.0,
        daysRemaining = null,
        isDue = false,
        loadsCounted = 22,
        progressFraction = 0.46f,
    ),
)

private fun playCompletedMaintenance(): List<MaintenanceTask> = listOf(
    MaintenanceTask(
        id = 3,
        title = "Brake inspection",
        startDate = "2026-08-04",
        reminderType = MaintenanceReminderType.MILES,
        isCompleted = true,
        completedAt = 1_760_000_000_000L,
    ),
)
