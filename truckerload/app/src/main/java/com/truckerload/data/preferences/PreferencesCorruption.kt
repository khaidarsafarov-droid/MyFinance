package com.truckerload.data.preferences

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences

/**
 * A damaged DataStore file throws CorruptionException from every read, which crashes the
 * app on each start. Start from defaults instead. The journal lives in Room, not in these
 * files, so only display settings and flags reset.
 */
internal val replaceCorruptedPreferences: ReplaceFileCorruptionHandler<Preferences> =
    ReplaceFileCorruptionHandler { emptyPreferences() }
