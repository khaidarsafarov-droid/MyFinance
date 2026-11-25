package com.truckerload.utils

import java.util.Calendar

/**
 * Wall clock for week chips, day dots, and the home greeting.
 *
 * Production uses the device clock. Tests may replace [millis] with a
 * fixed instant so listing shots and UI tests stay on a consistent date.
 */
object AppTime {
    @Volatile
    var millis: () -> Long = { System.currentTimeMillis() }

    fun now(): Long = millis()

    fun calendar(): Calendar = Calendar.getInstance().apply { timeInMillis = now() }

    fun reset() {
        millis = { System.currentTimeMillis() }
    }
}
