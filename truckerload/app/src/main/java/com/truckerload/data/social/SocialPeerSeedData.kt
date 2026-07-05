package com.truckerload.data.social

import com.truckerload.data.local.dao.SocialPeerDao
import com.truckerload.data.local.entities.SocialPeerEntity

object SocialPeerSeedData {
    suspend fun seedIfEmpty(peerDao: SocialPeerDao) {
        if (peerDao.count() > 0) return
        peerDao.upsertAll(
            listOf(
                SocialPeerEntity("peer_ivan", "Иван Петров", 4.9, 2847.0, 12_345.0, 8, 4.34),
                SocialPeerEntity("peer_alexey", "Алексей С.", 4.8, 2543.0, 11_234.0, 7, 4.41),
                SocialPeerEntity("peer_sergey", "Сергей К.", 4.7, 2345.0, 10_123.0, 6, 4.32),
                SocialPeerEntity("peer_dmitry", "Дмитрий Л.", 4.6, 1987.0, 9_876.0, 5, 4.28),
                SocialPeerEntity("peer_andrey", "Андрей М.", 4.5, 1876.0, 9_234.0, 5, 4.25),
                SocialPeerEntity("peer_maria", "Мария В.", 4.8, 1765.0, 8_900.0, 4, 4.38),
            ),
        )
    }
}
