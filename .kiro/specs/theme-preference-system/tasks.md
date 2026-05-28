# Implementation Plan: Theme Preference System

## Overview

This implementation plan breaks down the Theme Preference System into discrete, actionable coding tasks organized into five phases: Core Infrastructure, Data Model and Persistence, UI Components, Theme Application, and Testing and Validation. Each task builds incrementally on previous work, ensuring all components are properly integrated and tested.

---

## Phase 1: Core Infrastructure

- [ ] 1.1 Create ThemeType enum and ThemeColors data class
  - Define ThemeType enum with ROSE, OCEAN, and MIDNIGHT values
  - Create ThemeColors data class with all required color properties (primary, secondary, background, text, state colors, borders, etc.)
  - Add documentation and comments for each color property
  - _Requirements: 8.1, 8.3_

- [ ] 1.2 Implement ThemeManager with color palette definitions
  - Create ThemeManager class with MutableStateFlow for currentTheme and themeColors
  - Implement getRoseColors(), getOceanColors(), and getMidnightColors() functions with complete color definitions
  - Add getThemeColors(themeType: ThemeType) function to retrieve colors for any theme
  - Implement initializeTheme(userId: String) and setTheme(themeType: ThemeType, userId: String) suspend functions
  - Add isTransitioning StateFlow for animation state management
  - _Requirements: 8.1, 8.2, 8.3_

- [ ] 1.3 Create CompositionLocal for theme distribution
  - Define LocalThemeColors compositionLocalOf<ThemeColors>
  - Define LocalCurrentTheme compositionLocalOf<ThemeType>
  - Create ThemeProvider composable that wraps the app and provides theme values
  - Ensure proper error handling if CompositionLocal is not provided
  - _Requirements: 8.2, 8.4_

- [ ] 1.4 Set up ThemeRepository for Firebase operations
  - Create ThemeRepository class with FirebaseFirestore dependency
  - Implement getUserThemePreference(userId: String): ThemeType suspend function
  - Implement updateUserThemePreference(userId: String, theme: ThemeType) suspend function
  - Implement initializeThemeForNewUser(userId: String, theme: ThemeType) suspend function
  - Add themeTypeToString() and stringToThemeType() conversion functions
  - Add error handling and logging for all Firebase operations
  - _Requirements: 12.1, 12.2, 12.3_

- [ ]* 1.5 Write unit tests for ThemeManager
  - Test color palette definitions for all three themes
  - Test theme initialization and switching
  - Test StateFlow emissions on theme changes
  - Test error handling for invalid theme types
  - _Requirements: 8.1, 8.3_

- [ ]* 1.6 Write unit tests for ThemeRepository
  - Test successful theme retrieval from Firebase
  - Test successful theme update to Firebase
  - Test error handling for Firebase failures
  - Test theme conversion functions (ThemeType ↔ String)
  - _Requirements: 12.1, 12.2, 12.3_

- [ ] 1.7 Checkpoint - Verify core infrastructure
  - Ensure all classes compile without errors
  - Verify ThemeManager and ThemeRepository are properly integrated
  - Confirm CompositionLocal is accessible throughout the app
  - Ask the user if questions arise.

---

## Phase 2: Data Model and Persistence

- [ ] 2.1 Extend User model with theme_preference field
  - Add theme_preference: String property to User data class with default value "rose"
  - Add @PropertyName annotations for Firebase serialization/deserialization
  - Update User.toMap() to include theme_preference field
  - Ensure backward compatibility with existing user documents
  - _Requirements: 9.1, 9.2, 9.3_

- [ ] 2.2 Create migration logic for existing users
  - Implement function to check if user has theme_preference field
  - Add logic to initialize theme_preference to "rose" for users missing the field
  - Create migration task that runs on app startup for current user
  - Log migration events for debugging
  - _Requirements: 3.3, 9.5_

- [ ]* 2.3 Write property test for theme persistence round-trip
  - **Property 1: Theme Selection Persistence**
  - **Validates: Requirements 2.1, 2.2, 9.3**
  - Test that storing a theme and retrieving it returns the same value
  - Use property-based testing to generate random valid themes and user IDs
  - Verify round-trip consistency across multiple iterations

