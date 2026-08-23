package com.truckerload.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveSyncEligibilityTest {
    @Test
    fun rejectsGuestAndBlank() {
        assertFalse(DriveSyncEligibility.shouldEnqueuePeriodic(null))
        assertFalse(DriveSyncEligibility.shouldEnqueuePeriodic(""))
        assertFalse(DriveSyncEligibility.shouldEnqueuePeriodic("local_dev"))
    }

    @Test
    fun acceptsGoogleAndEmailAccounts() {
        assertTrue(DriveSyncEligibility.shouldEnqueuePeriodic("google_abc123"))
        assertTrue(DriveSyncEligibility.shouldEnqueuePeriodic("local_email_hash"))
        assertTrue(DriveSyncEligibility.shouldEnqueuePeriodic("uuid-from-supabase"))
    }
}
