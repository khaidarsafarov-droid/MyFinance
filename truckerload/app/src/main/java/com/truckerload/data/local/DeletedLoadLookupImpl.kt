package com.truckerload.data.local

import android.content.Context
import com.truckerload.domain.store.DeletedLoadLookup

/** Prefs-backed tombstone. Not a Room table: the block has to outlive the deleted row. */
class DeletedLoadLookupImpl(
    private val context: Context,
) : DeletedLoadLookup {
    override fun isBlocked(loadId: String?, tripId: String?): Boolean =
        DeletedLoadLedger.isBlocked(context, loadId, tripId)

    companion object {
        /** Uses the context captured when the database was opened. Allows every id when none was. */
        fun fromOpenDatabase(): DeletedLoadLookup {
            val ctx = AppDatabase.hostContext() ?: return DeletedLoadLookup.Allow
            return DeletedLoadLookupImpl(ctx)
        }
    }
}
