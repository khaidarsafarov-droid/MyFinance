package com.truckerload.presentation.screens.attach

import androidx.lifecycle.ViewModel
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.attach.AttachLoadSelection
import com.truckerload.domain.model.Load
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Load queries behind the camera/scanner "attach to a load" picker. */
@HiltViewModel
class AttachLoadPickViewModel @Inject constructor(private val loadRepository: LoadRepository) : ViewModel() {

    suspend fun recentLoadsThisWeek(): List<Load> = withContext(Dispatchers.IO) {
        loadRepository.getRecentLoadsThisWeek(AttachLoadSelection.QUICK_PICK_LIMIT)
    }

    suspend fun hasLoadsToLink(): Boolean = withContext(Dispatchers.IO) {
        loadRepository.getLoadsForLinking(limit = 1).isNotEmpty()
    }

    suspend fun loadsForLinking(): List<Load> = withContext(Dispatchers.IO) {
        loadRepository.getLoadsForLinking(limit = ALL_LIMIT)
    }

    private companion object {
        const val ALL_LIMIT = 200
    }
}
