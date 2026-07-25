package com.truckerload.data.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaCloudPolicyTest {
    @Test
    fun `gate requires flag url and non-local account but queues while token refreshes`() {
        fun enabled(
            flag: Boolean = true,
            url: String = "https://api.example.com",
            local: Boolean = false,
            user: String? = "11111111-1111-4111-8111-111111111111",
        ) = MediaCloudPolicy.enabled(flag, url, local, user)

        assertTrue(enabled())
        assertFalse(enabled(flag = false))
        assertFalse(enabled(url = ""))
        assertFalse(enabled(local = true))
        assertFalse(enabled(user = null))
        assertFalse(enabled(user = "local_dev"))
        assertFalse(enabled(user = "local_email_hash"))
        assertFalse(enabled(user = "google_subject_hash"))
    }
}
