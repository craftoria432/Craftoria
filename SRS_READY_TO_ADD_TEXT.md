# Craftoria SRS - Ready-to-Add Text
## Copy-Paste Ready SRS Updates

This document contains all the SRS text ready to add or update in your SRS document. Simply copy and paste the sections below into the appropriate locations in your SRS.

---

## SECTION 2.1: PRODUCT PERSPECTIVE (FIX)

**Location:** Section 2.1 Product Perspective  
**Action:** Replace existing text  
**Status:** ✅ Ready to add

### Current Text (INCORRECT):
```
Craftoria is a two-part integrated platform designed to connect women artisans 
with buyers through a secure and inclusive digital marketplace:
```

### New Text (CORRECT):
```
Craftoria is a three-component integrated platform designed to connect women 
artisans with buyers through a secure and inclusive digital marketplace:

1. Android Mobile Application (Frontend) – for sellers and buyers
2. Web Admin Dashboard (Backend Control) – for administrators
3. Firebase Backend Infrastructure – for data management and automation
```

---

## SECTION 4.1: FUNCTIONAL REQUIREMENTS

### FR-17: Commission System (UPDATE)

**Location:** Section 4.1, FR-17  
**Action:** Update existing requirement  
**Status:** ✅ Ready to update

### Current Text (INCORRECT):
```
FR-17: Commission System
...
Description: The system shall track admin commissions at a fixed 5% per completed order...
```

### New Text (CORRECT):
```
FR-17: Commission System

Identifier: FR-17

Description: The system shall track admin commissions at a configurable rate 
(default: 5%, maximum: 5%) per completed order. Administrators can modify the 
commission rate through the Settings screen. Aggregated commission earnings shall 
be visible in the admin dashboard with historical tracking.

Rationale: Platform revenue tracking, transparency, and flexibility for business 
model adjustments. Aligns with proposal document commitment of "low commission 
fee (≤5%)".

Dependencies: FR-15 (payment must be processed); Firebase Cloud Functions active; 
Admin Settings access required.

Priority: Medium
```

---

### FR-28: Session Management (NEW)

**Location:** Section 4.1, Functional Requirements  
**Action:** Add as new requirement  
**Status:** ✅ Ready to add

```
FR-28: Session Management

Identifier: FR-28

Description: The system shall provide users with an explicit logout option that 
immediately invalidates the current authentication token. Upon logout, all cached 
user data shall be cleared from the device and the user shall be redirected to 
the login screen. The navigation stack shall be cleared to prevent back-button 
access to authenticated screens.

Rationale: Security best practice to prevent unauthorized access on shared devices.

Dependencies: Firebase Authentication; local device storage management.

Priority: High
```

---

### FR-29: Offline Error Handling (NEW)

**Location:** Section 4.1, Functional Requirements  
**Action:** Add as new requirement  
**Status:** ✅ Ready to add

```
FR-29: Offline Error Handling

Identifier: FR-29

Description: The system shall detect internet connectivity loss in real time and 
display a user-friendly error message. Connection state shall be monitored 
continuously and classified as GOOD, SLOW, or OFFLINE. Unsaved form data 
including cart contents, product listings, and checkout information shall be 
preserved until connectivity is restored. Upon reconnection, the system shall 
automatically re-synchronize with Firebase.

Rationale: Critical for users on unstable networks; improves user experience and 
data integrity for low-connectivity environments.

Dependencies: FirebaseConnectionManager utility; Firebase real-time listeners; 
Android ConnectivityManager API.

Priority: High
```

---

## SECTION 4.2: NON-FUNCTIONAL REQUIREMENTS

### NFR-08: Data Integrity Requirements (NEW)

**Location:** Section 4.2, Non-Functional Requirements  
**Action:** Add as new requirement  
**Status:** ✅ Ready to add

