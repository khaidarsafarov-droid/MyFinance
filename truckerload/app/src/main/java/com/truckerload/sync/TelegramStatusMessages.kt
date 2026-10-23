package com.truckerload.sync

import android.content.Context
import android.util.Log
import com.truckerload.R
import com.truckerload.domain.money.sum
import com.truckerload.domain.units.DisplayUnits
import com.truckerload.domain.units.MeasurementUnits
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.utils.getCurrentWeekNumberAndYear
import kotlinx.coroutines.flow.first
import java.util.Locale

/**
 * Builds user-facing Telegram replies for status and weekly stats commands.
 */
object TelegramStatusMessages {

    suspend fun buildStatsMessage(
        context: Context,
        loadRepository: LoadRepository,
    ): String {
        val total = loadRepository.getAllLoadsOnce().size
        val (weekNumber, year) = getCurrentWeekNumberAndYear()
        val weekLoads = loadRepository.getLoadsByWeek(weekNumber, year).first()
        val weekCount = weekLoads.size
        val weekIncome = weekLoads.map { it.totalRate }.sum().cents / 100.0
        val weekMiles = weekLoads.sumOf { it.drivenMiles }
        Log.d(TAG, "/stats total=$total week=$weekCount income=$weekIncome miles=$weekMiles")
        return if (DisplayUnits.isMetric) {
            context.getString(
                R.string.sync_stats_km,
                total,
                weekCount,
                String.format(Locale.US, "%,.2f", weekIncome),
                String.format(Locale.US, "%,.0f", MeasurementUnits.displayDistance(weekMiles)),
            )
        } else {
            context.getString(
                R.string.sync_stats,
                total,
                weekCount,
                String.format(Locale.US, "%,.2f", weekIncome),
                String.format(Locale.US, "%,.0f", weekMiles),
            )
        }
    }

    suspend fun buildStatusMessage(
        context: Context,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
    ): String {
        val loads = loadRepository.getAllLoadsOnce().size
        val paychecks = paycheckRepository.getAllPaychecksOnce().size
        val diesel = dieselRepository.getAllDieselOnce().size
        return context.getString(R.string.sync_status, loads, paychecks, diesel)
    }

    private const val TAG = "BackupRestore"
}
