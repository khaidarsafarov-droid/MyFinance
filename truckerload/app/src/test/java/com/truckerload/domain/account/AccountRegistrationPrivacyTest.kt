package com.truckerload.domain.account

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.preferences.AuthCredentialsStore
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.ConsentStore
import com.truckerload.data.preferences.RegistrationProgressStore
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.privacy.AesGcmSensitiveFieldCipher
import com.truckerload.data.repository.account.AccountDeletionService
import com.truckerload.data.repository.account.AccountIdentityRepository
import com.truckerload.data.repository.account.CommunityProfileRepository
import com.truckerload.data.repository.account.DriverProfessionalRepository
import com.truckerload.data.repository.account.RegistrationService
import com.truckerload.domain.account.ProfessionalAccess
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AccountRegistrationPrivacyTest {

    private lateinit var db: AppDatabase
    private lateinit var cipher: AesGcmSensitiveFieldCipher
    private lateinit var identity: AccountIdentityRepository
    private lateinit var professional: DriverProfessionalRepository
    private lateinit var community: CommunityProfileRepository
    private lateinit var registration: RegistrationService
    private lateinit var deletion: AccountDeletionService
    private lateinit var progressStore: RegistrationProgressStore
    private lateinit var userProfileStore: UserProfileStore

    private val userId = "user-owner"
    private val now = 1_700_000_000_000L

    @Before
    fun setUp() {
        AuthStore.resetForTests()
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cipher = AesGcmSensitiveFieldCipher(ByteArray(32) { 7 })
        identity = AccountIdentityRepository(db.userAccountDao())
        professional = DriverProfessionalRepository(db.driverProfessionalDao(), cipher)
        community = CommunityProfileRepository(db.communityProfileDao())
        progressStore = RegistrationProgressStore(context).also { it.bindUser(userId, false) }
        userProfileStore = UserProfileStore(context).also { it.bindUser(userId) }
        registration = RegistrationService(
            userId = userId,
            identity = identity,
            professional = professional,
            community = community,
            progressStore = progressStore,
            consentStore = ConsentStore(context),
            userProfileStore = userProfileStore,
            nowMillis = { now },
        )
        deletion = AccountDeletionService(
            context = context,
            userId = userId,
            database = db,
            identity = identity,
            professional = professional,
            community = community,
            cipher = cipher,
            consentStore = ConsentStore(context),
            progressStore = progressStore,
            userProfileStore = userProfileStore,
            authStore = AuthStore(context),
            credentialsStore = AuthCredentialsStore(context),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun successfulRegistrationCreatesSeparatedEntities() = runBlocking {
        val created = registration.completeCredentials(
            phone = "+15551212",
            email = "a@example.com",
            authProvider = "EMAIL",
            displayName = "Ann Driver",
            consents = AccountConsents(tosAccepted = true, analyticsAccepted = false, ageConfirmed = true),
            isVerified = true,
        )
        assertTrue(created.isSuccess)
        assertTrue(registration.completeBasicProfile("Ann Driver", DriverRole.OWNER_OPERATOR, "+15551212").isSuccess)
        registration.completeProfessional(
            companyName = "Solo LLC",
            cdlNumber = "CDL-SECRET-99",
            cdlDocumentUrl = "s3://private/cdl.pdf",
            vehicleType = "Reefer",
            primaryRegion = "TX",
        )
        assertTrue(
            registration.completeCommunity(
                nickname = "RoadAnn",
                avatarUrl = null,
                bio = "nights",
                visibility = CommunityVisibilitySettings(bioVisible = false),
            ).isSuccess,
        )

        val user = identity.get(userId)!!
        assertEquals("a@example.com", user.email)
        assertTrue(user.ageConfirmed)
        assertNotNull(user.acceptedTosAt)
        assertNull(user.analyticsConsentAt)

        val pro = professional.getOwn(userId)!!
        assertEquals("CDL-SECRET-99", pro.cdlNumber)
        assertEquals(DriverRole.OWNER_OPERATOR, pro.role)

        val storedCipher = db.driverProfessionalDao().get(userId)!!.cdlNumberCiphertext
        assertNotNull(storedCipher)
        assertFalse(storedCipher!!.contains("CDL-SECRET-99"))
        assertTrue(storedCipher.startsWith(AesGcmSensitiveFieldCipher.PREFIX_V1))

        val pub = community.getPublic(userId)!!
        assertEquals("RoadAnn", pub.nickname)
        assertNull(pub.bio)
        assertFalse(pub.toString().contains("CDL"))
        val communityRow = db.communityProfileDao().get(userId)!!
        assertFalse(communityRow.nickname.contains("CDL"))
        assertFalse(communityRow.bio.orEmpty().contains("CDL-SECRET"))
    }

    @Test
    fun skipOptionalStepsLeavesReminders() = runBlocking {
        assertTrue(
            registration.completeCredentials(
                phone = null,
                email = "b@example.com",
                authProvider = "EMAIL",
                displayName = "Bob",
                consents = AccountConsents(tosAccepted = true, ageConfirmed = true),
                isVerified = true,
            ).isSuccess,
        )
        assertTrue(registration.completeBasicProfile("Bob", DriverRole.HIRED_DRIVER).isSuccess)
        registration.skipProfessional()
        registration.skipCommunity()
        val progress = registration.progress()
        assertTrue(progress.basicComplete)
        assertTrue(progress.professionalSkipped)
        assertTrue(progress.communitySkipped)
        assertTrue(progress.professionalPending)
        assertTrue(progress.communityPending)
        assertFalse(registration.needsRequiredOnboarding())
        assertTrue(professional.getOwn(userId)!!.skipped)
        assertTrue(community.getOwn(userId)!!.skipped)
    }

    @Test
    fun foreignViewerCannotReadDriverProfile() = runBlocking {
        registration.completeCredentials(
            phone = null,
            email = "owner@example.com",
            authProvider = "EMAIL",
            displayName = "Owner",
            consents = AccountConsents(tosAccepted = true, ageConfirmed = true),
            isVerified = true,
        )
        registration.completeBasicProfile("Owner", DriverRole.OWNER_OPERATOR)
        registration.completeProfessional(
            companyName = "Fleet",
            cdlNumber = "HIDDEN",
            cdlDocumentUrl = null,
            vehicleType = "Van",
            primaryRegion = "CA",
            dispatcherUserId = "dispatcher-1",
        )
        assertTrue(
            professional.getForViewer(userId, "stranger") is ProfessionalAccess.Denied,
        )
        val dispatcher = professional.getForViewer(userId, "dispatcher-1")
        assertTrue(dispatcher is ProfessionalAccess.Allowed)
        assertNull((dispatcher as ProfessionalAccess.Allowed).profile.cdlNumber)
        val owner = professional.getForViewer(userId, userId)
        assertEquals("HIDDEN", (owner as ProfessionalAccess.Allowed).profile.cdlNumber)
    }

    @Test
    fun cascadeDeleteRemovesIdentityAndLoads() = runBlocking {
        registration.completeCredentials(
            phone = null,
            email = "gone@example.com",
            authProvider = "EMAIL",
            displayName = "Gone",
            consents = AccountConsents(tosAccepted = true, ageConfirmed = true),
            isVerified = true,
        )
        registration.completeBasicProfile("Gone", DriverRole.OWNER_OPERATOR)
        registration.completeProfessional("Co", "CDL-1", null, "Van", "FL")
        registration.completeCommunity("nick", null, "bio", CommunityVisibilitySettings())
        db.loadDao().insert(
            LoadEntity(
                id = "load-1",
                tripId = "T-1",
                date = "2026-01-01",
                totalRate = 100.0,
                totalMiles = 50.0,
                pointA = "A",
                pointB = "B",
                puCount = 1,
                delCount = 1,
                weekNumber = 1,
                year = 2026,
                rawMessage = "secret note",
                parsedAt = 1L,
                updatedAt = 1L,
            ),
        )
        deletion.cascadeDeleteLocalTables()
        assertNull(identity.get(userId))
        assertNull(professional.getOwn(userId))
        assertNull(community.getOwn(userId))
        assertTrue(db.loadDao().getAllLoadsOnce().isEmpty())
        assertFalse(registration.progress().basicComplete)
    }

    @Test
    fun credentialsRejectedWithoutAgeAndTos() = runBlocking {
        val noAge = registration.completeCredentials(
            phone = null,
            email = "x@example.com",
            authProvider = "EMAIL",
            displayName = "X",
            consents = AccountConsents(tosAccepted = true, ageConfirmed = false),
            isVerified = true,
        )
        assertTrue(noAge.isFailure)
        val noTos = registration.completeCredentials(
            phone = null,
            email = "x@example.com",
            authProvider = "EMAIL",
            displayName = "X",
            consents = AccountConsents(tosAccepted = false, ageConfirmed = true),
            isVerified = true,
        )
        assertTrue(noTos.isFailure)
    }
}

class AgeGateAndCipherTest {
    @Test
    fun ageGateRequires18() {
        val today = 20_000L
        assertTrue(AgeGate.isAtLeast18(today - (18 * 365) - 10, today))
        assertFalse(AgeGate.isAtLeast18(today - 100, today))
    }

    @Test
    fun aesGcmRoundTripAndPlainPrefix() {
        val cipher = AesGcmSensitiveFieldCipher(ByteArray(32) { 3 })
        val encoded = cipher.encrypt("CDL-123")
        assertTrue(encoded.startsWith(AesGcmSensitiveFieldCipher.PREFIX_V1))
        assertNotEquals("CDL-123", encoded)
        assertEquals("CDL-123", cipher.decrypt(encoded))
        assertEquals("legacy", cipher.decrypt(AesGcmSensitiveFieldCipher.wrapPlaintextForMigration("legacy")))
    }

    @Test
    fun publicCommunityProfileTypeHasNoCdlFields() {
        val names = PublicCommunityProfile::class.java.declaredFields.map { it.name }
        assertFalse(names.any { it.contains("cdl", ignoreCase = true) })
        assertFalse(names.any { it.contains("company", ignoreCase = true) })
    }
}
