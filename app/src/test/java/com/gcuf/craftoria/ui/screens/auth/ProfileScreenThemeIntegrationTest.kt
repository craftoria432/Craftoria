package com.gcuf.craftoria.ui.screens.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.data.model.UserRole
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenThemeIntegrationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val testUser = User(
        id = "user123",
        email = "test@example.com",
        name = "Test User",
        role = UserRole.BUYER,
        phone = "1234567890",
        address = "Test Address",
        themePreference = "rose"
    )
    
    @Test
    fun testProfileScreenDisplaysThemeSection() {
        composeTestRule.setContent {
            ProfileScreen(
                user = testUser,
                onBackClick = {},
                onLogout = {},
                onNavigateTo = {}
            )
        }
        
        // Verify theme section is displayed
        composeTestRule.onNodeWithText("APPEARANCE").assertExists()
        composeTestRule.onNodeWithText("Theme Selection").assertExists()
    }
    
    @Test
    fun testProfileScreenThemeSectionIsVisible() {
        composeTestRule.setContent {
            ProfileScreen(
                user = testUser,
                onBackClick = {},
                onLogout = {},
                onNavigateTo = {}
            )
        }
        
        // Verify appearance section exists
        composeTestRule.onNodeWithText("APPEARANCE").assertExists()
    }
}
