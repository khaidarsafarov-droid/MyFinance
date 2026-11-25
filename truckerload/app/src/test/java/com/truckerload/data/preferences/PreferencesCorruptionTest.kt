package com.truckerload.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PreferencesCorruptionTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun damagedFileResetsToDefaultsInsteadOfThrowing() = runBlocking {
        val file = folder.newFile("settings.preferences_pb")
        file.writeBytes(byteArrayOf(0x7F, 0x00, 0x13, 0x37, 0x42, 0x99.toByte(), 0xFF.toByte()))
        val key = booleanPreferencesKey("flag")
        val store = PreferenceDataStoreFactory.create(
            corruptionHandler = replaceCorruptedPreferences,
            produceFile = { file },
        )

        assertEquals(null, store.data.first()[key])
        store.edit { it[key] = true }
        assertEquals(true, store.data.first()[key])
    }
}