- [ ]* 2.4 Write property test for valid theme identifiers
  - **Property 2: Valid Theme Identifiers**
  - **Validates: Requirements 2.3, 9.4**
  - Test that all retrieved theme_preference values are valid (rose, ocean, or midnight)
  - Generate random user profiles and verify theme validity
  - Test edge cases with missing or corrupted theme values

- [ ]* 2.5 Write property test for default theme on missing preference
  - **Property 3: Default Theme for Missing Preference**
  - **Validates: Requirements 2.4, 3.2, 9.5, 11.3**
  - Test that missing or invalid theme_preference defaults to Rose
  - Verify default behavior across multiple user profiles
  - Test with various invalid theme values

- [ ] 2.6 Checkpoint - Verify data model and persistence
  - Ensure User model compiles with new theme_preference field
  - Verify migration logic works for existing users
  - Confirm Firebase serialization/deserialization works correctly
  - Ask the user if questions arise.

---

## Phase 3: UI Components

- [ ] 3.1 Create ThemePreferenceSelector component
  - Build composable function that displays three theme options (Rose, Ocean, Midnight)
  - Add visual icons/emojis for each theme (🌸 Rose, 🌊 Ocean, 🌙 Midnight)
  - Implement selection state with visual indication (checkmark or highlight)
  - Add theme preview colors for each option
  - Implement onThemeSelected callback for parent composition
  - Add loading state support with disabled selection during updates
  - Make component responsive for various screen sizes
  - _Requirements: 1.1, 1.2, 1.4, 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 3.2 Create ThemeViewModel for theme selection logic
  - Create ThemeViewModel class extending ViewModel
  - Add selectedTheme, isLoading, and errorMessage StateFlows
  - Implement selectTheme(theme: ThemeType, userId: String) function
  - Implement loadUserTheme(userId: String) function
  - Add clearError() function for error dismissal
  - Integrate with ThemeRepository and ThemeManager
  - _Requirements: 1.3, 12.1, 12.2, 12.3_

- [ ]* 3.3 Write unit tests for ThemePreferenceSelector component
  - Test rendering of all three theme options
  - Test visual indication of selected theme
  - Test callback emission on theme selection
  - Test loading state UI
  - Test responsive layout on different screen sizes
  - _Requirements: 10.1, 10.3, 10.4, 10.5_

- [ ]* 3.4 Write unit tests for ThemeViewModel
  - Test theme selection with successful Firebase update
  - Test theme selection with Firebase failure
  - Test error message display and clearing
  - Test loading state during updates
  - Test theme loading from Firebase
  - _Requirements: 1.3, 12.1, 12.2, 12.3_

- [ ] 3.5 Integrate ThemePreferenceSelector into ProfileScreen
  - Add ThemePreferenceSelector component to ProfileScreen
  - Connect to ThemeViewModel for state management
  - Display current theme selection
  - Handle theme change events
  - Show loading indicator during updates
  - Display error messages if update fails
  - _Requirements: 1.1, 1.2, 1.3, 1.5_

- [ ] 3.6 Add theme selector to signup flow
  - Create or modify signup screen to include ThemePreferenceSelector
  - Position theme selection after account creation but before completion
  - Allow users to skip theme selection (defaults to Rose)
  - Store selected theme in new user profile during signup
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ]* 3.7 Write unit tests for ProfileScreen theme integration
  - Test theme selector display on ProfileScreen
  - Test theme change from ProfileScreen
  - Test error handling and recovery
  - Test loading states
  - _Requirements: 1.1, 1.2, 1.3, 1.5_

- [ ]* 3.8 Write unit tests for signup flow theme integration
  - Test theme selector display during signup
  - Test theme selection during signup
  - Test default theme if skipped
  - Test theme storage in new user profile
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 3.9 Checkpoint - Verify UI components
  - Ensure ThemePreferenceSelector renders correctly
  - Verify ProfileScreen integration works
  - Confirm signup flow includes theme selection
  - Test theme selection and persistence
  - Ask the user if questions arise.

---

## Phase 4: Theme Application

