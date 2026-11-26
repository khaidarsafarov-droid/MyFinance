package com.truckerload.presentation.screens.home

import android.app.Application
import com.truckerload.R
import com.truckerload.domain.model.EquipmentType
import com.truckerload.domain.report.PeriodReportLabels
import com.truckerload.domain.report.PeriodReportNumbers
import com.truckerload.domain.report.PeriodReportPdfLabels
import com.truckerload.domain.report.PeriodReportPeriodTemplates
import com.truckerload.domain.units.DisplayUnits
import com.truckerload.domain.units.MeasurementUnits
import com.truckerload.presentation.screens.add.labelRes
import com.truckerload.presentation.utils.MoneyFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal fun periodReportLabels(app: Application): PeriodReportLabels = PeriodReportLabels(
    appName = app.getString(R.string.home_brand_title),
    title = app.getString(R.string.period_report_title),
    driver = app.getString(R.string.period_report_driver),
    period = app.getString(R.string.period_report_period),
    generated = app.getString(R.string.period_report_generated),
    summary = app.getString(R.string.period_report_summary),
    gross = app.getString(R.string.period_report_gross),
    loads = app.getString(R.string.period_report_loads),
    loadedMiles = app.getString(R.string.period_report_loaded_miles),
    emptyMiles = app.getString(R.string.period_report_empty_miles),
    totalMiles = app.getString(R.string.period_report_total_miles),
    avgRate = app.getString(R.string.period_report_avg_rate),
    fuel = app.getString(R.string.period_report_fuel),
    fuelVolume = app.getString(R.string.period_report_fuel_volume),
    expenses = app.getString(R.string.period_report_expenses),
    penalties = app.getString(R.string.period_report_penalties),
    net = app.getString(R.string.period_report_net),
    goal = app.getString(R.string.period_report_goal),
    goalJoin = app.getString(R.string.period_report_goal_join),
    loadsSection = app.getString(R.string.period_report_loads_section),
    emptyLoads = app.getString(R.string.period_report_empty),
    pickup = app.getString(R.string.period_report_pickup),
    delivery = app.getString(R.string.period_report_delivery),
    reference = app.getString(R.string.period_report_reference),
    miles = app.getString(R.string.period_report_miles),
    milesSplit = app.getString(R.string.period_report_miles_split),
    rate = app.getString(R.string.period_report_rate),
    ratePerMile = app.getString(R.string.period_report_rpm),
    equipment = app.getString(R.string.period_report_equipment),
    status = app.getString(R.string.period_report_status),
    notes = app.getString(R.string.period_report_notes),
    disputeOpen = app.getString(R.string.period_report_status_open),
    disputeClosed = app.getString(R.string.period_report_status_closed),
    disputeAmount = app.getString(R.string.period_report_dispute_amount),
    footer = app.getString(R.string.period_report_footer),
    cancelledTonu = app.getString(R.string.period_report_status_tonu),
    equipmentNames = EquipmentType.entries.associateWith { app.getString(it.labelRes()) },
    pdf = PeriodReportPdfLabels(
        title = app.getString(R.string.period_report_pdf_title),
        period = app.getString(R.string.period_report_period),
        reportDate = app.getString(R.string.period_report_pdf_date),
        loadNumber = app.getString(R.string.period_report_col_load),
        origin = app.getString(R.string.period_report_col_origin),
        destination = app.getString(R.string.period_report_col_destination),
        shipDate = app.getString(R.string.period_report_col_ship),
        deliveryDate = app.getString(R.string.period_report_col_delivery),
        miles = app.getString(R.string.period_report_col_miles),
        gross = app.getString(R.string.period_report_col_gross),
        ratePerMile = app.getString(R.string.period_report_col_rpm),
        total = app.getString(R.string.period_report_col_total),
        summary = app.getString(R.string.period_report_pdf_summary),
        loadsTotal = app.getString(R.string.period_report_loads_total),
        grossTotal = app.getString(R.string.period_report_gross_total),
        milesTotal = app.getString(R.string.period_report_miles_total),
        avgRate = app.getString(R.string.period_report_avg_rate),
    ),
)

internal fun periodReportTemplates(app: Application): PeriodReportPeriodTemplates = PeriodReportPeriodTemplates(
    weekRange = app.getString(R.string.period_report_week_range),
    monthRange = app.getString(R.string.period_report_month_range),
    yearRange = app.getString(R.string.period_report_year),
    disputes = app.getString(R.string.period_report_disputes),
    allLoads = app.getString(R.string.period_report_all),
)

internal fun periodReportNumbers(locale: Locale): PeriodReportNumbers = PeriodReportNumbers(
    money = { MoneyFormat.formatCurrency(it) },
    miles = { MoneyFormat.formatMiles(it) },
    rpm = { MoneyFormat.formatRpm(it) },
    volume = { gallons ->
        if (DisplayUnits.isMetric) {
            String.format(Locale.US, "%.0f L", MeasurementUnits.displayVolume(gallons))
        } else {
            String.format(Locale.US, "%.0f gal", gallons)
        }
    },
    date = { day ->
        day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    },
)
