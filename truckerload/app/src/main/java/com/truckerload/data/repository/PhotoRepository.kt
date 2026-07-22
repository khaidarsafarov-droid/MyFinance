package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.PhotoEntity
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

class PhotoRepository(private val db: AppDatabase) {

    private val photoDao = db.photoDao()

    fun watchPhotos(): Flow<List<PhotoEntity>> = photoDao.getAllPhotos()

    fun watchPhotosByLoadId(loadId: String): Flow<List<PhotoEntity>> =
        photoDao.getPhotosByLoadId(loadId)

    fun watchPhotosFiltered(
        loadId: String? = null,
        dayStartMillis: Long? = null,
        dayEndMillis: Long? = null,
    ): Flow<List<PhotoEntity>> =
        photoDao.getPhotosFiltered(loadId, dayStartMillis, dayEndMillis)

    suspend fun getPhotoById(id: String): PhotoEntity? = photoDao.getById(id)

    suspend fun savePhoto(
        fileName: String,
        filePath: String,
        latitude: Double,
        longitude: Double,
        city: String,
        state: String,
        zipCode: String,
        timestamp: Long,
        loadId: String? = null,
    ): PhotoEntity {
        val entity = PhotoEntity(
            id = UUID.randomUUID().toString(),
            fileName = fileName,
            filePath = filePath,
            latitude = latitude,
            longitude = longitude,
            city = city,
            state = state,
            zipCode = zipCode,
            timestamp = timestamp,
            loadId = loadId,
        )
        photoDao.insert(entity)
        return entity
    }

    suspend fun linkPhotoToLoad(photoId: String, loadId: String?) {
        photoDao.updateLoadId(photoId, loadId)
    }

    /** Deletes DB row and the file on disk when present. */
    suspend fun deletePhoto(id: String) {
        val existing = photoDao.getById(id)
        photoDao.deleteById(id)
        existing?.filePath?.takeIf { it.isNotBlank() }?.let { path ->
            runCatching { File(path).delete() }
        }
    }

    suspend fun deletePhotosForLoad(loadId: String) {
        val photos = photoDao.getPhotosByLoadIdOnce(loadId)
        photoDao.deleteByLoadId(loadId)
        photos.forEach { photo ->
            runCatching { File(photo.filePath).delete() }
        }
    }

    suspend fun deleteAllPhotosAndFiles() {
        val all = photoDao.getAllPhotosOnce()
        photoDao.deleteAll()
        all.forEach { photo ->
            runCatching { File(photo.filePath).delete() }
        }
    }
}
