package com.truckerload.presentation.theme

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MotionPolicyTest {

  @Test
  fun isSystemReducedMotion_defaultsToFalseWhenAnimatorScaleUnset() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertFalse(MotionPolicy.isSystemReducedMotion(context))
  }
}
