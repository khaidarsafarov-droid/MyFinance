package com.truckerload.presentation.store

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckerload.R
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.RpmThresholds
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.ScanDocumentCategory
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.domain.model.analytics.DailyData
import com.truckerload.domain.model.analytics.PeriodFinance
import com.truckerload.domain.model.analytics.RouteData
import com.truckerload.domain.model.analytics.WeekData
import com.truckerload.domain.money.Money
import com.truckerload.domain.money.UnitPrice
import com.truckerload.presentation.components.DashboardHero
import com.truckerload.presentation.components.LoadCard
import com.truckerload.presentation.components.OneUiBottomActionBar
import com.truckerload.presentation.components.TlButton
import com.truckerload.presentation.components.charts.DailyDistributionChart
import com.truckerload.presentation.components.charts.TopRoutesBarChart
import com.truckerload.presentation.components.charts.WeeklyRevenueLineChart
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.screens.add.AddLoadInputMode
import com.truckerload.presentation.screens.add.AddLoadManualForm
import com.truckerload.presentation.screens.add.AddLoadModeSelector
import com.truckerload.presentation.screens.add.DieselAmountFields
import com.truckerload.presentation.screens.add.ManualLoadFields
import com.truckerload.presentation.screens.analytics.AnalyticsFinanceSection
import com.truckerload.presentation.screens.auth.TelegramOnboardingScreen
import com.truckerload.presentation.screens.camera.CapturedPhoto
import com.truckerload.presentation.screens.camera.PhotoPreviewScreen
import com.truckerload.presentation.screens.diesel.DieselFillCard
import com.truckerload.presentation.screens.home.HomeScreenTopBar
import com.truckerload.presentation.screens.home.HomeWeekHero
import com.truckerload.presentation.screens.scanner.PendingScan
import com.truckerload.presentation.screens.scanner.ScanResultScreen
import com.truckerload.presentation.screens.settings.SettingsPremiumHeroModel
import com.truckerload.presentation.screens.settings.SettingsPremiumPhase
import com.truckerload.presentation.screens.settings.SettingsStylePreview
import com.truckerload.presentation.screens.social.ProfileCard
import com.truckerload.presentation.screens.social.ProfileHero
import com.truckerload.presentation.screens.social.ProfileNavRow
import com.truckerload.presentation.screens.social.ProfileSectionHeader
import com.truckerload.presentation.screens.social.ProfileTotals
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.utils.LocationData
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal val PlayThresholds = RpmThresholds(minProfit = 2.0, targetProfit = 2.50)

internal fun playLoads(): List<Load> = listOf(
    playLoad(
        id = "load-relay",
        tripId = "T-116KYL6KW",
        rate = 2500.0,
        miles = 850.0,
        origin = "Garner, NC",
        dest = "Dallas, TX",
        raw = "Amazon Relay\nTrip ID: T-116KYL6KW\nTotal Rate: 2500.00\nPu-address: SWF2, Garner, NC\nDel-address: DAL3, Dallas, TX",
        date = "2026-10-19",
    ),
    playLoad(
        id = "load-manual",
        tripId = "M-0826-CHI",
        rate = 2400.0,
        miles = 720.0,
        origin = "Chicago, IL",
        dest = "Atlanta, GA",
        raw = "",
        date = "2026-10-18",
    ),
    playLoad(
        id = "load-broker",
        tripId = "KC-PHX-19",
        rate = 2890.0,
        miles = 1180.0,
        origin = "Kansas City, MO",
        dest = "Phoenix, AZ",
        raw = "Broker load\nRate confirmation KC-PHX-19",
        date = "2026-10-17",
    ),
    playLoad(
        id = "load-amarillo",
        tripId = "114RS221Y",
        rate = 3815.90,
        miles = 1369.0,
        origin = "Aurora, CO",
        dest = "Arlington, WA",
        raw = "Amazon Relay\nTrip ID: 114RS221Y\nTotal Rate: 3815.90\nPu-address: DEN4, Aurora, CO\nDel-address: SEA8, Arlington, WA",
        date = "2026-10-16",
    ),
)