- [ ] 4.1 Implement theme initialization on app startup
  - Add theme initialization logic to MainActivity or app initialization
  - Check if user is authenticated
  - If authenticated: retrieve user's theme preference from ThemeRepository
  - If not authenticated: apply default Rose theme
  - Update ThemeManager with retrieved or default theme
  - _Requirements: 11.1, 11.2, 11.3, 11.4_

- [ ]* 4.2 Write property test for theme initialization on startup
  - **Property 11: Theme Initialization on Startup**
  - **Validates: Requirements 11.1, 11.2, 11.4**
  - Test that authenticated users get their stored theme on startup
  - Test that unauthenticated users get Rose theme on startup
  - Verify theme is applied to all components after initialization

- [ ]* 4.3 Write property test for unauthenticated user default
  - **Property 12: Unauthenticated User Default**
  - **Validates: Requirements 11.3**
  - Test that unauthenticated users always get Rose theme
  - Verify default is applied consistently

- [ ] 4.4 Update HomeScreen to use theme colors
  - Replace hardcoded color references with LocalThemeColors.current
  - Update banner carousel colors
  - Update product card colors
  - Update featured stores section colors
  - Update text and border colors
  - _Requirements: 5.1, 5.4, 6.1_

- [ ] 4.5 Update ProductDetailsScreen to use theme colors
  - Replace hardcoded colors with LocalThemeColors.current
  - Update product image background
  - Update action button colors
  - Update text and border colors
  - Update state-specific colors (success, error, warning)
  - _Requirements: 5.1, 5.4, 6.1_

- [ ] 4.6 Update CartScreen to use theme colors
  - Replace hardcoded colors with LocalThemeColors.current
  - Update cart item colors
  - Update checkout button colors
  - Update price display colors
  - Update text and border colors
  - _Requirements: 5.1, 5.4, 6.1_

- [ ] 4.7 Update CheckoutScreen to use theme colors
  - Replace hardcoded colors with LocalThemeColors.current
  - Update form field colors
  - Update payment method selection colors
  - Update confirmation button colors
  - Update text and border colors
  - _Requirements: 5.1, 5.4, 6.1_

- [ ] 4.8 Update OrdersScreen to use theme colors
  - Replace hardcoded colors with LocalThemeColors.current
  - Update order card colors
  - Update status badge colors
  - Update action button colors
  - Update text and border colors
  - _Requirements: 5.1, 5.4, 6.1_

- [ ] 4.9 Update ChatScreen to use theme colors
  - Replace hardcoded colors with LocalThemeColors.current
  - Update message bubble colors
  - Update input field colors
  - Update send button colors
  - Update text and border colors
  - _Requirements: 5.1, 5.4, 6.1_

- [ ] 4.10 Update SettingsScreen to use theme colors
  - Replace hardcoded colors with LocalThemeColors.current
  - Update settings option colors
  - Update profile section colors
  - Update text and border colors
  - Ensure theme selector is visible and functional
  - _Requirements: 5.1, 5.4, 6.1_

- [ ] 4.11 Update SellerDashboardScreen to use theme colors
  - Replace hardcoded colors with LocalThemeColors.current
  - Update dashboard card colors
  - Update chart colors
  - Update action button colors
  - Update text and border colors
  - _Requirements: 5.1, 5.4, 6.2_

- [ ] 4.12 Update SellerOrdersScreen to use theme colors
  - Replace hardcoded colors with LocalThemeColors.current
  - Update order list colors
  - Update status indicator colors
  - Update action button colors
  - Update text and border colors
  - _Requirements: 5.1, 5.4, 6.2_

- [ ] 4.13 Update CoSellerStoreManagementScreen to use theme colors
  - Replace hardcoded colors with LocalThemeColors.current
  - Update store management card colors
  - Update action button colors
  - Update text and border colors
  - _Requirements: 5.1, 5.4, 6.3_

- [ ] 4.14 Update CoSellerStorePaymentScreen to use theme colors
  - Replace hardcoded colors with LocalThemeColors.current
  - Update payment card colors
  - Update status indicator colors
  - Update text and border colors
  - _Requirements: 5.1, 5.4, 6.3_

