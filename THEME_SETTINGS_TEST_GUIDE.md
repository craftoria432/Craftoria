# Theme Settings Implementation - Comprehensive Test Guide

## Overview
This guide provides step-by-step instructions to test the complete theme selection feature, including persistence, error handling, and all three themes (Rose, Ocean, Midnight).

---

## Pre-Test Checklist

- [ ] App builds successfully without compilation errors
- [ ] Firebase is properly configured and connected
- [ ] User is logged in with a valid account
- [ ] Network connectivity is available
- [ ] Device/emulator has sufficient storage

---

## Test Scenarios

### Test 1: Navigation to Settings Screen

**Objective**: Verify that users can navigate to the Settings screen from Profile

**Steps**:
1. Launch the app and log in with a test account
2. Navigate to Profile screen (bottom navigation or menu)
3. Scroll down to find "Preferences" section
4. Tap on "Appearance & Theme" menu item
5. Verify that SettingsScreen loads successfully

**Expected Results**:
- ✅ Settings screen displays with title "Settings"
- ✅ Back button is visible and functional
- ✅ "APPEARANCE" section is visible with "Choose Your Theme" text
- ✅ Three theme options are displayed: Rose 🌸, Ocean 🌊, Midnight 🌙

---

### Test 2: Theme Selection - Rose Theme

**Objective**: Verify that Rose theme can be selected and persists

**Steps**:
1. From Settings screen, tap on "Rose 🌸" theme option
2. Observe the loading indicator
3. Wait for the theme to apply
4. Verify the checkmark appears on Rose theme
5. Close the app completely (force stop)
6. Reopen the app and navigate back to Settings
7. Verify Rose theme is still selected

**Expected Results**:
- ✅ Rose theme option shows loading indicator briefly
- ✅ Checkmark appears on Rose theme after selection
- ✅ Other themes show chevron icon (not selected)
- ✅ Theme persists after app restart
- ✅ No error messages appear

---

### Test 3: Theme Selection - Ocean Theme

**Objective**: Verify that Ocean theme can be selected and persists

**Steps**:
1. From Settings screen, tap on "Ocean 🌊" theme option
2. Observe the loading indicator
3. Wait for the theme to apply
4. Verify the checkmark appears on Ocean theme
5. Verify Rose theme now shows chevron icon
6. Close the app completely
7. Reopen the app and navigate back to Settings
8. Verify Ocean theme is still selected

**Expected Results**:
- ✅ Ocean theme option shows loading indicator briefly
- ✅ Checkmark appears on Ocean theme after selection
- ✅ Rose theme shows chevron icon (not selected)
- ✅ Theme persists after app restart
- ✅ No error messages appear

---

### Test 4: Theme Selection - Midnight Theme

**Objective**: Verify that Midnight theme can be selected and persists

**Steps**:
1. From Settings screen, tap on "Midnight 🌙" theme option
2. Observe the loading indicator
3. Wait for the theme to apply
4. Verify the checkmark appears on Midnight theme
5. Verify Ocean theme now shows chevron icon
6. Close the app completely
7. Reopen the app and navigate back to Settings
8. Verify Midnight theme is still selected

**Expected Results**:
- ✅ Midnight theme option shows loading indicator briefly
- ✅ Checkmark appears on Midnight theme after selection
- ✅ Ocean theme shows chevron icon (not selected)
- ✅ Theme persists after app restart
- ✅ No error messages appear

---

### Test 5: Rapid Theme Switching

**Objective**: Verify that rapid theme switching doesn't cause crashes or inconsistencies

**Steps**:
1. From Settings screen, rapidly tap between different theme options (Rose → Ocean → Midnight → Rose)
2. Observe loading indicators
3. Wait for all operations to complete
4. Verify the final selected theme is correct
5. Navigate away and back to Settings
6. Verify the theme is still correct

**Expected Results**:
- ✅ App doesn't crash during rapid switching
- ✅ Loading indicators appear for each selection
- ✅ Final selected theme is correctly displayed
- ✅ Theme persists correctly after navigation

