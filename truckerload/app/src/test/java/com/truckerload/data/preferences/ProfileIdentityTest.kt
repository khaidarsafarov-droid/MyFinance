package com.truckerload.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileIdentityTest {

    private fun googleProfile(
        given: String = "Google",
        family: String = "User",
        photo: String? = "https://lh3.googleusercontent.com/a/photo",
        nickname: String? = null,
    ) = UserProfile(
        email = "driver@gmail.com",
        givenName = given,
        familyName = family,
        photoUrl = photo,
        nickname = nickname,
    )

    @Test
    fun firstLoginKeepsGoogleNameAndPhoto() {
        val merged = ProfileIdentity.mergeLoginProfile(googleProfile(), existing = null)
        assertEquals("Google", merged.givenName)
        assertEquals("https://lh3.googleusercontent.com/a/photo", merged.photoUrl)
        assertFalse(merged.customDisplayName)
        assertFalse(merged.customPhoto)
    }

    @Test
    fun customNameSurvivesGoogleLogin() {
        val existing = googleProfile(given = "Ivan", family = "Petrov").copy(customDisplayName = true)
        val merged = ProfileIdentity.mergeLoginProfile(googleProfile(given = "Gmail", family = "Name"), existing)
        assertEquals("Ivan", merged.givenName)
        assertEquals("Petrov", merged.familyName)
        assertTrue(merged.customDisplayName)
    }

    @Test
    fun customPhotoSurvivesGoogleLoginIncludingCleared() {
        val existing = googleProfile(photo = null).copy(customPhoto = true)
        val merged = ProfileIdentity.mergeLoginProfile(googleProfile(), existing)
        assertNull(merged.photoUrl)
        assertTrue(merged.customPhoto)
    }

    @Test
    fun localFilePhotoIsTreatedAsCustom() {
        val existing = googleProfile(photo = "/data/user/0/avatar.jpg")
        val merged = ProfileIdentity.mergeLoginProfile(googleProfile(), existing)
        assertEquals("/data/user/0/avatar.jpg", merged.photoUrl)
        assertTrue(merged.customPhoto)
    }

    @Test
    fun blankLoginNicknameDoesNotWipeHandle() {
        val existing = googleProfile(nickname = "DriverOne")
        val merged = ProfileIdentity.mergeLoginProfile(googleProfile(nickname = null), existing)
        assertEquals("DriverOne", merged.nickname)
    }

    @Test
    fun cloudNicknameWinsOnLogin() {
        val existing = googleProfile(nickname = "OldNick")
        val merged = ProfileIdentity.mergeLoginProfile(googleProfile(nickname = "NewNick"), existing)
        assertEquals("NewNick", merged.nickname)
    }

    @Test
    fun mergeRoomAvatarDoesNotRestoreGoogleAfterClear() {
        val merged = ProfileIdentity.mergeRoomAvatar(
            existingAvatar = null,
            providerPhotoUrl = "https://lh3.googleusercontent.com/a/photo",
            customPhoto = true,
        )
        assertNull(merged)
    }

    @Test
    fun mergeRoomAvatarSeedsGoogleWhenUserHasNotChosen() {
        val merged = ProfileIdentity.mergeRoomAvatar(
            existingAvatar = null,
            providerPhotoUrl = "https://lh3.googleusercontent.com/a/photo",
            customPhoto = false,
        )
        assertEquals("https://lh3.googleusercontent.com/a/photo", merged)
    }

    @Test
    fun displayPhotoUrlIgnoresGoogleFallbackAfterCustomPhoto() {
        assertNull(
            ProfileIdentity.displayPhotoUrl(
                roomAvatar = null,
                providerPhotoUrl = "https://lh3.googleusercontent.com/a/photo",
                customPhoto = true,
            ),
        )
        assertEquals(
            "https://lh3.googleusercontent.com/a/photo",
            ProfileIdentity.displayPhotoUrl(
                roomAvatar = null,
                providerPhotoUrl = "https://lh3.googleusercontent.com/a/photo",
                customPhoto = false,
            ),
        )
    }

    @Test
    fun customRoomNameIsNotReplacedByGoogle() {
        assertEquals(
            "Ivan",
            ProfileIdentity.mergeRoomDisplayName("Ivan", "Google User"),
        )
        assertEquals(
            "Google User",
            ProfileIdentity.mergeRoomDisplayName("Driver", "Google User"),
        )
    }
}