- [ ] 4.15 Implement theme transition animations
  - Create ThemeTransitionWrapper composable with animation logic
  - Use Animatable for smooth color transitions
  - Set animation duration to 300ms with EaseInOutCubic easing
  - Ensure UI remains interactive during transition
  - Apply animation to all theme color changes
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ]* 4.16 Write property test for theme application across screens
  - **Property 5: Theme Application Across Screens**
  - **Validates: Requirements 5.1, 5.4, 6.1, 6.2, 6.3, 8.2**
  - Test that all screens use correct theme colors
  - Generate random screen sequences and verify theme consistency
  - Test all three themes across all screen types

- [ ]* 4.17 Write property test for theme consistency on navigation
  - **Property 6: Theme Consistency on Navigation**
  - **Validates: Requirements 5.3, 6.4, 6.5**
  - Test that theme remains consistent when navigating between screens
  - Verify colors don't change unexpectedly during navigation
  - Test navigation sequences with theme changes

- [ ]* 4.18 Write property test for theme update propagation
  - **Property 7: Theme Update Propagation**
  - **Validates: Requirements 1.3, 5.2, 7.4**
  - Test that all visible components update when theme changes
  - Verify no screen refresh is required
  - Test rapid theme changes

- [ ]* 4.19 Write property test for multi-device theme sync
  - **Property 20: Multi-Device Theme Sync**
  - **Validates: Requirements 2.5**
  - Test that same theme is retrieved on different devices
  - Verify Firebase consistency across devices
  - Test with multiple user profiles

- [ ] 4.20 Checkpoint - Verify theme application
  - Ensure all screens display correct theme colors
  - Verify theme changes apply smoothly across all screens
  - Test navigation maintains theme consistency
  - Test theme transitions are smooth and professional
  - Ask the user if questions arise.

---

## Phase 5: Testing and Validation

- [ ] 5.1 Write property test for theme selection indication
  - **Property 8: Theme Selection Indication**
  - **Validates: Requirements 1.2, 10.3**
  - Test that ThemePreferenceSelector visually indicates selected theme
  - Verify checkmark or highlight appears for current selection
  - Test with all three themes

- [ ] 5.2 Write property test for theme selection component emission
  - **Property 9: Theme Selection Component Emission**
  - **Validates: Requirements 10.2**
  - Test that ThemePreferenceSelector emits correct selection event
  - Verify emitted theme matches user selection
  - Test with all three themes

- [ ] 5.3 Write property test for complete color palette definition
  - **Property 10: Complete Color Palette Definition**
  - **Validates: Requirements 8.1, 8.3**
  - Test that all three themes have complete color palettes
  - Verify all required colors are defined (primary, secondary, background, text, borders, states)
  - Test color values are valid and distinct

- [ ] 5.4 Write property test for theme update success
  - **Property 13: Theme Update Success**
  - **Validates: Requirements 12.2**
  - Test that successful Firebase update applies new theme
  - Verify error messages are cleared on success
  - Test with all three themes

- [ ] 5.5 Write property test for theme update failure handling
  - **Property 14: Theme Update Failure Handling**
  - **Validates: Requirements 12.3**
  - Test that failed Firebase update displays error message
  - Verify theme reverts to previous value on failure
  - Test with various failure scenarios

- [ ] 5.6 Write property test for theme configuration round-trip
  - **Property 15: Theme Configuration Round-Trip**
  - **Validates: Requirements 13.1, 13.4, 13.5**
  - Test that parsing and formatting theme config produces equivalent result
  - Verify round-trip consistency across multiple iterations
  - Test with all three theme configurations

- [ ] 5.7 Write property test for theme configuration validation
  - **Property 16: Theme Configuration Validation**
  - **Validates: Requirements 13.2, 13.3**
  - Test that invalid theme configurations are rejected
  - Verify descriptive error messages for missing colors
  - Test with various invalid configurations

- [ ] 5.8 Write property test for theme definition availability
  - **Property 17: Theme Definition Availability**
  - **Validates: Requirements 14.1, 14.2**
  - Test that all three theme definitions are available on startup
  - Verify all definitions are valid and complete
  - Test theme definitions persist across app sessions