---

### Test 6: Error Handling - Network Failure

**Objective**: Verify that error messages display when network is unavailable

**Steps**:
1. Enable Airplane Mode on device/emulator
2. From Settings screen, tap on a different theme option
3. Observe the loading indicator
4. Wait for error message to appear
5. Verify snackbar shows error message
6. Disable Airplane Mode
7. Try selecting the theme again
8. Verify theme selection succeeds

**Expected Results**:
- ✅ Loading indicator appears initially
- ✅ Error snackbar appears with message like "Failed to update theme: ..."
- ✅ Theme doesn't change (reverts to previous selection)
- ✅ After network is restored, theme selection works normally

---

### Test 7: Back Navigation

**Objective**: Verify that back button works correctly

**Steps**:
1. From Settings screen, tap the back arrow button
2. Verify navigation back to Profile screen
3. Navigate to Settings again
4. Verify the previously selected theme is still active

**Expected Results**:
- ✅ Back button navigates to Profile screen
- ✅ Settings screen closes properly
- ✅ Theme selection is preserved

---

### Test 8: Theme Persistence Across Multiple Sessions

**Objective**: Verify that theme persists across multiple app sessions

**Steps**:
1. Select Rose theme and close app
2. Reopen app and verify Rose theme is active
3. Navigate to Settings and select Ocean theme
4. Close app
5. Reopen app and verify Ocean theme is active
6. Navigate to Settings and select Midnight theme
7. Close app
8. Reopen app and verify Midnight theme is active

**Expected Results**:
- ✅ Each theme persists correctly across app restarts
- ✅ No data loss or corruption
- ✅ Theme loads immediately on app startup

---

### Test 9: Multiple User Accounts

**Objective**: Verify that theme preferences are user-specific

**Steps**:
1. Log in with User A and select Rose theme
2. Log out
3. Log in with User B and select Ocean theme
4. Log out
5. Log in with User A and verify Rose theme is selected
6. Log out
7. Log in with User B and verify Ocean theme is selected

**Expected Results**:
- ✅ Each user's theme preference is independent
- ✅ Theme switches correctly when switching between users
- ✅ No theme data is mixed between users

---

### Test 10: Loading State Display

**Objective**: Verify that loading states display correctly during theme selection

**Steps**:
1. From Settings screen, tap on a theme option
2. Observe the loading indicator on the selected theme
3. Verify other theme options are disabled during loading
4. Wait for loading to complete
5. Verify loading indicator disappears

**Expected Results**:
- ✅ Loading indicator (spinner) appears on selected theme
- ✅ Other theme options appear disabled/grayed out
- ✅ Loading indicator disappears after theme is applied
- ✅ All theme options are enabled again

---

### Test 11: Theme Application Across App

**Objective**: Verify that selected theme is applied throughout the entire app

**Steps**:
1. Select Rose theme (pink colors)
2. Navigate to different screens (Home, Products, Orders, etc.)
3. Verify pink theme colors are applied consistently
4. Go back to Settings and select Ocean theme (blue colors)
5. Navigate to different screens
6. Verify blue theme colors are applied consistently
7. Go back to Settings and select Midnight theme (purple colors)
8. Navigate to different screens
9. Verify purple theme colors are applied consistently

**Expected Results**:
- ✅ Rose theme: Pink primary colors visible on all screens
- ✅ Ocean theme: Blue primary colors visible on all screens
- ✅ Midnight theme: Purple primary colors visible on all screens
- ✅ Theme colors are consistent across all screens
- ✅ No color inconsistencies or mismatches

---

### Test 12: Initial Theme Load

**Objective**: Verify that user's theme is loaded correctly on app startup

**Steps**:
1. Select Ocean theme and close app
2. Reopen app
3. Observe the initial theme colors as app loads
4. Verify Ocean theme colors are applied immediately
5. Navigate to Settings
6. Verify Ocean theme is marked as selected

**Expected Results**:
- ✅ Ocean theme colors are applied on app startup
- ✅ No flash of default theme before switching
- ✅ Settings screen shows Ocean theme as selected

