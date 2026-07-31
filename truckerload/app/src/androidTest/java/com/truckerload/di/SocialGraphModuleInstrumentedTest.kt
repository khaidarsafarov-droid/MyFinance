package com.truckerload.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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

/**
 * Verifies account-scoped social repositories wire correctly through [SocialGraphModule].
 */
@RunWith(AndroidJUnit4::class)
class SocialGraphModuleInstrumentedTest {

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

        val bundle = SocialGraphModule.create(
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
    fun syncCoordinator_ensureInitialized() = runBlocking {
        val userProfileStore = UserProfileStore(context)
        userProfileStore.bindUser(userId)
        val db = AppDatabase.getInstance(context, userId)
        val loadRepository = LoadRepository(db)
        val bundle = SocialGraphModule.create(
            context = context,
            db = db,
            loadRepository = loadRepository,
            userProfileStore = userProfileStore,
        )

        bundle.syncCoordinator.ensureInitialized()
        assertNotNull(bundle.profile.watchMyProfile())
    }
}
