package com.truckerload.data.repository.social

import com.truckerload.data.repository.SocialRepository
import com.truckerload.domain.social.DriverStatus
import com.truckerload.domain.social.SocialResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Verifies the deprecated [SocialRepository] facade delegates to focused repos
 * (no logic of its own beyond forwarding).
 */
@Suppress("DEPRECATION")
class SocialRepositoryFacadeTest {

    private val profile: ProfileRepository = mock()
    private val chat: ChatRepository = mock()
    private val group: GroupRepository = mock()
    private val status: StatusRepository = mock()
    private val media: MediaRepository = mock()
    private val sync: SocialSyncCoordinator = mock()

    private val facade = SocialRepository(
        profileRepository = profile,
        chatRepository = chat,
        groupRepository = group,
        statusRepository = status,
        mediaRepository = media,
        syncCoordinator = sync,
    )

    @Test
    fun markChatRead_delegatesToChatRepository() = runBlocking {
        facade.markChatRead("chat-1")
        verify(chat).markChatRead("chat-1")
    }

    @Test
    fun updateStatus_delegatesToProfileRepository() = runBlocking {
        whenever(profile.updateStatus(DriverStatus.ONLINE))
            .thenReturn(SocialResult.Success(Unit))
        val result = facade.updateStatus(DriverStatus.ONLINE)
        assertTrue(result is SocialResult.Success)
        verify(profile).updateStatus(DriverStatus.ONLINE)
        Unit
    }

    @Test
    fun ensureInitialized_delegatesToSyncCoordinator() = runBlocking {
        facade.ensureInitialized()
        verify(sync).ensureInitialized()
    }

    @Test
    fun createPrivateChat_delegatesToChatRepository() = runBlocking {
        whenever(chat.createPrivateChat("Alice")).thenReturn(SocialResult.Success("dm_1"))
        val result = facade.createPrivateChat("Alice")
        assertEquals("dm_1", (result as SocialResult.Success).data)
        verify(chat).createPrivateChat("Alice")
        Unit
    }

    @Test
    fun exposedSubRepositories_areSameInstances() {
        assertSame(profile, facade.profileRepository)
        assertSame(chat, facade.chatRepository)
        assertSame(group, facade.groupRepository)
        assertSame(status, facade.statusRepository)
        assertSame(media, facade.mediaRepository)
        assertSame(sync, facade.syncCoordinator)
    }
}
