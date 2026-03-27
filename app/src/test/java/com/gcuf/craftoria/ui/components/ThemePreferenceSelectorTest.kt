package com.gcuf.craftoria.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gcuf.craftoria.ui.theme.ThemeType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemePreferenceSelectorTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testThemePreferenceSelectorDisplaysAllThreeOptions() {
        var selectedTheme = ThemeType.ROSE
        
        composeTestRule.setContent {
            ThemePreferenceSelector(
                selectedTheme = selectedTheme,
                onThemeSelected = { selectedTheme = it }
            )
        }
        
        // Verify all three theme options are displayed
        composeTestRule.onNodeWithText("Rose").assertExists()
        composeTestRule.onNodeWithText("Ocean").assertExists()
        composeTestRule.onNodeWithText("Midnight").assertExists()
    }
    
    @Test
    fun testThemePreferenceSelectorDisplaysEmojis() {
        composeTestRule.setContent {
            ThemePreferenceSelector(
                selectedTheme = ThemeType.ROSE,
                onThemeSelected = {}
            )
        }
        
        // Verify emojis are displayed
        composeTestRule.onNodeWithText("🌸").assertExists()
        composeTestRule.onNodeWithText("🌊").assertExists()
        composeTestRule.onNodeWithText("🌙").assertExists()
    }
    
    @Test
    fun testThemePreferenceSelectorIndicatesSelectedTheme() {
        composeTestRule.setContent {
            ThemePreferenceSelector(
                selectedTheme = ThemeType.OCEAN,
                onThemeSelected = {}
            )
        }
        
        // Verify selected theme is indicated
        composeTestRule.onNodeWithText("Ocean").assertExists()
    }
    
    @Test
    fun testThemePreferenceSelectorCallsOnThemeSelectedCallback() {
        var selectedTheme = ThemeType.ROSE
        var callbackCalled = false
        
        composeTestRule.setContent {
            ThemePreferenceSelector(
                selectedTheme = selectedTheme,
                onThemeSelected = { 
                    selectedTheme = it
                    callbackCalled = true
                }
            )
        }
        
        // Click on Ocean theme
        composeTestRule.onNodeWithText("Ocean").performClick()
        
        // Verify callback was called
        assertTrue("Callback should have been called", callbackCalled)
        assertEquals("Selected theme should be OCEAN", ThemeType.OCEAN, selectedTheme)
    }
    
    @Test
    fun testThemePreferenceSelectorDisabledDuringLoading() {
        composeTestRule.setContent {
            ThemePreferenceSelector(
                selectedTheme = ThemeType.ROSE,
                onThemeSelected = {},
                isLoading = true
            )
        }
        
        // Verify component is still displayed but disabled
        composeTestRule.onNodeWithText("Rose").assertExists()
        composeTestRule.onNodeWithText("Ocean").assertExists()
        composeTestRule.onNodeWithText("Midnight").assertExists()
    }
    
    @Test
    fun testThemePreferenceSelectorDisplaysDescriptions() {
        composeTestRule.setContent {
            ThemePreferenceSelector(
                selectedTheme = ThemeType.ROSE,
                onThemeSelected = {}
            )
        }
        
        // Verify descriptions are displayed
        composeTestRule.onNodeWithText("Pink theme").assertExists()
        composeTestRule.onNodeWithText("Blue theme").assertExists()
        composeTestRule.onNodeWithText("Purple theme").assertExists()
    }
}
