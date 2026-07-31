package com.truckerload.presentation.screens.home

import android.app.Application
import com.truckerload.R
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.model.Load
import java.text.DateFormatSymbols
import java.util.Locale

fun dedupeByTripId(loads: List<Load>): List<Load> {
    if (loads.isEmpty()) return loads
    val seen = LinkedHashSet<String>()
    return loads.filter { load ->
        val key = load.tripId.ifBlank { load.id }
        seen.add(key)
    }
}

fun monthName(month: Int): String {
    val long = DateFormatSymbols(Locale.getDefault())
        .months
        .getOrNull((month - 1).coerceIn(0, 11))
        .orEmpty()
    return long.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

fun groupedLoadsByYearMonth(loads: List<Load>): List<YearSection> {
    if (loads.isEmpty()) return emptyList()
    val unique = dedupeByTripId(loads)
    val byYear = unique.groupBy { load ->
        if (load.date.length >= 4) load.date.substring(0, 4).toIntOrNull() ?: 0 else 0
    }.filterKeys { it > 0 }

    return byYear.keys.sortedDescending().map { year ->
        val yearLoads = byYear[year] ?: emptyList()
        val byMonth = yearLoads.groupBy { load ->
            if (load.date.length >= 7) load.date.substring(5, 7).toIntOrNull() ?: 0 else 0
        }.filterKeys { it in 1..12 }

        val monthSections = byMonth.keys.sortedDescending().map { month ->
            val monthLoads = (byMonth[month] ?: emptyList())
                .sortedWith(compareByDescending<Load> { it.date }.thenByDescending { it.parsedAt })
            MonthSection(year, month, monthName(month), monthLoads)
        }
        YearSection(
            year,
            yearLoads.size,
            yearLoads.sumOf { it.totalRate },
            yearLoads.sumOf { it.totalMiles },
            monthSections,
        )
    }
}

fun buildYearMonthSections(loads: List<Load>): List<YearSection> {
    if (loads.isEmpty()) return emptyList()
    return groupedLoadsByYearMonth(loads)
}

fun loadWord(app: Application, n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> app.getString(R.string.home_load_word_one)
    n % 10 in 2..4 && (n % 100 < 10 || n % 100 >= 20) -> app.getString(R.string.home_load_word_few)
    else -> app.getString(R.string.home_load_word_many)
}

fun formatFilterLabel(app: Application, baseLabel: String, count: Int): String =
    app.getString(R.string.home_filter_label_with_count, baseLabel, count, loadWord(app, count))

fun formatDateLabel(date: String): String {
    if (date.length < 10) return date
    val parts = date.split("-")
    if (parts.size != 3) return date
    val (y, m, d) = parts
    val mi = m.toIntOrNull() ?: return date
    val short = DateFormatSymbols(Locale.getDefault())
        .shortMonths
        .getOrNull((mi - 1).coerceIn(0, 11))
        .orEmpty()
        .replace(".", "")
        .lowercase(Locale.getDefault())
    return "$d $short $y"
}

/** Плоский список для LazyColumn. */
fun flattenedListItems(
    filter: LoadFilter,
    selectedYear: Int?,
    filteredLoads: List<Load>,
): List<HomeListItem> {
    if (filter != LoadFilter.ALL) {
        return filteredLoads.map { HomeListItem.LoadItem(it) }
    }

    if (selectedYear != null) {
        // Period summary already shows year totals — avoid a second "Всего N • $ • mi" block.
        val sections = buildYearMonthSections(dedupeByTripId(filteredLoads))
        val result = mutableListOf<HomeListItem>()
        for (ys in sections) {
            for (ms in ys.months) {
                result.add(HomeListItem.MonthHeader(ms))
                result.addAll(ms.loads.map { HomeListItem.LoadItem(it) })
            }
        }
        return result
    }

    val sections = groupedLoadsByYearMonth(dedupeByTripId(filteredLoads))
    val result = mutableListOf<HomeListItem>()
    for (yearSection in sections) {
        result.add(HomeListItem.YearHeader(yearSection))
        for (monthSection in yearSection.months) {
            result.add(HomeListItem.MonthHeader(monthSection))
            result.addAll(monthSection.loads.map { HomeListItem.LoadItem(it) })
        }
    }
    return result
}
