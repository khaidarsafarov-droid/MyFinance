package com.truckerload.data.preferences

import com.truckerload.domain.friends.FriendsLocationSharePolicy
import kotlinx.coroutines.runBlocking
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
class FriendsLocationShareStoreTest {

    private lateinit var authStore: AuthStore
    private lateinit var profiles: UserProfileStore
    private lateinit var store: FriendsLocationShareStore

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        authStore = AuthStore(context)
        profiles = UserProfileStore(context)
        store = FriendsLocationShareStore(context)
        authStore.logout()
    }

    @Test
    fun motionAndLiveUntil_areIsolatedPerUser() = runBlocking {
        loginAs("user-a")
        store.setLastMotion(FriendsLocationSharePolicy.Motion.STILL)
        store.setLiveUntilMs(9_000L)
        store.markPublished(3_000L)
        assertEquals(FriendsLocationSharePolicy.Motion.STILL, store.lastMotion())
        assertEquals(9_000L, store.liveUntilMs())
        assertEquals(3_000L, store.lastPublishedAtMs())

        authStore.logout()
        loginAs("user-b")
        assertEquals(FriendsLocationSharePolicy.Motion.UNKNOWN, store.lastMotion())
        assertEquals(0L, store.liveUntilMs())
        assertEquals(0L, store.lastPublishedAtMs())
        assertFalse(store.isLiveSessionActive(10_000L))
    }

    @Test
    fun liveSessionActive_respectsDeadline() {
        loginAs("user-a")
        store.setLiveUntilMs(50L)
        assertTrue(store.isLiveSessionActive(40L))
        assertFalse(store.isLiveSessionActive(50L))
        store.clearLiveUntil()
        assertFalse(store.isLiveSessionActive(1L))
    }

    private fun loginAs(userId: String) {
        AuthLogin.completeLogin(
            authStore = authStore,
            userProfileStore = profiles,
            userId = userId,
            profile = UserProfile(
                email = "$userId@example.com",
                givenName = "Test",
                familyName = "User",
                photoUrl = null,
            ),
        )
    }
}
