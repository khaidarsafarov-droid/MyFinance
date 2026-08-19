package com.truckerload.di

import android.content.Context
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.LastUsedDefaultsStore
import com.truckerload.data.preferences.RpmThresholdsStore
import com.truckerload.data.preferences.SelectedStateStore
import com.truckerload.data.preferences.StatsSelectionStore
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.preferences.WeeklyProfitGoalStore
import com.truckerload.data.remote.ktor.HttpClientProvider
import com.truckerload.data.repository.AiRepository
import com.truckerload.data.repository.AnalyticsRepository
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.MaintenanceRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.repository.PhotoRepository
import com.truckerload.data.repository.ScanRepository
import com.truckerload.data.repository.VoiceRepository
import com.truckerload.data.voice.VoiceTokenClient
import com.truckerload.data.repository.WeekRepository
import com.truckerload.data.repository.social.ChatRepository
import com.truckerload.data.repository.social.GroupRepository
import com.truckerload.data.repository.social.MediaRepository
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.data.repository.social.SocialSyncCoordinator
import com.truckerload.data.repository.social.StatusRepository

/**
 * Account-scoped object graph for one [userId].
 *
 * Not a Hilt `@DefineComponent`: ViewModelComponent cannot parent a custom user
 * component, so [UserComponentManager] owns this graph and [UserAccountModule]
 * bridges repositories into SingletonComponent for `@HiltViewModel` injection.
 */
@UserScope
class UserComponent private constructor(
    @UserId val userId: String,
    val database: AppDatabase,
    val loadRepository: LoadRepository,
    val paycheckRepository: PaycheckRepository,
    val dieselRepository: DieselRepository,
    val weekRepository: WeekRepository,
    val rpmThresholdsStore: RpmThresholdsStore,
    val selectedStateStore: SelectedStateStore,
    val statsSelectionStore: StatsSelectionStore,
    val weeklyProfitGoalStore: WeeklyProfitGoalStore,
    val lastUsedDefaultsStore: LastUsedDefaultsStore,
    val analyticsRepository: AnalyticsRepository,
    val photoRepository: PhotoRepository,
    val scanRepository: ScanRepository,
    val profileRepository: ProfileRepository,
    val chatRepository: ChatRepository,
    val groupRepository: GroupRepository,
    val statusRepository: StatusRepository,
    val mediaRepository: MediaRepository,
    val socialSyncCoordinator: SocialSyncCoordinator,
    val voiceRepository: VoiceRepository,
    val aiRepository: AiRepository,
    val maintenanceRepository: MaintenanceRepository,
) {
    companion object {
        fun create(
            context: Context,
            userId: String,
            userProfileStore: UserProfileStore,
            httpClientProvider: HttpClientProvider? = null,
        ): UserComponent {
            val id = userId.trim()
            require(id.isNotBlank()) { "userId required" }
            userProfileStore.bindUser(id)
            val db = AppDatabase.getInstance(context, id)
            val loadRepository = LoadRepository(db)
            val paycheckRepository = PaycheckRepository(db)
            val dieselRepository = DieselRepository(db)
            val social = SocialGraphModule.create(
                context = context,
                db = db,
                loadRepository = loadRepository,
                userProfileStore = userProfileStore,
            )
            return UserComponent(
                userId = id,
                database = db,
                loadRepository = loadRepository,
                paycheckRepository = paycheckRepository,
                dieselRepository = dieselRepository,
                weekRepository = WeekRepository(loadRepository, paycheckRepository, dieselRepository),
                rpmThresholdsStore = RpmThresholdsStore(context, id),
                selectedStateStore = SelectedStateStore(context, id),
                statsSelectionStore = StatsSelectionStore(context, id),
                weeklyProfitGoalStore = WeeklyProfitGoalStore(context, id),
                lastUsedDefaultsStore = LastUsedDefaultsStore(context, id),
                analyticsRepository = AnalyticsRepository(db),
                photoRepository = PhotoRepository(db),
                scanRepository = ScanRepository(db),
                profileRepository = social.profile,
                chatRepository = social.chat,
                groupRepository = social.group,
                statusRepository = social.status,
                mediaRepository = social.media,
                socialSyncCoordinator = social.syncCoordinator,
                voiceRepository = VoiceRepository(
                    db,
                    context,
                    social.voiceRemote,
                    social.actorId,
                    httpClientProvider?.let(::VoiceTokenClient),
                ),
                aiRepository = AiRepository(),
                maintenanceRepository = MaintenanceRepository(db),
            )
        }
    }
}
