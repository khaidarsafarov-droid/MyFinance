package com.truckerload.utils

import androidx.core.content.FileProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** Share intents keep working for photos, scans and backups in internal storage. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MediaFileProviderTest {

    private val context = RuntimeEnvironment.getApplication()

    private fun uriPath(file: File): String {
        file.parentFile?.mkdirs()
        file.writeText("x")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file).path.orEmpty()
    }

    @Test
    fun internalMediaFoldersAreShareable() {
        assertEquals("/photos_private/a.jpg", uriPath(File(MediaStorage.photosDir(context), "a.jpg")))
        assertEquals("/scans_private/b.pdf", uriPath(File(MediaStorage.scansDir(context), "b.pdf")))
        assertEquals("/backups/user/c.zip", uriPath(File(MediaStorage.backupsRoot(context), "user/c.zip")))
    }
}
