package com.truckerload.data.preferences

/**
 * Health of the Google / cloud session relative to the offline-first Room DB.
 * The app never blocks local work when health is degraded.
 */
enum class AuthSessionHealth {
    /** Silent re-auth / token check succeeded (or local-only / email session). */
    VERIFIED,

    /** No network — continue with Room; sync deferred. */
    OFFLINE_LOCAL,

    /** Online but silent Google / refresh failed — keep working locally. */
    SESSION_UNCONFIRMED,
}

enum class AuthProvider {
    LOCAL,
    EMAIL,
    GOOGLE,
}
