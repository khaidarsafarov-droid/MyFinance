package com.truckerload.domain.week

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide reporting-week starts. Hydrated from DataStore on launch / login
 * and updated immediately when the driver changes Settings.
 *
 * [loads] drives Home, weekly goal, widgets, analytics, paychecks, Telegram load ingest.
 * [diesel] drives Add Diesel, diesel journal, fuel analytics, Telegram diesel ingest.
 */
object WeekStartRuntime {
    @Volatile
    var loads: WeekStartDay = WeekStartDay.DEFAULT
        private set

    @Volatile
    var diesel: WeekStartDay = WeekStartDay.DEFAULT
        private set

    private val revisionState = MutableStateFlow(0)
    val revision: StateFlow<Int> = revisionState.asStateFlow()

    fun install(loads: WeekStartDay, diesel: WeekStartDay) {
        val changed = this.loads != loads || this.diesel != diesel
        this.loads = loads
        this.diesel = diesel
        if (changed) bump()
    }

    fun installLoads(value: WeekStartDay) {
        if (loads == value) return
        loads = value
        bump()
    }

    fun installDiesel(value: WeekStartDay) {
        if (diesel == value) return
        diesel = value
        bump()
    }

    private fun bump() {
        revisionState.value = revisionState.value + 1
    }
}
