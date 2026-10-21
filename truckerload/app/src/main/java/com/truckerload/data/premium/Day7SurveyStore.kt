package com.truckerload.data.premium

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.truckerload.domain.premium.Day7SurveyPolicy
import java.time.ZoneId
import kotlinx.coroutines.flow.first

/**
 * Sibling of [PremiumOfferStore]. Own DataStore file so the weekly survey
 * does not share keys with the first-run Play offer (another branch edits
 * those prefs).
 */
private val Context.day7SurveyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "truckorig_day7_survey",
)

class Day7SurveyStore(context: Context) {
    private val dataStore = context.applicationContext.day7SurveyDataStore

    suspend fun read(): Day7SurveyPolicy.Record = read(dataStore.data.first())

    /** Writes [nowMillis] only when no first launch is stored yet. */
    suspend fun ensureFirstLaunch(nowMillis: Long) {
        dataStore.edit { prefs ->
            if (prefs[KEY_FIRST_LAUNCH_AT] == null) {
                prefs[KEY_FIRST_LAUNCH_AT] = nowMillis
            }
        }
    }

    /**
     * Atomically records one display when the policy says it is due.
     * Returns true only for the caller that stored [nowMillis]. Banner and
     * notification both go through this, so they cannot both win a week.
     */
    suspend fun claimDisplay(
        nowMillis: Long,
        premiumActive: Boolean,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        var claimed = false
        dataStore.edit { prefs ->
            val record = read(prefs)
            if (!Day7SurveyPolicy.shouldShow(record, nowMillis, premiumActive, zone)) {
                claimed = false
                return@edit
            }
            val updated = Day7SurveyPolicy.recordDisplay(record, nowMillis)
            val stamp = updated.lastShownAt
            if (stamp != null && stamp != record.lastShownAt) {
                prefs[KEY_LAST_SHOWN_AT] = stamp
                claimed = true
            } else {
                claimed = false
            }
        }
        return claimed
    }

    suspend fun optOut() {
        dataStore.edit { prefs ->
            prefs[KEY_OPTED_OUT] = true
        }
    }

    private fun read(prefs: Preferences): Day7SurveyPolicy.Record =
        Day7SurveyPolicy.Record(
            firstLaunchAt = prefs[KEY_FIRST_LAUNCH_AT],
            lastShownAt = prefs[KEY_LAST_SHOWN_AT],
            optedOut = prefs[KEY_OPTED_OUT] == true,
        )

    private companion object {
        val KEY_FIRST_LAUNCH_AT = longPreferencesKey("first_launch_at")
        val KEY_LAST_SHOWN_AT = longPreferencesKey("last_shown_at")
        val KEY_OPTED_OUT = booleanPreferencesKey("survey_opted_out")
    }
}
