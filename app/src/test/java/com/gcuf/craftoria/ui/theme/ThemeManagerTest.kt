package com.gcuf.craftoria.ui.theme

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ThemeManagerTest {
    
    private lateinit var themeManager: ThemeManager
    
    @Before
    fun setUp() {
        themeManager = ThemeManager.getInstance()
    }
    
    @Test
    fun testInitialThemeIsRose() {
        assertEquals(ThemeType.ROSE, themeManager.currentTheme.value)
    }
    
    @Test
    fun testRoseColorPaletteIsComplete() = runTest {
        val colors = themeManager.getThemeColors(ThemeType.ROSE)
        
        // Verify all colors are defined
        assertNotNull(colors.primary)
        assertNotNull(colors.primaryLight)
        assertNotNull(colors.primaryDark)
        assertNotNull(colors.secondary)
        assertNotNull(colors.secondaryLight)
        assertNotNull(colors.background)
        assertNotNull(colors.backgroundSecondary)
        assertNotNull(colors.backgroundLight)
        assertNotNull(colors.textPrimary)
        assertNotNull(colors.textSecondary)
        assertNotNull(colors.textLight)
        assertNotNull(colors.success)
        assertNotNull(colors.warning)
        assertNotNull(colors.error)
        assertNotNull(colors.info)
        assertNotNull(colors.borderColor)
        assertNotNull(colors.dividerColor)
        assertNotNull(colors.surfaceColor)
        assertNotNull(colors.accentColor)
        assertNotNull(colors.disabledColor)
    }
    
    @Test
    fun testOceanColorPaletteIsComplete() = runTest {
        val colors = themeManager.getThemeColors(ThemeType.OCEAN)
        
        // Verify all colors are defined
        assertNotNull(colors.primary)
        assertNotNull(colors.primaryLight)
        assertNotNull(colors.primaryDark)
        assertNotNull(colors.secondary)
        assertNotNull(colors.secondaryLight)
        assertNotNull(colors.background)
        assertNotNull(colors.backgroundSecondary)
        assertNotNull(colors.backgroundLight)
        assertNotNull(colors.textPrimary)
        assertNotNull(colors.textSecondary)
        assertNotNull(colors.textLight)
        assertNotNull(colors.success)
        assertNotNull(colors.warning)
        assertNotNull(colors.error)
        assertNotNull(colors.info)
        assertNotNull(colors.borderColor)
        assertNotNull(colors.dividerColor)
        assertNotNull(colors.surfaceColor)
        assertNotNull(colors.accentColor)
        assertNotNull(colors.disabledColor)
    }
    
    @Test
    fun testMidnightColorPaletteIsComplete() = runTest {
        val colors = themeManager.getThemeColors(ThemeType.MIDNIGHT)
        
        // Verify all colors are defined
        assertNotNull(colors.primary)
        assertNotNull(colors.primaryLight)
        assertNotNull(colors.primaryDark)
        assertNotNull(colors.secondary)
        assertNotNull(colors.secondaryLight)
        assertNotNull(colors.background)
        assertNotNull(colors.backgroundSecondary)
        assertNotNull(colors.backgroundLight)
        assertNotNull(colors.textPrimary)
        assertNotNull(colors.textSecondary)
        assertNotNull(colors.textLight)
        assertNotNull(colors.success)
        assertNotNull(colors.warning)
        assertNotNull(colors.error)
        assertNotNull(colors.info)
        assertNotNull(colors.borderColor)
        assertNotNull(colors.dividerColor)
        assertNotNull(colors.surfaceColor)
        assertNotNull(colors.accentColor)
        assertNotNull(colors.disabledColor)
    }
    
    @Test
    fun testSetThemeUpdatesCurrentTheme() = runTest {
        themeManager.setTheme(ThemeType.OCEAN)
        assertEquals(ThemeType.OCEAN, themeManager.currentTheme.value)
    }
    
    @Test
    fun testSetThemeUpdatesThemeColors() = runTest {
        val oceanColors = themeManager.getThemeColors(ThemeType.OCEAN)
        themeManager.setTheme(ThemeType.OCEAN)
        
        assertEquals(oceanColors.primary, themeManager.themeColors.value.primary)
    }
    
    @Test
    fun testSetThemeMidnightUpdatesColors() = runTest {
        val midnightColors = themeManager.getThemeColors(ThemeType.MIDNIGHT)
        themeManager.setTheme(ThemeType.MIDNIGHT)
        
        assertEquals(midnightColors.primary, themeManager.themeColors.value.primary)
    }
    
    @Test
    fun testThemesHaveDifferentPrimaryColors() {
        val roseColors = themeManager.getThemeColors(ThemeType.ROSE)
        val oceanColors = themeManager.getThemeColors(ThemeType.OCEAN)
        val midnightColors = themeManager.getThemeColors(ThemeType.MIDNIGHT)
        
        assertNotEquals(roseColors.primary, oceanColors.primary)
        assertNotEquals(oceanColors.primary, midnightColors.primary)
        assertNotEquals(roseColors.primary, midnightColors.primary)
    }
    
    @Test
    fun testInitializeThemeSetsTheme() = runTest {
        themeManager.initializeTheme(ThemeType.MIDNIGHT)
        assertEquals(ThemeType.MIDNIGHT, themeManager.currentTheme.value)
    }
    
    @Test
    fun testIsTransitioningFlagDuringThemeChange() = runTest {
        // Note: This test may be flaky due to timing, but demonstrates the concept
        themeManager.setTheme(ThemeType.OCEAN)
        // After completion, transitioning should be false
        assertEquals(false, themeManager.isTransitioning.value)
    }
}
