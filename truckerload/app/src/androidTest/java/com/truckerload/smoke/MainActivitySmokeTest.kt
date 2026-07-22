package com.truckerload.smoke

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.truckerload.presentation.MainActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Lightweight UI smoke: app launches [MainActivity] without immediately finishing.
 */
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActivity_launches() {
        composeRule.waitForIdle()
        assertNotNull(composeRule.activity)
        assertFalse(composeRule.activity.isFinishing)
    }
}
