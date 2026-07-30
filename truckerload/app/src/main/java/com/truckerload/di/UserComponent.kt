package com.truckerload.di

import android.content.Context
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.RpmThresholdsStore
import com.truckerload.data.preferences.SelectedStateStore
import com.truckerload.data.preferences.StatsSelectionStore
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.preferences.WeeklyProfitGoalStore
import com.truckerload.data.repository.AiRepository
import com.truckerload.data.repository.AnalyticsRepository
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.MaintenanceRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.repository.PhotoRepository
import com.truckerload.data.repository.ScanRepository
import com.truckerload.data.repository.SocialRepository
import com.truckerload.data.repository.VoiceRepository
import com.truckerload.data.repository.WeekRepository
import com.truckerload.data.repository.social.ChatRepository
import com.truckerload.data.repository.social.ChatRepositoryImpl
import com.truckerload.data.repository.social.GroupRepository
import com.truckerload.data.repository.social.GroupRepositoryImpl
import com.truckerload.data.repository.social.MediaRepository
import com.truckerload.data.repository.social.MediaRepositoryImpl
import com.truckerload.data.repository.social.ProfileChallengeHelper
import com.truckerload.data.repository.social.ProfileLocalDataSource
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.data.repository.social.ProfileRepositoryImpl
import com.truckerload.data.repository.social.SocialSyncCoordinator
import com.truckerload.data.repository.social.StatusRepository
import com.truckerload.data.repository.social.StatusRepositoryImpl

/**
 * Account-scoped object graph for one [userId].
 *
 * Not a Hilt `@DefineComponent`: ViewModelComponent cannot parent a custom user
 * component, so [UserComponentManager] owns this graph and [UserAccountModule] /
 * [SocialRepositoryModule] bridge repositories into SingletonComponent for
 * `@HiltViewModel` injection.
 */
@UserScope
@Suppress("DEPRECATION") // SocialRepository facade retained until ViewModels migrate
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
    val analyticsRepository: AnalyticsRepository,
    val photoRepository: PhotoRepository,
    val scanRepository: ScanRepository,
    val mediaRepository: MediaRepository,
    val chatRepository: ChatRepository,
    val groupRepository: GroupRepository,
    val statusRepository: StatusRepository,
    val profileRepository: ProfileRepository,
    val socialSyncCoordinator: SocialSyncCoordinator,
    val socialRepository: SocialRepository,
    val voiceRepository: VoiceRepository,
    val aiRepository: AiRepository,
    val maintenanceRepository: MaintenanceRepository,
) {
    companion object {
        fun create(
            context: Context,
            userId: String,
            userProfileStore: UserProfileStore,
        ): UserComponent {
            val id = userId.trim()
            require(id.isNotBlank()) { "userId required" }
            userProfileStore.bindUser(id)
            val db = AppDatabase.getInstance(context, id)
            val loadRepository = LoadRepository(db)
            val paycheckRepository = PaycheckRepository(db)
            val dieselRepository = DieselRepository(db)
            val mediaRepository = MediaRepositoryImpl(context)
            val chatRepository = ChatRepositoryImpl(db, mediaRepository, context)
            val groupRepository = GroupRepositoryImpl(db, context)
            val statusRepository = StatusRepositoryImpl(db, mediaRepository, context)
            val profileLocal = ProfileLocalDataSource(db, userProfileStore)
            val profileChallenges = ProfileChallengeHelper(
                db = db,
                loadRepository = loadRepository,
                userProfileStore = userProfileStore,
                local = profileLocal,
                context = context,
            )
            val profileRepository = ProfileRepositoryImpl(
                db = db,
                loadRepository = loadRepository,
                userProfileStore = userProfileStore,
                mediaRepository = mediaRepository,
                chatRepository = chatRepository,
                local = profileLocal,
                challenges = profileChallenges,
                context = context,
            )
            val socialSyncCoordinator = SocialSyncCoordinator(
                db = db,
                userProfileStore = userProfileStore,
                profileRepository = profileRepository,
                statusRepository = statusRepository,
            )
            @Suppress("DEPRECATION")
            val socialRepository = SocialRepository(
                profile = profileRepository,
                chat = chatRepository,
                group = groupRepository,
                status = statusRepository,
                media = mediaRepository,
                sync = socialSyncCoordinator,
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
                analyticsRepository = AnalyticsRepository(db),
                photoRepository = PhotoRepository(db),
                scanRepository = ScanRepository(db),
                mediaRepository = mediaRepository,
                chatRepository = chatRepository,
                groupRepository = groupRepository,
                statusRepository = statusRepository,
                profileRepository = profileRepository,
                socialSyncCoordinator = socialSyncCoordinator,
                socialRepository = socialRepository,
                voiceRepository = VoiceRepository(db, context),
                aiRepository = AiRepository(),
                maintenanceRepository = MaintenanceRepository(db),
            )
        }
    }
}
