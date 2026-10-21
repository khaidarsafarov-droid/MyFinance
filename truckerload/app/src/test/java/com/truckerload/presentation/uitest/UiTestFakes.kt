package com.truckerload.presentation.uitest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Demo GPS fix used by camera/location UI tests. Not a real driver location.
 */
data class FakeLocationStamp(
    val latitude: Double = 35.711,
    val longitude: Double = -78.614,
    val city: String = "Garner",
    val state: String = "NC",
    val zipCode: String = "27529",
)

/**
 * Writes a tiny JPEG stand-in for a camera capture. No device camera is opened.
 */
class FakeCameraCapture @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun writeDemoJpeg(name: String = "demo-camera-capture.jpg"): File {
        val file = File(context.cacheDir, name)
        val bitmap = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.DKGRAY)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()
        return file
    }
}

/**
 * Test-only bindings. Hilt includes this module in the unit-test component and
 * leaves the production graph unchanged.
 */
@Module
@InstallIn(SingletonComponent::class)
object UiTestFakeModule {
    @Provides
    @Singleton
    fun provideFakeLocationStamp(): FakeLocationStamp = FakeLocationStamp()
}
