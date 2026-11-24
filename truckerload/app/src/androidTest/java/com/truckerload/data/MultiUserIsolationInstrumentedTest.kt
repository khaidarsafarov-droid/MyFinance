package com.truckerload.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.preferences.AuthStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device smoke: each AuthStore user gets a separate Room file; loads do not leak.
 */
@RunWith(AndroidJUnit4::class)
class MultiUserIsolationInstrumentedTest {

    private val context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }
    private val userA = "smoke_user_a"
    private val userB = "smoke_user_b"

    @Before
    fun setUp() {
        AppDatabase.closeCurrent()
        AuthStore(context).logout()
        deleteDbFiles(userA)
        deleteDbFiles(userB)
    }

    @After
    fun tearDown() {
        AppDatabase.closeCurrent()
        AuthStore(context).logout()
        deleteDbFiles(userA)
        deleteDbFiles(userB)
    }

    @Test
    fun loadsStayIsolatedAcrossUsers() = runBlocking {
        // Room files are keyed by userId; open them directly so this does not
        // depend on AuthStore keeping a single active device profile.
        val dbA = AppDatabase.getInstance(context, userA)
        dbA.loadDao().insert(sampleLoad(id = "load-a", tripId = "TRIP-A"))
        assertEquals(listOf("TRIP-A"), dbA.loadDao().getAllLoadsOnce().map { it.tripId })
        assertTrue(context.getDatabasePath(AppDatabase.databaseNameFor(userA)).exists())

        AppDatabase.closeCurrent()
        val dbB = AppDatabase.getInstance(context, userB)
        assertEquals(0, dbB.loadDao().getAllLoadsOnce().size)
        dbB.loadDao().insert(sampleLoad(id = "load-b", tripId = "TRIP-B"))
        assertEquals(listOf("TRIP-B"), dbB.loadDao().getAllLoadsOnce().map { it.tripId })

        AppDatabase.closeCurrent()
        val dbAAgain = AppDatabase.getInstance(context, userA)
        assertEquals(listOf("TRIP-A"), dbAAgain.loadDao().getAllLoadsOnce().map { it.tripId })

        assertNotEquals(
            AppDatabase.databaseNameFor(userA),
            AppDatabase.databaseNameFor(userB),
        )
        assertTrue(context.getDatabasePath(AppDatabase.databaseNameFor(userA)).exists())
        assertTrue(context.getDatabasePath(AppDatabase.databaseNameFor(userB)).exists())
    }

    @Test
    fun loginAfterLogout_opensTheRequestedUserFile() = runBlocking {
        val auth = AuthStore(context)
        auth.login(userA, "a@smoke.test", rememberMe = true)
        AppDatabase.getInstanceForActiveUser(context)!!
            .loadDao()
            .insert(sampleLoad(id = "load-a", tripId = "TRIP-A"))

        AppDatabase.closeCurrent()
        auth.logout()
        assertNull(auth.currentUserIdOrNull())
        assertNull(AppDatabase.getInstanceForActiveUser(context))

        auth.login(userB, "b@smoke.test", rememberMe = true)
        val dbB = AppDatabase.getInstanceForActiveUser(context)!!
        assertEquals(userB, auth.currentUserIdOrNull())
        assertEquals(0, dbB.loadDao().getAllLoadsOnce().size)
        dbB.loadDao().insert(sampleLoad(id = "load-b", tripId = "TRIP-B"))

        // A second login while the device profile is still active keeps the
        // current id; isolation is recovered only after logout.
        auth.login(userA, "a@smoke.test", rememberMe = true)
        assertEquals(userB, auth.currentUserIdOrNull())

        AppDatabase.closeCurrent()
        auth.logout()
        auth.login(userA, "a@smoke.test", rememberMe = true)
        val dbAAgain = AppDatabase.getInstanceForActiveUser(context)!!
        assertEquals(userA, auth.currentUserIdOrNull())
        assertEquals(listOf("TRIP-A"), dbAAgain.loadDao().getAllLoadsOnce().map { it.tripId })
    }

    @Test
    fun rememberMeFalse_keepsProcessSession_forBackgroundJobs() {
        val auth = AuthStore(context)
        auth.login(userA, "a@smoke.test", rememberMe = false)
        assertEquals(userA, auth.currentUserIdOrNull())
        // New AuthStore instance must still see the same process-wide session
        // (Telegram workers / widgets construct their own AuthStore).
        assertEquals(userA, AuthStore(context).currentUserIdOrNull())
        assertEquals(userA, AuthStore(context).requireUserId())
        assertEquals(
            userA,
            AppDatabase.getInstanceForActiveUser(context)?.let {
                AppDatabase.currentUserIdOrNull()
            } ?: AuthStore(context).currentUserIdOrNull(),
        )
    }

    private fun deleteDbFiles(userId: String) {
        val name = AppDatabase.databaseNameFor(userId)
        val base = context.getDatabasePath(name)
        listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
            java.io.File(base.path + suffix).delete()
        }
    }

    private fun sampleLoad(id: String, tripId: String) = LoadEntity(
        id = id,
        tripId = tripId,
        date = "2026-07-20",
        totalRate = 250_000L, // $2,500 in cents
        totalMiles = 850.0,
        pointA = "Garner, NC",
        pointB = "Dallas, TX",
        puCount = 1,
        delCount = 1,
        weekNumber = 30,
        year = 2026,
        rawMessage = "smoke",
        parsedAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )
}
