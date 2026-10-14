package com.example.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AuthAttemptLimiter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthCooldownNoticeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun forcedDarkThemeShowsCooldownAndRemovesItAtDeadline() {
        var now = 0L
        val limiter = AuthAttemptLimiter(object : AuthAttemptLimiter.Store {
            override fun attempts() = 3
            override fun deadline() = 30_000L
            override fun write(attempts: Int, deadline: Long) = Unit
        }) { now }
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MyApplicationTheme(darkTheme = true, accentColorHex = "Default") {
                AuthCooldownNotice(limiter)
            }
        }
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithTag("settings_auth_cooldown").assertIsDisplayed()
        compose.runOnIdle { now = 30_000L }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithTag("settings_auth_cooldown").assertDoesNotExist()
    }
}
