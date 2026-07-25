package com.truckerload.data.sync

import com.truckerload.contract.MediaKind
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MediaFilePolicyTest {
    @Test
    fun `validates mime size and sha256 from bytes`() {
        val file = File.createTempFile("media-policy", ".jpg")
        try {
            file.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 1, 2, 3))
            val validated = MediaFilePolicy.validateUpload(file, MediaKind.PHOTO)

            assertEquals("image/jpeg", validated.contentType)
            assertEquals(file.length(), validated.sizeBytes)
            assertEquals(64, validated.sha256.length)
            MediaFilePolicy.verifyChecksum(file, validated.sha256)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `rejects kind mismatch malformed checksum and unsafe client id`() {
        val file = File.createTempFile("media-policy", ".pdf")
        try {
            file.writeText("%PDF-1.7")
            assertThrows(MediaValidationException::class.java) {
                MediaFilePolicy.validateUpload(file, MediaKind.PHOTO)
            }
            assertThrows(MediaValidationException::class.java) {
                MediaFilePolicy.validateRemote(
                    MediaKind.SCAN,
                    "../escape",
                    "scan.pdf",
                    "application/pdf",
                    8,
                    "bad",
                )
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `destination uses content type and cannot carry path separators`() {
        assertEquals(
            "client_1.pdf",
            MediaFilePolicy.destinationName(MediaKind.SCAN, "client:1", "application/pdf"),
        )
        assertThrows(MediaValidationException::class.java) {
            MediaFilePolicy.destinationName(MediaKind.PHOTO, "../photo", "image/jpeg")
        }
    }
}
