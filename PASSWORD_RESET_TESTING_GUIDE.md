# Password Reset - Testing Guide

## Quick Test Scenarios

### Test 1: Happy Path (Complete Success)
**Objective:** User successfully resets password end-to-end

**Steps:**
1. Open app and go to Login screen
2. Click "Forgot Password?" link
3. Enter registered email (e.g., `test@example.com`)
4. Click "Send OTP"
5. Check email inbox for OTP code
6. Enter OTP in dialog (e.g., `123456`)
7. Click "Verify"
8. Enter new password (e.g., `NewPassword123`)
9. Confirm password (e.g., `NewPassword123`)
10. Click "Reset Password"
11. See success message
12. Click "Done"
13. Try logging in with new password

**Expected Result:** ✅ Login successful with new password

---

### Test 2: Invalid Email
**Objective:** System rejects non-existent email

**Steps:**
1. Click "Forgot Password?"
2. Enter non-existent email (e.g., `nonexistent@example.com`)
3. Click "Send OTP"

**Expected Result:** ❌ Error message: "No account found with this email"

---

### Test 3: Expired OTP
**Objective:** System rejects OTP after 10 minutes

**Steps:**
1. Click "Forgot Password?"
2. Enter email and click "Send OTP"
3. Wait 10+ minutes
4. Enter OTP and click "Verify"

**Expected Result:** ❌ Error message: "OTP expired. Request a new one."

---

### Test 4: Incorrect OTP
**Objective:** System rejects wrong OTP

**Steps:**
1. Click "Forgot Password?"
2. Enter email and click "Send OTP"
3. Enter wrong OTP (e.g., `000000`)
4. Click "Verify"

**Expected Result:** ❌ Error message: "Incorrect OTP. Please try again."

---

### Test 5: Resend OTP
**Objective:** User can request new OTP

**Steps:**
1. Click "Forgot Password?"
2. Enter email and click "Send OTP"
3. In OTP step, click "Resend"
4. Check email for new OTP
5. Enter new OTP and click "Verify"

**Expected Result:** ✅ New OTP received and verified successfully

---

### Test 6: Password Too Short
**Objective:** System rejects password < 8 characters

**Steps:**
1. Complete Steps 0-1 (email + OTP)
2. In password step, enter `Pass123` (7 chars)
3. Click "Reset Password"

**Expected Result:** ❌ Error message: "Password must be at least 8 characters"

---

### Test 7: Passwords Don't Match
**Objective:** System rejects mismatched passwords

**Steps:**
1. Complete Steps 0-1 (email + OTP)
2. Enter password: `NewPassword123`
3. Enter confirm: `DifferentPass123`
4. Click "Reset Password"

**Expected Result:** ❌ Error message: "Passwords do not match"

---

### Test 8: Back Button
**Objective:** User can go back from password step

**Steps:**
1. Complete Steps 0-1 (email + OTP)
2. In password step, click "Back"

**Expected Result:** ✅ Returns to OTP step, can re-enter OTP

---

### Test 9: OTP Already Used
**Objective:** System rejects already-used OTP

**Steps:**
1. Complete Steps 0-1 (email + OTP) successfully
2. Go back to OTP step
3. Try to use same OTP again

**Expected Result:** ❌ Error message: "OTP already used. Request a new one."

---

### Test 10: Old Password Doesn't Work
**Objective:** Verify old password is invalidated

**Steps:**
1. Complete full password reset
2. Try logging in with old password

**Expected Result:** ❌ Login fails with old password

---

## Automated Test Cases

### Unit Tests (AuthViewModel)

```kotlin
// Test OTP generation
fun testSendPasswordResetOtp_GeneratesValidOtp() {
    // Verify OTP is 6 digits
    // Verify OTP stored in Firestore
    // Verify expiry is 10 minutes
}

// Test OTP validation
fun testVerifyOtpAndResetPassword_ValidatesOtp() {
    // Verify OTP must exist
    // Verify OTP must not be expired
    // Verify OTP must not be used
    // Verify OTP must match stored value
}

// Test password validation
fun testVerifyOtpAndResetPassword_ValidatesPassword() {
    // Verify password is not empty
    // Verify password is 8+ characters
    // Verify password is stored securely
}
```

### UI Tests (LoginScreen)

```kotlin
// Test dialog navigation
fun testForgotPasswordDialog_NavigatesThroughSteps() {
    // Verify Step 0 displays email input
    // Verify Step 1 displays OTP input
    // Verify Step 2 displays password input
    // Verify Step 3 displays success message
}

// Test button states
fun testForgotPasswordDialog_ButtonsDisabledWhenInvalid() {
    // Verify Send OTP disabled if email empty
    // Verify Verify disabled if OTP not 6 digits
    // Verify Reset Password disabled if passwords empty
}

// Test error messages
fun testForgotPasswordDialog_DisplaysErrorMessages() {
    // Verify error for invalid email
    // Verify error for expired OTP
    // Verify error for incorrect OTP
    // Verify error for short password
    // Verify error for mismatched passwords
}
```

