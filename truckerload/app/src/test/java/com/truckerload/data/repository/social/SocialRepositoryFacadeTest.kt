package com.truckerload.data.repository.social

import com.truckerload.data.repository.SocialRepository
import com.truckerload.domain.social.DriverStatus
import com.truckerload.domain.social.SocialResult
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@Suppress("DEPRECATION")
class SocialRepositoryFacadeTest {
    private lateinit var profile: ProfileRepository
    private lateinit var chat: ChatRepository
    private lateinit var group: GroupRepository
    private lateinit var status: StatusRepository
    private lateinit var media: MediaRepository
    private lateinit var sync: SocialSyncCoordinator
    private lateinit var facade: SocialRepository

    @Before
    fun setUp() {
        profile = mock()
        chat = mock()
        group = mock()
        status = mock()
        media = mock()
        sync = mock()
        facade = SocialRepository(profile, chat, group, status, media, sync)
    }

    @Test
    fun ensureInitialized_delegatesToSyncCoordinator() = runBlocking<Unit> {
        facade.ensureInitialized()
        verify(sync).ensureInitialized()
    }

    @Test
    fun profileCalls_delegateToProfileRepository() = runBlocking<Unit> {
        whenever(profile.needsProfileSetup()).thenReturn(true)
        whenever(profile.removeAvatar()).thenReturn(SocialResult.Success(Unit))
        whenever(profile.watchMyProfile()).thenReturn(emptyFlow())

        assertTrue(facade.needsProfileSetup())
        assertEquals(SocialResult.Success(Unit), facade.removeAvatar())
        facade.watchMyProfile()

        verify(profile).needsProfileSetup()
        verify(profile).removeAvatar()
        verify(profile).watchMyProfile()
    }

    @Test
    fun chatCalls_delegateToChatRepository() = runBlocking<Unit> {
        whenever(chat.createPrivateChat("Ann")).thenReturn(SocialResult.Success("dm_1"))

        facade.markChatRead("c1")
        assertEquals(SocialResult.Success("dm_1"), facade.createPrivateChat("Ann"))

        verify(chat).markChatRead("c1")
        verify(chat).createPrivateChat("Ann")
    }

    @Test
    fun groupAndStatusCalls_delegateToDomainRepos() = runBlocking<Unit> {
        whenever(group.joinGroup("g1", "Me")).thenReturn(SocialResult.Success(Unit))
        whenever(status.updateStatus(DriverStatus.ONLINE)).thenReturn(SocialResult.Success(Unit))

        assertEquals(SocialResult.Success(Unit), facade.joinGroup("g1", "Me"))
        assertEquals(SocialResult.Success(Unit), facade.updateStatus(DriverStatus.ONLINE))

        verify(group).joinGroup("g1", "Me")
        verify(status).updateStatus(DriverStatus.ONLINE)
    }

    @Test
    fun mediaRepository_exposesInjectedMediaBound() {
        assertSame(media, facade.mediaRepository())
    }
}
