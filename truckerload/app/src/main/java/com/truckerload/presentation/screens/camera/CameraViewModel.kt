package com.truckerload.presentation.screens.camera

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
    val capturedPhoto: CapturedPhoto? = null,
    val saveSuccess: Boolean = false,
)

class CameraViewModel(
    private val context: Context,
    private val photoRepository: PhotoRepository,
) : ViewModel() {

    private val locationHelper = LocationHelper(context)
    private val photoManager = PhotoManager(context)

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun onCaptureError(message: String) {
        _uiState.update { it.copy(errorMessage = message, isProcessing = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun processCapturedImage(imageFile: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            try {
                val bitmap = decodeBitmap(imageFile) ?: run {
                    imageFile.delete()
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
                imageFile.delete()
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        capturedPhoto = CapturedPhoto(
                            file = savedFile,
                            locationData = location,
                            timestamp = timestamp,
                        ),
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isProcessing = false, errorMessage = "save_failed")
                }
            }
        }
    }

    fun discardCapturedPhoto() {
        val photo = _uiState.value.capturedPhoto ?: return
        photo.file.delete()
        if (photo.savedToDb && photo.dbId != null) {
            viewModelScope.launch {
                photoRepository.deletePhoto(photo.dbId)
            }
        }
        _uiState.update { it.copy(capturedPhoto = null, saveSuccess = false) }
    }

    fun persistCapturedPhoto() {
        val photo = _uiState.value.capturedPhoto ?: return
        if (photo.savedToDb) {
            _uiState.update { it.copy(saveSuccess = true) }
            return
        }
        viewModelScope.launch {
            try {
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
                _uiState.update {
                    it.copy(
                        capturedPhoto = photo.copy(savedToDb = true, dbId = entity.id),
                        saveSuccess = true,
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "save_failed") }
            }
        }
    }

    fun clearCapturedPhotoState() {
        _uiState.update { it.copy(capturedPhoto = null, saveSuccess = false) }
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
            return CameraViewModel(context.applicationContext, photoRepository) as T
        }
    }
}