---

## Performance Testing

### Test 13: Theme Switch Performance

**Objective**: Verify that theme switching is responsive and doesn't cause lag

**Steps**:
1. From Settings screen, select a different theme
2. Measure the time from tap to visual change (should be < 1 second)
3. Verify no UI freezing or stuttering
4. Repeat for all three themes

**Expected Results**:
- ✅ Theme switch completes in < 1 second
- ✅ No UI freezing or lag
- ✅ Smooth transition between themes

---

## Edge Cases

### Test 14: Settings Screen with No Network

**Objective**: Verify Settings screen loads even without network

**Steps**:
1. Enable Airplane Mode
2. Navigate to Settings screen
3. Verify Settings screen loads
4. Verify current theme is displayed
5. Try to select a different theme
6. Verify error message appears

**Expected Results**:
- ✅ Settings screen loads without network
- ✅ Current theme is displayed correctly
- ✅ Error message appears when trying to change theme

---

### Test 15: Firebase Firestore Offline

**Objective**: Verify graceful handling when Firestore is unavailable

**Steps**:
1. Disable Firebase connectivity (if possible)
2. Navigate to Settings screen
3. Try to select a different theme
4. Verify error message appears
5. Re-enable Firebase connectivity
6. Try again and verify theme selection works

**Expected Results**:
- ✅ Error message appears when Firestore is unavailable
- ✅ App doesn't crash
- ✅ Theme selection works after Firestore is restored

---

## Verification Checklist

After completing all tests, verify:

- [ ] All three themes (Rose, Ocean, Midnight) work correctly
- [ ] Theme selection persists after app restart
- [ ] Theme persists across multiple sessions
- [ ] Theme is user-specific (different for different accounts)
- [ ] Error handling works for network failures
- [ ] Loading states display correctly
- [ ] Back navigation works
- [ ] Theme is applied consistently across all screens
- [ ] No crashes or errors occur
- [ ] Performance is acceptable (< 1 second for theme switch)
- [ ] Settings screen is accessible from Profile
- [ ] All UI elements are visible and functional

---

## Troubleshooting

### Issue: Theme doesn't persist after app restart
- **Solution**: Check Firebase Firestore rules and ensure user has write permissions
- **Check**: Verify `theme_preference` field is being saved in user document

### Issue: Error message appears when selecting theme
- **Solution**: Check network connectivity and Firebase configuration
- **Check**: Verify Firebase Firestore is properly initialized

### Issue: Settings screen doesn't load
- **Solution**: Check if user is logged in and has valid ID
- **Check**: Verify ThemeRepository and ThemeManager are properly injected

### Issue: Theme colors don't apply across all screens
- **Solution**: Verify ThemeManager singleton is being used throughout app
- **Check**: Check if all screens are using the theme colors from ThemeManager

---

## Test Results Summary

| Test # | Test Name | Status | Notes |
|--------|-----------|--------|-------|
| 1 | Navigation to Settings | ⬜ | |
| 2 | Rose Theme Selection | ⬜ | |
| 3 | Ocean Theme Selection | ⬜ | |
| 4 | Midnight Theme Selection | ⬜ | |
| 5 | Rapid Theme Switching | ⬜ | |
| 6 | Error Handling - Network | ⬜ | |
| 7 | Back Navigation | ⬜ | |
| 8 | Theme Persistence | ⬜ | |
| 9 | Multiple Users | ⬜ | |
| 10 | Loading State Display | ⬜ | |
| 11 | Theme Application | ⬜ | |
| 12 | Initial Theme Load | ⬜ | |
| 13 | Performance | ⬜ | |
| 14 | No Network | ⬜ | |
| 15 | Firestore Offline | ⬜ | |

---

## Notes

- Use ⬜ for Not Started, 🟨 for In Progress, ✅ for Passed, ❌ for Failed
- Document any issues found and their resolutions
- Include screenshots or logs if issues occur
- Test on both emulator and physical device if possible
