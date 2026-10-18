package com.truckerload.widget.glance

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.widget.WidgetDeepLink

/**
 * Left-to-right order. Every size reads [priority] and drops from the end
 * when the cell cannot fit another 48dp target.
 */
internal enum class CabinQuickAction(
    val iconRes: Int,
    val labelRes: Int,
) {
    CAMERA(R.drawable.ic_widget_camera, R.string.widget_camera_short),
    SCANNER(R.drawable.ic_widget_scanner, R.string.widget_scanner_short),
    DIESEL(R.drawable.ic_widget_diesel, R.string.widget_diesel_short),
    ;

    fun launchIntent(context: Context): Intent = when (this) {
        CAMERA -> routeIntent(context, WidgetDeepLink.ROUTE_ATTACH_CAMERA)
        SCANNER -> routeIntent(context, WidgetDeepLink.ROUTE_ATTACH_SCANNER)
        DIESEL -> WidgetDeepLink.dieselQuickAddIntent(context)
    }

    companion object {
        val priority: List<CabinQuickAction> = entries

        fun visible(count: Int): List<CabinQuickAction> =
            priority.take(count.coerceIn(1, priority.size))

        /** How many [priority] actions fit in [width], never fewer than Camera. */
        fun countFor(width: Dp): Int {
            val slot = 56.dp
            val available = (width - 16.dp).coerceAtLeast(slot)
            return (available.value / slot.value).toInt().coerceIn(1, priority.size)
        }
    }
}
