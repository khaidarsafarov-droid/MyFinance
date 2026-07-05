package com.truckerload.widget

import android.content.Context
import android.os.Build
import androidx.annotation.ColorInt
import com.google.android.material.color.MaterialColors

/** Resolves Material You / system theme colors for App Widgets. */
object WidgetThemeColors {

    fun themedContext(context: Context): Context = context.applicationContext

    @ColorInt
    fun primary(context: Context): Int =
        resolve(context, com.google.android.material.R.attr.colorPrimary)

    @ColorInt
    fun onPrimary(context: Context): Int =
        resolve(context, com.google.android.material.R.attr.colorOnPrimary)

    @ColorInt
    fun primaryContainer(context: Context): Int =
        resolve(context, com.google.android.material.R.attr.colorPrimaryContainer)

    @ColorInt
    fun surface(context: Context): Int =
        resolve(context, com.google.android.material.R.attr.colorSurface)

    @ColorInt
    fun onSurface(context: Context): Int =
        resolve(context, com.google.android.material.R.attr.colorOnSurface)

    @ColorInt
    fun onSurfaceVariant(context: Context): Int =
        resolve(context, com.google.android.material.R.attr.colorOnSurfaceVariant)

    @ColorInt
    fun surfaceVariant(context: Context): Int =
        resolve(context, com.google.android.material.R.attr.colorSurfaceVariant)

    @ColorInt
    fun tertiary(context: Context): Int =
        resolve(context, com.google.android.material.R.attr.colorTertiary)

    @ColorInt
    fun error(context: Context): Int =
        resolve(context, com.google.android.material.R.attr.colorError)

    fun supportsDynamicColor(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    @ColorInt
    private fun resolve(context: Context, attr: Int): Int {
        val themed = themedContext(context)
        return MaterialColors.getColor(themed, attr, "WidgetThemeColors")
    }
}
