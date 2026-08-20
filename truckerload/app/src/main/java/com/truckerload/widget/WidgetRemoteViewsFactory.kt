package com.truckerload.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.truckerload.R
import com.truckerload.presentation.MainActivity
import com.truckerload.utils.AppLocale

enum class WidgetKind { SQUARE, WIDE }

object WidgetRemoteViewsFactory {

  private const val TAG = "TruckLogWidget"
  private const val THRESHOLD_COMPACT_HEIGHT_DP = 90
  const val THRESHOLD_EXPANDED_WIDTH_DP = 180
  const val THRESHOLD_WIDE_HEIGHT_DP = 180

  enum class LayoutTier { COMPACT, STANDARD, EXPANDED }

  private val compactDayIds = intArrayOf(
    R.id.widget_day_0,
    R.id.widget_day_1,
    R.id.widget_day_2,
    R.id.widget_day_3,
    R.id.widget_day_4,
    R.id.widget_day_5,
    R.id.widget_day_6,
  )

  fun build(
    context: Context,
    appWidgetId: Int,
    stats: WidgetStats,
    kind: WidgetKind = WidgetKind.SQUARE,
  ): RemoteViews {
    val appContext = AppLocale.wrap(context.applicationContext)
    val prefs = WidgetPrefsStore.load(appContext, appWidgetId)
    val tier = resolveTier(appContext, appWidgetId, prefs, kind)
    val layoutRes = layoutResFor(tier)
    val views = RemoteViews(appContext.packageName, layoutRes)
    val dark = isDarkTheme(appContext, prefs.themeMode)
    val themed = themedContext(appContext, dark)

    applyTheme(themed, views)
    bindHeader(appContext, views, stats, tier)
    bindRingHero(appContext, views, stats, prefs, tier, themed)
    bindWeekDays(appContext, views, appWidgetId, stats, tier, themed)
    bindRpmBlock(appContext, views, stats, tier, themed)
    bindEmptyState(appContext, views, stats, prefs, tier)
    bindQuickActions(appContext, views, tier)
    bindClickTargets(appContext, views, appWidgetId, tier)
    return views
  }

  private fun layoutResFor(tier: LayoutTier): Int = when (tier) {
    LayoutTier.COMPACT -> R.layout.widget_compact
    LayoutTier.STANDARD -> R.layout.widget_standard
    LayoutTier.EXPANDED -> R.layout.widget_expanded
  }

  private fun resolveTier(
    context: Context,
    appWidgetId: Int,
    prefs: WidgetPrefs,
    kind: WidgetKind,
  ): LayoutTier {
    return when (prefs.sizeMode) {
      WidgetSizeMode.SMALL -> LayoutTier.COMPACT
      WidgetSizeMode.MEDIUM -> LayoutTier.STANDARD
      WidgetSizeMode.LARGE -> LayoutTier.EXPANDED
      WidgetSizeMode.AUTO -> resolveTierFromOptions(context, appWidgetId, kind)
    }
  }

