package com.truckerload.presentation.store

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import androidx.compose.ui.graphics.asImageBitmap
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
                        gross = Money.fromDollars(shown.sumOf { it.totalRate.cents } / 100.0),
                        miles = shown.sumOf { it.drivenMiles },
                        loadCount = shown.size,
                        avgRpm = 2.94,
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
        LocalDate.of(2026, 10, 19).format(
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
        Image(
            bitmap = page.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                .height(196.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
        Box(modifier = Modifier.weight(1f)) {
            PlayScanner(pending, attached = false)
        }
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
    val width = 1080
    val height = (source.height * (width.toFloat() / source.width)).toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(source, width, height, true)
    val stamped = PhotoManager(context).addWatermark(
        bitmap = scaled,
        locationData = com.truckerload.utils.LocationData(
            latitude = 35.711,
            longitude = -78.614,
            city = "Garner",
            state = "NC",
            zipCode = "27529",
        ),
        timestamp = 1_792_402_860_000L,
        title = "T-116KYL6KW",
    )
    val file = File(File(System.getProperty("java.io.tmpdir"), "play-photo").apply { mkdirs() }, "garner-stop.jpg")
    file.outputStream().use { stamped.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    if (scaled !== source) scaled.recycle()
    if (stamped !== scaled) stamped.recycle()
    return file
}

/**
 * Gold corner brackets on the demo page. Play Services draws the live edge
 * UI; this test only shows the detected quad the way a scan result looks.
 */
internal fun withDetectedEdges(source: Bitmap): Bitmap {
    val width = 900
    val height = (source.height * (width.toFloat() / source.width)).toInt().coerceAtLeast(1)
    val page = Bitmap.createScaledBitmap(source, width, height, true)
    val out = page.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(out)
    val gold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C8A867")
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val inset = 36f
    val arm = 78f
    val right = width - inset
    val bottom = height - inset
    canvas.drawLine(inset, inset, inset + arm, inset, gold)
    canvas.drawLine(inset, inset, inset, inset + arm, gold)
    canvas.drawLine(right, inset, right - arm, inset, gold)
    canvas.drawLine(right, inset, right, inset + arm, gold)
    canvas.drawLine(inset, bottom, inset + arm, bottom, gold)
    canvas.drawLine(inset, bottom, inset, bottom - arm, gold)
    canvas.drawLine(right, bottom, right - arm, bottom, gold)
    canvas.drawLine(right, bottom, right, bottom - arm, gold)
    if (page !== source) page.recycle()
    return out
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
        Date: October 19, 2026
    """.trimIndent(),
    timestamp = 1_792_402_860_000L,
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
