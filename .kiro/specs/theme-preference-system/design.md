# Theme Preference System - Technical Design

## Overview

The Theme Preference System enables users to customize their Craftoria app experience by selecting from three distinct visual themes: Rose (pink), Ocean (blue), and Midnight (purple/dark). The system persists theme selections in Firebase user profiles, applies themes dynamically across all screens and user roles, and provides smooth transitions when themes change.

### Key Design Goals

- **Centralized Color Management**: Single source of truth for all theme colors
- **Dynamic Application**: Real-time theme switching without app restart
- **Persistent Storage**: Theme preferences saved in Firebase user profiles
- **Consistent UX**: Unified theme application across all screens and roles
- **Smooth Transitions**: Polished animations when themes change
- **Backward Compatibility**: Existing users default to Rose theme

---

## Architecture

### High-Level Theme Flow

```
User Selection (Settings/Signup)
    ↓
ThemePreferenceSelector Component
    ↓
ThemeViewModel (Update Logic)
    ↓
Firebase User Profile (Persistence)
    ↓
ThemeRepository (Retrieval)
    ↓
ThemeManager (Application)
    ↓
CompositionLocal (Distribution)
    ↓
All UI Components (Consumption)
```

### Component Interaction Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
│  (All Screens: Home, Settings, Cart, Orders, Chat, etc.)    │
└────────────────────┬────────────────────────────────────────┘
                     │ Consumes
                     ↓
┌─────────────────────────────────────────────────────────────┐
│              CompositionLocal<ThemeColors>                   │
│         (Provides current theme colors to all components)    │
└────────────────────┬────────────────────────────────────────┘
                     │ Provided by
                     ↓
┌─────────────────────────────────────────────────────────────┐
│                  ThemeManager                                │
│  (Manages theme state, animations, and distribution)        │
└────────────────────┬────────────────────────────────────────┘
                     │ Uses
                     ↓
┌─────────────────────────────────────────────────────────────┐
│              ThemeRepository                                 │
│  (Retrieves/updates theme from Firebase)                    │
└────────────────────┬────────────────────────────────────────┘
                     │ Accesses
                     ↓
┌─────────────────────────────────────────────────────────────┐
│           Firebase User Profile                              │
│  (Persists theme_preference field)                          │
└─────────────────────────────────────────────────────────────┘
```

---

## Components and Interfaces

### 1. Theme Data Models

#### ThemeType Enum
```kotlin
enum class ThemeType {
    ROSE,      // Pink theme (default)
    OCEAN,     // Blue theme
    MIDNIGHT   // Purple/dark theme
}
```

#### ThemeColors Data Class
```kotlin
data class ThemeColors(
    // Primary colors
    val primary: Color,
    val primaryLight: Color,
    val primaryDark: Color,
    
    // Secondary colors
    val secondary: Color,
    val secondaryLight: Color,
    
    // Background colors
    val background: Color,
    val backgroundSecondary: Color,
    val backgroundLight: Color,
    
    // Text colors
    val textPrimary: Color,
    val textSecondary: Color,
    val textLight: Color,
    
    // State colors
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    
    // Border and UI
    val borderColor: Color,
    val dividerColor: Color,
    val surfaceColor: Color,
    
    // Additional theme-specific colors
    val accentColor: Color,
    val disabledColor: Color
)
```

### 2. Theme Manager

**Responsibility**: Manages theme state, handles transitions, and provides theme colors to the app

```kotlin
class ThemeManager(
    private val themeRepository: ThemeRepository
) {
    private val _currentTheme = MutableStateFlow<ThemeType>(ThemeType.ROSE)
    val currentTheme: StateFlow<ThemeType> = _currentTheme.asStateFlow()
    
    private val _themeColors = MutableStateFlow<ThemeColors>(getRoseColors())
    val themeColors: StateFlow<ThemeColors> = _themeColors.asStateFlow()
    
    private val _isTransitioning = MutableStateFlow(false)
    val isTransitioning: StateFlow<Boolean> = _isTransitioning.asStateFlow()
    
    suspend fun initializeTheme(userId: String)
    suspend fun setTheme(themeType: ThemeType, userId: String)
    fun getThemeColors(themeType: ThemeType): ThemeColors
    private fun getRoseColors(): ThemeColors
    private fun getOceanColors(): ThemeColors
    private fun getMidnightColors(): ThemeColors
}
```

### 3. Theme Repository

**Responsibility**: Handles Firebase operations for theme persistence and retrieval

```kotlin
class ThemeRepository(
    private val firestore: FirebaseFirestore
) {
    suspend fun getUserThemePreference(userId: String): ThemeType
    suspend fun updateUserThemePreference(userId: String, theme: ThemeType)
    suspend fun initializeThemeForNewUser(userId: String, theme: ThemeType)
    private fun themeTypeToString(theme: ThemeType): String
    private fun stringToThemeType(value: String): ThemeType
}
```

### 4. Theme ViewModel

**Responsibility**: Manages theme selection UI state and coordinates with repository

```kotlin
class ThemeViewModel(
    private val themeRepository: ThemeRepository,
    private val themeManager: ThemeManager
) : ViewModel() {
    private val _selectedTheme = MutableStateFlow<ThemeType>(ThemeType.ROSE)
    val selectedTheme: StateFlow<ThemeType> = _selectedTheme.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun selectTheme(theme: ThemeType, userId: String)
    fun loadUserTheme(userId: String)
    fun clearError()
}
```

### 5. Theme Selection Component

**Responsibility**: Reusable UI component for theme selection

```kotlin
@Composable
fun ThemePreferenceSelector(
    selectedTheme: ThemeType,
    onThemeSelected: (ThemeType) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
)
```

**Features**:
- Displays three theme options with icons and labels
- Shows visual preview of each theme
- Indicates currently selected theme with checkmark/highlight
- Provides immediate visual feedback on selection
- Responsive design for various screen sizes

### 6. CompositionLocal for Theme Distribution

```kotlin
val LocalThemeColors = compositionLocalOf<ThemeColors> {
    error("ThemeColors not provided")
}

