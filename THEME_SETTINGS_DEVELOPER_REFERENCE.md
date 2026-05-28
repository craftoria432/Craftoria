# Theme Settings - Developer Quick Reference

## Quick Navigation

| Component | File | Purpose |
|-----------|------|---------|
| **UI Screen** | `SettingsScreen.kt` | Theme selection UI |
| **State Management** | `ThemeViewModel.kt` | Theme state & logic |
| **Data Persistence** | `ThemeRepository.kt` | Firebase operations |
| **Theme Application** | `ThemeManager.kt` | Theme colors & switching |
| **Navigation** | `NavGraph.kt` | Route configuration |
| **Profile Integration** | `ProfileScreen.kt` | Settings menu item |

---

## Code Snippets

### 1. Access SettingsScreen from Profile

**File**: `ProfileScreen.kt` (line ~603)

```kotlin
MenuSection(
    title = "Preferences",
    items = listOf(IconMenuItem(Icons.Outlined.Palette, "Appearance & Theme", "settings")),
    onItemClick = onNavigateTo
)
```

**Navigation Handler** (line ~650):
```kotlin
"settings" -> navController.navigate(Screen.Settings.route)
```

---

### 2. SettingsScreen Implementation

**File**: `SettingsScreen.kt`

```kotlin
@Composable
fun SettingsScreen(
    user: User,
    onBackClick: () -> Unit,
    themeRepository: ThemeRepository? = null,
    themeManager: ThemeManager? = null
) {
    // Create ViewModel with dependencies
    val themeViewModel = remember {
        if (themeRepository != null && themeManager != null) {
            ThemeViewModel(themeRepository, themeManager)
        } else {
            null
        }
    }
    
    // Load user's current theme
    LaunchedEffect(user.id) {
        themeViewModel?.loadUserTheme(user.id)
    }
    
    // Display three theme options
    ThemeOptionButton(
        emoji = "🌸",
        label = "Rose",
        description = "Pink theme",
        isSelected = selectedTheme == ThemeType.ROSE,
        onClick = {
            themeViewModel?.selectTheme(ThemeType.ROSE, user.id)
        }
    )
    // ... Ocean and Midnight themes
}
```

---

### 3. ThemeViewModel State Management

**File**: `ThemeViewModel.kt`

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
                // Save to Firebase
                themeRepository.updateUserThemePreference(userId, theme)
                
                // Apply theme
                themeManager.setTheme(theme)
                
                // Update state
                _selectedTheme.value = theme
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update theme: ${e.message}"
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
                themeManager.setTheme(theme)
            } catch (e: Exception) {
                _selectedTheme.value = ThemeType.ROSE
                themeManager.setTheme(ThemeType.ROSE)
            }
        }
    }
}
```

---

### 4. ThemeRepository Firebase Operations

**File**: `ThemeRepository.kt`

```kotlin
class ThemeRepository(
    private val firestore: FirebaseFirestore
) {
    suspend fun getUserThemePreference(userId: String): ThemeType {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            val themeStr = doc.getString("theme_preference") ?: "rose"
            stringToThemeType(themeStr)
        } catch (e: Exception) {
            ThemeType.ROSE  // Default fallback
        }
    }
    
    suspend fun updateUserThemePreference(userId: String, theme: ThemeType) {
        firestore.collection("users").document(userId).update(
            mapOf("theme_preference" to themeTypeToString(theme))
        ).await()
    }
}
```

---

### 5. ThemeManager Singleton

**File**: `ThemeManager.kt`

```kotlin
class ThemeManager private constructor() {
    private val _currentTheme = MutableStateFlow<ThemeType>(ThemeType.ROSE)
    val currentTheme: StateFlow<ThemeType> = _currentTheme.asStateFlow()
    
    private val _themeColors = MutableStateFlow<ThemeColors>(getRoseColors())
    val themeColors: StateFlow<ThemeColors> = _themeColors.asStateFlow()
    
    companion object {
        @Volatile
        private var instance: ThemeManager? = null
        
        fun getInstance(): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager().also { instance = it }
            }
        }
    }
    
    fun setTheme(themeType: ThemeType) {
        _currentTheme.value = themeType
        _themeColors.value = getThemeColors(themeType)
    }
    
    fun getThemeColors(themeType: ThemeType): ThemeColors {
        return when (themeType) {
            ThemeType.ROSE -> getRoseColors()
            ThemeType.OCEAN -> getOceanColors()
            ThemeType.MIDNIGHT -> getMidnightColors()
        }
    }
}
```

---

### 6. NavGraph Settings Route

**File**: `NavGraph.kt` (line ~593)

```kotlin
composable(Screen.Settings.route) {
    currentUser?.let { user ->
        // Inject dependencies properly
        val firestore = FirebaseFirestore.getInstance()
        val themeRepository = ThemeRepository(firestore)
        val themeManager = ThemeManager.getInstance()
        
        SettingsScreen(
            user = user,
            onBackClick = { navController.popBackStack() },
            themeRepository = themeRepository,
            themeManager = themeManager
        )
    }
}
```

---

## Key Implementation Details

### Dependency Injection Pattern

```kotlin
// ✅ CORRECT: Pass FirebaseFirestore instance
val themeRepository = ThemeRepository(firestore)

