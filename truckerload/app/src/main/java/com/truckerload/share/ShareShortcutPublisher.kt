package com.truckerload.share

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.truckerload.R

/**
 * One Direct Share target. The launcher shortcuts in shortcuts.xml stay as they are.
 */
internal object ShareShortcutPublisher {
    const val CATEGORY = "com.truckorig.category.SHARE_LOAD"

    fun publish(context: Context) {
        val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
            .setShortLabel(context.getString(R.string.share_direct_label))
            .setLongLabel(context.getString(R.string.share_direct_label))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(
                Intent(Intent.ACTION_SEND).apply {
                    setClassName(context.packageName, ShareReceiverActivity::class.java.name)
                    type = "text/plain"
                },
            )
            .setCategories(setOf(CATEGORY))
            .setRank(5)
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }

    private const val SHORTCUT_ID = "share_load"
}