val LocalCurrentTheme = compositionLocalOf<ThemeType> {
    error("CurrentTheme not provided")
}
```

---

## Data Models

### User Model Extension

The existing `User` model will be extended with a theme preference field:

```kotlin
data class User(
    // ... existing fields ...
    
    @get:PropertyName("theme_preference")
    @set:PropertyName("theme_preference")
    var themePreference: String = "rose",  // Default to rose
    
    // ... rest of fields ...
)
```

### Firebase User Document Structure

```json
{
  "id": "user123",
  "email": "user@example.com",
  "name": "John Doe",
  "role": "buyer",
  "theme_preference": "ocean",
  "created_at": 1234567890,
  "profile_image": "https://...",
  // ... other fields ...
}
```

### Theme Configuration Storage

Theme definitions are stored in-code as constants in `ThemeManager`:

```kotlin
private fun getRoseColors(): ThemeColors = ThemeColors(
    primary = Color(0xFFE91E63),
    primaryLight = Color(0xFFF06292),
    // ... all colors ...
)

private fun getOceanColors(): ThemeColors = ThemeColors(
    primary = Color(0xFF0288D1),
    primaryLight = Color(0xFF03A9F4),
    // ... all colors ...
)

private fun getMidnightColors(): ThemeColors = ThemeColors(
    primary = Color(0xFF7C4DFF),
    primaryLight = Color(0xFF9575CD),
    // ... all colors ...
)
```

---

## Color System Architecture

### Theme Color Palettes

#### Rose Theme (Pink - Default)
- Primary: #E91E63 (Pink)
- Primary Light: #F06292
- Primary Dark: #C2185B
- Secondary: #625B71
- Background: #FFFFFF
- Text Primary: #333333
- Success: #4CAF50
- Error: #F44336

#### Ocean Theme (Blue)
- Primary: #0288D1 (Light Blue)
- Primary Light: #03A9F4
- Primary Dark: #0277BD
- Secondary: #0097A7 (Cyan)
- Background: #FFFFFF
- Text Primary: #1A237E (Dark Blue)
- Success: #00897B (Teal)
- Error: #D32F2F (Red)

#### Midnight Theme (Purple/Dark)
- Primary: #7C4DFF (Purple)
- Primary Light: #9575CD
- Primary Dark: #512DA8
- Secondary: #512DA8 (Deep Purple)
- Background: #121212 (Dark)
- Text Primary: #FFFFFF
- Success: #66BB6A (Green)
- Error: #EF5350 (Red)

### Color Access Pattern

Components access colors through CompositionLocal:

```kotlin
@Composable
fun MyComponent() {
    val colors = LocalThemeColors.current
    
    Button(
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary
        )
    ) {
        Text("Click me", color = colors.textPrimary)
    }
}
```

---

## Repository and ViewModel Patterns

### Theme Repository Implementation

```kotlin
class ThemeRepository(
    private val firestore: FirebaseFirestore
) {
    suspend fun getUserThemePreference(userId: String): ThemeType = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("users").document(userId).get().await()
            val themeStr = doc.getString("theme_preference") ?: "rose"
            stringToThemeType(themeStr)
        } catch (e: Exception) {
            Log.e("ThemeRepository", "Error retrieving theme", e)
            ThemeType.ROSE  // Default fallback
        }
    }
    
    suspend fun updateUserThemePreference(userId: String, theme: ThemeType) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(userId).update(
                "theme_preference" to themeTypeToString(theme)
            ).await()
        } catch (e: Exception) {
            Log.e("ThemeRepository", "Error updating theme", e)
            throw e
        }
    }
    
    suspend fun initializeThemeForNewUser(userId: String, theme: ThemeType) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("users").document(userId).update(
                "theme_preference" to themeTypeToString(theme)
            ).await()
        } catch (e: Exception) {
            Log.e("ThemeRepository", "Error initializing theme", e)
            throw e
        }
    }
    
    private fun themeTypeToString(theme: ThemeType): String = theme.name.lowercase()
    
    private fun stringToThemeType(value: String): ThemeType =
        ThemeType.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ThemeType.ROSE
}
```

### Theme ViewModel Implementation

```kotlin
class ThemeViewModel(
    private val themeRepository: ThemeRepository,
    private val themeManager: ThemeManager
) : ViewModel() {
    private val _selectedTheme = MutableStateFlow<ThemeType>(ThemeType.ROSE)
    val selectedTheme: StateFlow<ThemeType> = _selectedTheme.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun selectTheme(theme: ThemeType, userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                themeRepository.updateUserThemePreference(userId, theme)
                themeManager.setTheme(theme, userId)
                _selectedTheme.value = theme
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update theme: ${e.message}"
                Log.e("ThemeViewModel", "Error selecting theme", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadUserTheme(userId: String) {
        viewModelScope.launch {
            try {
                val theme = themeRepository.getUserThemePreference(userId)
                _selectedTheme.value = theme
                themeManager.setTheme(theme, userId)
            } catch (e: Exception) {
                Log.e("ThemeViewModel", "Error loading theme", e)
                _selectedTheme.value = ThemeType.ROSE
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
```

---

## Theme Persistence and Retrieval Flow

### Initialization Flow (App Startup)

```
App Launch
    ↓
Check if user authenticated
    ├─ YES → Retrieve user ID
    │         ↓
    │    ThemeRepository.getUserThemePreference(userId)
    │         ↓
    │    ThemeManager.setTheme(retrievedTheme, userId)
    │         ↓
    │    CompositionLocal updated with theme colors
    │         ↓
    │    All screens render with correct theme
    │
    └─ NO → Apply default Rose theme
            ↓
            CompositionLocal updated with Rose colors
            ↓
            All screens render with Rose theme
```

### Theme Change Flow (User Selection)

```
User selects theme in Settings
    ↓
ThemePreferenceSelector emits selection
    ↓
ThemeViewModel.selectTheme(newTheme, userId)
    ↓
Show loading indicator
    ↓
ThemeRepository.updateUserThemePreference(userId, newTheme)
    ↓
Firebase update successful?
    ├─ YES → ThemeManager.setTheme(newTheme, userId)
    │         ↓
    │    Animate theme transition (200-400ms)
    │         ↓
    │    CompositionLocal updated
    │         ↓
    │    All visible components update colors
    │         ↓
    │    Show success feedback
    │
    └─ NO → Show error message
            ↓
            Revert to previous theme
```

### Multi-Device Sync Flow

```
User logs in on Device A
    ↓
ThemeRepository retrieves theme from Firebase
    ↓
Device A applies theme
    ↓
User logs in on Device B
    ↓
ThemeRepository retrieves same theme from Firebase
    ↓
Device B applies same theme
    ↓
Both devices display consistent theme
```

---

## UI Component Updates

### Integration Points

#### 1. Settings/Profile Screen
- Add ThemePreferenceSelector component
- Display current theme selection
- Handle theme change events
- Show loading/error states

#### 2. Signup Flow
- Add ThemePreferenceSelector after account creation
- Allow users to select theme before completing signup
- Store selection in new user profile

#### 3. All Screens
- Replace hardcoded color references with LocalThemeColors
- Update component color parameters to use theme colors
- Ensure consistent color application

### Component Color Updates

**Before**:
```kotlin
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = Primary  // Hardcoded
    )
)
```

**After**:
```kotlin
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = LocalThemeColors.current.primary
    )
)
```

---

## Animation and Transition Approach

### Theme Transition Animation

```kotlin
@Composable
fun ThemeTransitionWrapper(
    themeColors: ThemeColors,
    content: @Composable () -> Unit
) {
    val animatedColors = remember { Animatable(themeColors.primary) }
    
    LaunchedEffect(themeColors.primary) {
        animatedColors.animateTo(
            targetValue = themeColors.primary,
            animationSpec = tween(
                durationMillis = 300,
                easing = EaseInOutCubic
            )
        )
    }
    
    CompositionLocalProvider(
        LocalThemeColors provides themeColors
    ) {
        content()
    }
}
```

### Transition Specifications

- **Duration**: 200-400 milliseconds (300ms recommended)
- **Easing**: EaseInOutCubic for smooth, natural feel
- **Scope**: All visible UI components
- **Interactivity**: UI remains responsive during transition
- **Dialog/Modal**: Updates theme colors while remaining open

---

## Integration Points with Existing Screens

### Home Screen
- Apply theme colors to banner carousel
- Update product cards with theme colors
- Apply theme to featured stores section

### Product Details Screen
- Update product image background
- Apply theme to action buttons
- Update text colors and borders

### Cart Screen
- Apply theme to cart items
- Update checkout button colors
- Apply theme to price displays

### Checkout Screen
- Update form field colors
- Apply theme to payment method selection
- Update confirmation button colors

### Orders Screen
- Apply theme to order cards
- Update status badge colors
- Apply theme to action buttons

### Chat Screen
- Update message bubble colors
- Apply theme to input field
- Update send button colors

### Settings/Profile Screen
- Add theme selector component
- Apply theme to all settings options
- Update profile section colors

### Seller Dashboard
- Apply theme to dashboard cards
- Update chart colors
- Apply theme to action buttons

### Seller Orders Screen
- Apply theme to order list
- Update status indicators
- Apply theme to action buttons

---

## Error Handling and Fallback Strategies

### Firebase Retrieval Failures

```kotlin
suspend fun getUserThemePreference(userId: String): ThemeType {
    return try {
        val doc = firestore.collection("users").document(userId).get().await()
        val themeStr = doc.getString("theme_preference") ?: "rose"
        stringToThemeType(themeStr)
    } catch (e: FirebaseFirestoreException) {
        Log.e("ThemeRepository", "Firestore error: ${e.message}")
        ThemeType.ROSE  // Fallback to default
    } catch (e: Exception) {
        Log.e("ThemeRepository", "Unexpected error: ${e.message}")
        ThemeType.ROSE  // Fallback to default
    }
}
```

### Invalid Theme Values

```kotlin
private fun stringToThemeType(value: String): ThemeType {
    return ThemeType.entries.firstOrNull { 
        it.name.equals(value, ignoreCase = true) 
    } ?: ThemeType.ROSE  // Default if invalid
}
```

### Missing Theme Preference Field

```kotlin
// In User model
@get:PropertyName("theme_preference")
@set:PropertyName("theme_preference")
var themePreference: String = "rose"  // Default value
```

### Update Failures

```kotlin
fun selectTheme(theme: ThemeType, userId: String) {
    viewModelScope.launch {
        _isLoading.value = true
        try {
            themeRepository.updateUserThemePreference(userId, theme)
            themeManager.setTheme(theme, userId)
            _selectedTheme.value = theme
            _errorMessage.value = null
        } catch (e: Exception) {
            _errorMessage.value = "Failed to update theme: ${e.message}"
            // Theme remains unchanged on UI
        } finally {
            _isLoading.value = false
        }
    }
}
```

### Offline Handling

- App uses last known theme if offline
- Theme update queued when connection restored
- User notified of pending changes

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Theme Selection Persistence

*For any* user and any valid theme selection, storing the theme in Firebase and then retrieving it should return the same theme value.

**Validates: Requirements 2.1, 2.2, 9.3**

### Property 2: Valid Theme Identifiers

*For any* user profile retrieved from Firebase, the theme_preference field should contain one of the valid theme identifiers (rose, ocean, or midnight).

**Validates: Requirements 2.3, 9.4**

### Property 3: Default Theme for Missing Preference

*For any* user profile with a missing or invalid theme_preference field, the system should default to the Rose theme.

**Validates: Requirements 2.4, 3.2, 9.5, 11.3**

### Property 4: Existing User Default

*For any* existing user without an explicit theme selection, the system should apply the Rose theme by default.

**Validates: Requirements 3.1, 3.3, 3.4**

### Property 5: Theme Application Across Screens

*For any* selected theme, all visible UI components on any screen should use colors from that theme's color palette.

**Validates: Requirements 5.1, 5.4, 6.1, 6.2, 6.3, 8.2**

### Property 6: Theme Consistency on Navigation

*For any* theme selection, navigating between screens should maintain the same theme colors across all screens.

**Validates: Requirements 5.3, 6.4, 6.5**

### Property 7: Theme Update Propagation

*For any* theme change, all currently visible UI components should update to use the new theme's colors without requiring a screen refresh.

**Validates: Requirements 1.3, 5.2, 7.4**

### Property 8: Theme Selection Indication

*For any* theme selection in the ThemePreferenceSelector component, the component should visually indicate which theme is currently selected.

**Validates: Requirements 1.2, 10.3**

### Property 9: Theme Selection Component Emission

*For any* user interaction with the ThemePreferenceSelector component, the component should emit a selection event containing the chosen theme.

**Validates: Requirements 10.2**

### Property 10: Complete Color Palette Definition

*For any* theme type (Rose, Ocean, Midnight), the color system should define all required colors including primary, secondary, background, text, borders, and state colors.

**Validates: Requirements 8.1, 8.3**

### Property 11: Theme Initialization on Startup

*For any* authenticated user, when the application starts, the system should retrieve the user's theme preference from Firebase and apply it to all UI components.

**Validates: Requirements 11.1, 11.2, 11.4**

### Property 12: Unauthenticated User Default

*For any* unauthenticated user, the system should apply the Rose theme as the default.

**Validates: Requirements 11.3**

### Property 13: Theme Update Success

*For any* successful Firebase update of a user's theme preference, the system should apply the new theme to all UI components and clear any error messages.

**Validates: Requirements 12.2**

### Property 14: Theme Update Failure Handling

*For any* failed Firebase update, the system should display an error message and revert to the previous theme.

**Validates: Requirements 12.3**

### Property 15: Theme Configuration Round-Trip

*For any* valid theme configuration, parsing it from JSON, then formatting it back to JSON, then parsing again should produce an equivalent configuration.

**Validates: Requirements 13.1, 13.4, 13.5**

### Property 16: Theme Configuration Validation

*For any* theme configuration with missing required colors, the parser should reject it and return a descriptive error message.

**Validates: Requirements 13.2, 13.3**

### Property 17: Theme Definition Availability

*For any* application initialization, all three theme definitions (Rose, Ocean, Midnight) should be available and valid.

**Validates: Requirements 14.1, 14.2**

### Property 18: Theme Definition Fallback

*For any* missing or corrupted theme definition, the system should use a built-in fallback definition instead of failing.

**Validates: Requirements 14.3**

### Property 19: Theme Definition Consistency

*For any* access to theme definitions during application runtime, the definitions should remain consistent across multiple accesses.

**Validates: Requirements 14.5**

### Property 20: Multi-Device Theme Sync

*For any* user logging in on different devices, the system should retrieve and apply the same theme preference from Firebase on each device.

**Validates: Requirements 2.5**

---

## Testing Strategy

### Dual Testing Approach

The Theme Preference System requires both unit tests and property-based tests for comprehensive coverage:

**Unit Tests** (Specific Examples and Edge Cases):
- Theme selection UI component rendering
- Firebase update success/failure scenarios
- Error message display
- Loading indicator visibility
- Theme selection during signup flow
- Profile screen theme selector integration
- Dialog/modal theme updates

**Property-Based Tests** (Universal Properties):
- Theme persistence round-trip (select → store → retrieve → verify)
- Valid theme identifier validation
- Default theme application for missing preferences
- Theme application across all screen types
- Theme consistency on navigation
- Color palette completeness
- Theme configuration parsing round-trip
- Multi-device theme synchronization

### Property-Based Testing Configuration

**Testing Framework**: Use Kotest or QuickCheck for Kotlin

**Test Configuration**:
- Minimum 100 iterations per property test
- Generate random valid theme selections
- Generate random user profiles
- Generate random screen navigation sequences
- Generate random theme configurations

**Test Tagging Format**:
```kotlin
// Feature: theme-preference-system, Property 1: Theme Selection Persistence
@Test
fun testThemeSelectionPersistence() {
    // Test implementation
}
```

### Unit Test Examples

```kotlin
// Example: Theme Selection Component Rendering
@Test
fun testThemeSelectionComponentDisplaysAllOptions() {
    composeTestRule.setContent {
        ThemePreferenceSelector(
            selectedTheme = ThemeType.ROSE,
            onThemeSelected = {}
        )
    }
    
    composeTestRule.onNodeWithText("Rose").assertExists()
    composeTestRule.onNodeWithText("Ocean").assertExists()
    composeTestRule.onNodeWithText("Midnight").assertExists()
}

// Example: Firebase Update Failure
@Test
fun testThemeUpdateFailureShowsError() {
    val viewModel = ThemeViewModel(mockRepository, mockThemeManager)
    
    // Mock Firebase failure
    coEvery { mockRepository.updateUserThemePreference(any(), any()) } throws Exception("Network error")
    
    viewModel.selectTheme(ThemeType.OCEAN, "user123")
    
    assertEquals("Failed to update theme: Network error", viewModel.errorMessage.value)
}
```

### Property Test Examples

```kotlin
// Example: Theme Persistence Round-Trip
@Test
fun testThemePersistenceRoundTrip() {
    forAll(
        Arb.enum<ThemeType>(),
        Arb.string(1..50)
    ) { theme, userId ->
        // Store theme
        repository.updateUserThemePreference(userId, theme)
        
        // Retrieve theme
        val retrieved = repository.getUserThemePreference(userId)
        
        // Verify round-trip
        retrieved == theme
    }
}

// Example: Valid Theme Identifiers
@Test
fun testValidThemeIdentifiers() {
    forAll(
        Arb.list(Arb.string(1..100))
    ) { userIds ->
        userIds.all { userId ->
            val theme = repository.getUserThemePreference(userId)
            theme in listOf(ThemeType.ROSE, ThemeType.OCEAN, ThemeType.MIDNIGHT)
        }
    }
}

// Example: Theme Application Across Screens
@Test
fun testThemeApplicationAcrossScreens() {
    forAll(
        Arb.enum<ThemeType>(),
        Arb.list(Arb.enum<ScreenType>())
    ) { theme, screens ->
        themeManager.setTheme(theme, "user123")
        
        screens.all { screen ->
            val colors = getScreenColors(screen)
            colors.primary == theme.getColors().primary
        }
    }
}
```

### Test Coverage Goals

- **Unit Tests**: 80%+ coverage of UI components and error handling
- **Property Tests**: 100% coverage of all testable acceptance criteria
- **Integration Tests**: Theme persistence, retrieval, and application flows
- **Edge Cases**: Missing preferences, invalid values, offline scenarios

---

## Implementation Roadmap

### Phase 1: Core Infrastructure
1. Create ThemeType enum and ThemeColors data class
2. Implement ThemeManager with color palette definitions
3. Create CompositionLocal for theme distribution
4. Set up ThemeRepository for Firebase operations

### Phase 2: Data Model and Persistence
1. Add theme_preference field to User model
2. Update User.toMap() to include theme_preference
3. Create migration logic for existing users
4. Implement Firebase update/retrieval operations

### Phase 3: UI Components
1. Create ThemePreferenceSelector component
2. Integrate theme selector into ProfileScreen
3. Add theme selector to signup flow
4. Update all screens to use LocalThemeColors

### Phase 4: Theme Application
1. Implement theme initialization on app startup
2. Add theme transition animations
3. Update all UI components to use theme colors
4. Test theme application across all screens

### Phase 5: Testing and Refinement
1. Write unit tests for all components
2. Write property-based tests for all properties
3. Perform integration testing
4. Optimize performance and animations

---

## Deployment Considerations

### Firebase Firestore Migration

```kotlin
// Migration script for existing users
db.collection("users").get().addOnSuccessListener { documents ->
    for (document in documents) {
        if (!document.contains("theme_preference")) {
            document.reference.update("theme_preference", "rose")
        }
    }
}
```

### Backward Compatibility

- Existing users without theme_preference field default to Rose
- App handles missing theme_preference gracefully
- No breaking changes to existing User model

### Performance Optimization

- Cache theme colors in memory
- Minimize Firebase queries for theme retrieval
- Use efficient color transitions
- Lazy load theme definitions

---

## Future Enhancements

- Custom theme creation by users
- Theme scheduling (automatic theme changes at specific times)
- Theme synchronization with system settings
- Additional theme options based on user feedback
- Theme preview before applying
- Theme history/undo functionality