```
NFR-08: Data Integrity Requirements

The system shall implement the following data integrity measures:

• Payment processing transactions shall use Firestore atomic batch writes to 
  ensure data consistency within transaction scope.

• Cross-collection transactions shall implement application-level consistency 
  checks and retry logic via PaymentValidator and PaymentRetryManager utilities.

• Payment validation shall prevent duplicate or invalid transactions through 
  idempotency keys and transaction status tracking.

• Order status updates shall be atomic and consistent within individual document 
  writes; related entity updates shall be coordinated through Cloud Functions.

• All payment operations shall be logged with complete audit trails including 
  action type, old/new values, timestamps, and user context.

Rationale: Firestore provides atomic writes within transaction scope but not full 
ACID across distributed transactions. Application-level consistency checks and 
retry logic ensure data integrity across related entities.

Dependencies: Firestore batch writes; Cloud Functions; PaymentValidator and 
PaymentRetryManager utilities.

Priority: High
```

---

### NFR-04: Usability Requirements (RECOMMENDED - NEW)

**Location:** Section 4.2, Non-Functional Requirements  
**Action:** Add as new requirement (RECOMMENDED)  
**Status:** ⚠️ Not yet implemented - Consider for future sprint

```
NFR-04: Usability Requirements

The system shall conform to WCAG 2.1 Level AA accessibility guidelines where 
applicable, including:

• Color contrast ratios of at least 4.5:1 for normal text and 3:1 for large text
• Keyboard navigation support for all interactive elements
• Screen reader compatibility for critical user flows
• Alternative text for all images and icons
• Proper heading hierarchy and semantic HTML structure
• Focus indicators visible on all interactive elements

Rationale: Ensures inclusive design for users with disabilities. Target users 
(women artisans with limited technical skills) benefit significantly from 
accessibility compliance. Improves market reach and user satisfaction.

Dependencies: UI/UX framework support; accessibility testing tools; user testing 
with assistive technologies.

Priority: High
```

---

## IMPLEMENTATION CHECKLIST

### Ready to Add Immediately (✅):
- [ ] Update Section 2.1 (Three-Component)
- [ ] Update FR-17 (Commission Rate)
- [ ] Add FR-28 (Session Management)
- [ ] Add FR-29 (Offline Error Handling)
- [ ] Add NFR-08 (Data Integrity)

### Recommended for Future (⚠️):
- [ ] Add NFR-04 (WCAG 2.1 Accessibility) - Plan implementation in next sprint

---

## VERIFICATION EVIDENCE

All requirements have been verified against actual implementation:

| Requirement | Verification File | Status |
|---|---|---|
| FR-28 | `FR_28_SESSION_MANAGEMENT_VERIFICATION.md` | ✅ Verified |
| FR-29 | `PAYMENT_INTEGRITY_VERIFICATION.md` | ✅ Verified |
| NFR-08 | `PAYMENT_INTEGRITY_VERIFICATION.md` | ✅ Verified |
| FR-17 | `SRS_REQUIREMENTS_VERIFICATION_REPORT.md` | ✅ Verified |
| Section 2.1 | `SRS_REQUIREMENTS_VERIFICATION_REPORT.md` | ✅ Verified |
| NFR-04 | `SRS_FINAL_VERIFICATION_SUMMARY.md` | ⚠️ Recommended |

---

## NOTES

1. **Commission Rate (FR-17):** Changed from "fixed 5%" to "configurable ≤5%" to match your proposal document and actual implementation.

2. **Section 2.1:** Changed from "two-part" to "three-component" to accurately reflect the Android app, Web dashboard, and Firebase backend.

3. **FR-28 & FR-29:** These are security and reliability features already implemented in your codebase. Adding them to SRS documents the existing functionality.

4. **NFR-08:** Clarifies that Firestore provides atomic writes within transaction scope, not full ACID across distributed transactions. Application-level consistency is maintained through PaymentValidator and PaymentRetryManager.

5. **NFR-04 (Accessibility):** Recommended for future implementation to ensure inclusive design for your target users (women artisans with limited technical skills).

---

## QUICK COPY-PASTE GUIDE

1. **For Section 2.1:** Copy the "New Text (CORRECT)" section
2. **For FR-17:** Replace the entire FR-17 section with the new text
3. **For FR-28, FR-29, NFR-08:** Add these as new requirements in Section 4.1 and 4.2
4. **For NFR-04:** Consider adding in a future sprint

All text is ready to copy directly into your SRS document.