private fun playLoad(
    id: String,
    tripId: String,
    rate: Double,
    miles: Double,
    origin: String,
    dest: String,
    raw: String,
    date: String,
): Load {
    val (oCity, oState) = splitPlace(origin)
    val (dCity, dState) = splitPlace(dest)
    return Load(
        id = id,
        tripId = tripId,
        date = date,
        totalRate = Money.fromDollars(rate),
        totalMiles = miles,
        pointA = origin,
        pointB = dest,
        puCount = 1,
        delCount = 1,
        weekNumber = 43,
        year = 2026,
        rawMessage = raw,
        parsedAt = 1_760_000_000_000L,
        updatedAt = 1_760_000_000_000L,
        firstPuCityState = origin,
        lastDelCityState = dest,
        stopCount = 2,
        stops = listOf(
            Stop(1, id, 1, StopType.PU, "PU1", null, "08:00", "America/Chicago", "SWF2", origin, oCity, oState, "27529"),
            Stop(2, id, 2, StopType.DEL, null, null, "18:00", "America/Chicago", "DAL3", dest, dCity, dState, "75201"),
        ),
    )
}

private fun splitPlace(place: String): Pair<String, String> {
    val parts = place.split(",").map { it.trim() }
    return (parts.getOrElse(0) { place }) to (parts.getOrElse(1) { "" })
}

@Composable
internal fun PlayJournal(loads: List<Load>, dense: Boolean) {
    val shown = if (dense) loads else loads.take(3)
    BentoGlassScreenBackground {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            HomeScreenTopBar(
                welcomeName = "Alex",
                weekLabel = "",
                filter = LoadFilter.THIS_WEEK,
                openDrawer = {},
                onSearchToggle = {},
                onAvatar = {},
            )
            if (!dense) {
                HomeWeekHero(
                    periodLabel = "",
                    filter = LoadFilter.THIS_WEEK,
                    gross = Money.fromDollars(shown.sumOf { it.totalRate.cents } / 100.0),
                    miles = shown.sumOf { it.drivenMiles },
                    loadCount = shown.size,
                    avgRpm = 2.72,
                    weeklyGoal = 13_000.0,
                    summaryContentDescription = "",
                    onOpenWeeklyGoal = {},
                )
                Spacer(Modifier.height(8.dp))
            }
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
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun PlayTabletJournal(loads: List<Load>) {
    BentoGlassScreenBackground {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1.15f).verticalScroll(rememberScrollState())) {
                HomeScreenTopBar(
                    welcomeName = "Alex",
                    weekLabel = "",
                    filter = LoadFilter.THIS_WEEK,
                    openDrawer = {},
                    onSearchToggle = {},
                    onAvatar = {},
                )
                loads.forEach { load ->
                    LoadCard(
                        load = load,
                        onClick = {},
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        rpmThresholds = PlayThresholds,
                        onCameraClick = {},
                        onScanClick = {},
                    )
                }
            }
            Column(modifier = Modifier.weight(0.85f)) {
                HomeWeekHero(
                    periodLabel = "",
                    filter = LoadFilter.THIS_WEEK,
                    gross = Money.fromDollars(loads.sumOf { it.totalRate.cents } / 100.0),
                    miles = loads.sumOf { it.drivenMiles },
                    loadCount = loads.size,
                    avgRpm = 2.81,
                    weeklyGoal = 13_000.0,
                    summaryContentDescription = "",
                    onOpenWeeklyGoal = {},
                )
            }
        }
    }
}

@Composable
internal fun PlayAddLoad() {
    val context = LocalContext.current
    val store = remember { RpmThresholdsStoreSafe(context) }
    Column(modifier = Modifier.fillMaxSize().background(LocalBrandPalette.current.page)) {
        DashboardHero(
            title = stringResource(R.string.add_load_title),
            showBack = true,
            onBack = {},
        )
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AddLoadModeSelector(selected = AddLoadInputMode.MANUAL, onSelect = {})
            CompositionLocalProvider(LocalRpmThresholdsStore provides store) {
                AddLoadManualForm(
                    fields = ManualLoadFields(
                        tripId = "T-116KYL6KW",
                        date = "2026-10-19",
                        rate = "2500.00",
                        miles = "850",
                        pointA = "Garner, NC",
                        pointB = "Dallas, TX",
                    ),
                    onTripId = {},
                    onDate = {},
                    onRate = {},
                    onMiles = {},
                    onPointChange = { _, _ -> },
                    onAddPoint = {},
                    onRemovePoint = {},
                )
            }
        }
        OneUiBottomActionBar {
            TlButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.common_save))
            }
        }
    }
}

@Composable
internal fun PlayCamera(photo: CapturedPhoto) {
    PhotoPreviewScreen(
        photo = photo,
        onCancel = {},
        onRetake = {},
        onSaved = {},
        onSave = {},
        saveSuccess = false,
    )
}

