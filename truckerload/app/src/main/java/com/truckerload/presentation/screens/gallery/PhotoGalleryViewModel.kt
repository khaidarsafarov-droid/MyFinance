package com.truckerload.presentation.screens.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PhotoRepository
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.formatLoadRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class PhotoGalleryFilter {
    ALL,
    TODAY,
    THIS_WEEK,
    BY_LOAD,
}

data class PhotoGalleryUiState(
    val photos: List<PhotoEntity> = emptyList(),
    val loads: List<Load> = emptyList(),
    val loadLabels: Map<String, String> = emptyMap(),
    val filter: PhotoGalleryFilter = PhotoGalleryFilter.ALL,
    val selectedLoadId: String? = null,
    val isLoadingLoads: Boolean = true,
)

class PhotoGalleryViewModel(
    private val photoRepository: PhotoRepository,
    private val loadRepository: LoadRepository,
) : ViewModel() {

    private val filterState = MutableStateFlow(
        PhotoGalleryFilterState(PhotoGalleryFilter.ALL, null),
    )

    private val _loads = MutableStateFlow<List<Load>>(emptyList())
    val loadsForLinking: StateFlow<List<Load>> = _loads

    val uiState: StateFlow<PhotoGalleryUiState> = combine(
        photoRepository.watchPhotos(),
        filterState,
        _loads,
    ) { photos, filter, loads ->
        val loadLabels = loads.associate { it.id to formatLoadRoute(it) }
        val filtered = applyFilter(photos, filter)
        PhotoGalleryUiState(
            photos = filtered,
            loads = loads,
            loadLabels = loadLabels,
            filter = filter.mode,
            selectedLoadId = filter.loadId,
            isLoadingLoads = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhotoGalleryUiState())

    init {
        viewModelScope.launch {
            _loads.value = loadRepository.getLoadsForLinking()
        }
    }

    fun setFilter(mode: PhotoGalleryFilter) {
        filterState.update {
            PhotoGalleryFilterState(
                mode = mode,
                loadId = if (mode == PhotoGalleryFilter.BY_LOAD) it.loadId else null,
            )
        }
    }

    fun setLoadFilter(loadId: String?) {
        filterState.update {
            PhotoGalleryFilterState(mode = PhotoGalleryFilter.BY_LOAD, loadId = loadId)
        }
    }

    fun loadLabel(loadId: String?): String? {
        if (loadId.isNullOrBlank()) return null
        return _loads.value.find { it.id == loadId }?.let { formatLoadRoute(it) }
    }

    private fun applyFilter(photos: List<PhotoEntity>, filter: PhotoGalleryFilterState): List<PhotoEntity> {
        val now = Calendar.getInstance()
        return when (filter.mode) {
            PhotoGalleryFilter.ALL -> photos
            PhotoGalleryFilter.TODAY -> {
                val start = startOfDay(now)
                val end = endOfDay(now)
                photos.filter { it.timestamp in start..end }
            }
            PhotoGalleryFilter.THIS_WEEK -> {
                val weekStart = startOfWeek(now)
                photos.filter { it.timestamp >= weekStart }
            }
            PhotoGalleryFilter.BY_LOAD -> {
                val id = filter.loadId
                if (id.isNullOrBlank()) photos.filter { it.loadId.isNullOrBlank() }
                else photos.filter { it.loadId == id }
            }
        }
    }

    private fun startOfDay(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun endOfDay(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }

    private fun startOfWeek(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.DAY_OF_WEEK, c.firstDayOfWeek)
        return startOfDay(c)
    }

    private data class PhotoGalleryFilterState(
        val mode: PhotoGalleryFilter,
        val loadId: String?,
    )

    class Factory(
        private val photoRepository: PhotoRepository,
        private val loadRepository: LoadRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PhotoGalleryViewModel(photoRepository, loadRepository) as T
        }
    }
}
