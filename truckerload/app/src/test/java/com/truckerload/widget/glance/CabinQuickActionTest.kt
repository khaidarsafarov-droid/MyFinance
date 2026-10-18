package com.truckerload.widget.glance

import androidx.compose.ui.unit.dp
import com.truckerload.presentation.MainActivity
import com.truckerload.presentation.screens.add.DieselQuickAddActivity
import com.truckerload.widget.WidgetDeepLink
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CabinQuickActionTest {

    @Test
    fun priority_isCameraThenScannerThenDiesel() {
        assertEquals(
            listOf(
                CabinQuickAction.CAMERA,
                CabinQuickAction.SCANNER,
                CabinQuickAction.DIESEL,
            ),
            CabinQuickAction.priority,
        )
    }

    @Test
    fun visible_dropsFromTheEnd() {
        assertEquals(listOf(CabinQuickAction.CAMERA), CabinQuickAction.visible(1))
        assertEquals(
            listOf(CabinQuickAction.CAMERA, CabinQuickAction.SCANNER),
            CabinQuickAction.visible(2),
        )
        assertEquals(CabinQuickAction.priority, CabinQuickAction.visible(3))
        assertEquals(1, CabinQuickAction.countFor(110.dp))
        assertEquals(2, CabinQuickAction.countFor(170.dp))
        assertEquals(3, CabinQuickAction.countFor(360.dp))
    }

    @Test
    fun scannerAndCamera_openAttachRoutesOnMain() {
        val context = RuntimeEnvironment.getApplication()
        val scanner = CabinQuickAction.SCANNER.launchIntent(context)
        val camera = CabinQuickAction.CAMERA.launchIntent(context)
        assertEquals(MainActivity::class.java.name, scanner.component!!.className)
        assertEquals(MainActivity::class.java.name, camera.component!!.className)
        assertEquals(
            WidgetDeepLink.ROUTE_ATTACH_SCANNER,
            scanner.getStringExtra(MainActivity.EXTRA_ROUTE),
        )
        assertEquals(
            WidgetDeepLink.ROUTE_ATTACH_CAMERA,
            camera.getStringExtra(MainActivity.EXTRA_ROUTE),
        )
    }

    @Test
    fun diesel_opensQuickAddOverlay() {
        val context = RuntimeEnvironment.getApplication()
        val intent = CabinQuickAction.DIESEL.launchIntent(context)
        assertEquals(DieselQuickAddActivity::class.java.name, intent.component!!.className)
    }
}
