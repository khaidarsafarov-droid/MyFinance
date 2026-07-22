package com.truckerload.data.social

import com.truckerload.BuildConfig
import com.truckerload.data.local.dao.SocialPeerDao
import com.truckerload.data.local.entities.SocialPeerEntity

object SocialPeerSeedData {
    suspend fun seedIfEmpty(peerDao: SocialPeerDao) {
        if (peerDao.count() > 0) return
        val demoSuffix = if (BuildConfig.LOCAL_ONLY_MODE) " (demo)" else ""
        peerDao.upsertAll(
            listOf(
                SocialPeerEntity("peer_ivan", "Ivan Petrov$demoSuffix", 4.9, 2847.0, 12_345.0, 8, 4.34),
                SocialPeerEntity("peer_alexey", "Alexey S.$demoSuffix", 4.8, 2543.0, 11_234.0, 7, 4.41),
                SocialPeerEntity("peer_sergey", "Sergey K.$demoSuffix", 4.7, 2345.0, 10_123.0, 6, 4.32),
                SocialPeerEntity("peer_dmitry", "Dmitry L.$demoSuffix", 4.6, 1987.0, 9_876.0, 5, 4.28),
                SocialPeerEntity("peer_andrey", "Andrey M.$demoSuffix", 4.5, 1876.0, 9_234.0, 5, 4.25),
                SocialPeerEntity("peer_maria", "Maria V.$demoSuffix", 4.8, 1765.0, 8_900.0, 4, 4.38),
            ),
        )
    }
}
