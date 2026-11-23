package com.truckerload.domain.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharePayloadPolicyTest {

    @Test
    fun allowsOnlyTextPdfAndImages() {
        assertTrue(SharePayloadPolicy.isAllowedMime("text/plain"))
        assertTrue(SharePayloadPolicy.isAllowedMime("application/pdf"))
        assertTrue(SharePayloadPolicy.isAllowedMime("image/jpeg"))
        assertTrue(SharePayloadPolicy.isAllowedMime("image/*"))
        assertFalse(SharePayloadPolicy.isAllowedMime("video/mp4"))
        assertFalse(SharePayloadPolicy.isAllowedMime("application/octet-stream"))
        assertFalse(SharePayloadPolicy.isAllowedMime(null))
    }

    @Test
    fun displayLabelDropsPathsAndTraversal() {
        assertEquals("rate.pdf", SharePayloadPolicy.displayLabel("../../etc/passwd/rate.pdf"))
        assertEquals("system32", SharePayloadPolicy.displayLabel("..\\..\\windows\\system32"))
        assertEquals("shared", SharePayloadPolicy.displayLabel(".."))
        assertEquals("shared", SharePayloadPolicy.displayLabel(null))
        assertFalse(SharePayloadPolicy.displayLabel("a/../../b.pdf").contains("/"))
        assertFalse(SharePayloadPolicy.displayLabel("a/../../b.pdf").contains(".."))
    }

    @Test
    fun storageNameIgnoresTheSender() {
        assertEquals("share-0", SharePayloadPolicy.storageStem(0))
        assertEquals("pdf", SharePayloadPolicy.extensionFor("application/pdf"))
        assertEquals("jpg", SharePayloadPolicy.extensionFor("image/jpeg"))
    }

    @Test
    fun magicBytesMustAgreeWithTheDeclaredType() {
        assertTrue(SharePayloadPolicy.mimeAgrees("application/pdf", SharePayloadPolicy.sniff("%PDF-1.7".toByteArray())))
        assertTrue(
            SharePayloadPolicy.mimeAgrees(
                "image/jpeg",
                SharePayloadPolicy.sniff(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())),
            ),
        )
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0)
        assertEquals(SharePayloadPolicy.Sniffed.PNG, SharePayloadPolicy.sniff(png))
        assertFalse(SharePayloadPolicy.mimeAgrees("text/plain", SharePayloadPolicy.sniff(png)))
        assertFalse(SharePayloadPolicy.mimeAgrees("image/jpeg", SharePayloadPolicy.sniff("%PDF-1.7".toByteArray())))
        assertEquals(
            SharePayloadPolicy.Sniffed.TEXT,
            SharePayloadPolicy.sniff("Trip ID: T-1\n".toByteArray()),
        )
    }

    @Test
    fun subjectIsKeptInFrontOfTheBody() {
        val text = ShareTextBundle.combine(
            subject = "Rate confirmation 99881",
            extraText = null,
            clipText = "see attached",
        )
        assertTrue(text.startsWith("Subject: Rate confirmation 99881"))
        assertTrue(text.contains("see attached"))
    }

    @Test
    fun sizeCapsStaySmallEnoughToCopy() {
        assertTrue(SharePayloadPolicy.MAX_FILE_BYTES < SharePayloadPolicy.MAX_TOTAL_BYTES)
        assertTrue(SharePayloadPolicy.MAX_FILES in 1..20)
        assertTrue(SharePayloadPolicy.MAX_FILE_BYTES <= 32L * 1024 * 1024)
    }
}
