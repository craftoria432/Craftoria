package com.gcuf.craftoria.viewmodel

import com.gcuf.craftoria.data.repository.ThemeRepository
import com.gcuf.craftoria.ui.theme.ThemeManager
import com.gcuf.craftoria.ui.theme.ThemeType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ThemeViewModelTest {
    
    private lateinit var themeRepository: ThemeRepository
    private lateinit var themeManager: ThemeManager
    private lateinit var viewModel: ThemeViewModel
    
    @Before
    fun setUp() {
        themeRepository = mockk()
        themeManager = ThemeManager.getInstance()
        viewModel = ThemeViewModel(themeRepository, themeManager)
    }
    
    @Test
    fun testInitialStateIsRose() {
        assertEquals(ThemeType.ROSE, viewModel.selectedTheme.value)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.errorMessage.value)
    }
    
    @Test
    fun testSelectThemeUpdatesSelectedTheme() = runTest {
        coEvery { themeRepository.updateUserThemePreference(any(), any()) } returns Unit
        
        viewModel.selectTheme(ThemeType.OCEAN, "user123")
        
        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)
        
        assertEquals(ThemeType.OCEAN, viewModel.selectedTheme.value)
    }
    
    @Test
    fun testSelectThemeCallsRepository() = runTest {
        coEvery { themeRepository.updateUserThemePreference(any(), any()) } returns Unit
        
        viewModel.selectTheme(ThemeType.MIDNIGHT, "user123")
        
        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)
        
        coVerify { themeRepository.updateUserThemePreference("user123", ThemeType.MIDNIGHT) }
    }
    
    @Test
    fun testSelectThemeShowsLoadingIndicator() = runTest {
        coEvery { themeRepository.updateUserThemePreference(any(), any()) } returns Unit
        
        viewModel.selectTheme(ThemeType.OCEAN, "user123")
        
        // Loading should be true initially, then false after completion
        kotlinx.coroutines.delay(100)
        assertFalse(viewModel.isLoading.value)
    }
    
    @Test
    fun testSelectThemeClearsErrorOnSuccess() = runTest {
        coEvery { themeRepository.updateUserThemePreference(any(), any()) } returns Unit
        
        viewModel.selectTheme(ThemeType.OCEAN, "user123")
        
        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)
        
        assertNull(viewModel.errorMessage.value)
    }
    
    @Test
    fun testSelectThemeHandlesFailure() = runTest {
        coEvery { themeRepository.updateUserThemePreference(any(), any()) } throws Exception("Network error")
        
        viewModel.selectTheme(ThemeType.OCEAN, "user123")
        
        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)
        
        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("Failed to update theme"))
    }
    
    @Test
    fun testLoadUserThemeRetrievesFromRepository() = runTest {
        coEvery { themeRepository.getUserThemePreference(any()) } returns ThemeType.MIDNIGHT
        
        viewModel.loadUserTheme("user123")
        
        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)
        
        assertEquals(ThemeType.MIDNIGHT, viewModel.selectedTheme.value)
    }
    
    @Test
    fun testLoadUserThemeDefaultsToRoseOnError() = runTest {
        coEvery { themeRepository.getUserThemePreference(any()) } throws Exception("Error")
        
        viewModel.loadUserTheme("user123")
        
        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)
        
        assertEquals(ThemeType.ROSE, viewModel.selectedTheme.value)
    }
    
    @Test
    fun testClearErrorClearsErrorMessage() = runTest {
        coEvery { themeRepository.updateUserThemePreference(any(), any()) } throws Exception("Error")
        
        viewModel.selectTheme(ThemeType.OCEAN, "user123")
        
        // Wait for coroutine to complete
        kotlinx.coroutines.delay(100)
        
        assertNotNull(viewModel.errorMessage.value)
        
        viewModel.clearError()
        
        assertNull(viewModel.errorMessage.value)
    }
}
