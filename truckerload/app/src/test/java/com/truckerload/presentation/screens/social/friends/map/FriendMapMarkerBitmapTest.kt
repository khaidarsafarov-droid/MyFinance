package com.truckerload.presentation.screens.social.friends.map

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FriendMapMarkerBitmapTest {

    @Test
    fun createPerson_drawsLabeledAvatar() {
        val bmp = FriendMapMarkerBitmap.createPerson(
            density = 2f,
            label = "I am",
            ringColor = FriendMapMarkerBitmap.RING_ME,
            photo = null,
        )
        assertTrue(bmp.width > 80)
        assertTrue(bmp.height > 80)
        assertEquals(Bitmap.Config.ARGB_8888, bmp.config)
    }

    @Test
    fun createPerson_usesPhotoWhenProvided() {
        val photo = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        val bmp = FriendMapMarkerBitmap.createPerson(
            density = 1f,
            label = "Alex",
            ringColor = FriendMapMarkerBitmap.RING_FRIEND,
            photo = photo,
        )
        assertTrue(bmp.width > 40)
        assertTrue(bmp.height > 40)
    }

    @Test
    fun createDestination_isSmallerThanPersonPin() {
        val dest = FriendMapMarkerBitmap.createDestination(density = 2f, label = "Delivery")
        val person = FriendMapMarkerBitmap.createPerson(
            density = 2f,
            label = "I am",
            ringColor = FriendMapMarkerBitmap.RING_ME,
            photo = null,
        )
        assertTrue(dest.height < person.height)
    }

    @Test
    fun loadPhoto_blank_returnsNull() {
        org.junit.Assert.assertNull(FriendMapMarkerBitmap.loadPhoto(null, 48))
        org.junit.Assert.assertNull(FriendMapMarkerBitmap.loadPhoto("  ", 48))
    }
}
