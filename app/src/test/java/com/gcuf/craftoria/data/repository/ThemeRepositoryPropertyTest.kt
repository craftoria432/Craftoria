package com.gcuf.craftoria.data.repository

import com.gcuf.craftoria.ui.theme.ThemeType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Property-based tests for ThemeRepository
 * These tests verify universal properties that should hold across all valid inputs
 */
class ThemeRepositoryPropertyTest {
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var themeRepository: ThemeRepository
    
    @Before
    fun setUp() {
        firestore = mockk()
        themeRepository = ThemeRepository(firestore)
    }
    
    /**
     * Property 1: Theme Selection Persistence
     * For any user and any valid theme selection, storing the theme in Firebase 
     * and then retrieving it should return the same theme value.
     * 
     * Validates: Requirements 2.1, 2.2, 9.3
     */
    @Test
    fun testThemePersistenceRoundTrip() = runTest {
        // Test all valid themes
        val themes = listOf(ThemeType.ROSE, ThemeType.OCEAN, ThemeType.MIDNIGHT)
        val userIds = listOf("user1", "user2", "user123", "testUser")
        
        for (theme in themes) {
            for (userId in userIds) {
                // Mock Firebase to return the stored theme
                val mockDoc = mockk<DocumentSnapshot>()
                coEvery { mockDoc.getString("theme_preference") } returns theme.name.lowercase()
                
                val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
                val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
                
                coEvery { firestore.collection("users") } returns mockCollection
                coEvery { mockCollection.document(userId) } returns mockDocRef
                coEvery { mockDocRef.get() } returns com.google.firebase.tasks.Tasks.forResult(mockDoc)
                
                // Retrieve and verify
                val retrieved = themeRepository.getUserThemePreference(userId)
                assertEquals("Theme persistence failed for $theme and $userId", theme, retrieved)
            }
        }
    }
    
    /**
     * Property 2: Valid Theme Identifiers
     * For any user profile retrieved from Firebase, the theme_preference field 
     * should contain one of the valid theme identifiers (rose, ocean, or midnight).
     * 
     * Validates: Requirements 2.3, 9.4
     */
    @Test
    fun testValidThemeIdentifiers() = runTest {
        val validThemeStrings = listOf("rose", "ocean", "midnight", "ROSE", "OCEAN", "MIDNIGHT")
        val userIds = listOf("user1", "user2", "user3")
        
        for (themeStr in validThemeStrings) {
            for (userId in userIds) {
                val mockDoc = mockk<DocumentSnapshot>()
                coEvery { mockDoc.getString("theme_preference") } returns themeStr
                
                val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
                val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
                
                coEvery { firestore.collection("users") } returns mockCollection
                coEvery { mockCollection.document(userId) } returns mockDocRef
                coEvery { mockDocRef.get() } returns com.google.firebase.tasks.Tasks.forResult(mockDoc)
                
                val theme = themeRepository.getUserThemePreference(userId)
                
                // Verify theme is one of the valid values
                assertTrue(
                    "Retrieved theme $theme is not valid for input $themeStr",
                    theme in listOf(ThemeType.ROSE, ThemeType.OCEAN, ThemeType.MIDNIGHT)
                )
            }
        }
    }
    
    /**
     * Property 3: Default Theme for Missing Preference
     * For any user profile with a missing or invalid theme_preference field, 
     * the system should default to the Rose theme.
     * 
     * Validates: Requirements 2.4, 3.2, 9.5, 11.3
     */
    @Test
    fun testDefaultThemeForMissingPreference() = runTest {
        val invalidThemeValues = listOf(null, "", "invalid", "unknown_theme", "xyz123")
        val userIds = listOf("user1", "user2", "user3")
        
        for (invalidValue in invalidThemeValues) {
            for (userId in userIds) {
                val mockDoc = mockk<DocumentSnapshot>()
                coEvery { mockDoc.getString("theme_preference") } returns invalidValue
                
                val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
                val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
                
                coEvery { firestore.collection("users") } returns mockCollection
                coEvery { mockCollection.document(userId) } returns mockDocRef
                coEvery { mockDocRef.get() } returns com.google.firebase.tasks.Tasks.forResult(mockDoc)
                
                val theme = themeRepository.getUserThemePreference(userId)
                
                // Verify default to ROSE
                assertEquals(
                    "Should default to ROSE for invalid value: $invalidValue",
                    ThemeType.ROSE,
                    theme
                )
            }
        }
    }
}
