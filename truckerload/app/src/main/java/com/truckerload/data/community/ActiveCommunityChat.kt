package com.truckerload.data.community

/** Currently open community chat, if any — used to skip shade alerts while reading. */
object ActiveCommunityChat {
    @Volatile
    var chatId: String? = null
}
