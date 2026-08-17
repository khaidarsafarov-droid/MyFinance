package com.truckerload.utils

import android.app.Activity
import android.content.ContextWrapper
import android.view.ContextThemeWrapper
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContextFindActivityTest {

    @Test
    fun unwrapsThemeWrapperToActivity() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val wrapped = ContextThemeWrapper(activity, android.R.style.Theme)
        val nested = ContextWrapper(wrapped)
        assertSame(activity, nested.findActivity())
        assertSame(activity, activity.findActivity())
    }

    @Test
    fun applicationHasNoActivity() {
        assertNull(RuntimeEnvironment.getApplication().findActivity())
    }
}
