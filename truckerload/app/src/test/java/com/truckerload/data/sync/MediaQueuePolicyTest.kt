package com.truckerload.data.sync

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaQueuePolicyTest {
    @Test
    fun `io and transient http return to pending with bounded safe errors`() {
        val io = MediaQueuePolicy.afterFailure(2, IOException("private path must not escape"))
        assertTrue(io.retry)
        assertEquals("PENDING", io.status)
        assertEquals(3, io.attempts)
        assertEquals("io_error", io.safeError)

        val http = MediaQueuePolicy.afterFailure(
            0,
            MediaCloudException("http_503", httpStatus = 503, retryable = true),
        )
        assertTrue(http.retry)
        assertEquals("http_503", http.safeError)
    }

    @Test
    fun `validation and permanent http failures stop retrying`() {
        val validation = MediaQueuePolicy.afterFailure(0, MediaValidationException("invalid_content"))
        assertFalse(validation.retry)
        assertEquals("FAILED", validation.status)

        val notFound = MediaQueuePolicy.afterFailure(
            1,
            MediaCloudException("http_404", httpStatus = 404, retryable = false),
        )
        assertFalse(notFound.retry)
        assertEquals(2, notFound.attempts)
    }
}
