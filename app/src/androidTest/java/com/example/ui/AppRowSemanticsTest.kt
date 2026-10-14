package com.example.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppRowSemanticsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun wholeRowExposesAndTogglesLockState() {
        compose.setContent {
            var locked by remember { mutableStateOf(false) }
            MyApplicationTheme {
                AppRowItem(
                    item = AppItem("test.protected", "Protected app", locked),
                    onLockToggled = { locked = it }
                )
            }
        }
        compose.onNodeWithTag("app_item_test.protected")
            .assertIsOff()
            .performClick()
            .assertIsOn()
            .performClick()
            .assertIsOff()
    }
}
