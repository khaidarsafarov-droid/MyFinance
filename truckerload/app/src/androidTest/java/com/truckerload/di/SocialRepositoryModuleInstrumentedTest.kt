package com.truckerload.di

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.data.repository.social.ProfileRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.core.app.ApplicationProvider

/**
 * Verifies account-scoped social repositories wire correctly through [SocialRepositoryModule].
 */
@RunWith(AndroidJUnit4::class)
class SocialRepositoryModuleInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val userId = "social_repo_module_test"

    @Before
    fun setUp() {
        AppDatabase.closeCurrent()
        context.getDatabasePath(AppDatabase.databaseNameFor(userId)).delete()
    }

    @After
    fun tearDown() {
        AppDatabase.closeCurrent()
        context.getDatabasePath(AppDatabase.databaseNameFor(userId)).delete()
    }

    @Test
    fun profileRepository_isCreatedAndInjectable() {
        val userProfileStore = UserProfileStore(context)
        userProfileStore.bindUser(userId)
        val db = AppDatabase.getInstance(context, userId)
        val loadRepository = LoadRepository(db)

        val bundle = SocialRepositoryModule.create(
            context = context,
            db = db,
            loadRepository = loadRepository,
            userProfileStore = userProfileStore,
        )

        val profile: ProfileRepository = bundle.profile
        assertNotNull(profile)
        assertTrue(profile is ProfileRepositoryImpl)
    }

    @Test
    fun facade_delegatesEnsureInitialized() = runBlocking {
        val userProfileStore = UserProfileStore(context)
        userProfileStore.bindUser(userId)
        val db = AppDatabase.getInstance(context, userId)
        val loadRepository = LoadRepository(db)
        val bundle = SocialRepositoryModule.create(
            context = context,
            db = db,
            loadRepository = loadRepository,
            userProfileStore = userProfileStore,
        )

        bundle.facade.ensureInitialized()
        assertNotNull(bundle.profile.watchMyProfile())
    }
}
