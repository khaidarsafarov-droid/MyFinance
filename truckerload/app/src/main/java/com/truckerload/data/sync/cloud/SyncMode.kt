package com.truckerload.data.sync.cloud

/**
 * Cloud sync feature gate (runtime SharedPreferences; default [HYBRID]).
 *
 * - [DEVICE_ONLY] — pure local Room + local account mirror; no Ktor calls.
 * - [HYBRID] — Room first, then Ktor snapshots when `SYNC_BACKEND_URL` is set.
 * - [SERVER_PRIMARY] — same local-first UI, but cloud refresh is preferred on session
 *   ready / FCM wake (still never blocks home-screen reads on network).
 */
enum class SyncMode {
    DEVICE_ONLY,
    HYBRID,
    SERVER_PRIMARY,
    ;

    val allowsCloudCalls: Boolean
        get() = this != DEVICE_ONLY

    val prefersCloudRefresh: Boolean
        get() = this == SERVER_PRIMARY

    companion object {
        fun parse(raw: String?): SyncMode = when (raw?.trim()?.uppercase()) {
            "DEVICE_ONLY", "DEVICE", "LOCAL" -> DEVICE_ONLY
            "SERVER_PRIMARY", "SERVER", "CLOUD" -> SERVER_PRIMARY
            else -> HYBRID
        }
    }
}
