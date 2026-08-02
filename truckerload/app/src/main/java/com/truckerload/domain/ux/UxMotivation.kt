package com.truckerload.domain.ux

/**
 * Loss-aversion + contrast helpers for weekly goal and period summaries.
 * Pure math — UI maps results to localized strings.
 */
enum class LossAversionKind {
    NONE,
    GOAL_BEHIND,
    GOAL_UNSET_WITH_EARNINGS,
    NO_LOADS_THIS_WEEK,
}

data class LossAversionSignal(
    val kind: LossAversionKind,
    val remainingAmount: Double = 0.0,
    val daysRemaining: Int = 0,
    val currentGross: Double = 0.0,
)

data class ContrastBreakdown(
    /** Anchor shown first (e.g. gross earnings). */
    val anchorAmount: Double,
    /** Secondary amount contrasted against the anchor (e.g. diesel or remaining). */
    val comparedAmount: Double,
) {
    val comparedPercentOfAnchor: Double
        get() = if (anchorAmount <= 0) 0.0 else (comparedAmount / anchorAmount) * 100.0
}

object UxMotivation {
    fun lossAversion(
        goalConfigured: Boolean,
        targetAmount: Double,
        currentGross: Double,
        remainingAmount: Double,
        daysRemaining: Int,
        isBehindPace: Boolean,
        loadCountThisWeek: Int,
    ): LossAversionSignal {
        if (goalConfigured && targetAmount > 0 && isBehindPace && remainingAmount > 0) {
            return LossAversionSignal(
                kind = LossAversionKind.GOAL_BEHIND,
                remainingAmount = remainingAmount,
                daysRemaining = daysRemaining,
                currentGross = currentGross,
            )
        }
        if (!goalConfigured && currentGross > 0) {
            return LossAversionSignal(
                kind = LossAversionKind.GOAL_UNSET_WITH_EARNINGS,
                currentGross = currentGross,
            )
        }
        if (loadCountThisWeek == 0) {
            return LossAversionSignal(kind = LossAversionKind.NO_LOADS_THIS_WEEK)
        }
        return LossAversionSignal(kind = LossAversionKind.NONE)
    }

    /** Suggested weekly goals — ownership / IKEA effect chips. */
    fun suggestedWeeklyGoals(recentWeekGross: Double = 0.0): List<Double> {
        val base = when {
            recentWeekGross >= 8000 -> listOf(8000.0, 10000.0, 12000.0)
            recentWeekGross >= 5000 -> listOf(5000.0, 7000.0, 9000.0)
            recentWeekGross >= 2500 -> listOf(3000.0, 4500.0, 6000.0)
            recentWeekGross > 0 -> {
                val rounded = (kotlin.math.ceil(recentWeekGross / 500.0) * 500.0)
                    .coerceAtLeast(2000.0)
                listOf(rounded, rounded + 1000.0, rounded + 2000.0)
            }
            else -> listOf(3000.0, 5000.0, 7000.0)
        }
        return base.distinct().sorted()
    }

    fun dieselContrast(gross: Double, diesel: Double): ContrastBreakdown? {
        if (gross <= 0 || diesel <= 0) return null
        return ContrastBreakdown(anchorAmount = gross, comparedAmount = diesel)
    }

    fun remainingContrast(gross: Double, remaining: Double): ContrastBreakdown? {
        if (gross < 0 || remaining <= 0) return null
        val anchor = (gross + remaining).coerceAtLeast(remaining)
        return ContrastBreakdown(anchorAmount = anchor, comparedAmount = remaining)
    }
}
