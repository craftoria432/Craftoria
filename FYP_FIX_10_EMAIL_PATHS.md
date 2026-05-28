# Fix 10 — FR-21 Email Paths (Apply to FYP Word Document)

Use **Find & Replace** in your FYP `.docx` for each item below. Order matters for overlapping phrases — do **Gmail SMTP / JavaMail removals first**, then apply the new text.

---

## FR-21 (Section 4.1) — Replace entire Description and Dependencies

### Description — REPLACE WITH:
```
Transactional emails are delivered via EmailJS templates and HTTP APIs as follows: (1) Order confirmation — a Firestore-triggered Cloud Function (sendOrderEmail) sends EmailJS email when an order document is created; the Android checkout flow may additionally dispatch a confirmation email at order success without blocking order completion. (2) Seller approval and rejection — HTTP/callable Firebase Cloud Functions (sendSellerApprovalEmail, sendSellerApplicationApprovalEmail) invoked from the admin dashboard, using EmailJS. (3) Password reset — OTP sent via EmailJS HTTP API from the Android app (EmailService.sendPasswordResetOtp); after OTP verification, Firebase Authentication sends the password reset link.
```

### Dependencies — REPLACE WITH:
```
EmailJS account and templates active (order confirmation, seller approval/rejection, password reset OTP). Order confirmation: Cloud Function sendOrderEmail (functions/index.js, functions/emailService.js); optional Android trigger at checkout (CheckoutScreen.kt → EmailService.sendOrderConfirmationEmail). Seller approval/rejection: Cloud Functions called from admin dashboard (e.g. SellerApplicationsAndVerifications_UPDATED.jsx, emailNotificationService.js). Password reset OTP: Android EmailService.sendPasswordResetOtp().
```

**Do not write** “Android order confirmation via EmailJS” unless the Android method is migrated to EmailJS (currently only OTP uses EmailJS on Android; order confirmation on Android still uses SMTP in EmailService.kt — see Implementation Note at end of this file).

### Rationale — keep as-is (optional tweak):
```
Keep users informed beyond push notifications.
```

---

## Section 2.4 — Email Services line

### FIND:
```
Email Services: Gmail SMTP via JavaMail (order confirmations, seller approvals); EmailJS HTTP API (password reset OTP)
```

### REPLACE WITH:
```
Email Services: EmailJS HTTP API (order confirmations via Android client and Cloud Functions; seller approval/rejection emails via Cloud Functions; password reset OTP via Android client)
```

---

## Section 1.2 — In Scope, Backend bullet

### FIND:
```
Gmail SMTP (via JavaMail) for order confirmation and seller approval emails; EmailJS for password reset OTP notifications
```

### REPLACE WITH:
```
EmailJS HTTP API for transactional emails (order confirmations via Android client and Cloud Functions; seller approval/rejection via Cloud Functions; password reset OTP via Android client)
```

---

## Section 1.7 — Constraints #2

### FIND:
```
Firebase, ML Kit, Cloudinary, Gmail SMTP, and EmailJS services
```

### REPLACE WITH:
```
Firebase, ML Kit, Cloudinary, and EmailJS services
```

---

## Section 2.1 — Product Perspective, Backend (d.)

### FIND:
```
Order confirmation and seller approval emails are sent directly from Android EmailService via Gmail SMTP; password reset OTPs via EmailJS.
```

### REPLACE WITH:
```
Transactional emails use EmailJS: order confirmations via Cloud Function on order creation (and optional Android trigger at checkout); seller approval and rejection emails via Cloud Functions when the admin dashboard acts; password reset OTP via EmailJS from the Android client.
```

---

## Section 2.6 — Assumptions #7

### FIND:
```
Gmail SMTP and EmailJS services remain operational for transactional email notifications.
```

### REPLACE WITH:
```
EmailJS services remain operational for transactional email notifications.
```

---

## Section 3.3 — Software Interfaces table

### 1) Firebase Cloud Functions row — Description column

**FIND:**
```
Automates push notifications (order, payment, chat events) via FCM. Transactional emails are NOT handled by Cloud Functions. Negotiation logic is handled client-side.
```

**REPLACE WITH:**
```
Automates push notifications (order, payment, chat events) via FCM. Sends transactional emails via EmailJS (order confirmation on order creation; seller approval/rejection from admin dashboard). Negotiation logic is handled client-side.
```

### 2) DELETE entire Gmail SMTP row

**DELETE this row:**
| Gmail SMTP (via JavaMail) | Android email client library | Sends order confirmation and seller approval emails directly from Android EmailService on Dispatchers.IO |

### 3) ADD EmailJS row (after Firebase Cloud Messaging or Cloud Functions)

| EmailJS HTTP API | Email delivery service | Order confirmations (Android client + Cloud Functions on order create); seller approval/rejection (Cloud Functions from admin dashboard); password reset OTP (Android client) |

---

## Section 3.4 — Communication Interfaces table

### 1) DELETE Gmail SMTP row

**DELETE:**
| Gmail SMTP ↔ Android EmailService | HTTPS / REST API | Sends order confirmation and seller approval emails via JavaMail on Dispatchers.IO |

### 2) UPDATE EmailJS row

**FIND:**
```
EmailJS ↔ Android EmailService 	HTTPS API	Sends password reset OTP emails via EmailJS HTTP API
```

