package com.truckerload.share

import android.content.Context
import com.truckerload.R
import com.truckerload.data.local.DeletedLoadLedger
import com.truckerload.data.share.PrefsShareImportIndex
import com.truckerload.di.userComponentManager
import com.truckerload.domain.parser.LoadField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class ShareNoticeKind { ADDED, ALREADY }

internal data class ShareImportNotice(
    val loads: List<ShareAddedLoad>,
    val kind: ShareNoticeKind,
    val token: Long = System.nanoTime(),
)

/** Hands the Loads journal a highlight and a snackbar after the share activity finishes. */
internal object ShareImportBus {
    private val pending = MutableStateFlow<ShareImportNotice?>(null)
    val notice: StateFlow<ShareImportNotice?> = pending.asStateFlow()

    fun publish(notice: ShareImportNotice) {
        pending.value = notice
    }

    fun clear(notice: ShareImportNotice) {
        if (pending.value?.token == notice.token) pending.value = null
    }
}

internal object ShareImportCopy {
    fun snackbar(context: Context, notice: ShareImportNotice): String {
        if (notice.loads.size > 1) {
            return context.resources.getQuantityString(
                R.plurals.share_loads_added,
                notice.loads.size,
                notice.loads.size,
            )
        }
        val missing = notice.loads.firstOrNull()?.missing.orEmpty()
        if (missing.isEmpty()) return context.getString(R.string.share_load_added)
        val labels = missing.joinToString(", ") { fieldName(context, it) }
        return context.getString(R.string.share_load_added_missing, labels)
    }

    private fun fieldName(context: Context, field: LoadField): String = when (field) {
        LoadField.RATE -> context.getString(R.string.share_field_rate)
        LoadField.PICKUP -> context.getString(R.string.share_field_pickup)
        LoadField.DELIVERY -> context.getString(R.string.share_field_delivery)
        LoadField.MILES -> context.getString(R.string.share_field_miles)
        LoadField.DATE -> context.getString(R.string.share_field_date)
        LoadField.TRIP_ID -> context.getString(R.string.share_field_load_number)
    }
}

internal object ShareImportUndo {
    suspend fun undo(context: Context, notice: ShareImportNotice) {
        val component = context.userComponentManager().currentOrNull() ?: return
        val index = PrefsShareImportIndex(context, component.userId)
        for (load in notice.loads) {
            component.loadRepository.deleteLoad(load.loadId)
            index.forget(load.loadId)
            DeletedLoadLedger.allowAgain(context, load.loadId, load.tripId)
        }
    }
}
