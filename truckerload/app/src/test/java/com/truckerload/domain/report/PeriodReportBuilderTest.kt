package com.truckerload.domain.report

import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.EquipmentType
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.MiscExpense
import com.truckerload.domain.model.Penalty
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.domain.money.Money
import com.truckerload.domain.money.UnitPrice
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getMonthRange
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getWeekRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class PeriodReportBuilderTest {

    private val generated = LocalDate.of(2026, 11, 21)
    private val numbers = PeriodReportNumbers(
        money = { value ->
            val negative = value.cents < 0
            val body = value.formatPlain().removePrefix("-")
            if (negative) "-$$body" else "$$body"
        },
        miles = { String.format(Locale.US, "%.0f mi", it) },
        rpm = { String.format(Locale.US, "$%.2f/mi", it) },
        volume = { String.format(Locale.US, "%.0f gal", it) },
        date = { it.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)) },
    )

    @Test
    fun totals_ordering_andFormatting_matchTheContract() {
        val report = reportFor(sampleLoads(), fuel = 180.0, gallons = 40.0, expense = 25.0, goal = 12_000.0)
        val totals = report.totals

        assertEquals(209_100L, totals.gross.cents)
        assertEquals(3, totals.loadCount)
        assertEquals(900.0, totals.loadedMiles, 0.001)
        assertEquals(31.0, totals.emptyMiles, 0.001)
        assertEquals(931.0, totals.totalMiles, 0.001)
        assertEquals(2091.0 / 931.0, totals.avgRpm, 0.0001)
        assertEquals(18_000L, totals.fuel.cents)
        assertEquals(40.0, totals.fuelGallons, 0.001)
        assertEquals(2_500L, totals.expenses.cents)
        assertEquals(2_000L, totals.penalties.cents)
        assertEquals(186_600L, totals.net.cents)
        assertEquals(1_200_000L, totals.goal?.cents)
        assertEquals(17, totals.goalPercent)

        val text = report.plainText
        assertBefore(text, "Driver: Alex Morgan", "Period:")
        assertBefore(text, "Period: Week 47", "Generated: Nov 21, 2026")
        assertBefore(text, "Summary", "Gross: $2091.00")
        assertBefore(text, "Gross: $2091.00", "Loads: 3")
        assertBefore(text, "Loads: 3", "Loaded miles: 900 mi")
        assertBefore(text, "Loaded miles: 900 mi", "Empty miles: 31 mi")
        assertBefore(text, "Empty miles: 31 mi", "Total miles: 931 mi")
        assertBefore(text, "Total miles: 931 mi", "Avg rate per mile: $2.25/mi")
        assertBefore(text, "Fuel: $180.00", "Fuel volume: 40 gal")
        assertBefore(text, "Fuel volume: 40 gal", "Expenses: $25.00")
        assertBefore(text, "Expenses: $25.00", "Penalties: $20.00")
        assertBefore(text, "Penalties: $20.00", "Net: $1866.00")
        assertBefore(text, "Net: $1866.00", "Goal: $2091.00 of $12000.00 (17%)")
        assertBefore(text, "1. Nov 15, 2026", "Reference: T-EARLY")
        assertBefore(text, "Reference: T-EARLY", "2. Nov 15, 2026")
        assertBefore(text, "2. Nov 15, 2026", "Reference: T-LATER")
        assertBefore(text, "Reference: T-LATER", "3. Nov 16, 2026 – Nov 18, 2026")
        val betweenLoads = text.substring(
            text.indexOf("Reference: T-LATER"),
            text.indexOf("3. Nov 16, 2026 – Nov 18, 2026"),
        )
        assertTrue(betweenLoads.contains(PERIOD_REPORT_LOAD_DIVIDER))
        assertBefore(text, "Pickup: Garner, NC", "Delivery: Dallas, TX")
        assertBefore(text, "Delivery: Dallas, TX", "Reference: T-116")
        assertBefore(text, "Miles: 300 mi loaded, 31 mi empty", "Rate: $891.00")
        assertBefore(text, "Rate: $891.00", "Rate per mile: $2.69/mi")
        assertTrue(text.contains("Equipment: Dry Van"))
        assertTrue(text.contains("Status: Dispute open"))
        assertTrue(text.contains("Notes: Gate 12; Late fee ($20.00); Dispute amount: $50.00"))
        assertEquals(2, text.split(PERIOD_REPORT_LOAD_DIVIDER).size - 1)
        assertTrue(text.endsWith("Stored only on this device until you share it.\n"))
        assertFalse(text.contains("Broker"))
    }

    @Test
    fun screenshotWeek_showsGoalPercentAndZeroFuel() {
        val only = load(
            id = "one",
            trip = "T-116KYL6KW",
            date = "2026-11-16",
            finish = "2026-11-16",
            rate = 891.0,
            miles = 331.0,
            pickup = "Garner, NC",
            delivery = "Dallas, TX",
            parsedAt = 1L,
        )
        val report = reportFor(listOf(only), fuel = 0.0, gallons = 0.0, expense = 0.0, goal = 12_000.0)
        assertEquals(7, report.totals.goalPercent)
        assertEquals(89100L, report.totals.gross.cents)
        assertEquals(331.0, report.totals.totalMiles, 0.001)
        val text = report.plainText
        assertTrue(text.contains("Gross: $891.00"))
        assertTrue(text.contains("Loads: 1"))
        assertTrue(text.contains("Empty miles: 0 mi"))
        assertTrue(text.contains("Avg rate per mile: $2.69/mi"))
        assertTrue(text.contains("Fuel: $0.00"))
        assertFalse(text.contains("Fuel volume"))
        assertFalse(text.contains("Penalties"))
        assertTrue(text.contains("Goal: $891.00 of $12000.00 (7%)"))
        assertTrue(text.contains("Reference: T-116KYL6KW"))
        assertFalse(text.contains(PERIOD_REPORT_LOAD_DIVIDER))
    }

    @Test
    fun emptyPeriod_keepsSummaryAndOmitsLoadFields() {
        val report = reportFor(emptyList(), fuel = 40.0, gallons = 0.0, expense = 10.0, goal = 12_000.0)
        assertEquals(0, report.totals.loadCount)
        assertEquals(-5_000L, report.totals.net.cents)
        assertEquals(0, report.totals.goalPercent)
        assertEquals(PERIOD_REPORT_EMPTY_VALUE, report.summary.first { it.label == "Avg rate per mile" }.value)
        val text = report.plainText
        assertTrue(text.contains("No loads in this period."))
        assertTrue(text.contains("Net: -$50.00"))
        assertFalse(text.contains("Reference:"))
        assertFalse(text.contains("Pickup:"))
        assertEquals(0, report.loads.size)
    }

    @Test
    fun blankDriverAndBlankReference_areOmitted() {
        val bare = load(
            id = "bare",
            trip = "  ",
            date = "2026-11-15",
            finish = "2026-11-15",
            rate = 10.0,
            miles = 0.0,
            pickup = "",
            delivery = "",
            parsedAt = 1L,
        )
        val report = PeriodReportBuilder.build(
            draft = PeriodReportAssembler.assemble(
                loads = listOf(bare),
                diesel = emptyList(),
                expenses = emptyList(),
                window = weekWindow(),
                driverName = "   ",
                periodLabel = "Week 47",
                generatedOn = generated,
                locale = Locale.US,
                goal = null,
                labels = labels(),
                numbers = numbers,
            ),
            labels = labels(),
            numbers = numbers,
        )
        assertFalse(report.plainText.contains("Driver:"))
        assertFalse(report.plainText.contains("Reference:"))
        assertFalse(report.plainText.contains("Pickup:"))
        assertFalse(report.plainText.contains("Rate per mile:"))
        assertNull(report.totals.goal)
        assertFalse(report.summary.any { it.label == "Goal" })
    }

    @Test
    fun fuelFollowsWeekId_expensesFollowDates_dayOmitsFuel() {
        val week = weekWindow()
        val diesel = listOf(
            diesel(id = 1, week = 47, year = 2026, amount = 180.0, gallons = 40.0),
            diesel(id = 2, week = 46, year = 2026, amount = 90.0, gallons = 20.0),
        )
        val expenses = listOf(
            expense(1, "2026-11-15", 25.0),
            expense(2, "2026-11-14", 99.0),
        )
        val included = PeriodReportAssembler.assemble(
            loads = emptyList(),
            diesel = diesel,
            expenses = expenses,
            window = week,
            driverName = "Alex Morgan",
            periodLabel = "Week 47",
            generatedOn = generated,
            locale = Locale.US,
            goal = Money.fromDollars(1.0),
            labels = labels(),
            numbers = numbers,
        )
        assertEquals(18_000L, included.fuel.cents)
        assertEquals(2_500L, included.expenses.cents)

        val day = PeriodReportWindow(
            kind = PeriodReportKind.DAY,
            start = LocalDate.of(2026, 11, 15),
            end = LocalDate.of(2026, 11, 15),
            includeExpenses = true,
        )
        val dayDraft = PeriodReportAssembler.assemble(
            loads = emptyList(),
            diesel = diesel,
            expenses = expenses,
            window = day,
            driverName = "",
            periodLabel = "Nov 15, 2026",
            generatedOn = generated,
            locale = Locale.US,
            goal = Money.fromDollars(12_000.0),
            labels = labels(),
            numbers = numbers,
        )
        assertFalse(dayDraft.showFuel)
        assertEquals(Money.ZERO, dayDraft.fuel)
        assertEquals(2_500L, dayDraft.expenses.cents)
        assertNull(dayDraft.goal)
    }

    @Test
    fun monthIncludesOverlappingDieselWeek() {
        val month = PeriodReportWindow(
            kind = PeriodReportKind.MONTH,
            start = LocalDate.of(2026, 11, 1),
            end = LocalDate.of(2026, 11, 30),
            includeFuel = true,
            includeExpenses = true,
        )
        val rows = listOf(
            diesel(1, 44, 2026, 10.0, 1.0, start = "2026-10-26", end = "2026-11-01"),
            diesel(2, 40, 2026, 10.0, 1.0, start = "2026-10-01", end = "2026-10-07"),
        )
        assertTrue(rows[0].belongsTo(month))
        assertFalse(rows[1].belongsTo(month))
    }

    @Test
    fun window_weekMonthDayAndDispute_matchTheJournalRanges() {
        val (week, year) = getWeekNumberAndYearFromDate("2026-11-15")
        val (start, end, _) = getWeekRange(week, year)
        val calendar = PeriodReportWindowResolver.resolve(
            PeriodReportSelection(
                filter = LoadFilter.CALENDAR_WEEK,
                selectedWeekStart = "2026-11-15",
            ),
        )
        assertEquals(PeriodReportKind.WEEK, calendar.kind)
        assertEquals(LocalDate.parse(start), calendar.start)
        assertEquals(LocalDate.parse(end), calendar.end)
        assertEquals(week, calendar.weekNumber)
        assertTrue(calendar.includeWeeklyGoal)
        assertTrue(calendar.includeFuel)

        val (monthStart, monthEnd) = getMonthRange(11, 2026)
        val month = PeriodReportWindowResolver.resolve(
            PeriodReportSelection(
                filter = LoadFilter.THIS_MONTH,
                selectedMonthYear = 2026,
                selectedMonth = 11,
                selectedMonthLabel = "November 2026",
            ),
        )
        assertEquals(LocalDate.parse(monthStart), month.start)
        assertEquals(LocalDate.parse(monthEnd), month.end)
        assertFalse(month.includeWeeklyGoal)
        assertTrue(month.includeFuel)
        val monthLabel = PeriodReportWindowResolver.label(
            month,
            Locale.US,
            { it.toString() },
            templates(),
        )
        assertEquals("November 2026 · $monthStart – $monthEnd", monthLabel)

        val (currentWeek, currentYear) = getCurrentWeekNumberAndYear()
        val (currentStart, currentEnd, _) = getWeekRange(currentWeek, currentYear)
        listOf(LoadFilter.YESTERDAY, LoadFilter.CALENDAR_DATE, LoadFilter.DISPUTE, LoadFilter.ALL).forEach { filter ->
            val fallback = PeriodReportWindowResolver.resolve(PeriodReportSelection(filter = filter))
            assertEquals(PeriodReportKind.WEEK, fallback.kind)
            assertEquals(LocalDate.parse(currentStart), fallback.start)
            assertEquals(LocalDate.parse(currentEnd), fallback.end)
            assertEquals(currentWeek, fallback.weekNumber)
            assertTrue(fallback.includeWeeklyGoal)
        }

        val current = PeriodReportWindowResolver.resolve(
            PeriodReportSelection(filter = LoadFilter.THIS_WEEK),
        )
        assertEquals(currentWeek, current.weekNumber)
        assertEquals(currentYear, current.weekYear)
    }

    @Test
    fun cancelledLoad_countsTonuAsGross_keepsDeadhead_andIsMarkedTonuInTheStatement() {
        val booked = load(
            id = "tonu",
            trip = "T-TONU",
            date = "2026-11-16",
            finish = "2026-11-16",
            rate = 4_418.93,
            miles = 900.0,
            deadhead = 55.0,
            pickup = "Garner, NC",
            delivery = "Dallas, TX",
            parsedAt = 5L,
        )
        val cancelled = com.truckerload.domain.model.LoadCancellation.cancel(booked, Money.fromCents(17_500L), 10L)
        val report = reportFor(listOf(cancelled), fuel = 0.0, gallons = 0.0, expense = 0.0, goal = null)

        assertEquals(17_500L, report.totals.gross.cents)
        assertEquals(0.0, report.totals.loadedMiles, 0.001)
        assertEquals(55.0, report.totals.emptyMiles, 0.001)
        val row = report.statementRows.single()
        assertEquals("T-TONU (TONU)", row.reference)
        assertEquals(PERIOD_REPORT_EMPTY_VALUE, row.ratePerMile)
        assertTrue(report.plainText.contains("Cancelled · TONU"))
    }

    private fun reportFor(
        loads: List<Load>,
        fuel: Double,
        gallons: Double,
        expense: Double,
        goal: Double?,
    ) = PeriodReportBuilder.build(
        draft = PeriodReportAssembler.assemble(
            loads = loads,
            diesel = if (fuel == 0.0 && gallons == 0.0) {
                emptyList()
            } else {
                listOf(diesel(1, 47, 2026, fuel, gallons))
            },
            expenses = if (expense == 0.0) emptyList() else listOf(expense(1, "2026-11-16", expense)),
            window = weekWindow(),
            driverName = "Alex Morgan",
            periodLabel = "Week 47",
            generatedOn = generated,
            locale = Locale.US,
            goal = goal?.let { Money.fromDollars(it) },
            labels = labels(),
            numbers = numbers,
        ),
        labels = labels(),
        numbers = numbers,
    )

    private fun weekWindow() = PeriodReportWindow(
        kind = PeriodReportKind.WEEK,
        start = LocalDate.of(2026, 11, 15),
        end = LocalDate.of(2026, 11, 21),
        weekNumber = 47,
        weekYear = 2026,
        includeWeeklyGoal = true,
        includeFuel = true,
        includeExpenses = true,
    )

    private fun sampleLoads(): List<Load> = listOf(
        load(
            id = "late",
            trip = "T-LATER",
            date = "2026-11-15",
            finish = "2026-11-15",
            rate = 1_000.0,
            miles = 500.0,
            pickup = "Charlotte, NC",
            delivery = "Atlanta, GA",
            parsedAt = 9L,
        ),
        load(
            id = "main",
            trip = "T-116",
            date = "2026-11-16",
            finish = "2026-11-18",
            rate = 891.0,
            miles = 300.0,
            deadhead = 31.0,
            pickup = "Garner, NC",
            delivery = "Dallas, TX",
            parsedAt = 4L,
            dispute = true,
            equipment = EquipmentType.DRY_VAN,
            note = "Gate 12",
            penalty = 20.0,
            disputeAmount = 50.0,
        ),
        load(
            id = "early",
            trip = "T-EARLY",
            date = "2026-11-15",
            finish = "2026-11-15",
            rate = 200.0,
            miles = 100.0,
            pickup = "Raleigh, NC",
            delivery = "Richmond, VA",
            parsedAt = 3L,
        ),
    )

    private fun load(
        id: String,
        trip: String,
        date: String,
        finish: String,
        rate: Double,
        miles: Double,
        pickup: String,
        delivery: String,
        parsedAt: Long,
        deadhead: Double = 0.0,
        dispute: Boolean = false,
        equipment: EquipmentType? = null,
        note: String? = null,
        penalty: Double = 0.0,
        disputeAmount: Double? = null,
    ) = Load(
        id = id,
        tripId = trip,
        date = date,
        totalRate = Money.fromDollars(rate),
        totalMiles = miles,
        pointA = pickup,
        pointB = delivery,
        puCount = 1,
        delCount = 1,
        weekNumber = 47,
        year = 2026,
        rawMessage = "",
        parsedAt = parsedAt,
        updatedAt = parsedAt,
        firstPuCityState = pickup,
        lastDelCityState = delivery,
        actualFinishDate = finish,
        deadheadMiles = deadhead,
        isDispute = dispute,
        disputeCompleted = false,
        disputeAmount = disputeAmount?.let { Money.fromDollars(it) },
        equipmentType = equipment,
        stops = listOf(
            Stop(0, id, 1, StopType.PU, null, note, date, "EST", null, pickup, "", "", ""),
        ),
        penalties = if (penalty > 0.0) {
            listOf(Penalty(1, id, "Late fee", Money.fromDollars(penalty)))
        } else {
            emptyList()
        },
    )

    private fun diesel(
        id: Int,
        week: Int,
        year: Int,
        amount: Double,
        gallons: Double,
        start: String = "2026-11-15",
        end: String = "2026-11-21",
    ) = Diesel(
        id = id,
        weekNumber = week,
        year = year,
        weekLabel = "W$week",
        weekStartDate = start,
        weekEndDate = end,
        totalAmount = Money.fromDollars(amount),
        gallons = gallons,
        pricePerGallon = UnitPrice.fromDollars(4.0),
        location = "Pilot",
        rawExtractedText = "",
        sourceFileName = null,
        addedAt = 1L,
    )

    private fun expense(id: Int, date: String, amount: Double) = MiscExpense(
        id = id,
        amount = Money.fromDollars(amount),
        description = "Scale",
        date = date,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun labels() = PeriodReportLabels(
        appName = "TruckoRig",
        title = "Period report",
        driver = "Driver",
        period = "Period",
        generated = "Generated",
        summary = "Summary",
        gross = "Gross",
        loads = "Loads",
        loadedMiles = "Loaded miles",
        emptyMiles = "Empty miles",
        totalMiles = "Total miles",
        avgRate = "Avg rate per mile",
        fuel = "Fuel",
        fuelVolume = "Fuel volume",
        expenses = "Expenses",
        penalties = "Penalties",
        net = "Net",
        goal = "Goal",
        goalJoin = "of",
        loadsSection = "Loads",
        emptyLoads = "No loads in this period.",
        pickup = "Pickup",
        delivery = "Delivery",
        reference = "Reference",
        miles = "Miles",
        milesSplit = "%1\$s loaded, %2\$s empty",
        rate = "Rate",
        ratePerMile = "Rate per mile",
        equipment = "Equipment",
        status = "Status",
        notes = "Notes",
        disputeOpen = "Dispute open",
        disputeClosed = "Dispute closed",
        disputeAmount = "Dispute amount",
        footer = "Stored only on this device until you share it.",
        equipmentNames = mapOf(EquipmentType.DRY_VAN to "Dry Van"),
    )

    private fun templates() = PeriodReportPeriodTemplates(
        weekRange = "Week %1\$d · %2\$s – %3\$s",
        monthRange = "%1\$s · %2\$s – %3\$s",
        yearRange = "%1\$d · %2\$s – %3\$s",
        disputes = "Disputes",
        allLoads = "All loads",
    )

    private fun assertBefore(text: String, earlier: String, later: String) {
        val start = text.indexOf(earlier)
        val end = text.indexOf(later)
        assertTrue("missing '$earlier'", start >= 0)
        assertTrue("'$earlier' should precede '$later'", start < end)
    }
}
