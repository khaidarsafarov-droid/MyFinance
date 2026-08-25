package com.truckerload.presentation

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards phone entry points for the trip camera (drawer, FAB sheet, load cards).
 * Feature code lives under screens/camera; this only ensures users can reach it.
 */
class CameraEntryPointsTest {

    @Test
    fun drawer_exposesCameraAndScanner() {
        val src = read("presentation/components/AppDrawer.kt")
        assertTrue(src.contains("CAMERA,"))
        assertTrue(src.contains("SCANNER,"))
        assertTrue(src.contains("DrawerDestination.CAMERA"))
        assertTrue(src.contains("DrawerDestination.SCANNER"))
        assertTrue(src.contains("R.string.camera"))
    }

    @Test
    fun navGraph_routesDrawerCameraToCameraScreen() {
        val src = read("presentation/navigation/NavGraph.kt")
        assertTrue(src.contains("DrawerDestination.CAMERA -> navController.navigate(Routes.CAMERA)"))
        assertTrue(src.contains("DrawerDestination.SCANNER -> navController.navigate(Routes.SCANNER)"))
        assertTrue(src.contains("onCamera = { navController.navigate(Routes.CAMERA)"))
        assertTrue(src.contains("onLoadCamera"))
    }

    @Test
    fun quickActions_includeCameraBeforeScanner() {
        val src = read("presentation/components/QuickActionsBottomSheet.kt")
        val camera = src.indexOf("onCamera()")
        val scanner = src.indexOf("onScan()")
        assertTrue(camera >= 0)
        assertTrue(scanner > camera)
        assertTrue(src.contains("R.string.widget_camera"))
        assertTrue(!src.contains("UNUSED_PARAMETER"))
    }

    @Test
    fun homeLoadCards_wireCameraAndScanChips() {
        val src = read("presentation/screens/home/HomeLoadGrid.kt")
        assertTrue(src.contains("onCameraClick = {"))
        assertTrue(src.contains("onLoadCamera("))
        assertTrue(src.contains("onScanClick = {"))
        assertTrue(src.contains("onLoadScan("))
        assertTrue(!src.contains("onCameraClick = null"))
    }

    @Test
    fun cameraFeaturePackage_stillPresent() {
        val dir = listOf(
            File("src/main/java/com/truckerload/presentation/screens/camera"),
            File("app/src/main/java/com/truckerload/presentation/screens/camera"),
        ).firstOrNull { it.isDirectory } ?: error("camera package missing")
        val names = dir.list()?.toSet().orEmpty()
        assertTrue(names.contains("CameraScreen.kt"))
        assertTrue(names.contains("CameraViewModel.kt"))
        assertTrue(names.contains("CameraFlashControl.kt"))
        assertTrue(names.contains("PhotoBatchReviewScreen.kt"))
        assertTrue(names.contains("PhotoPreviewScreen.kt"))
    }

    private fun read(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/com/truckerload/$relativePath"),
            File("app/src/main/java/com/truckerload/$relativePath"),
            File("../app/src/main/java/com/truckerload/$relativePath"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Source not found: $relativePath")
    }
}
