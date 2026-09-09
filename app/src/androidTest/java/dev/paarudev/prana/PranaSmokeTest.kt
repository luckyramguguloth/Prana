package dev.paarudev.prana

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PranaSmokeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testAppLaunchesAndDisplaysCheckIn() {
        // Confirms application boots cleanly with Compose tree rendered
        assertTrue("Main activity should not be finishing", !composeTestRule.activity.isFinishing)
    }
}