**REPLACE WITH:**
```
EmailJS ↔ Android EmailService / Firebase Cloud Functions	HTTPS REST API	Order confirmations (Android + Cloud Functions on order create); seller approval/rejection (Cloud Functions); password reset OTP (Android)
```

---

## Section 7 — Test Cases

### TC-15 — Expected Result

**FIND:**
```
email confirmation sent asynchronously via Gmail SMTP using JavaMail; cart cleared after navigation
```

**REPLACE WITH:**
```
email confirmation sent asynchronously via EmailJS from Android client (and optionally via Cloud Function sendOrderEmail on order creation); cart cleared after navigation
```

### TC-36 — Objective

**FIND:**
```
Verify order confirmation and seller rejection emails are sent via Gmail SMTP asynchronously, and that order completion is never blocked by email failure
```

**REPLACE WITH:**
```
Verify order confirmation emails via EmailJS (Android client and/or Cloud Functions), seller rejection/approval emails via EmailJS Cloud Functions from admin dashboard, and that order completion is never blocked by email failure
```

### TC-36 — Preconditions

**FIND:**
```
Gmail SMTP credentials configured in EmailService; order placed successfully; seller rejection scenario available
```

**REPLACE WITH:**
```
EmailJS configured (Android and Cloud Functions); order placed successfully; seller rejection scenario available
```

### TC-36 — Test Steps Part A, step 3

**FIND:**
```
Email dispatched via Gmail SMTP using JavaMail on Dispatchers.IO
```

**REPLACE WITH:**
```
Email dispatched via EmailJS HTTP API from Android EmailService (CheckoutScreen) on Dispatchers.IO; Cloud Function sendOrderEmail may also send on Firestore order create
```

### TC-36 — Test Steps Part B

Add after step 7 (admin Confirm Reject) if documenting rejection email:
```
Seller rejection email sent via Cloud Functions (EmailJS) when implemented; otherwise in-app notification only
```

### TC-36 — Expected Result Part A

**FIND:**
```
Confirmation email delivered containing order ID, total amount, payment method, and delivery address; email sent asynchronously after navigation to success screen; no email log created in Firestore
```

**REPLACE WITH:**
```
Confirmation email delivered via EmailJS containing order ID, total amount, payment method, and delivery address; sent asynchronously from Android after navigation to success screen; Cloud Function may send duplicate confirmation on order create (non-blocking)
```

### TC-36 — Expected Result Part B

**FIND:**
```
Rejection email delivered to seller containing rejection reason and re-application guidance; sent via Gmail SMTP asynchronously
```

**REPLACE WITH:**
```
Rejection/approval email delivered to seller via EmailJS Cloud Function (sendSellerApprovalEmail or rejection template) when admin acts; contains reason and guidance where applicable
```

---

## Global find-replace (run last — catches stragglers)

| Find | Replace |
|------|---------|
| `Gmail SMTP` | `EmailJS` |
| `via JavaMail` | *(delete — remove phrase)* |
| `JavaMail` | `EmailJS HTTP API` |
| `Gmail SMTP credentials` | `EmailJS configuration` |
| `using JavaMail` | `via EmailJS HTTP API` |

**After global replace:** Re-read FR-21, §2.4, §3.3, and TC-36 to ensure wording is not duplicated (e.g. “EmailJS via EmailJS”).

---

## Kotlin / implementation reference (for defense — not pasted into SRS verbatim)

| Email type | Documented (SRS) | Actual code path |
|------------|------------------|------------------|
| Password reset OTP | Android → EmailJS | `EmailService.sendPasswordResetOtp()` — **EmailJS** |
| Order confirmation | EmailJS (CF + optional Android) | **CF:** `functions/index.js` → `sendOrderEmail` → `emailService.js` — **EmailJS**. **Android:** `CheckoutScreen.kt` → `sendOrderConfirmationEmail()` — **SMTP/JavaMail** (legacy; migrate to EmailJS or document CF as primary) |
| Seller approval | Admin → Cloud Function → EmailJS | `SellerApplicationsAndVerifications_UPDATED.jsx` → `sendSellerApprovalEmail` HTTP — **EmailJS** |
| Seller rejection | Admin → Cloud Function → EmailJS | Same stack if rejection template is wired |

**SRS rule:** Do not mention Gmail SMTP or JavaMail in the FYP document. Describe the **EmailJS-based architecture** (Cloud Functions + admin + OTP). For viva, state that order confirmation is **primarily** sent by the Cloud Function on order create; Android sends at checkout as a redundant non-blocking path.

**Before defense (recommended):** Either migrate `sendOrderConfirmationEmail()` to EmailJS on Android, or remove the Android SMTP call and rely on `sendOrderEmail` only — then SRS and demo match exactly.

---

## Quick verification checklist

- [ ] FR-21 Description lists three paths (order dual, seller CF, OTP Android)
- [ ] §2.4 Email Services line matches FR-21
- [ ] No `Gmail SMTP` or `JavaMail` anywhere in document
- [ ] §3.3 Cloud Functions row says emails **are** sent via EmailJS
- [ ] §3.3 has EmailJS row; no Gmail row
- [ ] §3.4 has no Gmail row; EmailJS row covers Android + CF
- [ ] TC-15 and TC-36 reference EmailJS, not Gmail
