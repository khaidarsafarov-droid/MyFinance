package com.truckerload.domain.store

/**
 * Whether a load id or trip id was deleted and must not be inserted again.
 * The tombstone lives outside the load table, so the domain does not open it.
 */
fun interface DeletedLoadLookup {
    fun isBlocked(loadId: String?, tripId: String?): Boolean

    companion object {
        /** Nothing is blocked. Tests and callers without a ledger use this. */
        val Allow: DeletedLoadLookup = DeletedLoadLookup { _, _ -> false }
    }
}
