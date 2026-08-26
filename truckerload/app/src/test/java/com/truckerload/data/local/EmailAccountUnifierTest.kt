package com.truckerload.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.truckerload.data.preferences.AccountIds
import org.junit.After
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
class EmailAccountUnifierTest {

    private val email = "driver@test.com"
    private val uuid = "11111111-2222-3333-4444-555555555555"

    @Before
    @After
    fun clean() {
        val context = RuntimeEnvironment.getApplication()
        listOf(uuid, AccountIds.fromEmail(email), AccountIds.LOCAL_DEV).forEach { id ->
            DatabaseFileCopy.deleteDbTree(context.getDatabasePath(AppDatabase.databaseNameFor(id)))
        }
        AppDatabase.closeCurrent()
    }

    @Test
    fun relocate_copiesLocalHashJournalOntoUuidWhenUuidEmpty() {
        val context = RuntimeEnvironment.getApplication()
        val alias = AccountIds.fromEmail(email)
        writeSqlite(context, alias)

        assertTrue(EmailAccountUnifier.relocateLocalEmailJournal(context, uuid, email))
        val target = context.getDatabasePath(AppDatabase.databaseNameFor(uuid))
        assertTrue(DatabaseFileCopy.isHealthyDatabase(target))
    }

    @Test
    fun relocate_skipsWhenCanonicalAlreadyHealthy() {
        val context = RuntimeEnvironment.getApplication()
        writeSqlite(context, uuid)
        writeSqlite(context, AccountIds.fromEmail(email))

        assertFalse(EmailAccountUnifier.relocateLocalEmailJournal(context, uuid, email))
    }

    @Test
    fun relocate_skipsLocalDevAndGoogleIds() {
        val context = RuntimeEnvironment.getApplication()
        writeSqlite(context, AccountIds.fromEmail(email))
        assertFalse(
            EmailAccountUnifier.relocateLocalEmailJournal(context, AccountIds.LOCAL_DEV, email),
        )
        assertFalse(
            EmailAccountUnifier.relocateLocalEmailJournal(
                context,
                AccountIds.fromGoogleSub("sub"),
                email,
            ),
        )
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
