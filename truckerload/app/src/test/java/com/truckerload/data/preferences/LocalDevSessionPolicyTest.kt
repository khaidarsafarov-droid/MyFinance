package com.truckerload.data.preferences

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDevSessionPolicyTest {

    @Test
    fun localDevSessionRejectedOnlyWhenNotLocalOnlyMode() {
        assertTrue(LocalDevSessionPolicy.shouldRejectLocalDevSession("local_dev", localOnlyMode = false))
        assertFalse(LocalDevSessionPolicy.shouldRejectLocalDevSession("local_dev", localOnlyMode = true))
        assertFalse(LocalDevSessionPolicy.shouldRejectLocalDevSession("google_abc", localOnlyMode = false))
    }
}
