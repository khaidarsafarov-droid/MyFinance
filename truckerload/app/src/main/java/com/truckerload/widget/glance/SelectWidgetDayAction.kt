package com.truckerload.widget.glance

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.truckerload.widget.WidgetDaySelectionStore

/** Tap a weekday chip: persist selection and redraw that widget. */
class SelectWidgetDayAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val offset = parameters[DAY_OFFSET] ?: return
        WidgetDaySelectionStore.save(context, glanceId, offset)
        OneUiGlanceWidgets.updateAll(context)
    }

    companion object {
        val DAY_OFFSET = ActionParameters.Key<Int>("day_offset")
    }
}
