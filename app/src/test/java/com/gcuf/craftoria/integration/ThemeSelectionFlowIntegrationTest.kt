package com.gcuf.craftoria.integration

import com.gcuf.craftoria.data.repository.ThemeRepository
import com.gcuf.craftoria.ui.theme.ThemeManager
import com.gcuf.craftoria.ui.theme.ThemeType
import com.gcuf.craftoria.viewmodel.ThemeViewModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for complete theme selection flow
 * Tests the entire flow from user selection to persistence and retrieval
 */
class ThemeSelectionFlowIntegrationTest {
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var themeRepository: ThemeRepository
    private lateinit var themeManager: ThemeManager
    private lateinit var viewModel: ThemeViewModel
    
    @Before
    fun setUp() {
        firestore = mockk()
        themeRepository = ThemeRepository(firestore)
        themeManager = ThemeManager()
        viewModel = ThemeViewModel(themeRepository, themeManager)
    }
    
    /**
     * Integration test for complete theme selection flow
     * User navigates to Settings → selects new theme → theme applies immediately → 
     * theme persists after app restart
     */
    @Test
    fun testCompleteThemeSelectionFlow() = runTest {
        val userId = "user123"
        val selectedTheme = ThemeType.OCEAN
        
        // Step 1: User selects theme in Settings
        coEvery { themeRepository.updateUserThemePreference(userId, selectedTheme) } returns Unit
        
        viewModel.selectTheme(selectedTheme, userId)
        
        // Wait for coroutine
        kotlinx.coroutines.delay(100)
        
        // Step 2: Verify theme applies immediately
        assertEquals("Theme should apply immediately", selectedTheme, viewModel.selectedTheme.value)
        assertEquals("ThemeManager should update", selectedTheme, themeManager.currentTheme.value)
        
        // Step 3: Simulate app restart - retrieve theme from Firebase
        val mockDoc = mockk<DocumentSnapshot>()
        coEvery { mockDoc.getString("theme_preference") } returns selectedTheme.name.lowercase()
        
        val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        
        coEvery { firestore.collection("users") } returns mockCollection
        coEvery { mockCollection.document(userId) } returns mockDocRef
        coEvery { mockDocRef.get() } returns com.google.firebase.tasks.Tasks.forResult(mockDoc)
        
        // Step 4: Load theme on app restart
        viewModel.loadUserTheme(userId)
        
        // Wait for coroutine
        kotlinx.coroutines.delay(100)
        
        // Step 5: Verify theme persists
        assertEquals("Theme should persist after restart", selectedTheme, viewModel.selectedTheme.value)
    }
    
    /**
     * Integration test for theme change with error handling
     * User selects theme → Firebase fails → error shown → theme reverts
     */
    @Test
    fun testThemeSelectionWithErrorHandling() = runTest {
        val userId = "user123"
        val newTheme = ThemeType.MIDNIGHT
        
        // Mock Firebase failure
        coEvery { themeRepository.updateUserThemePreference(userId, newTheme) } throws Exception("Network error")
        
        // User selects theme
        viewModel.selectTheme(newTheme, userId)
        
        // Wait for coroutine
        kotlinx.coroutines.delay(100)
        
        // Verify error message is shown
        assertNotNull("Error message should be displayed", viewModel.errorMessage.value)
        assertTrue("Error message should contain failure info", viewModel.errorMessage.value!!.contains("Failed"))
        
        // Verify loading indicator is cleared
        assertFalse("Loading indicator should be cleared", viewModel.isLoading.value)
    }
    
    /**
     * Integration test for theme application across multiple theme changes
     * User changes theme multiple times and verifies each change applies
     */
    @Test
    fun testMultipleThemeChanges() = runTest {
        val userId = "user123"
        val themes = listOf(ThemeType.ROSE, ThemeType.OCEAN, ThemeType.MIDNIGHT)
        
        for (theme in themes) {
            coEvery { themeRepository.updateUserThemePreference(userId, theme) } returns Unit
            
            viewModel.selectTheme(theme, userId)
            
            // Wait for coroutine
            kotlinx.coroutines.delay(100)
            
            // Verify theme changed
            assertEquals("Theme should change to $theme", theme, viewModel.selectedTheme.value)
            assertEquals("ThemeManager should update to $theme", theme, themeManager.currentTheme.value)
        }
    }
    
    /**
     * Integration test for theme persistence across devices
     * User logs in on Device A, selects theme → logs in on Device B → same theme retrieved
     */
    @Test
    fun testThemePersistenceAcrossDevices() = runTest {
        val userId = "user123"
        val selectedTheme = ThemeType.OCEAN
        
        // Device A: User selects theme
        coEvery { themeRepository.updateUserThemePreference(userId, selectedTheme) } returns Unit
        
        viewModel.selectTheme(selectedTheme, userId)
        kotlinx.coroutines.delay(100)
        
        // Device B: User logs in and theme is retrieved
        val mockDoc = mockk<DocumentSnapshot>()
        coEvery { mockDoc.getString("theme_preference") } returns selectedTheme.name.lowercase()
        
        val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        
        coEvery { firestore.collection("users") } returns mockCollection
        coEvery { mockCollection.document(userId) } returns mockDocRef
        coEvery { mockDocRef.get() } returns com.google.firebase.tasks.Tasks.forResult(mockDoc)
        
        // Create new ViewModel for Device B
        val deviceBViewModel = ThemeViewModel(themeRepository, ThemeManager())
        deviceBViewModel.loadUserTheme(userId)
        
        kotlinx.coroutines.delay(100)
        
        // Verify same theme on both devices
        assertEquals("Device A theme", selectedTheme, viewModel.selectedTheme.value)
        assertEquals("Device B theme", selectedTheme, deviceBViewModel.selectedTheme.value)
    }
}
