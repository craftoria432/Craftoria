# Email Implementation - ACTUAL vs. UNUSED

## Summary
Your project uses **TWO separate email solutions**:

1. **Gmail SMTP (JavaMail)** — Used for ORDER CONFIRMATIONS & SELLER APPROVALS
2. **EmailJS** — Used ONLY for PASSWORD RESET OTPs

The EmailJS **Order Confirmation** template (`template_4988y7h`) visible in your EmailJS dashboard is **UNUSED and leftover**.

---

## Detailed Breakdown

### ✅ IMPLEMENTED: Gmail SMTP (JavaMail)

**Location:** `app/src/main/java/com/gcuf/craftoria/services/EmailService.kt`

**Service ID:** `service_muotzyb` (actually Gmail, not EmailJS)  
**Email Account:** `itxzmaheri@gmail.com`  
**App Password:** `etsj ljwo shbt nhod`

**What it handles:**
- ✅ Order confirmation emails (`sendOrderConfirmationEmail`)
- ✅ Seller approval emails (`sendSellerApprovalEmail`)

**Flow:**
1. Buyer places order → Android app calls `EmailService.sendOrderConfirmationEmail()`
2. Function uses Gmail SMTP via JavaMail
3. Email sent directly from `itxzmaheri@gmail.com`
4. **No EmailJS involvement**

---

### ✅ IMPLEMENTED: EmailJS HTTP API

**Location:** `app/src/main/java/com/gcuf/craftoria/services/EmailService.kt` (lines ~80-130)

**Service ID:** `service_muotzyb`  
**Template ID:** `template_k3yupgg` (Password Reset OTP)  
**Public Key:** `wfWfBLv5JKIDKJkj_`

**What it handles:**
- ✅ Password reset OTP emails only (`sendPasswordResetOtp`)

**Flow:**
1. User requests password reset → Android app calls `EmailService.sendPasswordResetOtp()`
2. Function makes HTTP POST to EmailJS API
3. EmailJS sends email using `template_k3yupgg`
4. **Order confirmations do NOT go through this**

---

### ❌ UNUSED: EmailJS Order Confirmation Template

**Dashboard Template ID:** `template_4988y7h`  
**Status:** LEFTOVER/OBSOLETE

**Evidence:**
- ✅ `EmailService.kt` uses Gmail SMTP for order confirmations
- ✅ Web-side `src/services/emailService.js` has EmailJS code but isn't called for order processing
- ✅ Cloud functions `functions/emailService.js` has EmailJS code but **order confirmations are sent from Android side via Gmail**
- ✅ No code references `template_4988y7h` anywhere

**Why it exists:**
Likely created during development/testing and never removed from EmailJS dashboard.

---

## Documentation Issues

Your code comments are **misleading**:

```kotlin
// ── Password Reset OTP via EmailJS HTTP API ───────────────────────────────
// Uses a separate EmailJS account/template dedicated to password reset.
```

This is accurate. ✅

But the code **lacks documentation** for the order confirmation flow, making it unclear that Gmail is being used.

---

## Recommendation: Clean Up

### Option 1: Keep Current (Simpler)
- **Keep:** Gmail SMTP for orders + EmailJS for OTP
- **Delete:** EmailJS `template_4988y7h` from dashboard
- **Update:** Add clear comments explaining why two services are used
- **Reason:** Order volume won't overwhelm Gmail limits; OTP needs fast delivery

### Option 2: Consolidate to EmailJS (Recommended for Scale)
- **Migrate:** Order confirmations to EmailJS using a dedicated template
- **Migrate:** Seller approval emails to EmailJS
- **Keep:** OTP template for consistency
- **Reason:** Single vendor, cleaner integration, better logging, easier to audit
- **Steps:**
  1. Create new EmailJS templates for orders & approvals
  2. Update `EmailService.kt` to use EmailJS HTTP API instead of Gmail SMTP
  3. Delete Gmail password from code
  4. Test thoroughly

---

## Current Code State

### What's Working
| Feature | Method | Service | Status |
|---------|--------|---------|--------|
| Order Confirmation | Android → EmailService | Gmail SMTP | ✅ Working |
| Seller Approval | Android → EmailService | Gmail SMTP | ✅ Working |
| Password Reset OTP | Android → EmailService | EmailJS API | ✅ Working |
| Web Order Confirmation | src/services/emailService.js | EmailJS | ❌ Not called |
| Cloud Fn Order Confirmation | functions/emailService.js | EmailJS | ❌ Not called |

### Security Notes
- Gmail app password is **hardcoded in Kotlin** (should use Firebase Secrets)
- EmailJS public key is visible in code (acceptable - it's meant to be public)
- OTP template parameters are sent over HTTPS ✅

---

## Action Items

1. **Immediate:** Delete unused `template_4988y7h` from EmailJS dashboard
2. **Soon:** Add code comments explaining why two email services exist
3. **Later:** Either consolidate to EmailJS or migrate Gmail credentials to Firebase Secrets Manager
4. **Testing:** Verify password reset OTP still works (it uses EmailJS)

---

## Questions to Clarify

- **Why two services?** Was this intentional or legacy?
- **Web dashboard:** Do you need email sending capability on the web side?
- **Scale:** Are you concerned about Gmail's sending limits (typically 500 emails/day free)?
