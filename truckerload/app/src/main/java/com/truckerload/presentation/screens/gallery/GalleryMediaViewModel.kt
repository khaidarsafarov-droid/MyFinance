package com.truckerload.presentation.screens.gallery

import android.net.Uri
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GalleryMediaUiState(
    val kind: GalleryKind = GalleryKind.PHOTOS,
    val loadKey: String = GalleryCatalog.UNLINKED,
    val title: String = "",
    val route: String = "",
    val items: List<GalleryMediaRef> = emptyList(),
    val focusId: String? = null,
    val unlinked: Boolean = false,
    val phase: ScreenPhase = ScreenPhase.Loading,
)

/**
 * One load's photos or scans, shared by the grid and the fullscreen viewer.
 * A photo or scan id (from a load card) resolves to that file's siblings.
 */
@HiltViewModel
class GalleryMediaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoRepository: PhotoRepository,
    private val scanRepository: ScanRepository,
    loadRepository: LoadRepository,
) : ViewModel() {

    private val explicitKind = GalleryKind.fromRoute(arg(savedStateHandle, "kind"))
    private val explicitLoadKey = arg(savedStateHandle, "loadKey")
    private val photoId = arg(savedStateHandle, "photoId")
    private val scanId = arg(savedStateHandle, "scanId")
    private val startId = arg(savedStateHandle, "startId")
    private val reload = MutableStateFlow(0)
    private val photos = reload.reloadList { photoRepository.watchPhotos() }
        .map { result -> result.map { rows -> rows.map { it.asGalleryMedia() } } }
    private val scans = reload.reloadList { scanRepository.watchScans() }
        .map { result -> result.map { rows -> rows.map { it.asGalleryMedia() } } }
    private val loads = loadRepository.watchLoads()
        .map { list -> list.map { it.toGalleryLoadInfo() } }
        .catch { emit(emptyList()) }

    val uiState: StateFlow<GalleryMediaUiState> = combine(photos, scans, loads) { photoResult, scanResult, loadInfos ->
        val kind = when {
            photoId != null -> GalleryKind.PHOTOS
            scanId != null -> GalleryKind.SCANS
            else -> explicitKind
        }
        val source = if (kind == GalleryKind.PHOTOS) photoResult else scanResult
        if (source.isFailure) {
            return@combine GalleryMediaUiState(kind = kind, phase = ScreenPhase.Error)
        }
        val media = source.getOrDefault(emptyList())
        val known = loadInfos.mapTo(HashSet()) { it.id }
        val focus = photoId ?: scanId ?: startId
        val key = when {
            photoId != null -> media.find { it.id == photoId }?.let { GalleryCatalog.groupKey(it, known) }
                ?: GalleryCatalog.UNLINKED
            scanId != null -> media.find { it.id == scanId }?.let { GalleryCatalog.groupKey(it, known) }
                ?: GalleryCatalog.UNLINKED
            else -> explicitLoadKey ?: GalleryCatalog.UNLINKED
        }
        val items = GalleryCatalog.itemsForLoad(media, key, known)
        val info = loadInfos.find { it.id == key }
        val missingFocus = focus != null && media.none { it.id == focus } && explicitLoadKey == null
        GalleryMediaUiState(
            kind = kind,
            loadKey = key,
            title = info?.tripId?.takeIf { it.isNotBlank() } ?: if (key == GalleryCatalog.UNLINKED) "" else key.take(8),
            route = info?.let { GalleryCatalog.displayRoute(it.origin, it.destination, it.route) }.orEmpty(),
            items = items,
            focusId = focus,
            unlinked = key == GalleryCatalog.UNLINKED,
            phase = when {
                missingFocus || items.isEmpty() -> ScreenPhase.Empty
                else -> ScreenPhase.Content
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GalleryMediaUiState())

    fun retry() {
        reload.value += 1
    }

    fun delete(id: String, kind: GalleryKind) {
        viewModelScope.launch(Dispatchers.IO) {
            when (kind) {
                GalleryKind.PHOTOS -> photoRepository.deletePhoto(id)
                GalleryKind.SCANS -> scanRepository.deleteScan(id)
            }
        }
    }

    private fun arg(handle: SavedStateHandle, key: String): String? {
        val raw = handle.get<String>(key) ?: return null
        val decoded = Uri.decode(raw)
        if (decoded.isBlank() || decoded == "_") return null
        return decoded
    }
}
