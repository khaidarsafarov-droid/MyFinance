package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.PhotoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    ): Flow<List<PhotoEntity>> {
        return photoDao.getAllPhotos().map { photos ->
            photos.filter { photo ->
                val loadOk = loadId == null || photo.loadId == loadId
                val dateOk = when {
                    dayStartMillis == null || dayEndMillis == null -> true
                    else -> photo.timestamp in dayStartMillis..dayEndMillis
                }
                loadOk && dateOk
            }
        }
    }

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

    suspend fun deletePhoto(id: String) {
        photoDao.deleteById(id)
    }
}
