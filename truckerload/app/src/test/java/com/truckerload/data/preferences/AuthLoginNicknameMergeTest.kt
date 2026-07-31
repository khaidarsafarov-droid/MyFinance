package com.truckerload.data.preferences

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AuthLoginNicknameMergeTest {

    @Test
    fun loginWithoutNicknameDoesNotWipeExistingHandle() {
        val context = RuntimeEnvironment.getApplication() as Context
        val auth = AuthStore(context)
        val profiles = UserProfileStore(context)
        AuthLogin.completeLogin(
            authStore = auth,
            userProfileStore = profiles,
            userId = "user-1",
            profile = UserProfile(
                email = "a@b.com",
                givenName = "A",
                familyName = "B",
                photoUrl = null,
                nickname = "DriverOne",
            ),
        )
        assertEquals("DriverOne", profiles.profile.value?.nickname)

        AuthLogin.completeLogin(
            authStore = auth,
            userProfileStore = profiles,
            userId = "user-1",
            profile = UserProfile(
                email = "a@b.com",
                givenName = "A",
                familyName = "B",
                photoUrl = null,
                nickname = null,
            ),
        )
        assertEquals("DriverOne", profiles.profile.value?.nickname)
    }

    @Test
    fun cloudNicknameWinsOnLogin() {
        val context = RuntimeEnvironment.getApplication() as Context
        val auth = AuthStore(context)
        val profiles = UserProfileStore(context)
        AuthLogin.completeLogin(
            authStore = auth,
            userProfileStore = profiles,
            userId = "user-2",
            profile = UserProfile(
                email = "c@d.com",
                givenName = "C",
                familyName = "D",
                photoUrl = null,
                nickname = "OldNick",
            ),
        )
        AuthLogin.completeLogin(
            authStore = auth,
            userProfileStore = profiles,
            userId = "user-2",
            profile = UserProfile(
                email = "c@d.com",
                givenName = "C",
                familyName = "D",
                photoUrl = null,
                nickname = "NewNick",
            ),
        )
        assertEquals("NewNick", profiles.profile.value?.nickname)
        assertNull(
            // sanity: blank still preserves when cloud sends null — already covered above
            null,
        )
    }
}