@Composable
internal fun PlayScanner(pending: PendingScan, attached: Boolean = true) {
    ScanResultScreen(
        pending = pending,
        sessionCount = 1,
        isAttachedToLoad = attached,
        onSaveToApp = {},
        onSaveToPhone = {},
        onShare = {},
        onAddAnother = {},
        onOpenGallery = {},
        onClose = {},
    )
}

@Composable
internal fun PlayDiesel(uiState: com.truckerload.presentation.screens.add.AddDieselUiState, recent: Diesel) {
    Column(modifier = Modifier.fillMaxSize().background(LocalBrandPalette.current.page).verticalScroll(rememberScrollState())) {
        DashboardHero(
            title = stringResource(R.string.diesel_title),
            subtitle = recent.location,
            showBack = true,
            onBack = {},
        )
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DieselAmountFields(
                uiState = uiState,
                onGallonsChange = {},
                onPriceChange = {},
                onDiscountChange = {},
                onLocationChange = {},
                onImagePicked = {},
            )
            DieselFillCard(diesel = recent)
        }
    }
}

@Composable
internal fun PlayAnalytics(finance: PeriodFinance, routes: List<RouteData>, weeks: List<WeekData>, days: List<DailyData>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalBrandPalette.current.page)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DashboardHero(
            title = stringResource(R.string.analytics_title),
            subtitle = stringResource(R.string.analytics_subtitle),
            showBack = true,
            onBack = {},
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnalyticsFinanceSection(finance = finance)
            TopRoutesBarChart(routes = routes)
            DailyDistributionChart(dailyData = days)
            WeeklyRevenueLineChart(weeks = weeks, selectedIndex = weeks.lastIndex, onWeekSelected = {})
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun PlayFinance(finance: PeriodFinance, recent: Diesel) {
    Column(
        modifier = Modifier.fillMaxSize().background(LocalBrandPalette.current.page).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DashboardHero(
            title = stringResource(R.string.analytics_finance_title),
            subtitle = stringResource(R.string.analytics_subtitle),
            showBack = true,
            onBack = {},
        )
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnalyticsFinanceSection(finance = finance)
            DieselFillCard(diesel = recent)
        }
    }
}

@Composable
internal fun PlayWeeklyChart(weeks: List<WeekData>) {
    Column(modifier = Modifier.fillMaxSize().background(LocalBrandPalette.current.page)) {
        DashboardHero(
            title = stringResource(R.string.analytics_title),
            subtitle = stringResource(R.string.nav_stats),
            showBack = true,
            onBack = {},
        )
        WeeklyRevenueLineChart(
            weeks = weeks,
            selectedIndex = weeks.lastIndex,
            onWeekSelected = {},
            modifier = Modifier.padding(16.dp).height(280.dp),
        )
    }
}

@Composable
internal fun PlayTelegram() {
    val context = LocalContext.current
    val auth = remember {
        AuthStore.ignoreBootstrapForTests = true
        AuthStore(context)
    }
    CompositionLocalProvider(LocalAuthStore provides auth) {
        TelegramOnboardingScreen(onCompleted = {}, onSkip = {})
    }
}

@Composable
internal fun PlayProfile() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalBrandPalette.current.page)
            .verticalScroll(rememberScrollState()),
    ) {
        ProfileHero(
            givenName = "Alex",
            familyName = "Morgan",
            avatarUrl = null,
            subscribed = true,
            totalLoads = 48,
            totalRevenue = 128_400.0,
            averageRpm = 2.74,
            redThreshold = 2.0,
            greenThreshold = 2.50,
            showMenu = true,
            showBack = false,
            onBack = {},
            onEditName = {},
            onAddLoad = {},
        )
        ProfileTotals(loads = 48, miles = 46_200)
        ProfileSectionHeader(stringResource(R.string.profile_account_title))
        ProfileCard {
            ProfileNavRow(
                icon = AppIcons.Person,
                title = stringResource(R.string.profile_name_section_title),
                subtitle = "Alex Morgan",
                onClick = {},
            )
            ProfileNavRow(
                icon = AppIcons.CloudStorage,
                title = stringResource(R.string.drive_sync_title),
                subtitle = stringResource(R.string.profile_drive_never),
                premiumLocked = false,
                onClick = {},
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun PlaySettings() {
    SettingsStylePreview(
        model = SettingsPremiumHeroModel(SettingsPremiumPhase.ACTIVE, "$4.99"),
        dark = false,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
internal fun PlayWidgetLauncher(widget: Bitmap) {
    val image = widget.asImageBitmap()
    val ratio = image.width.toFloat() / image.height.toFloat().coerceAtLeast(1f)
    val locale = LocalLocale.current.platformLocale
    val date = remember(locale) {
        LocalDate.of(2026, 10, 19).format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFF3A4A5C), androidx.compose.ui.graphics.Color(0xFF1C2836)))),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DemoStatusBar(onDark = true)
            Spacer(Modifier.height(36.dp))
            Text(text = "9:41", color = androidx.compose.ui.graphics.Color.White, fontSize = 72.sp, fontWeight = FontWeight.Light)
            Text(
                text = date,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(28.dp))
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.padding(horizontal = 18.dp).fillMaxWidth().aspectRatio(ratio),
            )
        }
    }
}

