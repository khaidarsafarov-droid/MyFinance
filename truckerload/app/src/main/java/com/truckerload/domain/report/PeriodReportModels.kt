package com.truckerload.domain.report

import com.truckerload.domain.model.EquipmentType
import com.truckerload.domain.money.Money
import java.time.LocalDate

/**
 * Period report shared with a dispatcher.
 *
 * Field order is the contract with TruckoRig-Apple. Do not reorder lines.
 *
 * Header (omit Driver when the name is blank):
 * 1. Driver
 * 2. Period
 * 3. Generated
 *
 * Summary:
 * 1. Gross
 * 2. Loads
 * 3. Loaded miles
 * 4. Empty miles
 * 5. Total miles
 * 6. Avg rate per mile — em dash when total miles are 0
 * 7. Fuel — omitted when the period cannot attribute fuel (a single day, or disputes)
 * 8. Fuel volume — only when gallons are greater than 0
 * 9. Expenses — omitted for disputes; misc expenses dated in the period otherwise
 * 10. Penalties — only when the sum is greater than 0
 * 11. Net — gross minus the fuel, expenses, and penalties that are included
 * 12. Goal — only for a single reporting week when a weekly goal is set.
 *     Value is "gross {of} target (percent%)". The percent is not capped at 100.
 *
 * Each load, oldest first, numbered from 1. Rows, omitting blanks:
 * 1. Date heading (pickup, or pickup – delivery when they differ)
 * 2. Pickup
 * 3. Delivery
 * 4. Reference (trip / load number)
 * 5. Miles (loaded, plus empty when empty miles are greater than 0)
 * 6. Rate
 * 7. Rate per mile — omitted when the load has no miles
 * 8. Equipment — omitted when unset
 * 9. Status — dispute open / dispute closed, omitted otherwise
 * 10. Notes — stop notes, then penalties, then a dispute amount
 *
 * There is no broker or customer name on the load. Do not invent one.
 * Distance and volume follow the in-app unit setting (miles/km, gallons/liters).
 * Fuel on a week report is the diesel stored for that reporting week.
 * Fuel on a month or year report is every diesel week whose dates overlap the period,
 * so a week that crosses a month boundary is included in both months.
 * A one-day report does not include fuel, because diesel is stored per week.
 */
data class ReportLine(val label: String, val value: String)

data class ReportLoad(
    val number: Int,
    val heading: String,
    val rows: List<ReportLine>,
)

data class PeriodReportTotals(
    val gross: Money,
    val loadCount: Int,
    val loadedMiles: Double,
    val emptyMiles: Double,
    val totalMiles: Double,
    val avgRpm: Double,
    val fuel: Money,
    val fuelGallons: Double,
    val expenses: Money,
    val penalties: Money,
    val net: Money,
    val goal: Money?,
    val goalPercent: Int?,
)

data class PeriodReport(
    val appName: String,
    val title: String,
    val header: List<ReportLine>,
    val summaryTitle: String,
    val summary: List<ReportLine>,
    val loadsTitle: String,
    val emptyLoads: String?,
    val loads: List<ReportLoad>,
    val footer: String,
    val totals: PeriodReportTotals,
    val plainText: String,
)

data class PeriodReportLabels(
    val appName: String,
    val title: String,
    val driver: String,
    val period: String,
    val generated: String,
    val summary: String,
    val gross: String,
    val loads: String,
    val loadedMiles: String,
    val emptyMiles: String,
    val totalMiles: String,
    val avgRate: String,
    val fuel: String,
    val fuelVolume: String,
    val expenses: String,
    val penalties: String,
    val net: String,
    val goal: String,
    /** Word between the earned amount and the target, such as "of" or "из". */
    val goalJoin: String,
    val loadsSection: String,
    val emptyLoads: String,
    val pickup: String,
    val delivery: String,
    val reference: String,
    val miles: String,
    val milesSplit: String,
    val rate: String,
    val ratePerMile: String,
    val equipment: String,
    val status: String,
    val notes: String,
    val disputeOpen: String,
    val disputeClosed: String,
    val disputeAmount: String,
    val footer: String,
    val equipmentNames: Map<EquipmentType, String> = emptyMap(),
)

data class PeriodReportPeriodTemplates(
    val weekRange: String,
    val monthRange: String,
    val yearRange: String,
    val disputes: String,
    val allLoads: String,
)

class PeriodReportNumbers(
    val money: (Money) -> String,
    val miles: (Double) -> String,
    val rpm: (Double) -> String,
    val volume: (Double) -> String,
    val date: (LocalDate) -> String,
)

internal const val PERIOD_REPORT_EMPTY_VALUE = "\u2014"
internal const val PERIOD_REPORT_LOAD_DIVIDER = "----------"
