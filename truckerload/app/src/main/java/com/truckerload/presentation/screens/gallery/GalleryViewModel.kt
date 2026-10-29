package com.truckerload.presentation.screens.gallery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PhotoRepository
import com.truckerload.data.repository.ScanRepository
import com.truckerload.presentation.ScreenPhase
import com.truckerload.presentation.reloadList
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class GalleryUiState(
    val tab: GalleryKind = GalleryKind.PHOTOS,
    val filter: GalleryFilter = GalleryFilter.ALL,
    val query: String = "",
    val items: List<GalleryMediaRef> = emptyList(),
    val loads: List<GalleryLoadInfo> = emptyList(),
    val phase: ScreenPhase = ScreenPhase.Loading,
)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    photoRepository: PhotoRepository,
    scanRepository: ScanRepository,
    loadRepository: LoadRepository,
) : ViewModel() {

    private val controls = MutableStateFlow(
        GalleryControls(tab = GalleryKind.fromRoute(savedStateHandle.get<String>("tab"))),
    )
    private var appliedInitialTab = false
    private val reload = MutableStateFlow(0)
    private val photos = reload.reloadList { photoRepository.watchPhotos() }
        .map { result -> result.map { rows -> rows.map { it.asGalleryMedia() } } }
    private val scans = reload.reloadList { scanRepository.watchScans() }
        .map { result -> result.map { rows -> rows.map { it.asGalleryMedia() } } }
    private val loads = loadRepository.watchLoads()
        .map { list -> list.map { it.toGalleryLoadInfo() } }
        .catch { emit(emptyList()) }

    val uiState: StateFlow<GalleryUiState> = combine(
        photos,
        scans,
        loads,
        controls,
    ) { photoResult, scanResult, loadInfos, control ->
        val source = if (control.tab == GalleryKind.PHOTOS) photoResult else scanResult
        GalleryUiState(
            tab = control.tab,
            filter = control.filter,
            query = control.query,
            items = source.getOrDefault(emptyList()),
            loads = loadInfos,
            phase = if (source.isFailure) ScreenPhase.Error else ScreenPhase.Content,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        GalleryUiState(tab = controls.value.tab, phase = ScreenPhase.Loading),
    )

    /** Legacy routes have no `{tab}` arg. Apply once, before the first content frame. */
    fun ensureInitialTab(tab: GalleryKind) {
        if (appliedInitialTab) return
        appliedInitialTab = true
        controls.update { it.copy(tab = tab) }
    }

    fun retry() {
        reload.value += 1
    }

    fun setTab(tab: GalleryKind) {
        appliedInitialTab = true
        controls.update { it.copy(tab = tab) }
    }

    fun setFilter(filter: GalleryFilter) {
        controls.update { current ->
            current.copy(
                filter = filter,
                query = if (filter == GalleryFilter.BY_LOAD) current.query else "",
            )
        }
    }

    fun setQuery(query: String) {
        controls.update { it.copy(query = query, filter = GalleryFilter.BY_LOAD) }
    }
}
