package com.gcuf.craftoria.data.repository

import com.gcuf.craftoria.ui.theme.ThemeType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ThemeRepositoryTest {
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var themeRepository: ThemeRepository
    
    @Before
    fun setUp() {
        firestore = mockk()
        themeRepository = ThemeRepository(firestore)
    }
    
    @Test
    fun testGetUserThemePreferenceReturnsRoseWhenMissing() = runTest {
        // Mock Firebase to return document without theme_preference
        val mockDoc = mockk<DocumentSnapshot>()
        coEvery { mockDoc.getString("theme_preference") } returns null
        
        val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        
        coEvery { firestore.collection("users") } returns mockCollection
        coEvery { mockCollection.document("user123") } returns mockDocRef
        coEvery { mockDocRef.get() } returns com.google.firebase.tasks.Tasks.forResult(mockDoc)
        
        val theme = themeRepository.getUserThemePreference("user123")
        assertEquals(ThemeType.ROSE, theme)
    }
    
    @Test
    fun testGetUserThemePreferenceReturnsStoredTheme() = runTest {
        // Mock Firebase to return document with theme_preference
        val mockDoc = mockk<DocumentSnapshot>()
        coEvery { mockDoc.getString("theme_preference") } returns "ocean"
        
        val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        
        coEvery { firestore.collection("users") } returns mockCollection
        coEvery { mockCollection.document("user123") } returns mockDocRef
        coEvery { mockDocRef.get() } returns com.google.firebase.tasks.Tasks.forResult(mockDoc)
        
        val theme = themeRepository.getUserThemePreference("user123")
        assertEquals(ThemeType.OCEAN, theme)
    }
    
    @Test
    fun testGetUserThemePreferenceHandlesInvalidValue() = runTest {
        // Mock Firebase to return invalid theme value
        val mockDoc = mockk<DocumentSnapshot>()
        coEvery { mockDoc.getString("theme_preference") } returns "invalid_theme"
        
        val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        
        coEvery { firestore.collection("users") } returns mockCollection
        coEvery { mockCollection.document("user123") } returns mockDocRef
        coEvery { mockDocRef.get() } returns com.google.firebase.tasks.Tasks.forResult(mockDoc)
        
        val theme = themeRepository.getUserThemePreference("user123")
        assertEquals(ThemeType.ROSE, theme)  // Should default to ROSE
    }
    
    @Test
    fun testGetUserThemePreferenceHandlesException() = runTest {
        // Mock Firebase to throw exception
        val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        
        coEvery { firestore.collection("users") } returns mockCollection
        coEvery { mockCollection.document("user123") } returns mockDocRef
        coEvery { mockDocRef.get() } throws Exception("Network error")
        
        val theme = themeRepository.getUserThemePreference("user123")
        assertEquals(ThemeType.ROSE, theme)  // Should default to ROSE on error
    }
    
    @Test
    fun testUpdateUserThemePreferenceCallsFirestore() = runTest {
        // Mock Firebase update
        val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        
        coEvery { firestore.collection("users") } returns mockCollection
        coEvery { mockCollection.document("user123") } returns mockDocRef
        coEvery { mockDocRef.update(any()) } returns com.google.firebase.tasks.Tasks.forResult(null)
        
        themeRepository.updateUserThemePreference("user123", ThemeType.MIDNIGHT)
        
        coVerify { mockDocRef.update(any()) }
    }
    
    @Test
    fun testUpdateUserThemePreferenceThrowsOnFailure() = runTest {
        // Mock Firebase to throw exception
        val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        
        coEvery { firestore.collection("users") } returns mockCollection
        coEvery { mockCollection.document("user123") } returns mockDocRef
        coEvery { mockDocRef.update(any()) } throws Exception("Update failed")
        
        try {
            themeRepository.updateUserThemePreference("user123", ThemeType.OCEAN)
            fail("Should have thrown exception")
        } catch (e: Exception) {
            assertEquals("Update failed", e.message)
        }
    }
    
    @Test
    fun testInitializeThemeForNewUserCallsFirestore() = runTest {
        // Mock Firebase update
        val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        
        coEvery { firestore.collection("users") } returns mockCollection
        coEvery { mockCollection.document("newUser123") } returns mockDocRef
        coEvery { mockDocRef.update(any()) } returns com.google.firebase.tasks.Tasks.forResult(null)
        
        themeRepository.initializeThemeForNewUser("newUser123", ThemeType.ROSE)
        
        coVerify { mockDocRef.update(any()) }
    }
    
    @Test
    fun testThemeConversionRoundTrip() = runTest {
        // Test that all theme types can be converted to string and back
        val themes = listOf(ThemeType.ROSE, ThemeType.OCEAN, ThemeType.MIDNIGHT)
        
        for (theme in themes) {
            // This tests the private conversion functions indirectly
            val mockDoc = mockk<DocumentSnapshot>()
            coEvery { mockDoc.getString("theme_preference") } returns theme.name.lowercase()
            
            val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
            val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
            
            coEvery { firestore.collection("users") } returns mockCollection
            coEvery { mockCollection.document("user123") } returns mockDocRef
            coEvery { mockDocRef.get() } returns com.google.firebase.tasks.Tasks.forResult(mockDoc)
            
            val retrieved = themeRepository.getUserThemePreference("user123")
            assertEquals(theme, retrieved)
        }
    }
}
