package com.truckerload.di

import javax.inject.Qualifier
import javax.inject.Scope

/**
 * Lifetime of one logged-in account: DB file, repositories, per-user stores.
 * Created on login / account switch; destroyed on logout.
 */
@Scope
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class UserScope

/** Active account id bound into [UserComponent]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserId
