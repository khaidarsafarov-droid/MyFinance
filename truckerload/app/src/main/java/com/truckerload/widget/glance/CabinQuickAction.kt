package com.truckerload.widget.glance

import android.content.Context
import android.content.Intent
import com.truckerload.R
import com.truckerload.widget.WidgetDeepLink

/** Scanner, camera, then diesel. Declaration order is the row order. */
internal enum class CabinQuickAction(
    val iconRes: Int,
    val labelRes: Int,
) {
    SCANNER(R.drawable.ic_widget_scanner, R.string.widget_scanner_short),
    CAMERA(R.drawable.ic_widget_camera, R.string.widget_camera_short),
    DIESEL(R.drawable.ic_widget_diesel, R.string.widget_diesel_short),
    ;

    fun launchIntent(context: Context): Intent = when (this) {
        SCANNER -> routeIntent(context, WidgetDeepLink.ROUTE_ATTACH_SCANNER)
        CAMERA -> routeIntent(context, WidgetDeepLink.ROUTE_ATTACH_CAMERA)
        DIESEL -> WidgetDeepLink.dieselQuickAddIntent(context)
    }
}
