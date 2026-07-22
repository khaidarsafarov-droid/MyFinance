package com.truckerload.presentation.screens.camera

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.PhotoRepository
import com.truckerload.utils.LocationData
import com.truckerload.utils.LocationHelper
import com.truckerload.utils.PhotoManager
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
)

class CameraViewModel(
    private val app: Application,
    private val photoRepository: PhotoRepository,
) : ViewModel() {

    private val locationHelper = LocationHelper(app)
    private val photoManager = PhotoManager(app)

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun onCaptureError(message: String) {
        _uiState.update { it.copy(errorMessage = message, isProcessing = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun processCapturedImage(imageFile: File) {
        // Claim processing synchronously so a second shutter tap cannot race the coroutine start.
        val previous = _uiState.getAndUpdate { state ->
            if (state.isProcessing) state
            else state.copy(isProcessing = true, errorMessage = null)
        }
        if (previous.isProcessing) {
            imageFile.delete()
            return
        }
        viewModelScope.launch {
            var bitmap: Bitmap? = null
            try {
                bitmap = decodeBitmap(imageFile) ?: run {
                    _uiState.update {
                        it.copy(isProcessing = false, errorMessage = "decode_failed")
                    }
                    return@launch
                }
                val timestamp = System.currentTimeMillis()
                val location = locationHelper.getCurrentLocation() ?: LocationData(
                    latitude = 0.0,
                    longitude = 0.0,
                    city = "",
                    state = "",
                    zipCode = "",
                )
                val savedFile = photoManager.savePhoto(bitmap, location, timestamp)
                bitmap.recycle()
                bitmap = null
                val photo = CapturedPhoto(
                    file = savedFile,
                    locationData = location,
                    timestamp = timestamp,
                )
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        sessionPhotos = it.sessionPhotos + photo,
                    )
                }
            } catch (_: Exception) {
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

    fun discardSession() {
        _uiState.value.sessionPhotos.forEach { photo ->
            if (!photo.savedToDb) {
                photo.file.delete()
            }
        }
        _uiState.update { CameraUiState() }
    }

    /** Clears in-memory session after a successful save without deleting persisted files. */
    fun finishSession() {
        _uiState.update { CameraUiState() }
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
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            try {
                val updated = photos.map { photo ->
                    if (photo.savedToDb) return@map photo
                    val entity = photoRepository.savePhoto(
                        fileName = photo.file.name,
                        filePath = photo.file.absolutePath,
                        latitude = photo.locationData.latitude,
                        longitude = photo.locationData.longitude,
                        city = photo.locationData.city,
                        state = photo.locationData.state,
                        zipCode = photo.locationData.zipCode,
                        timestamp = photo.timestamp,
                    )
                    photo.copy(savedToDb = true, dbId = entity.id)
                }
                _uiState.update {
                    it.copy(
                        sessionPhotos = updated,
                        saveSuccess = true,
                        isProcessing = false,
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isProcessing = false, errorMessage = "save_failed")
                }
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
        } catch (_: Exception) {
            original
        }
    }

    class Factory(
        private val context: Context,
        private val photoRepository: PhotoRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CameraViewModel(context.applicationContext as Application, photoRepository) as T
        }
    }
}
