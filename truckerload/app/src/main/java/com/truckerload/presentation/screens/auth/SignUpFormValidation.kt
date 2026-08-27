package com.truckerload.presentation.screens.auth

import com.truckerload.R
import com.truckerload.data.auth.PasswordPolicy

object SignUpFormValidation {
    fun errorResId(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): Int? {
        if (name.isBlank()) return R.string.auth_error_name_required
        if (email.isBlank()) return R.string.auth_error_email_required
        val policy = PasswordPolicy.validate(password)
        if (!policy.ok) return policy.errorResId
        if (password != confirmPassword) return R.string.auth_error_password_mismatch
        return null
    }
}
