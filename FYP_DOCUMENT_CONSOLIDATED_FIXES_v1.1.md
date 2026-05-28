# Craftoria FYP — Consolidated Pre-Submission Fixes (v1.1)

**Purpose:** Single source of truth merging **Fix 10 (FR-21 Email)** and **Admin Dashboard audit** with **no contradictions**, aligned to IEEE Std 830-1998 (SRS) and Std 829 (test documentation).

**Apply in:** Word FYP document → bump to **Document Version 1.1** with revision history.

---

## 1. Executive verdict

| Area | Safe to claim in SRS? | Action |
|------|------------------------|--------|
| Android app (core features) | Mostly yes | Keep; fix FR-16, FR-13, order status, FR-29 |
| Email (EmailJS-only in doc) | **Partial** | Use **corrected FR-21** below; remove all Gmail/JavaMail |
| Admin dashboard (full UI) | **Only if your deployed admin matches** | See §4 — repo has partial `src/`; if you use a separate Vite admin build, apply full admin audit |
| Test cases all Pass | **Review** | Fix TC-21, TC-23, TC-25, TC-36 Part B, TC-58/59 roles |

---

## 2. CRITICAL: Resolved conflict — Email (Fix 10 vs Admin §2.9)

### The problem

| Source | Said |
|--------|------|
| **Fix 10** | Remove Gmail; order confirmation from “Android via EmailJS” |
| **Admin audit §2.9** | Split table: Gmail on Android + EmailJS on web |
| **Actual Android code** | `sendOrderConfirmationEmail()` = **SMTP/JavaMail**; only OTP = EmailJS |
| **Actual Cloud Functions** | `sendOrderEmail`, `sendSellerApprovalEmail` = **EmailJS** |

**One rule for the whole document:** Describe **EmailJS only** (no Gmail/JavaMail). Do **not** claim Android order email uses EmailJS until code is migrated.

### Corrected FR-21 (paste into §4.1 — replaces old FR-21 Description)

> Transactional emails are delivered via EmailJS templates and HTTP APIs as follows: (1) **Order confirmation** — a Firestore-triggered Cloud Function (`sendOrderEmail`) sends an EmailJS email when an order document is created; the Android checkout flow may additionally dispatch a confirmation email at order success without blocking order completion. (2) **Seller approval and rejection** — Firebase Cloud Functions invoked from the admin dashboard (`sendSellerApprovalEmail`, `sendSellerApplicationApprovalEmail`), using EmailJS. (3) **Password reset** — OTP sent via EmailJS HTTP API from the Android app (`EmailService.sendPasswordResetOtp`); after OTP verification, Firebase Authentication sends the password reset link.

### Corrected FR-21 Dependencies

> EmailJS account and templates active. Order confirmation: `functions/index.js` (`sendOrderEmail`), `functions/emailService.js`; optional Android trigger: `CheckoutScreen.kt` → `EmailService.sendOrderConfirmationEmail`. Seller approval/rejection: Cloud Functions from admin dashboard. Password reset OTP: `EmailService.sendPasswordResetOtp()` (Android).

### §2.4 Email Services (single line — matches FR-21)

> Email Services: EmailJS HTTP API (order confirmations via Cloud Functions on order create and optional Android checkout trigger; seller approval/rejection via Cloud Functions from admin dashboard; password reset OTP via Android client).

### DELETE everywhere (global find)

- `Gmail SMTP`, `JavaMail`, `via JavaMail`, `using JavaMail`, `Gmail SMTP credentials`

### DO NOT use (contradicts code + Fix 10)

- “Order confirmation from Android client **via EmailJS**” (unless you migrate `EmailService.kt`)
- Admin audit table splitting “Gmail on Android / EmailJS on web” — **remove**; use FR-21 three bullets only

### TC-36 Part B (admin path)

**Expected Result Part B — use:**

> Rejection/approval email delivered to seller via EmailJS Cloud Function when admin confirms reject/approve; in-app notification always sent; email failure does not block admin action.