---

## Manual Testing Checklist

### Pre-Testing Setup
- [ ] App is built and running
- [ ] Firebase is configured
- [ ] EmailJS is configured for OTP emails
- [ ] Test user account exists
- [ ] Email inbox is accessible

### Step 0: Email Input
- [ ] Email field accepts input
- [ ] Send OTP button is disabled when email is empty
- [ ] Send OTP button is enabled when email is filled
- [ ] Cancel button closes dialog
- [ ] OTP is sent to email within 5 seconds

### Step 1: OTP Verification
- [ ] OTP field accepts only numbers
- [ ] OTP field limits to 6 digits
- [ ] Verify button is disabled when OTP is not 6 digits
- [ ] Verify button is enabled when OTP is 6 digits
- [ ] Resend button works and sends new OTP
- [ ] Error message displays for invalid OTP
- [ ] Error message displays for expired OTP
- [ ] Error message displays for already-used OTP

### Step 2: Password Input
- [ ] New password field is masked
- [ ] Confirm password field is masked
- [ ] Reset Password button is disabled when fields are empty
- [ ] Reset Password button is enabled when fields are filled
- [ ] Back button returns to Step 1
- [ ] Error message displays for password < 8 chars
- [ ] Error message displays for mismatched passwords
- [ ] Password is stored securely

### Step 3: Success
- [ ] Success message displays
- [ ] Done button closes dialog
- [ ] User returns to login screen
- [ ] User can login with new password
- [ ] User cannot login with old password

### Edge Cases
- [ ] Multiple password reset attempts work
- [ ] OTP expires after 10 minutes
- [ ] OTP cannot be reused
- [ ] Password reset works for different users
- [ ] Dialog closes properly on all steps
- [ ] No crashes or errors in logs

---

## Performance Testing

### Load Testing
- [ ] System handles multiple simultaneous password resets
- [ ] OTP generation is fast (< 1 second)
- [ ] Email sending is reliable (> 99%)
- [ ] Firestore queries are optimized

### Stress Testing
- [ ] System handles 100+ password reset requests
- [ ] No database connection issues
- [ ] No email service timeouts
- [ ] Memory usage is stable

---

## Security Testing

### OTP Security
- [ ] OTP is random and unpredictable
- [ ] OTP cannot be brute-forced (rate limiting)
- [ ] OTP expires after 10 minutes
- [ ] OTP cannot be reused
- [ ] OTP is not logged in plain text

### Password Security
- [ ] Password is hashed before storage
- [ ] Password is not logged in plain text
- [ ] Password is transmitted over HTTPS
- [ ] Old password is invalidated
- [ ] Password reset token expires

### User Verification
- [ ] Email verification is required
- [ ] OTP verification is required
- [ ] User identity is confirmed before reset
- [ ] No unauthorized password resets

---

## Regression Testing

After each update, verify:
- [ ] Login still works with correct password
- [ ] Login fails with incorrect password
- [ ] Password reset flow works end-to-end
- [ ] All error messages display correctly
- [ ] No new crashes or errors
- [ ] Performance is acceptable

---

## Test Data

### Valid Test Cases
```
Email: test@example.com
Password: TestPassword123
OTP: 123456 (example)
```

### Invalid Test Cases
```
Email: nonexistent@example.com
Password: short (too short)
OTP: 12345 (not 6 digits)
OTP: 000000 (wrong code)
```

---

## Troubleshooting

### OTP not received
- [ ] Check spam folder
- [ ] Verify email address is correct
- [ ] Check EmailJS configuration
- [ ] Check Firebase Firestore rules

### Password reset fails
- [ ] Check Firebase Auth configuration
- [ ] Verify user exists in database
- [ ] Check Firestore rules for password_reset_otps
- [ ] Check Firestore rules for password_reset_tokens

### Dialog crashes
- [ ] Check for null pointer exceptions
- [ ] Verify all state variables are initialized
- [ ] Check for missing imports
- [ ] Review recent code changes

### Login fails after reset
- [ ] Verify password was stored correctly
- [ ] Check Firebase Auth password update
- [ ] Verify user can authenticate with new password
- [ ] Check for caching issues

---

## Sign-Off

- [ ] All test cases passed
- [ ] No critical bugs found
- [ ] Performance is acceptable
- [ ] Security requirements met
- [ ] Ready for production deployment

**Tested by:** _______________
**Date:** _______________
**Status:** ✅ APPROVED / ❌ NEEDS FIXES
