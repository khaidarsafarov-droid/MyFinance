package com.truckerload.data.preferences

/** Gate for guest [AccountIds.LOCAL_DEV] sessions (allowed in LOCAL_ONLY builds). */
object LocalDevSessionPolicy {
    fun shouldRejectLocalDevSession(userId: String?, localOnlyMode: Boolean): Boolean =
        userId == AccountIds.LOCAL_DEV && !localOnlyMode
}
