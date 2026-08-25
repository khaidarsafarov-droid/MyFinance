package com.truckerload.utils

import android.content.Context
import com.truckerload.data.repository.AnalyticsDashboard
import com.truckerload.domain.model.analytics.AnalyticsPeriod
import com.truckerload.utils.BrandConstants.DOWNLOADS_FOLDER
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object AnalyticsExporter {

    /** Pure CSV body — safe for empty dashboards (zero loads, empty weeks/routes). */
    fun buildCsvContent(dashboard: AnalyticsDashboard, period: AnalyticsPeriod): String = buildString {
        appendLine("${BrandConstants.DISPLAY_NAME} Analytics,${period.name}")
        appendLine()
        appendLine("Summary")
        val s = dashboard.summary
        appendLine("Total loads,${s.totalLoads}")
        appendLine("Total gross,${s.totalGross}")
        appendLine("Total miles,${s.totalMiles}")
        appendLine("Avg RPM,${"%.2f".format(Locale.US, s.avgRpm)}")
        appendLine("Avg per load,${"%.2f".format(Locale.US, s.avgGrossPerLoad)}")
        s.bestWeek?.let {
            appendLine("Best week,W${it.weekNumber} ${it.year},${it.gross}")
        }
        appendLine()
        appendLine("Finances")
        val f = dashboard.finance
        appendLine("Paycheck,${f.paycheckTotal}")
        appendLine("Diesel,${f.dieselTotal}")
        appendLine("Net profit,${f.netProfit}")
        appendLine("Diesel gallons,${f.dieselGallons}")
        appendLine("Diesel discount saved,${f.dieselSavings}")
        appendLine()
        appendLine("Weekly revenue")
        appendLine("Week,Year,Gross,Miles,Loads")
        dashboard.weeks.forEach { w ->
            appendLine("W${w.weekNumber},${w.year},${w.gross},${w.miles},${w.loadCount}")
        }
        appendLine()
        appendLine("Top routes")
        appendLine("Route,Gross,Miles,RPM,Loads")
        dashboard.routes.forEach { r ->
            appendLine("\"${r.route}\",${r.gross},${r.miles},${"%.2f".format(Locale.US, r.rpm)},${r.loadCount}")
        }
        appendLine()
        appendLine("Daily distribution")
        appendLine("Day,Gross,Loads")
        dashboard.daily.forEach { d ->
            appendLine("${d.dayLabel},${d.gross},${d.loadCount}")
        }
    }

    fun exportToCsv(context: Context, dashboard: AnalyticsDashboard, period: AnalyticsPeriod): Result<File> {
        return try {
            val dir = File(
                android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                ),
                DOWNLOADS_FOLDER
            )
            if (!dir.exists() && !dir.mkdirs()) {
                return Result.failure(IllegalStateException("Cannot create export directory"))
            }
            val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
            val file = File(dir, "analytics_${period.name.lowercase(Locale.US)}_$stamp.csv")
            file.writeText(buildCsvContent(dashboard, period), Charsets.UTF_8)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
