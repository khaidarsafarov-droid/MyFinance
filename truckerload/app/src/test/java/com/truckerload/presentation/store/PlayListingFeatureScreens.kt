package com.truckerload.presentation.store

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLocale
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
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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

@Composable
internal fun PlayWidgetHome(wallpaper: Bitmap, large: Bitmap, compact: Bitmap) {
    val locale = LocalLocale.current.platformLocale
    val date = remember(locale) {
        LocalDate.of(2026, 10, 20).format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale),
        )
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = wallpaper.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DemoStatusBar(onDark = true)
            Spacer(Modifier.height(18.dp))
            Text(
                text = "9:41",
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
            )
            Text(
                text = date,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.weight(1f))
            WidgetPlate(compact, horizontal = 36.dp)
            Spacer(Modifier.height(14.dp))
            WidgetPlate(large, horizontal = 14.dp)
        }
    }
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
