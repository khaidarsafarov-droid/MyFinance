package com.example.myfinance.data

sealed class FeedItem {
    data class WeeklyItem(val weeklyTotal: WeeklyTotal) : FeedItem()
    data class CompanyChangeItem(val change: CompanyChange) : FeedItem()
}

fun buildFeed(weeklyTotals: List<WeeklyTotal>, companyChanges: List<CompanyChange>): List<FeedItem> {
    val items = weeklyTotals.map { FeedItem.WeeklyItem(it) } +
        companyChanges.map { FeedItem.CompanyChangeItem(it) }
    return items.sortedByDescending {
        when (it) {
            is FeedItem.WeeklyItem -> it.weeklyTotal.date
            is FeedItem.CompanyChangeItem -> it.change.date
        }
    }
}
