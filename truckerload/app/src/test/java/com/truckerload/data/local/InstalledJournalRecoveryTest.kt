package com.truckerload.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthProvider
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.SecurePreferences
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.preferences.TelegramTokenStore
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InstalledJournalRecoveryTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        AuthStore.resetForTests()
        SecurePreferences.resetFallbackForTests()
        AuthStore(context).logout()
        AuthStore.resetForTests()
        deleteJournals()
    }

    @After
    fun tearDown() {
        AuthStore(context).logout()
        AuthStore.resetForTests()
        SecurePreferences.resetFallbackForTests()
        deleteJournals()
    }

    @Test
    fun signedOut_oneJournal_logsBackIntoThatProfile() {
        writeLoads("truckerload_google_deadbeef", listOf("load-1"))

        InstalledJournalRecovery.recover(context, AuthStore(context))

        val auth = AuthStore(context)
        assertEquals("google_deadbeef", auth.currentUserIdOrNull())
        assertEquals(AuthProvider.GOOGLE, auth.authProvider())
        assertEquals(listOf("load-1"), loadIds("truckerload_google_deadbeef"))
        assertFalse(database("truckerload_local_dev").exists())
    }

    @Test
    fun signedOut_twoJournals_doesNotGuess() {
        writeLoads("truckerload_google_one", listOf("a"))
        writeLoads("truckerload_google_two", listOf("b"))

        InstalledJournalRecovery.recover(context, AuthStore(context))

        assertNull(AuthStore(context).currentUserIdOrNull())
        assertEquals(listOf("a"), loadIds("truckerload_google_one"))
        assertEquals(listOf("b"), loadIds("truckerload_google_two"))
    }

    @Test
    fun signedOut_legacyFile_copiesIntoLocalProfile() {
        writeLoads(LegacyDatabaseAbsorb.LEGACY_SINGLE_FILE, listOf("old"))

        InstalledJournalRecovery.recover(context, AuthStore(context))

        assertEquals(AccountIds.LOCAL_DEV, AuthStore(context).currentUserIdOrNull())
        assertEquals(AuthProvider.LOCAL, AuthStore(context).authProvider())
        assertEquals(listOf("old"), loadIds(AppDatabase.databaseNameFor(AccountIds.LOCAL_DEV)))
        assertEquals(listOf("old"), loadIds(LegacyDatabaseAbsorb.LEGACY_SINGLE_FILE))
    }

    @Test
    fun emptyActiveProfile_adoptsTheOnlyOtherJournal() = runBlocking {
        val sourceId = "google_recovery_src"
        val targetId = "local_recovery_target"
        writeLoads(AppDatabase.databaseNameFor(sourceId), listOf("kept"))
        val auth = AuthStore(context)
        auth.login(sourceId, "driver@example.com", rememberMe = true, provider = AuthProvider.GOOGLE)
        TelegramTokenStore(context, sourceId).setToken("123456:ABC")
        val settings = SettingsDataStore(context)
        settings.saveTelegramChatId(555L)
        settings.saveLastUpdateOffset(900L)
        auth.logout()
        AuthStore.resetForTests()

        AuthStore(context).login(targetId, "local@device", rememberMe = true, provider = AuthProvider.EMAIL)
        InstalledJournalRecovery.recover(context, AuthStore(context))

        assertEquals(targetId, AuthStore(context).currentUserIdOrNull())
        assertEquals(listOf("kept"), loadIds(AppDatabase.databaseNameFor(targetId)))
        assertEquals(listOf("kept"), loadIds(AppDatabase.databaseNameFor(sourceId)))
        assertEquals("123456:ABC", TelegramTokenStore(context, targetId).getToken())
        assertEquals(555L, SettingsDataStore(context).getTelegramChatIdOnce())
        assertEquals(900L, SettingsDataStore(context).getLastUpdateOffset())
    }

    @Test
    fun emptyActiveProfile_twoOtherJournals_doesNotAdopt() {
        writeLoads("truckerload_google_one", listOf("a"))
        writeLoads("truckerload_google_two", listOf("b"))
        AuthStore(context).login(AccountIds.LOCAL_DEV, "local@device", rememberMe = true)

        InstalledJournalRecovery.recover(context, AuthStore(context))

        assertEquals(AccountIds.LOCAL_DEV, AuthStore(context).currentUserIdOrNull())
        assertFalse(database(AppDatabase.databaseNameFor(AccountIds.LOCAL_DEV)).exists())
        assertEquals(listOf("a"), loadIds("truckerload_google_one"))
        assertEquals(listOf("b"), loadIds("truckerload_google_two"))
    }

    @Test
    fun activeJournalWithLoads_isLeftAlone() {
        writeLoads(AppDatabase.databaseNameFor(AccountIds.LOCAL_DEV), listOf("mine"))
        writeLoads("truckerload_google_other", listOf("theirs"))
        AuthStore(context).login(AccountIds.LOCAL_DEV, "local@device", rememberMe = true)

        InstalledJournalRecovery.recover(context, AuthStore(context))

        assertEquals(listOf("mine"), loadIds(AppDatabase.databaseNameFor(AccountIds.LOCAL_DEV)))
        assertEquals(listOf("theirs"), loadIds("truckerload_google_other"))
    }

    @Test
    fun isPresentDatabase_acceptsCiphertextThatPlatformSqliteCannotOpen() {
        val file = database("truckerload_present_probe")
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(180) { 1 })
        assertTrue(InstalledJournalRecovery.isPresentDatabase(file))
        file.writeBytes(ByteArray(40) { 1 })
        assertFalse(InstalledJournalRecovery.isPresentDatabase(file))
    }

    private fun writeLoads(name: String, ids: List<String>) {
        val file = database(name)
        file.parentFile?.mkdirs()
        if (file.exists()) file.delete()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE loads (id TEXT PRIMARY KEY NOT NULL)")
            ids.forEach { id ->
                db.execSQL("INSERT INTO loads (id) VALUES ('$id')")
            }
        }
    }

    private fun loadIds(name: String): List<String> {
        val file = database(name)
        if (!file.exists()) return emptyList()
        return SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT id FROM loads ORDER BY id", null).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.getString(0))
                }
            }
        }
    }

    private fun database(name: String): File = context.getDatabasePath(name)

    private fun deleteJournals() {
        val dir = context.getDatabasePath("truckerload_db").parentFile ?: return
        dir.listFiles().orEmpty()
            .filter { it.name.startsWith("truckerload") }
            .forEach { it.delete() }
    }
}