- [ ] 5.9 Write property test for theme definition fallback
  - **Property 18: Theme Definition Fallback**
  - **Validates: Requirements 14.3**
  - Test that missing or corrupted theme definitions use fallback
  - Verify fallback definitions are valid
  - Test with various corruption scenarios

- [ ] 5.10 Write property test for theme definition consistency
  - **Property 19: Theme Definition Consistency**
  - **Validates: Requirements 14.5**
  - Test that theme definitions remain consistent across multiple accesses
  - Verify no unexpected mutations occur
  - Test with concurrent access patterns

- [ ] 5.11 Write property test for existing user default theme
  - **Property 4: Existing User Default**
  - **Validates: Requirements 3.1, 3.3, 3.4**
  - Test that existing users without explicit selection get Rose theme
  - Verify default persists across sessions
  - Test with migrated user profiles

- [ ] 5.12 Write integration test for complete theme selection flow
  - Test user navigates to Settings
  - Test user selects new theme
  - Test theme applies immediately to all visible components
  - Test theme persists after app restart
  - Test theme applies on re-login
  - _Requirements: 1.1, 1.2, 1.3, 1.5, 2.1, 2.2, 5.1, 5.2_

- [ ] 5.13 Write integration test for signup theme selection flow
  - Test new user completes signup
  - Test theme selection screen appears
  - Test user selects theme during signup
  - Test theme is stored in new user profile
  - Test theme applies on first login
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 5.14 Write integration test for theme persistence across devices
  - Test user logs in on Device A and selects theme
  - Test user logs in on Device B
  - Test Device B retrieves same theme from Firebase
  - Test both devices display consistent theme
  - _Requirements: 2.5_

- [ ] 5.15 Write integration test for theme application across all screens
  - Test theme applies to HomeScreen
  - Test theme applies to ProductDetailsScreen
  - Test theme applies to CartScreen
  - Test theme applies to CheckoutScreen
  - Test theme applies to OrdersScreen
  - Test theme applies to ChatScreen
  - Test theme applies to SettingsScreen
  - Test theme applies to SellerDashboardScreen
  - Test theme applies to SellerOrdersScreen
  - Test theme applies to CoSellerScreens
  - _Requirements: 5.1, 5.4, 6.1, 6.2, 6.3_

- [ ] 5.16 Write integration test for theme transitions
  - Test theme change triggers smooth animation
  - Test animation duration is 200-400ms
  - Test UI remains interactive during animation
  - Test all components update during transition
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 5.17 Write integration test for error handling
  - Test Firebase connection failure handling
  - Test invalid theme value handling
  - Test missing theme_preference field handling
  - Test error recovery and fallback to default
  - _Requirements: 2.4, 3.2, 9.5, 11.3_

- [ ] 5.18 Write integration test for multi-role theme consistency
  - Test Buyer role applies theme correctly
  - Test Seller role applies theme correctly
  - Test CoSeller role applies theme correctly
  - Test theme persists when switching roles
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 5.19 Checkpoint - Verify all tests pass
  - Run all unit tests and verify they pass
  - Run all property-based tests and verify they pass
  - Run all integration tests and verify they pass
  - Verify test coverage meets goals (80%+ unit, 100% property)
  - Ask the user if questions arise.

- [ ] 5.20 Performance and optimization review
  - Review theme color caching efficiency
  - Verify Firebase queries are minimized
  - Test animation performance on various devices
  - Optimize any slow transitions or updates
  - _Requirements: 7.2, 7.3_

- [ ] 5.21 Final checkpoint - Theme Preference System complete
  - Ensure all requirements are met
  - Verify all acceptance criteria are satisfied
  - Confirm all tests pass
  - Validate theme system works across all screens and roles
  - Ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP, but are strongly recommended for production quality
- Each task references specific requirements for traceability
- Property-based tests validate universal correctness properties across all valid inputs
- Unit tests validate specific examples and edge cases
- Integration tests verify end-to-end flows work correctly
- Checkpoints ensure incremental validation and allow for course correction
- All code should follow Kotlin best practices and Compose conventions
- Theme colors should be accessible and meet WCAG contrast requirements