/** Device chrome for listing shots: 9:41, full signal, full battery. Not app UI. */
@Composable
internal fun DemoStatusBar(onDark: Boolean) {
    val ink = if (onDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFF1A1A1A)
    Row(
        modifier = Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "9:41", color = ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            SignalGlyph(ink)
            BatteryGlyph(ink)
        }
    }
}

@Composable
private fun SignalGlyph(ink: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.width(16.dp).height(12.dp)) {
        val bars = 4
        val gap = size.width * 0.18f
        val w = (size.width - gap * (bars - 1)) / bars
        for (i in 0 until bars) {
            val h = size.height * (0.35f + 0.2f * i)
            drawRect(
                color = ink,
                topLeft = androidx.compose.ui.geometry.Offset(i * (w + gap), size.height - h),
                size = androidx.compose.ui.geometry.Size(w, h),
            )
        }
    }
}

@Composable
private fun BatteryGlyph(ink: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.width(22.dp).height(12.dp)) {
        val body = androidx.compose.ui.geometry.Size(size.width * 0.82f, size.height * 0.72f)
        val top = (size.height - body.height) / 2f
        drawRoundRect(
            color = ink,
            topLeft = androidx.compose.ui.geometry.Offset(0f, top),
            size = body,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6f),
        )
        drawRect(
            color = ink,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.86f, size.height * 0.35f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.1f, size.height * 0.3f),
        )
        drawRoundRect(
            color = ink,
            topLeft = androidx.compose.ui.geometry.Offset(1.8f, top + 1.8f),
            size = androidx.compose.ui.geometry.Size(body.width - 3.6f, body.height - 3.6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f, 1f),
        )
    }
}

internal fun demoPhotoFile(): File {
    val file = File.createTempFile("play-stop", ".png")
    val bitmap = Bitmap.createBitmap(900, 1200, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.parseColor("#8EADC4")
    canvas.drawRect(0f, 0f, 900f, 700f, paint)
    paint.color = Color.parseColor("#C4B496")
    canvas.drawRect(0f, 700f, 900f, 1200f, paint)
    paint.color = Color.parseColor("#3E4650")
    canvas.drawRect(180f, 860f, 720f, 1200f, paint)
    paint.color = Color.parseColor("#E6C36A")
    canvas.drawRoundRect(230f, 420f, 670f, 980f, 18f, 18f, paint)
    paint.color = Color.parseColor("#1E2150")
    canvas.drawRect(250f, 460f, 650f, 700f, paint)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    return file
}

internal fun demoScanFile(): File {
    val file = File(File(System.getProperty("java.io.tmpdir"), "play-scan").apply { mkdirs() }, "rate-confirmation.pdf")
    file.writeText(
        "Trip ID: T-116KYL6KW\nTotal Rate: 2500.00\nTotal Loaded Miles: 850 mi\nPu-address: SWF2, Garner, NC\nDel-address: DAL3, Dallas, TX\n",
    )
    return file
}

internal fun demoPending(file: File): PendingScan = PendingScan(
    file = file,
    pageCount = 2,
    ocrText = file.readText(),
    timestamp = 1_760_864_460_000L,
    category = ScanDocumentCategory.LOAD,
)

internal fun demoCaptured(file: File): CapturedPhoto = CapturedPhoto(
    file = file,
    locationData = LocationData(
        latitude = 35.222,
        longitude = -101.831,
        city = "Amarillo",
        state = "TX",
        zipCode = "79101",
    ),
    timestamp = 1_760_864_460_000L,
)

private fun RpmThresholdsStoreSafe(context: android.content.Context): com.truckerload.data.preferences.RpmThresholdsStore {
    AuthStore.ignoreBootstrapForTests = true
    return com.truckerload.data.preferences.RpmThresholdsStore(context)
}
