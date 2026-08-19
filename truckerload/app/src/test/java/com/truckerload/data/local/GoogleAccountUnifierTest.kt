package com.truckerload.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.truckerload.data.preferences.AccountIds
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GoogleAccountUnifierTest {

    private val aliasId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    private val sub = "unify-sub"

    @Before
    @After
    fun clean() {
        val context = RuntimeEnvironment.getApplication()
        listOf(
            AccountIds.fromGoogleSub(sub),
            AccountIds.fromGoogleSub("other-sub"),
            aliasId,
            AccountIds.LOCAL_DEV,
        ).forEach { id ->
            DatabaseFileCopy.deleteDbTree(context.getDatabasePath(AppDatabase.databaseNameFor(id)))
            context.getSharedPreferences(
                "truckerload_rpm_${AccountIds.sanitizeFilePart(id)}",
                Context.MODE_PRIVATE,
            ).edit().clear().commit()
        }
        AppDatabase.closeCurrent()
    }

    @Test
    fun relocateAliases_copiesUuidJournalOntoCanonicalGoogleId() {
        val context = RuntimeEnvironment.getApplication()
        val canonical = AccountIds.fromGoogleSub(sub)
        writeSqlite(context, aliasId)

        assertTrue(GoogleAccountUnifier.relocateAliases(context, canonical, listOf(aliasId)))
        val target = context.getDatabasePath(AppDatabase.databaseNameFor(canonical))
        assertTrue(DatabaseFileCopy.isHealthyDatabase(target))
        assertEquals(
            canonical,
            GoogleAccountUnifier.canonicalSessionUserId(context, aliasId, sub),
        )
    }

    @Test
    fun canonicalSessionUserId_rewritesUuidWhenGoogleSubPresent() {
        val context = RuntimeEnvironment.getApplication()
        val canonical = AccountIds.fromGoogleSub(sub)
        assertEquals(
            canonical,
            GoogleAccountUnifier.canonicalSessionUserId(context, aliasId, sub),
        )
    }

    @Test
    fun relocateAliases_copiesRpmPrefsWhenCanonicalEmpty() {
        val context = RuntimeEnvironment.getApplication()
        val canonical = AccountIds.fromGoogleSub(sub)
        context.getSharedPreferences(
            "truckerload_rpm_${AccountIds.sanitizeFilePart(aliasId)}",
            Context.MODE_PRIVATE,
        ).edit().putFloat("min_profit_threshold", 3.5f).commit()

        assertTrue(GoogleAccountUnifier.relocateAliases(context, canonical, listOf(aliasId)))
        val dest = context.getSharedPreferences(
            "truckerload_rpm_${AccountIds.sanitizeFilePart(canonical)}",
            Context.MODE_PRIVATE,
        )
        assertEquals(3.5f, dest.getFloat("min_profit_threshold", 0f))
    }

    @Test
    fun relocateAliases_doesNotCopyLocalDevOrAnotherGoogleId() {
        val context = RuntimeEnvironment.getApplication()
        val canonical = AccountIds.fromGoogleSub(sub)
        writeSqlite(context, AccountIds.LOCAL_DEV)
        writeSqlite(context, AccountIds.fromGoogleSub("other-sub"))

        assertFalse(
            GoogleAccountUnifier.relocateAliases(
                context,
                canonical,
                listOf(AccountIds.LOCAL_DEV, AccountIds.fromGoogleSub("other-sub")),
            ),
        )
        assertFalse(context.getDatabasePath(AppDatabase.databaseNameFor(canonical)).exists())
    }

    private fun writeSqlite(context: Context, userId: String) {
        val source = context.getDatabasePath(AppDatabase.databaseNameFor(userId))
        source.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(source, null).use { db ->
            db.execSQL("CREATE TABLE t(id INTEGER PRIMARY KEY)")
            db.execSQL("INSERT INTO t VALUES (1)")
        }
    }
}
