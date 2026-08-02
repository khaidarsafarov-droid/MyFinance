package com.truckerload.presentation.theme

import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MotionPolicyTest {

  @Test
  fun isSystemReducedMotion_defaultsToFalseWhenAnimatorScaleUnset() {
    val context = RuntimeEnvironment.getApplication()
    assertFalse(MotionPolicy.isSystemReducedMotion(context))
  }
}
