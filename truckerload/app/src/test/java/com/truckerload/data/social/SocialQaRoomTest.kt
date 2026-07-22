package com.truckerload.data.social

import androidx.room.Room
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.BlockedUserEntity
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.local.entities.DriverStatusEntity
import com.truckerload.data.local.entities.SocialChatEntity
import com.truckerload.domain.social.ChatType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SocialQaRoomTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun blockAndUnblockPeer() = runBlocking {
        val dao = db.blockedUserDao()
        val me = DriverProfileEntity.LOCAL_USER_ID
        dao.block(BlockedUserEntity(me, "peer_ivan", 1L))
        assertTrue(dao.isBlocked(me, "peer_ivan"))
        dao.unblock(me, "peer_ivan")
        assertFalse(dao.isBlocked(me, "peer_ivan"))
    }

    @Test
    fun statusPurgeExpiredRemovesOldRows() = runBlocking {
        val dao = db.driverStatusDao()
        val now = 1_000_000L
        dao.insert(
            DriverStatusEntity(
                id = "alive",
                userId = "peer_a",
                displayName = "A",
                type = "TEXT",
                text = "ok",
                mediaPath = null,
                createdAt = now - 10,
                expiresAt = now + 60_000,
            ),
        )
        dao.insert(
            DriverStatusEntity(
                id = "dead",
                userId = "peer_b",
                displayName = "B",
                type = "TEXT",
                text = "gone",
                mediaPath = null,
                createdAt = now - 100,
                expiresAt = now - 1,
            ),
        )
        dao.purgeExpired(now)
        val active = dao.watchActiveStatuses(now).first()
        assertEquals(1, active.size)
        assertEquals("alive", active.single().id)
    }

    @Test
    fun peerSeedIsIdempotent() = runBlocking {
        val peerDao = db.socialPeerDao()
        SocialPeerSeedData.seedIfEmpty(peerDao)
        val first = peerDao.count()
        assertTrue(first > 0)
        SocialPeerSeedData.seedIfEmpty(peerDao)
        assertEquals(first, peerDao.count())
    }

    @Test
    fun inviteCodeLookupBlankAndUnknown() = runBlocking {
        val chatDao = db.socialChatDao()
        chatDao.upsert(
            SocialChatEntity(
                id = "group_i95",
                title = "I-95",
                type = ChatType.GROUP.name,
                participantCount = 1,
                lastMessage = "",
                lastMessageAt = 1L,
                unreadCount = 0,
                avatarEmoji = "🗺️",
                inviteCode = "I95ROAD",
            ),
        )
        assertEquals("group_i95", chatDao.getChatByInviteCode("I95ROAD")?.id)
        assertNull(chatDao.getChatByInviteCode(""))
        assertNull(chatDao.getChatByInviteCode("NOPE99"))
    }
}
