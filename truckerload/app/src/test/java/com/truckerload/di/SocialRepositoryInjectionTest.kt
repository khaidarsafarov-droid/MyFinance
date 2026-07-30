package com.truckerload.di

import android.app.Application
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.repository.social.ChatRepository
import com.truckerload.data.repository.social.GroupRepository
import com.truckerload.data.repository.social.MediaRepository
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.data.repository.social.ProfileRepositoryImpl
import com.truckerload.data.repository.social.SocialSyncCoordinator
import com.truckerload.data.repository.social.StatusRepository
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
 * Hilt does not define a custom UserComponent graph — account repos are built in
 * [UserComponent.create] and bridged by [SocialRepositoryModule]. This test proves
 * [ProfileRepository] (and siblings) are available from the active session the same
 * way `@HiltViewModel` injection would resolve them via [UserComponentManager].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SocialRepositoryInjectionTest {

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
    fun profileRepository_isInjectableFromActiveUserComponent() {
        val component = manager.startSession("user-social")
        val profile: ProfileRepository = component.profileRepository
        assertNotNull(profile)
        assertTrue(profile is ProfileRepositoryImpl)
        assertSame(profile, manager.require().profileRepository)
    }

    @Test
    fun socialRepositoryModule_bindingsResolveFromManager() {
        manager.startSession("user-social")
        // Mirror SocialRepositoryModule @Provides bodies.
        val profile: ProfileRepository = manager.require().profileRepository
        val chat: ChatRepository = manager.require().chatRepository
        val group: GroupRepository = manager.require().groupRepository
        val status: StatusRepository = manager.require().statusRepository
        val media: MediaRepository = manager.require().mediaRepository
        val sync: SocialSyncCoordinator = manager.require().socialSyncCoordinator
        assertNotNull(profile)
        assertNotNull(chat)
        assertNotNull(group)
        assertNotNull(status)
        assertNotNull(media)
        assertNotNull(sync)
        @Suppress("DEPRECATION")
        assertSame(profile, manager.require().socialRepository.profileRepository)
    }
}
