package com.truckerload.widget

import android.content.Intent
import com.truckerload.presentation.screens.add.DieselQuickAddActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WidgetDieselQuickAddIntentTest {

    @Test
    fun dieselQuickAddIntent_opensOverlayActivityNotMain() {
        val context = RuntimeEnvironment.getApplication()
        val intent = WidgetDeepLink.dieselQuickAddIntent(context)
        assertEquals(DieselQuickAddActivity::class.java.name, intent.component!!.className)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS != 0)
    }
}
