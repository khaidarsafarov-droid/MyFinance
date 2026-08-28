package com.truckerload.data.preferences

/** Guest [AccountIds.LOCAL_DEV] is the on-device journal. Never force-logout. */
object LocalDevSessionPolicy {
    @Suppress("UNUSED_PARAMETER")
    fun shouldRejectLocalDevSession(userId: String?, localOnlyMode: Boolean): Boolean = false
}