**Not:** “Gmail SMTP asynchronously”

### Optional appendix (IEEE “Assumptions / Known limitations” — honest, defensible)

> *Implementation note:* The production email architecture uses EmailJS. The Android `EmailService` module also contains a legacy SMTP implementation for order confirmation; the primary documented path is the Cloud Function on order creation. Teams may disable the Android SMTP path to avoid duplicate emails.

---

## 3. Admin dashboard — unified with Fix 10 (no email conflict)

Apply **only** if your submitted admin is the **full React dashboard** (Vite, all routes, mockups 6.31–6.41). If the examiner only sees the fragmented `Craftoria/src/pages` subset, soften claims per §4.

### P0 — Architecture (§2.1, §3.4)

**Replace:** “secure backend APIs”  
**With:** “Firebase Authentication and Cloud Firestore via the Firebase Web SDK over HTTPS (TLS); authorization enforced client-side (RBAC) and by Firestore Security Rules.”

### P0 — Remove or move to §9 Future Work

- Light/Dark mode (not in theme)
- Dedicated **Audit Log Viewer** UI with search/filter (logging exists in `admin_activities`; no `/audit-log` page)
- NFR-07 table **pagination** for 100+ rows (not implemented)

### P0 — Fix §3.1 Web Admin modules

| Wrong in v1.0 | Correct |
|---------------|---------|
| Activity Audit Log screen | **Admin activity logging** (Firestore `admin_activities`); dashboard **Recent Activity** = operational feed |
| Chart.js or Recharts | **Recharts only** (if charts exist in your build) |
| KPI: revenue, orders today | **Four cards:** Total Users, Active Sellers, Approved Products, Total Orders (note placeholder % deltas if hardcoded) |
| Content Moderation module | **Product flagging** + **Reports & Complaints** |

### P0 — FR-26 badges (align Sidebar)

| Badge | Meaning |
|-------|---------|
| Seller Verification | Pending seller applications |
| Product Management | Pending product approvals (`viewed_by_admins`) |
| User Management | New users (24h, unviewed) |
| Order Oversight | Pending orders + recent refunds |
| Reports | Status `New` |

### P0 — FR-27 / TC-58, TC-59

**Learning resources CRUD:** **Super Admin only** (not generic “admin”).  
**TC-58/59 preconditions:** “Logged in as **Super Admin**” for create/edit/delete.

### P0 — FR-24 (align with TC-63)

> Admin actions are logged to Firestore `admin_activities` / `admin_audit_logs` where implemented (e.g. seller/product approval, commission settings, learning resource changes, report resolution, email events). A dedicated searchable audit UI is out of prototype scope. User suspend/activate and some product edits may not write to the global audit collection. Refunds maintain per-document `audit_trail`.

### P1 — Add to §2.4 Web Admin

| Item | Value |
|------|--------|
| Build | Vite (if used) |
| UI | React 18, MUI 5 |
| Charts | Recharts 2.x |
| Routing | React Router 6 |
| Toasts | react-hot-toast |

### P1 — Route table (§3.1)

`/login`, `/dashboard`, `/sellers`, `/products`, `/users`, `/orders`, `/co-seller-stores`, `/learning-resources`, `/reports`, `/commissions`, `/notifications`, `/settings`

Seller Verification: **Applications** tab + **Identity Verifications** tab (ML Kit).

---

## 4. Repository vs document scope (avoid examiner trap)

| Claim in FYP | `Craftoria` repo state | What to do |
|--------------|------------------------|------------|
| Full admin + Dashboard.jsx + charts | `src/pages/` has 6 pages; no `Dashboard.jsx` in repo | State “deployed admin build” URL or attach admin repo; or mark dashboard as partial in v1.1 |
| `craftoria-admin-web` | Not found under AndroidStudioProjects | If it exists elsewhere, cite it in revision history |
| All TC Pass | Some features unwired (refund auto-job, FirebaseConnectionManager) | Conditional Pass or fix code |
| EmailJS Android orders | SMTP in `EmailService.kt` | Use corrected FR-21; migrate or CF-only |

