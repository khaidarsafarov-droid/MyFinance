package com.truckerload.di

import android.app.Application
import com.truckerload.data.preferences.UserProfileStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UserComponentManagerTest {

    private lateinit var app: Application
    private lateinit var profileStore: UserProfileStore
    private lateinit var manager: UserComponentManager

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        profileStore = mock()
        manager = UserComponentManager(app, profileStore)
    }

    @Test
    fun startSession_createsComponentAndBindsProfile() {
        val component = manager.startSession("user-a")
        assertEquals("user-a", component.userId)
        assertSame(component, manager.currentOrNull())
        assertEquals("user-a", manager.currentUserIdOrNull())
        verify(profileStore).bindUser("user-a")
        assertNotNull(component.database)
        assertNotNull(component.loadRepository)
    }

    @Test
    fun startSession_sameUser_isIdempotent() {
        val first = manager.startSession("user-a")
        val second = manager.startSession("user-a")
        assertSame(first, second)
    }

    @Test
    fun startSession_differentUser_replacesComponent() {
        val first = manager.startSession("user-a")
        val second = manager.startSession("user-b")
        assertNotSame(first, second)
        assertEquals("user-b", manager.require().userId)
        verify(profileStore).bindUser("user-b")
        verify(profileStore).unbind()
    }

    @Test
    fun endSession_clearsActiveComponent() {
        manager.startSession("user-a")
        manager.endSession()
        assertNull(manager.currentOrNull())
        verify(profileStore).unbind()
    }
}
