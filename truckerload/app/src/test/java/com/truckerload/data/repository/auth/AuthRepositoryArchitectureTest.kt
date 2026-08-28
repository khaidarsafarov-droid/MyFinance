package com.truckerload.data.repository.auth

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryArchitectureTest {

    @Test
    fun module_bindsAuthRepository() {
        val source = readMainSource("com/truckerload/di/AuthRepositoryModule.kt")
        assertTrue(source.contains("@InstallIn(SingletonComponent::class)"))
        assertTrue(source.contains("AuthRepositoryImpl"))
        assertTrue(source.contains("AuthRepository"))
    }

    @Test
    fun firstRunScreen_delegatesToFirstRunViewModel() {
        val source = readMainSource("com/truckerload/presentation/screens/auth/FirstRunNameScreen.kt")
        assertTrue(source.contains("FirstRunViewModel"))
        assertTrue(source.contains("hiltViewModel"))
        assertTrue(!source.contains("SupabaseAuthService"))
        assertTrue(!source.contains("GoogleSignInButton"))
        assertTrue(source.contains("first_run_title"))
        assertTrue(source.contains("common_save"))
        val host = readMainSource("com/truckerload/presentation/navigation/AuthNavHost.kt")
        assertTrue(host.contains("FirstRunNameScreen"))
        assertTrue(!host.contains("LoginScreen"))
        assertTrue(!host.contains("SignUpScreen"))
    }

    @Test
    fun cloudLogin_registersDeviceSlotBeforeKeepingSession() {
        val auth = readMainSource("com/truckerload/data/repository/auth/AuthRepositoryImpl.kt")
        assertTrue(auth.contains("DeviceSlotLogin.beforeSessionPersisted"))
        val binder = readMainSource("com/truckerload/data/sync/DeviceSlotBinder.kt")
        assertTrue(binder.contains("registerWithAccessToken"))
        val engine = readMainSource("com/truckerload/data/sync/CloudSyncEngine.kt")
        assertTrue(engine.contains("DEVICE_SLOT_DENIED"))
        assertTrue(engine.contains("registerCurrentDevice"))
    }

    @Test
    fun emailOfflineFallback_reusesBoundUserId() {
        val auth = readMainSource("com/truckerload/data/repository/auth/AuthRepositoryImpl.kt")
        assertTrue(auth.contains("boundUserIdFor"))
        assertTrue(auth.contains("saveBoundUserId"))
        val store = readMainSource("com/truckerload/data/preferences/AuthStore.kt")
        assertTrue(store.contains("EmailAccountUnifier.relocateLocalEmailJournal"))
        val diesel = readMainSource("com/truckerload/presentation/screens/add/AddDieselViewModel.kt")
        assertTrue(diesel.contains("applyUtcDatePickerDay"))
        val paycheck = readMainSource("com/truckerload/presentation/screens/add/AddPaycheckViewModel.kt")
        assertTrue(paycheck.contains("applyUtcDatePickerDay"))
        assertTrue(paycheck.contains("getPaycheckForWeek"))
        assertTrue(auth.contains("isOfflineAuthFailure"))
        assertTrue(auth.contains("IOException"))
    }

    @Test
    fun googleSignIn_passesIdTokenThroughCompleteLogin() {
        val auth = readMainSource("com/truckerload/data/repository/auth/AuthRepositoryImpl.kt")
        assertTrue(auth.contains("googleIdToken = idToken"))
        assertTrue(auth.contains("googleIdToken = googleIdToken"))
        val login = readMainSource("com/truckerload/data/preferences/AuthLogin.kt")
        assertTrue(login.contains("googleIdToken: String?"))
        assertTrue(login.contains("googleIdToken = googleIdToken"))
    }

    private fun readMainSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/$relativePath"),
            File("app/src/main/java/$relativePath"),
            File("../app/src/main/java/$relativePath"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Main source not found: $relativePath")
    }
}
