package com.truckerload.data.backup

import android.app.Activity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveConnectInterpreterTest {

    @Test
    fun accountPickerSuccessWithoutDrive_requestsConsent() {
        val next = DriveConnectInterpreter.next(
            resultCode = Activity.RESULT_OK,
            accountEmail = "driver@example.com",
            grantedDriveScope = false,
            error = null,
            pending = DriveConnectPending.AccountPicker,
        )
        assertEquals(DriveConnectOutcome.RequestDriveConsent("driver@example.com"), next)
    }

    @Test
    fun driveConsentGranted_linksAccount() {
        val next = DriveConnectInterpreter.next(
            resultCode = Activity.RESULT_OK,
            accountEmail = "driver@example.com",
            grantedDriveScope = true,
            error = null,
            pending = DriveConnectPending.DriveConsent,
        )
        assertEquals(DriveConnectOutcome.Granted, next)
    }

    @Test
    fun driveConsentDenied_failsInsteadOfLooping() {
        val next = DriveConnectInterpreter.next(
            resultCode = Activity.RESULT_OK,
            accountEmail = "driver@example.com",
            grantedDriveScope = false,
            error = null,
            pending = DriveConnectPending.DriveConsent,
        )
        assertTrue(next is DriveConnectOutcome.Failed)
    }

    @Test
    fun canceledPickerWithoutApiError_isCancelled() {
        val next = DriveConnectInterpreter.next(
            resultCode = Activity.RESULT_CANCELED,
            accountEmail = null,
            grantedDriveScope = false,
            error = null,
            pending = DriveConnectPending.AccountPicker,
        )
        assertEquals(DriveConnectOutcome.Cancelled, next)
    }

    @Test
    fun canceledPickerWithDeveloperError_isFailed() {
        val err = ApiException(Status(CommonStatusCodes.DEVELOPER_ERROR))
        val next = DriveConnectInterpreter.next(
            resultCode = Activity.RESULT_CANCELED,
            accountEmail = null,
            grantedDriveScope = false,
            error = err,
            pending = DriveConnectPending.AccountPicker,
        )
        assertEquals(DriveConnectOutcome.Failed(err), next)
    }

    @Test
    fun canceledPickerWithSignInCancelled_isCancelled() {
        val err = ApiException(Status(12501))
        val next = DriveConnectInterpreter.next(
            resultCode = Activity.RESULT_CANCELED,
            accountEmail = null,
            grantedDriveScope = false,
            error = err,
            pending = DriveConnectPending.AccountPicker,
        )
        assertEquals(DriveConnectOutcome.Cancelled, next)
    }

    @Test
    fun tokenConsentOk_retriesBackup() {
        val next = DriveConnectInterpreter.next(
            resultCode = Activity.RESULT_OK,
            accountEmail = null,
            grantedDriveScope = false,
            error = null,
            pending = DriveConnectPending.TokenConsent,
        )
        assertEquals(DriveConnectOutcome.RetryBackup, next)
    }

    @Test
    fun trimsAccountEmailBeforeConsent() {
        val next = DriveConnectInterpreter.next(
            resultCode = Activity.RESULT_OK,
            accountEmail = "  driver@example.com  ",
            grantedDriveScope = false,
            error = null,
            pending = DriveConnectPending.AccountPicker,
        )
        assertEquals(DriveConnectOutcome.RequestDriveConsent("driver@example.com"), next)
    }
}
