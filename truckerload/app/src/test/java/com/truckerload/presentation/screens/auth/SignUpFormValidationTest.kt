package com.truckerload.presentation.screens.auth

import com.truckerload.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SignUpFormValidationTest {

    @Test
    fun requiresNameEmailMatchingPassword() {
        assertEquals(
            R.string.auth_error_name_required,
            SignUpFormValidation.errorResId("", "a@b.co", "Abcdefg1", "Abcdefg1"),
        )
        assertEquals(
            R.string.auth_error_email_required,
            SignUpFormValidation.errorResId("Ann", "  ", "Abcdefg1", "Abcdefg1"),
        )
        assertEquals(
            R.string.auth_error_password_short,
            SignUpFormValidation.errorResId("Ann", "a@b.co", "Ab1", "Ab1"),
        )
        assertEquals(
            R.string.auth_error_password_mismatch,
            SignUpFormValidation.errorResId("Ann", "a@b.co", "Abcdefg1", "Abcdefg2"),
        )
        assertNull(
            SignUpFormValidation.errorResId("Ann Driver", "a@b.co", "Abcdefg1", "Abcdefg1"),
        )
    }
}
