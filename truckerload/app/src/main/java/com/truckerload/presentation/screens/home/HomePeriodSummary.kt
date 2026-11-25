package com.truckerload.presentation.screens.home

import android.app.Application
import com.truckerload.R
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.filter.LoadFilterUseCase
import java.util.Calendar

/** Builds the period totals header shown above the Home filter. */
internal fun buildPeriodSummaryHeader(
    state: HomeUiState,
    totals: LoadFilterUseCase.Totals,
    app: Application,
): HomeListItem.FilteredSectionHeader? {
        val label = when (state.filter) {
            LoadFilter.CALENDAR_DATE -> if (state.selectedDateLabel.isNotBlank()) {
                formatFilterLabel(app, state.selectedDateLabel, totals.loadCount)
            } else {
                null
            }
            LoadFilter.CALENDAR_WEEK -> if (state.selectedWeekLabel.isNotBlank()) {
                formatFilterLabel(app, state.selectedWeekLabel, totals.loadCount)
            } else {
                null
            }
            LoadFilter.YESTERDAY -> formatFilterLabel(app, app.getString(R.string.home_filter_yesterday), totals.loadCount)
            LoadFilter.THIS_WEEK -> formatFilterLabel(app, app.getString(R.string.home_filter_this_week), totals.loadCount)
            LoadFilter.LAST_WEEK -> formatFilterLabel(app, app.getString(R.string.home_filter_last_week), totals.loadCount)
            LoadFilter.THIS_MONTH -> {
                val monthLabel = state.selectedMonthLabel.ifBlank {
                    app.getString(R.string.home_filter_month)
                }
                formatFilterLabel(app, monthLabel, totals.loadCount)
            }
            LoadFilter.DISPUTE -> formatFilterLabel(app, app.getString(R.string.home_filter_dispute), totals.loadCount)
            LoadFilter.ALL -> if (state.selectedYear != null) {
                app.getString(
                    R.string.home_year_selected_header,
                    state.selectedYear ?: Calendar.getInstance().get(Calendar.YEAR),
                    totals.loadCount,
                    loadWord(app, totals.loadCount),
                )
            } else {
                null
            }
        }
        return label?.let { HomeListItem.FilteredSectionHeader(it, totals) }
}
