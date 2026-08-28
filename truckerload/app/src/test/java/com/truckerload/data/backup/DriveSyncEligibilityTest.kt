package com.truckerload.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveSyncEligibilityTest {
    @Test
    fun rejectsBlank() {
        assertFalse(DriveSyncEligibility.shouldEnqueuePeriodic(null))
        assertFalse(DriveSyncEligibility.shouldEnqueuePeriodic(""))
        assertFalse(DriveSyncEligibility.shouldEnqueuePeriodic("   "))
    }

    @Test
    fun acceptsLocalAndCloudAccounts() {
        assertTrue(DriveSyncEligibility.shouldEnqueuePeriodic("local_dev"))
        assertTrue(DriveSyncEligibility.shouldEnqueuePeriodic("google_abc123"))
        assertTrue(DriveSyncEligibility.shouldEnqueuePeriodic("local_email_hash"))
        assertTrue(DriveSyncEligibility.shouldEnqueuePeriodic("uuid-from-supabase"))
    }
}
