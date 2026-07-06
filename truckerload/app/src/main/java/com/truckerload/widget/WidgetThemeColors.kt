package com.truckerload.widget

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.truckerload.R

/** Widget bitmap/ring colors — uses widget_colors.xml (works outside Activity theme). */
object WidgetThemeColors {

    @ColorInt
    fun primary(context: Context): Int =
        ContextCompat.getColor(context, R.color.widget_primary)

    @ColorInt
    fun onPrimary(context: Context): Int =
        ContextCompat.getColor(context, R.color.widget_text_primary)

    @ColorInt
    fun primaryContainer(context: Context): Int =
        ContextCompat.getColor(context, R.color.widget_secondary)

    @ColorInt
    fun surface(context: Context): Int =
        ContextCompat.getColor(context, R.color.widget_bg)

    @ColorInt
    fun onSurface(context: Context): Int =
        ContextCompat.getColor(context, R.color.widget_text_primary)

    @ColorInt
    fun onSurfaceVariant(context: Context): Int =
        ContextCompat.getColor(context, R.color.widget_text_secondary)

    @ColorInt
    fun surfaceVariant(context: Context): Int =
        ContextCompat.getColor(context, R.color.widget_progress_track)

    @ColorInt
    fun tertiary(context: Context): Int =
        ContextCompat.getColor(context, R.color.widget_green)

    @ColorInt
    fun error(context: Context): Int =
        ContextCompat.getColor(context, R.color.widget_remaining_warning)
}
