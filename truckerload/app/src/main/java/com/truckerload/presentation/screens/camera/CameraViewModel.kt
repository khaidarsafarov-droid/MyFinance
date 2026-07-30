package com.truckerload.presentation.screens.camera

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PhotoRepository
import com.truckerload.domain.model.effectiveFinishDate
import com.truckerload.utils.LocationData
import com.truckerload.utils.LocationHelper
import com.truckerload.utils.PhotoManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class CapturedPhoto(
    val file: File,
    val locationData: LocationData,
    val timestamp: Long,
    val savedToDb: Boolean = false,
    val dbId: String? = null,
)

data class CameraUiState(
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val sessionPhotos: List<CapturedPhoto> = emptyList(),
    val reviewingBatch: Boolean = false,
    val saveSuccess: Boolean = false,
    /** Trip ID used for watermark / filenames (from load card or latest load). */
    val watermarkTripId: String? = null,
    val confirmDiscardAttached: Boolean = false,
)

/** Resolves which load a camera session should attach to (pure for unit tests). */
data class CameraAttachContext(
    val loadId: String?,
    val tripId: String?,
    val loadDate: String?,
) {
    companion object {
        fun fromExplicit(
            attachLoadId: String?,
            attachTripId: String?,
            attachLoadDate: String?,
        ): CameraAttachContext? {
            if (attachLoadId.isNullOrBlank() && attachTripId.isNullOrBlank()) return null
            return CameraAttachContext(
                loadId = attachLoadId?.takeIf { it.isNotBlank() },
                tripId = attachTripId?.takeIf { it.isNotBlank() },
                loadDate = attachLoadDate?.takeIf { it.isNotBlank() },
            )
        }

        fun fromLatestLoad(
            loadId: String?,
            tripId: String?,
            loadDate: String?,
        ): CameraAttachContext = CameraAttachContext(
            loadId = loadId?.takeIf { it.isNotBlank() },
            tripId = tripId?.takeIf { it.isNotBlank() },
            loadDate = loadDate?.takeIf { it.isNotBlank() },
        )
    }
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val app: Application,
    private val photoRepository: PhotoRepository,
    private val loadRepository: LoadRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val attachLoadId = savedStateHandle.get<String>("loadId")
        ?.let { Uri.decode(it) }
        ?.takeIf { it.isNotBlank() && it != "_" }
    private val attachTripId = savedStateHandle.get<String>("tripId")
        ?.let { Uri.decode(it) }
        ?.takeIf { it.isNotBlank() && it != "_" }
    private val attachLoadDate = savedStateHandle.get<String>("loadDate")
        ?.let { Uri.decode(it) }
        ?.takeIf { it.isNotBlank() }

    private val locationHelper = LocationHelper(app)
    private val photoManager = PhotoManager(app)
    private val attachContext = CompletableDeferred<CameraAttachContext>()
    @Volatile
    private var resolvedAttach: CameraAttachContext? = null

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    val isAttachedToLoad: Boolean
        get() = !(resolvedAttach?.loadId ?: attachLoadId).isNullOrBlank()

    init {
        viewModelScope.launch {
            val ctx = resolveAttachContext()
            resolvedAttach = ctx
            attachContext.complete(ctx)
            _uiState.update { it.copy(watermarkTripId = ctx.tripId) }
        }
    }

    private suspend fun resolveAttachContext(): CameraAttachContext {
        CameraAttachContext.fromExplicit(attachLoadId, attachTripId, attachLoadDate)?.let { return it }
        // Free camera (drawer / Routes.CAMERA): watermark with latest Trip ID only —
        // do not auto-attach. Widget camera goes through attach_pick → CAMERA_FOR_LOAD.
        val latest = runCatching { loadRepository.getAllLoadsOnce().firstOrNull() }.getOrNull()
        return CameraAttachContext.fromLatestLoad(
            loadId = null,
            tripId = latest?.tripId,
            loadDate = latest?.effectiveFinishDate()
                ?: latest?.date?.takeIf { it.length >= 10 },
        )
    }

    fun onCaptureError(message: String) {
        _uiState.update { it.copy(errorMessage = message, isProcessing = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun processCapturedImage(imageFile: File) {
        val previous = _uiState.getAndUpdate { state ->
            if (state.isProcessing) state
            else state.copy(isProcessing = true, errorMessage = null)
        }
        if (previous.isProcessing) {
            imageFile.delete()
            _uiState.update { it.copy(errorMessage = "busy") }
            return
        }
        viewModelScope.launch {
            var bitmap: Bitmap? = null
            try {
                val ctx = attachContext.await()
                bitmap = decodeBitmap(imageFile) ?: run {
                    Log.w(TAG, "Failed to decode captured image: ${imageFile.absolutePath}")
                    _uiState.update {
                        it.copy(isProcessing = false, errorMessage = "decode_failed")
                    }
                    return@launch
                }
                val timestamp = System.currentTimeMillis()
                val location = locationHelper.getCurrentLocation() ?: LocationData()
                val savedFile = photoManager.savePhoto(
                    bitmap = bitmap,
                    locationData = location,
                    timestamp = timestamp,
                    tripId = ctx.tripId,
                    loadDate = ctx.loadDate,
                    watermarkTitle = ctx.tripId,
                )
                bitmap.recycle()
                bitmap = null
                var photo = CapturedPhoto(
                    file = savedFile,
                    locationData = location,
                    timestamp = timestamp,
                )
                if (!ctx.loadId.isNullOrBlank()) {
                    val entity = photoRepository.savePhoto(
                        fileName = photo.file.name,
                        filePath = photo.file.absolutePath,
                        latitude = photo.locationData.latitude ?: 0.0,
                        longitude = photo.locationData.longitude ?: 0.0,
                        city = photo.locationData.city,
                        state = photo.locationData.state,
                        zipCode = photo.locationData.zipCode,
                        timestamp = photo.timestamp,
                        loadId = ctx.loadId,
                    )
                    photo = photo.copy(savedToDb = true, dbId = entity.id)
                }
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        sessionPhotos = it.sessionPhotos + photo,
                        watermarkTripId = ctx.tripId,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save captured photo", e)
                bitmap?.recycle()
                _uiState.update {
                    it.copy(isProcessing = false, errorMessage = "save_failed")
                }
            } finally {
                imageFile.delete()
            }
        }
    }

    fun openBatchReview() {
        if (_uiState.value.sessionPhotos.isNotEmpty()) {
            _uiState.update { it.copy(reviewingBatch = true) }
        }
    }

    fun closeBatchReview() {
        _uiState.update { it.copy(reviewingBatch = false) }
    }

    fun removePhotoAt(index: Int) {
        val photos = _uiState.value.sessionPhotos.toMutableList()
        if (index !in photos.indices) return
        val removed = photos.removeAt(index)
        removed.file.delete()
        if (removed.savedToDb && removed.dbId != null) {
            viewModelScope.launch { photoRepository.deletePhoto(removed.dbId) }
        }
        _uiState.update {
            it.copy(
                sessionPhotos = photos,
                reviewingBatch = photos.isNotEmpty() && it.reviewingBatch,
            )
        }
    }

    fun requestDiscardSession() {
        val hasAttached = _uiState.value.sessionPhotos.any { it.savedToDb }
        if (hasAttached && isAttachedToLoad) {
            _uiState.update { it.copy(confirmDiscardAttached = true) }
        } else {
            discardSession()
        }
    }

    fun dismissDiscardConfirm() {
        _uiState.update { it.copy(confirmDiscardAttached = false) }
    }

    fun confirmDiscardSession() {
        _uiState.update { it.copy(confirmDiscardAttached = false) }
        discardSession(removeAttached = true)
    }

    fun discardSession(removeAttached: Boolean = false) {
        _uiState.value.sessionPhotos.forEach { photo ->
            if (!photo.savedToDb || removeAttached) {
                photo.file.delete()
                if (photo.savedToDb && photo.dbId != null) {
                    viewModelScope.launch { photoRepository.deletePhoto(photo.dbId) }
                }
            }
        }
        _uiState.update { CameraUiState(watermarkTripId = it.watermarkTripId) }
    }

    fun finishSession() {
        _uiState.update { CameraUiState(watermarkTripId = it.watermarkTripId) }
    }

    fun persistAllPhotos() {
        val state = _uiState.value
        val photos = state.sessionPhotos
        if (photos.isEmpty() || state.isProcessing) return
        if (photos.all { it.savedToDb }) {
            _uiState.update { it.copy(saveSuccess = true) }
            return
        }
        viewModelScope.launch {
            persistUnsavedPhotos()
            if (_uiState.value.errorMessage == null) {
                _uiState.update { it.copy(saveSuccess = true) }
            }
        }
    }

    /**
     * Ensures all session photos are saved/linked to the load, then invokes [onReady]
     * with the file list for sharing. Does not set [CameraUiState.saveSuccess] so the
     * caller can finish navigation after launching the share sheet.
     */
    fun persistThenShare(onReady: (List<File>) -> Unit) {
        val photos = _uiState.value.sessionPhotos
        if (photos.isEmpty() || _uiState.value.isProcessing) return
        viewModelScope.launch {
            persistUnsavedPhotos()
            if (_uiState.value.errorMessage != null) return@launch
            onReady(_uiState.value.sessionPhotos.map { it.file })
        }
    }

    private suspend fun persistUnsavedPhotos() {
        val ctx = attachContext.await()
        val photos = _uiState.value.sessionPhotos
        if (photos.all { it.savedToDb }) return
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
        try {
            val updated = photos.map { photo ->
                if (photo.savedToDb) return@map photo
                val entity = photoRepository.savePhoto(
                    fileName = photo.file.name,
                    filePath = photo.file.absolutePath,
                    latitude = photo.locationData.latitude ?: 0.0,
                    longitude = photo.locationData.longitude ?: 0.0,
                    city = photo.locationData.city,
                    state = photo.locationData.state,
                    zipCode = photo.locationData.zipCode,
                    timestamp = photo.timestamp,
                    loadId = ctx.loadId,
                )
                photo.copy(savedToDb = true, dbId = entity.id)
            }
            _uiState.update {
                it.copy(sessionPhotos = updated, isProcessing = false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist session photos", e)
            _uiState.update {
                it.copy(isProcessing = false, errorMessage = "save_failed")
            }
        }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    private fun decodeBitmap(file: File): Bitmap? {
        val original = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        return try {
            val exif = ExifInterface(file.absolutePath)
            val rotation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (rotation == 0f) return original
            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true).also {
                if (it !== original) original.recycle()
            }
        } catch (e: Exception) {
            Log.w(TAG, "EXIF orientation failed; using original", e)
            original
        }
    }

    companion object {
        private const val TAG = "CameraViewModel"
    }
}