// ❌ WRONG: Don't instantiate without parameter
val themeRepository = ThemeRepository()
```

### Singleton Pattern for ThemeManager

```kotlin
// ✅ CORRECT: Use getInstance() for singleton
val themeManager = ThemeManager.getInstance()

// ❌ WRONG: Don't create new instances
val themeManager = ThemeManager()
```

### State Flow Usage

```kotlin
// ✅ CORRECT: Collect state in Composable
val selectedTheme by themeViewModel?.selectedTheme?.collectAsState() 
    ?: remember { mutableStateOf(ThemeType.ROSE) }

// ✅ CORRECT: Use LaunchedEffect for side effects
LaunchedEffect(user.id) {
    themeViewModel?.loadUserTheme(user.id)
}
```

---

## Theme Types

```kotlin
enum class ThemeType {
    ROSE,      // Pink (default)
    OCEAN,     // Blue
    MIDNIGHT   // Purple
}
```

---

## Error Handling

### Network Failure
```kotlin
try {
    themeRepository.updateUserThemePreference(userId, theme)
} catch (e: Exception) {
    _errorMessage.value = "Failed to update theme: ${e.message}"
}
```

### Firebase Offline
```kotlin
// Gracefully falls back to default theme
val theme = themeRepository.getUserThemePreference(userId)
    ?: ThemeType.ROSE
```

---

## Testing Scenarios

### Test Theme Selection
```kotlin
// 1. Select theme
themeViewModel?.selectTheme(ThemeType.OCEAN, userId)

// 2. Verify state updated
assert(selectedTheme == ThemeType.OCEAN)

// 3. Verify Firebase saved
// Check Firestore: users/{userId}/theme_preference = "ocean"
```

### Test Theme Persistence
```kotlin
// 1. Select theme and close app
themeViewModel?.selectTheme(ThemeType.MIDNIGHT, userId)

// 2. Reopen app
// 3. Load theme
themeViewModel?.loadUserTheme(userId)

// 4. Verify theme is still MIDNIGHT
assert(selectedTheme == ThemeType.MIDNIGHT)
```

### Test Error Handling
```kotlin
// 1. Disable network
// 2. Try to select theme
themeViewModel?.selectTheme(ThemeType.OCEAN, userId)

// 3. Verify error message appears
assert(errorMessage != null)

// 4. Verify theme didn't change
assert(selectedTheme == ThemeType.ROSE)
```

---

## Firebase Firestore Rules

Ensure your Firestore rules allow users to update their own theme preference:

```javascript
match /users/{userId} {
  allow read: if request.auth.uid == userId;
  allow update: if request.auth.uid == userId;
}
```

---

## Common Issues & Solutions

### Issue: Theme doesn't persist
**Solution**: Check Firestore rules and verify `theme_preference` field is being saved

### Issue: Error "No value passed for parameter 'firestore'"
**Solution**: Pass FirebaseFirestore instance to ThemeRepository constructor
```kotlin
val themeRepository = ThemeRepository(firestore)  // ✅ Correct
```

### Issue: Theme doesn't apply across screens
**Solution**: Use ThemeManager singleton instance
```kotlin
val themeManager = ThemeManager.getInstance()  // ✅ Correct
```

### Issue: Settings screen doesn't load
**Solution**: Verify user is logged in and has valid ID
```kotlin
currentUser?.let { user ->
    // user.id must be non-empty
}
```

---

## Performance Tips

1. **Use Singleton**: ThemeManager uses singleton pattern to avoid multiple instances
2. **Lazy Loading**: Theme is loaded only when SettingsScreen is opened
3. **Non-Blocking**: Theme switching doesn't block UI thread
4. **Efficient State**: Uses StateFlow for reactive updates

---

## Integration Checklist

- [x] SettingsScreen created
- [x] ThemeViewModel created
- [x] ThemeRepository created
- [x] ThemeManager updated
- [x] NavGraph Settings route added
- [x] ProfileScreen navigation added
- [x] Dependency injection configured
- [x] Error handling implemented
- [x] Loading states implemented
- [x] All files compile without errors

---

## Related Files

- Theme colors: `app/src/main/java/com/gcuf/craftoria/ui/theme/Color.kt`
- Theme models: `app/src/main/java/com/gcuf/craftoria/ui/theme/ThemeModels.kt`
- Theme provider: `app/src/main/java/com/gcuf/craftoria/ui/theme/ThemeProvider.kt`
- Theme initialization: `app/src/main/java/com/gcuf/craftoria/services/ThemeInitializationService.kt`

---

## Documentation

- **Integration Guide**: `THEME_SETTINGS_INTEGRATION_GUIDE.md`
- **Quick Start**: `THEME_SETTINGS_QUICK_START.md`
- **Test Guide**: `THEME_SETTINGS_TEST_GUIDE.md`
- **Deployment Summary**: `THEME_SETTINGS_FINAL_DEPLOYMENT_SUMMARY.md`