  private fun resolveTierFromOptions(
    context: Context,
    appWidgetId: Int,
    kind: WidgetKind,
  ): LayoutTier {
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      return if (kind == WidgetKind.WIDE) LayoutTier.STANDARD else LayoutTier.COMPACT
    }
    val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
    return resolveTierFromOptions(options, kind)
  }

  fun resolveTierFromOptions(options: Bundle): LayoutTier =
    resolveTierFromOptions(options, WidgetKind.SQUARE)

  fun resolveTierFromOptions(options: Bundle, kind: WidgetKind): LayoutTier {
    val widthDp = options.getInt(
      AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
      options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110),
    )
    val heightDp = options.getInt(
      AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
      options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110),
    )
    return resolveTierFromSize(widthDp, heightDp, kind)
  }

  fun resolveTierFromSize(
    widthDp: Int,
    heightDp: Int,
    kind: WidgetKind = WidgetKind.SQUARE,
  ): LayoutTier {
    return when (kind) {
      WidgetKind.SQUARE -> when {
        heightDp < THRESHOLD_COMPACT_HEIGHT_DP || widthDp < THRESHOLD_EXPANDED_WIDTH_DP ->
          LayoutTier.COMPACT
        heightDp >= THRESHOLD_WIDE_HEIGHT_DP -> LayoutTier.EXPANDED
        else -> LayoutTier.STANDARD
      }
      WidgetKind.WIDE -> when {
        heightDp >= THRESHOLD_WIDE_HEIGHT_DP -> LayoutTier.EXPANDED
        else -> LayoutTier.STANDARD
      }
    }
  }

  private fun isDarkTheme(context: Context, mode: WidgetThemeMode): Boolean = when (mode) {
    WidgetThemeMode.DARK -> true
    WidgetThemeMode.LIGHT -> false
    WidgetThemeMode.SYSTEM -> {
      val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
      night == Configuration.UI_MODE_NIGHT_YES
    }
  }

  private fun themedContext(context: Context, dark: Boolean): Context {
    val config = Configuration(context.resources.configuration)
    config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
      if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    return context.createConfigurationContext(config)
  }

  private fun applyTheme(context: Context, views: RemoteViews) {
    views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_oneui_plate)

    val primary = ContextCompat.getColor(context, R.color.widget_text_primary)
    val secondary = ContextCompat.getColor(context, R.color.widget_text_secondary)
    val hint = ContextCompat.getColor(context, R.color.widget_text_hint)
    val accent = ContextCompat.getColor(context, R.color.widget_accent)

    views.setTextColor(R.id.widget_header_title, primary)
    views.setTextColor(R.id.widget_subtitle, secondary)
    views.setTextColor(R.id.widget_week, secondary)
    views.setTextColor(R.id.widget_gross_hero, primary)
    views.setTextColor(R.id.widget_goal_subtitle, secondary)
    views.setTextColor(R.id.widget_ring_percent, hint)
    views.setTextColor(R.id.widget_rpm_label, secondary)
    views.setTextColor(R.id.widget_rpm_value, primary)
    views.setTextColor(R.id.widget_empty_message, secondary)
    views.setTextColor(R.id.widget_empty_add_btn, accent)
    views.setTextColor(R.id.widget_btn_camera_label, secondary)
    views.setTextColor(R.id.widget_btn_scanner_label, secondary)
  }

  private fun bindHeader(
    context: Context,
    views: RemoteViews,
    stats: WidgetStats,
    tier: LayoutTier,
  ) {
    views.setTextViewText(
      R.id.widget_header_title,
      context.getString(R.string.widget_brand_title_plain).uppercase(),
    )

    when (tier) {
      LayoutTier.COMPACT -> {
        views.setViewVisibility(R.id.widget_subtitle, View.GONE)
        views.setViewVisibility(R.id.widget_week, View.GONE)
      }
      LayoutTier.STANDARD -> {
        views.setViewVisibility(R.id.widget_subtitle, View.GONE)
        views.setViewVisibility(R.id.widget_truck_icon, View.GONE)
        if (stats.weekLabel.isNotBlank()) {
          views.setViewVisibility(R.id.widget_week, View.VISIBLE)
          views.setTextViewText(R.id.widget_week, stats.weekLabel)
        } else {
          views.setViewVisibility(R.id.widget_week, View.GONE)
        }
      }
      LayoutTier.EXPANDED -> {
        views.setViewVisibility(R.id.widget_subtitle, View.GONE)
        views.setViewVisibility(R.id.widget_truck_icon, View.GONE)
        if (stats.weekLabel.isNotBlank()) {
          views.setViewVisibility(R.id.widget_week, View.VISIBLE)
          views.setTextViewText(R.id.widget_week, stats.weekLabel)
        } else {
          views.setViewVisibility(R.id.widget_week, View.GONE)
        }
      }
    }
  }

  private fun bindRingHero(
    context: Context,
    views: RemoteViews,
    stats: WidgetStats,
    prefs: WidgetPrefs,
    tier: LayoutTier,
    themed: Context,
  ) {
    val progress = stats.goalProgressPercent.coerceIn(0f, 100f)
    val goalSet = stats.weeklyProfitGoal > 0
    val goalMet = goalSet && stats.totalLoadRate >= stats.weeklyProfitGoal

    val ringSizePx = WidgetProgressRingBitmap.ringSizePx(
      context,
      compact = tier == LayoutTier.COMPACT,
      expanded = tier == LayoutTier.EXPANDED,
      splitLayout = tier == LayoutTier.STANDARD,
    )
    val progressColor = WidgetProgressRingBitmap.progressColorForStatus(
      themed,
      stats.goalPaceStatus,
      goalMet,
    )
    val ringBitmap = runCatching {
      WidgetProgressRingBitmap.create(
        themed,
        if (goalSet) progress else 0f,
        ringSizePx,
        progressColor,
      )
    }.getOrElse { error ->
      Log.e(TAG, "Progress ring bitmap failed", error)
      null
    }
    if (ringBitmap != null) {
      views.setImageViewBitmap(R.id.widget_progress_ring, ringBitmap)
    } else {
      views.setViewVisibility(R.id.widget_progress_ring, View.GONE)
    }

    if (tier == LayoutTier.COMPACT) {
      views.setTextViewText(
        R.id.widget_gross_hero,
        WidgetStatsFormatter.formatGrossUsd(stats.totalLoadRate),
      )
      views.setViewVisibility(R.id.widget_goal_subtitle, View.GONE)
    } else {
      views.setTextViewText(
        R.id.widget_gross_hero,
        WidgetStatsFormatter.formatGrossUsd(stats.totalLoadRate),
      )
      val goalSubtitle = when {
        !prefs.showGoal || !goalSet ->
          context.getString(R.string.widget_goal_not_set)
        else -> context.getString(
          R.string.widget_goal_out_of,
          WidgetStatsFormatter.formatGrossUsd(stats.weeklyProfitGoal),
        )
      }
      views.setViewVisibility(R.id.widget_goal_subtitle, View.VISIBLE)
      views.setTextViewText(R.id.widget_goal_subtitle, goalSubtitle)
    }

    views.setTextViewText(
      R.id.widget_ring_percent,
      if (goalSet) {
        context.getString(R.string.widget_ring_percent_decimal, progress.toDouble())
      } else {
        WidgetStatsFormatter.formatProgressPercent(0f)
      },
    )
  }

  private fun bindWeekDays(
    context: Context,
    views: RemoteViews,
    appWidgetId: Int,
    stats: WidgetStats,
    tier: LayoutTier,
    themed: Context,
  ) {
    val chips = WidgetWeekDayHelper.chips(stats.weekLoadMask)

    when (tier) {
      LayoutTier.COMPACT -> {
        views.setViewVisibility(R.id.widget_day_dots_row, View.VISIBLE)
        compactDayIds.forEachIndexed { index, viewId ->
          val chip = chips.getOrElse(index) {
            WidgetWeekDayHelper.DayChip(
              label = WidgetWeekDayHelper.dayLabels.getOrElse(index) { "?" },
              date = java.time.LocalDate.now(),
              hasLoad = false,
              isToday = false,
              isFuture = true,
            )
          }
          views.setImageViewResource(viewId, dayDrawableFor(chip))
        }
      }
      LayoutTier.STANDARD, LayoutTier.EXPANDED -> {
        views.setViewVisibility(R.id.widget_right_column, View.VISIBLE)
        views.setViewVisibility(R.id.widget_week_days, View.VISIBLE)
        val paddingDp = when (tier) {
          LayoutTier.EXPANDED -> 20
          else -> 16
        }
        val ringWidthDp = when (tier) {
          LayoutTier.EXPANDED -> 160
          else -> 88
        }
        val gapDp = when (tier) {
          LayoutTier.EXPANDED -> 12
          else -> 8
        }
        val width = WidgetWeekDaysBitmap.columnWidthPx(
          context,
          appWidgetId,
          paddingDp,
          ringWidthDp,
          gapDp,
        )
        val height = when (tier) {
          LayoutTier.EXPANDED -> WidgetWeekDaysBitmap.rowHeightPx(context, compact = false)
          else -> WidgetWeekDaysBitmap.rowHeightPx(context, compact = true)
        }
        val bitmap = runCatching {
          WidgetWeekDaysBitmap.create(themed, chips, width, height)
        }.getOrElse { error ->
          Log.e(TAG, "Week days bitmap failed", error)
          null
        }
        if (bitmap != null) {
          views.setImageViewBitmap(R.id.widget_week_days, bitmap)
        } else {
          views.setViewVisibility(R.id.widget_week_days, View.GONE)
        }
      }
    }
  }

  private fun bindRpmBlock(
    context: Context,
    views: RemoteViews,
    stats: WidgetStats,
    tier: LayoutTier,
    themed: Context,
  ) {
    if (tier == LayoutTier.COMPACT) return

    views.setViewVisibility(R.id.widget_rpm_divider, View.GONE)
    views.setViewVisibility(R.id.widget_rpm_label, View.VISIBLE)
    views.setViewVisibility(R.id.widget_rpm_value, View.VISIBLE)

    views.setTextViewText(R.id.widget_rpm_label, context.getString(R.string.widget_rpm_label))

    val rpm = WidgetDataProvider.weeklyRpm(stats)
    views.setTextViewText(
      R.id.widget_rpm_value,
      WidgetStatsFormatter.formatRpmPerMile(context, rpm),
    )

    val rpmColor = if (stats.totalMiles > 0 && rpm >= stats.cpmTarget) {
      ContextCompat.getColor(themed, R.color.widget_rpm_good)
    } else {
      ContextCompat.getColor(themed, R.color.widget_text_primary)
    }
    views.setTextColor(R.id.widget_rpm_value, rpmColor)
  }

  private fun dayDrawableFor(chip: WidgetWeekDayHelper.DayChip): Int = when {
    chip.hasLoad && chip.isToday -> R.drawable.widget_day_has_load_today
    chip.hasLoad -> R.drawable.widget_day_has_load
    chip.isToday -> R.drawable.widget_day_today
    chip.isPast -> R.drawable.widget_day_past
    else -> R.drawable.widget_day_future
  }

  private fun bindEmptyState(
    context: Context,
    views: RemoteViews,
    stats: WidgetStats,
    prefs: WidgetPrefs,
    tier: LayoutTier,
  ) {
    if (stats.hasWeekLoads() || stats.weeklyProfitGoal > 0) {
      views.setViewVisibility(R.id.widget_empty_section, View.GONE)
      views.setViewVisibility(R.id.widget_content_section, View.VISIBLE)
      if (tier == LayoutTier.COMPACT) {
        views.setViewVisibility(R.id.widget_day_dots_row, View.VISIBLE)
      }
      if (tier != LayoutTier.COMPACT) {
        views.setViewVisibility(R.id.widget_right_column, View.VISIBLE)
        views.setViewVisibility(R.id.widget_rpm_divider, View.GONE)
        views.setViewVisibility(R.id.widget_rpm_label, View.VISIBLE)
        views.setViewVisibility(R.id.widget_rpm_value, View.VISIBLE)
        views.setViewVisibility(R.id.widget_quick_actions, View.VISIBLE)
      }
      return
    }

    views.setViewVisibility(R.id.widget_empty_section, View.VISIBLE)
    views.setViewVisibility(R.id.widget_content_section, View.GONE)
    if (tier != LayoutTier.COMPACT) {
      views.setViewVisibility(R.id.widget_right_column, View.GONE)
      views.setViewVisibility(R.id.widget_rpm_divider, View.GONE)
      views.setViewVisibility(R.id.widget_rpm_label, View.GONE)
      views.setViewVisibility(R.id.widget_rpm_value, View.GONE)
      views.setViewVisibility(R.id.widget_quick_actions, View.GONE)
    }
    if (tier == LayoutTier.COMPACT) {
      views.setViewVisibility(R.id.widget_day_dots_row, View.GONE)
      views.setViewVisibility(R.id.widget_hero_section, View.GONE)
    }
    views.setTextViewText(R.id.widget_empty_message, context.getString(R.string.widget_empty_no_loads))
    views.setTextViewText(R.id.widget_empty_add_btn, context.getString(R.string.widget_empty_add_short))
  }

  private fun bindQuickActions(
    context: Context,
    views: RemoteViews,
    tier: LayoutTier,
  ) {
    if (tier == LayoutTier.COMPACT) {
      views.setViewVisibility(R.id.widget_quick_actions, View.VISIBLE)
      views.setViewVisibility(R.id.widget_btn_camera_label, View.GONE)
      views.setViewVisibility(R.id.widget_btn_scanner_label, View.GONE)
      return
    }
    views.setViewVisibility(R.id.widget_quick_actions, View.VISIBLE)
    views.setViewVisibility(R.id.widget_btn_camera_label, View.VISIBLE)
    views.setViewVisibility(R.id.widget_btn_scanner_label, View.VISIBLE)
    views.setTextViewText(R.id.widget_btn_camera_label, context.getString(R.string.widget_camera_short))
    views.setTextViewText(R.id.widget_btn_scanner_label, context.getString(R.string.widget_scanner_short))
  }

  private fun bindClickTargets(
    context: Context,
    views: RemoteViews,
    appWidgetId: Int,
    tier: LayoutTier,
  ) {
    views.setOnClickPendingIntent(
      R.id.widget_root,
      activityPendingIntent(context, appWidgetId, WidgetDeepLink.ROUTE_HOME, 10),
    )
    views.setOnClickPendingIntent(
      R.id.widget_header,
      activityPendingIntent(context, appWidgetId, WidgetDeepLink.ROUTE_HOME, 11),
    )
    views.setOnClickPendingIntent(
      R.id.widget_hero_section,
      activityPendingIntent(context, appWidgetId, WidgetDeepLink.ROUTE_WEEKLY_GOAL, 13),
    )
    views.setOnClickPendingIntent(
      R.id.widget_progress_ring,
      activityPendingIntent(context, appWidgetId, WidgetDeepLink.ROUTE_WEEKLY_GOAL, 14),
    )
    views.setOnClickPendingIntent(
      R.id.widget_gross_hero,
      activityPendingIntent(context, appWidgetId, WidgetDeepLink.ROUTE_WEEKLY_GOAL, 15),
    )
    if (tier != LayoutTier.COMPACT) {
      views.setOnClickPendingIntent(
        R.id.widget_week_days,
        activityPendingIntent(context, appWidgetId, WidgetDeepLink.ROUTE_WEEKLY_GOAL, 19),
      )
      views.setOnClickPendingIntent(
        R.id.widget_rpm_value,
        activityPendingIntent(context, appWidgetId, WidgetDeepLink.ROUTE_STATS, 21),
      )
    }
    views.setOnClickPendingIntent(
      R.id.widget_empty_add_btn,
      activityPendingIntent(context, appWidgetId, WidgetDeepLink.ROUTE_ADD_LOAD, 17),
    )
    views.setOnClickPendingIntent(
      R.id.widget_btn_camera,
      activityPendingIntent(context, appWidgetId, WidgetDeepLink.ROUTE_ATTACH_CAMERA, 22),
    )
    views.setOnClickPendingIntent(
      R.id.widget_btn_scanner,
      activityPendingIntent(context, appWidgetId, WidgetDeepLink.ROUTE_ATTACH_SCANNER, 23),
    )
  }

  private fun activityPendingIntent(
    context: Context,
    appWidgetId: Int,
    route: String,
    requestCode: Int,
  ): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
      putExtra(MainActivity.EXTRA_ROUTE, route)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    return PendingIntent.getActivity(
      context,
      appWidgetId * 100 + requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}
