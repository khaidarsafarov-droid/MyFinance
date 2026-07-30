package com.truckerload.data.repository.social

import android.app.Application
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.di.UserComponentManager
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Verifies account-scoped social repositories are wired through [UserComponentManager]
 * (the project's stand-in for `@HiltAndroidTest` + UserComponent injection).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProfileRepositoryInjectionTest {

    private lateinit var app: Application
    private lateinit var profileStore: UserProfileStore
    private lateinit var manager: UserComponentManager

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        profileStore = mock()
        manager = UserComponentManager(app, profileStore)
    }

    @After
    fun tearDown() {
        manager.endSession()
        AppDatabase.closeCurrent()
    }

    @Test
    fun startSession_injectsProfileRepositoryIntoUserComponent() {
        val component = manager.startSession("social-user")

        assertNotNull(component.profileRepository)
        assertTrue(component.profileRepository is ProfileRepositoryImpl)
        assertSame(component.profileRepository, manager.require().profileRepository)
    }

    @Test
    fun startSession_wiresAllSocialDomainRepos() {
        val component = manager.startSession("social-user")

        assertTrue(component.chatRepository is ChatRepositoryImpl)
        assertTrue(component.groupRepository is GroupRepositoryImpl)
        assertTrue(component.statusRepository is StatusRepositoryImpl)
        assertTrue(component.mediaRepository is MediaRepositoryImpl)
        assertNotNull(component.socialSyncCoordinator)
        assertNotNull(component.socialRepository)
    }

    @Test
    fun socialRepositoryModule_styleBridge_resolvesSameProfileInstance() {
        val component = manager.startSession("social-user")
        // Mirrors SocialRepositoryModule.provideProfileRepository(manager)
        val bridged: ProfileRepository = manager.require().profileRepository
        assertSame(component.profileRepository, bridged)
    }
}
