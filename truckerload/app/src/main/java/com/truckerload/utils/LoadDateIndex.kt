package com.truckerload.utils

import com.truckerload.domain.model.Load
import java.util.Calendar

/**
 * Индекс грузов по датам. O(1) поиск вместо O(n).
 * Ключ: YYYY-MM-DD, значение: грузы, активные в эту дату.
 * Пересчитывается только при изменении массива loads.
 */
object LoadDateIndex {

    /**
     * Строит Map: дата -> список грузов.
     * Multi-day loads индексируются по каждой дате в диапазоне (load_date + stops).
     * Используется для фильтрации / поиска по дню поездки.
     */
    fun build(loads: List<Load>): Map<String, List<Load>> {
        val index = mutableMapOf<String, MutableList<Load>>()
        for (load in loads) {
            val dates = getLoadDateRange(load)
            for (date in dates) {
                index.getOrPut(date) { mutableListOf() }.add(load)
            }
        }
        return index
    }

    /**
     * Даты для зелёных точек календаря: только точная дата груза (`load.date`),
     * без заполнения всех дней между PU и DEL (из‑за этого точки «размазывались»
     * по будущим месяцам). Даты строго после сегодня не показываем — журнал
     * отмечает уже состоявшиеся грузы.
     */
    fun calendarMarkerDates(
        loads: List<Load>,
        today: String = todayIsoDate(),
    ): Set<String> {
        val markers = LinkedHashSet<String>()
        for (load in loads) {
            val date = canonicalDateString(load.date) ?: continue
            if (date <= today) {
                markers.add(date)
            }
        }
        return markers
    }

    private fun todayIsoDate(): String {
        val cal = Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }
}