---

## 5. Master contradiction matrix (must be zero before print)

| # | Topic | v1.0 conflict | v1.1 resolution |
|---|--------|---------------|-----------------|
| 1 | Order email | Fix 10: Android EmailJS vs code: SMTP | FR-21: CF EmailJS primary; Android “optional trigger” |
| 2 | Order email | Admin §2.9 Gmail Android | **Delete** §2.9 split table; use FR-21 only |
| 3 | Seller email | FR-21: CF from admin | Matches `sendSellerApprovalEmail` + admin fetch |
| 4 | OTP | EmailJS Android | Matches code |
| 5 | Duplicate order emails | CF + Android both send | Document in FR-21 / TC-15 / TC-36 |
| 6 | Payment split | FR-16 equal vs TC-23 50/50 vs 60/40 | **Product-proportional** after commission |
| 7 | Order status | FR-09 vs TC-21 `Confirmed` | **pending → processing → shipped → delivered → completed** |
| 8 | Refund auto | FR-13 buyer-only vs manager all pending | Say “scheduled auto-approval” only if wired; else manual |
| 9 | FR-29 offline | Full UX vs unused manager | Partial / checkout cache only |
| 10 | Moderator products | TC-66 no approve vs permissions | Can approve products; cannot verify sellers |
| 11 | Audit | FR-24 all actions vs TC-63 partial | Use FR-24 text in §3 |
| 12 | Cloudinary delete | NFR-03 immediate | Metadata removed; CDN lifecycle / CF cleanup |
| 13 | Architecture | Backend APIs vs Firebase SDK | Firebase Web SDK wording |
| 14 | Charts | Chart.js or Recharts | Recharts only |
| 15 | Featured products | TC-51 | Super Admin only if implemented; else Future Work |

---

## 6. IEEE 830 compliance checklist

- [ ] **Revision history** (v1.0 → v1.1, date, summary of email + admin alignment)
- [ ] Each FR: single clear “shall”; no conflicting shall in same FR
- [ ] **Out of scope** lists Gmail, JavaMail, real payment gateway, iOS
- [ ] **Assumptions** §2.6: EmailJS only (not Gmail)
- [ ] **No implementation code** in main SRS (file names OK in dependencies)
- [ ] **Traceability:** RTM column “Implemented (Y/N/Partial)”
- [ ] **Known limitations** subsection (pagination, audit UI, KPI placeholders, Android SMTP legacy)
- [ ] Test cases (829): Expected results match FR wording; no Pass for unwired features
- [ ] Diagrams: Firebase SDK boundary, not generic API server
- [ ] Definitions §1.4: Payment Split = proportional, not equal

---

## 7. Priority edit order in Word

1. Global remove: Gmail SMTP, JavaMail  
2. Paste FR-21 + §2.4 (§2)  
3. §3.3 Cloud Functions row + EmailJS row (see `FYP_FIX_10_EMAIL_PATHS.md`)  
4. TC-15, TC-36  
5. Admin P0 (§3)  
6. FR-16, FR-09, TC-21, TC-23  
7. FR-24, FR-26, FR-27, TC-58/59/66  
8. NFR-03, NFR-07  
9. §9 Future Work moves  
10. Re-read entire doc for “EmailJS via EmailJS” duplicates  

---

## 8. Files to use

| File | Use for |
|------|---------|
| `FYP_FIX_10_EMAIL_PATHS.md` | Word find/replace for email sections + TC-15/36 |
| This file | Master consistency + FR-21 final text + admin P0 |
| Previous review | Android FR-16, FR-13, FR-29 detail |

---

## 9. Viva one-liner (email)

> “Craftoria uses EmailJS for transactional mail: Cloud Functions send order confirmations and seller approval emails; the Android app sends password reset OTPs via EmailJS; order emails are triggered when orders are written to Firestore, and checkout can fire a second non-blocking send.”

Do **not** say “all emails from Android EmailJS” unless code is migrated.
