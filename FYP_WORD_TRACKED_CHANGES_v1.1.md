# Craftoria FYP — Word Tracked-Changes Checklist (v1.0 → v1.1)

**How to use in Microsoft Word**

1. Save a backup: `FYP Document Final_backup.docx`
2. Set **Document Version** to **1.1**
3. Paste **§0 Revision History** (below) after the title page
4. Run edits in **§Order of application** (end of file) — do **not** skip Step 1 (global Gmail removal)
5. For each change: **Ctrl+H** Find → Replace, or select paragraph and paste **REPLACE WITH**
6. Mark completed rows in the checklist tables
7. Final pass: **Ctrl+F** search `Gmail`, `JavaMail`, `Chart.js`, `secure backend APIs`, `equally among`

**IEEE:** Std 830-1998 (SRS), Std 829 (test cases). Keep requirement IDs (FR-xx, TC-xx, NFR-xx) unchanged unless adding new FRs at end of §4.1.

---

## §0 — INSERT: Revision history (new page or after Declaration)

**INSERT this table:**

| Version | Date | Author(s) | Summary of changes |
|---------|------|-----------|-------------------|
| 1.0 | [Original date] | Ahmed, Malik Bilal, Haider Ali | Initial SRS per IEEE 830-1998 |
| 1.1 | [Submission date] | Same | Aligned email architecture to EmailJS; corrected admin dashboard interfaces (Firebase SDK); updated FR-09/16/21/24/26/29, NFR-03/07; harmonized test cases TC-15/21/23/36/58/59/66; added known limitations and RBAC matrix |

---

## §0 — INSERT: Known limitations (new subsection before Section 5 or as Appendix A)

**INSERT heading:** `1.8 Known Limitations (Prototype)`

**INSERT body:**

> The following limitations apply to the prototype submitted for academic evaluation and do not reduce the validity of core functional requirements where implemented.
>
> 1. **Email:** Order confirmation is primarily sent via EmailJS when an order is created in Firestore (Cloud Function). The Android checkout flow may trigger an additional non-blocking confirmation send. Duplicate emails are possible if both paths are enabled.
> 2. **Admin audit:** Admin actions are persisted in Firestore (`admin_activities`, `admin_audit_logs`, refund `audit_trail`); a dedicated searchable audit-log screen is not included in the prototype scope.
> 3. **Dashboard KPIs:** Period-over-period percentage indicators on the admin dashboard may use illustrative placeholders rather than live calculated deltas.
> 4. **Pagination:** Admin tables load client-side datasets; server-side pagination for 100+ records is planned for future releases (see Section 9).
> 5. **Offline handling:** Connectivity classification utility exists; full integration across all Android screens is not complete; checkout form data may be cached.
> 6. **Verification images:** Seller verification metadata is updated in Firestore upon admin review; immediate deletion of Cloudinary assets may require server-side cleanup automation.
> 7. **Refund auto-approval:** Buyer-initiated refunds may be auto-approved after a defined period without seller/admin response when the background approval job is active; seller-initiated refunds require admin approval.

---

## STEP 1 — Global find-replace (run first)

| # | Find | Replace with | Notes |
|---|------|--------------|-------|
| G1 | `Gmail SMTP` | `EmailJS` | Then fix any awkward "EmailJS and EmailJS" manually |
| G2 | `via JavaMail` | *(empty — delete phrase)* | |
| G3 | `using JavaMail` | `via EmailJS HTTP API` | Only where sentence still makes sense |
| G4 | `JavaMail` | `EmailJS HTTP API` | Re-read FR-21 after this |
| G5 | `Gmail SMTP credentials` | `EmailJS configuration` | |
| G6 | `Chart.js or Recharts` | `Recharts` | All occurrences |
| G7 | `Chart.js / Recharts` | `Recharts` | |
| G8 | `secure backend APIs` | `Firebase Authentication and Cloud Firestore via the Firebase Web SDK` | §2.1, §3.4 only — verify each hit |

**After G1–G8:** Search `Gmail`, `JavaMail`, `Chart.js` — expect **zero** hits.

---

# SECTION 1 — INTRODUCTION

## Change 1.1 — Document version line

| | |
|---|---|
| **FIND** | `Document Version: 1.0` |
| **REPLACE WITH** | `Document Version: 1.1` |

---

## Change 1.2 — §1.2 In Scope, Backend (Firebase) bullet

| | |
|---|---|
| **FIND** | `Gmail SMTP (via JavaMail) for order confirmation and seller approval emails; EmailJS for password reset OTP notifications` |
| **REPLACE WITH** | `EmailJS HTTP API for transactional emails (order confirmations via Cloud Functions on order create and optional Android checkout trigger; seller approval and rejection via Cloud Functions from the admin dashboard; password reset OTP via the Android client)` |

*If Step G1 already changed this, use REPLACE WITH above as final text.*

---

## Change 1.3 — §1.4 Definitions, Payment Split row

| | |
|---|---|
| **FIND** | `Distribution of order payment among co-seller store members` |
| **REPLACE WITH** | `Distribution of order payment among co-seller store members in proportion to each member's share of product sales in that order, after platform commission deduction` |

---

## Change 1.4 — §1.4 Definitions, Audit Log row

| | |
|---|---|
| **FIND** | `A chronological record of all admin actions, including timestamps and admin ID, stored in Firestore.` |
| **REPLACE WITH** | `A chronological record of admin actions (where implemented), including timestamps and admin ID, stored in Firestore collections such as admin_activities and admin_audit_logs; refund workflows also maintain per-refund audit_trail.` |

---

## Change 1.5 — §1.7 Constraints, item 2

| | |
|---|---|
| **FIND** | `Firebase, ML Kit, Cloudinary, Gmail SMTP, and EmailJS services` |
| **REPLACE WITH** | `Firebase, ML Kit, Cloudinary, and EmailJS services` |

---

## Change 1.6 — §1.6 Overview, bullet for Section 9

**No change required** — optional add after Section 8 bullet:

**INSERT bullet:**

> Section 1.8 – Known Limitations: Documents prototype constraints for academic evaluation.

*(Only if you inserted §1.8 Known Limitations.)*

---

# SECTION 2 — OVERALL DESCRIPTION

## Change 2.1 — §2.1 Product Perspective, paragraph 2 (Admin Dashboard)

| | |
|---|---|
| **FIND** | `Developed using React.js, it communicates with Firebase through the Firebase SDK, enabling real-time data synchronization via Firestore listeners and Cloud Functions. The dashboard allows administrators to verify sellers, manage users, approve product listings, and maintain platform integrity.` |
| **REPLACE WITH** | `Developed using React.js with Material UI, it communicates with Firebase Authentication and Cloud Firestore through the Firebase Web SDK over HTTPS (TLS), enabling real-time data synchronization via Firestore listeners. Cloud Functions support push notifications and transactional email (EmailJS). The dashboard allows administrators to verify sellers, manage users, approve product listings, process refunds, and maintain platform integrity. Authorization is enforced client-side through role-based access control (RBAC) and complemented by Firestore Security Rules.` |

---

## Change 2.2 — §2.1 System Architecture, bullet Admin Dashboard

| | |
|---|---|
| **FIND** | `The Admin Dashboard oversees system operations through secure backend APIs.` |
| **REPLACE WITH** | `The Admin Dashboard oversees system operations through the Firebase Web SDK (Firestore and Authentication).` |

---

## Change 2.3 — §2.1 Backend (c.) Cloud Storage line

| | |
|---|---|
| **FIND** | `Cloud Storage – Cloudinary manages all media assets (product images, seller verification photos)` |
| **REPLACE WITH** | `Media storage – Cloudinary CDN manages all media assets (product images, seller verification photos); Firebase Cloud Storage is not used for these assets` |

---

## Change 2.4 — §2.1 Backend (d.) email sentence

| | |
|---|---|
| **FIND** | `Cloud Functions – for automation (e.g., push notifications via FCM, order status updates). Order confirmation and seller approval emails are sent directly from Android EmailService via Gmail SMTP; password reset OTPs via EmailJS.` |
| **REPLACE WITH** | `Cloud Functions – for automation (e.g., push notifications via FCM, order status updates, transactional emails via EmailJS on order creation and seller approval/rejection from the admin dashboard). Password reset OTP emails are sent from the Android client via EmailJS; after OTP verification, Firebase Authentication sends the password reset link.` |

---

## Change 2.5 — §2.2 Product Functions, Payment Split row

| | |
|---|---|
| **FIND** | `Payment Split System` / `Distribute co-seller order payments among store members.` |
| **REPLACE WITH (Description cell)** | `Distribute co-seller order payments among store members in proportion to each member's contributed product sales, after platform commission deduction.` |

---

## Change 2.6 — §2.2 Activity Logs row

| | |
|---|---|
| **FIND** | `Audit trail of all admin actions with timestamps and admin user ID for compliance and troubleshooting.` |
| **REPLACE WITH** | `Audit trail of implemented admin actions (approvals, settings, reports, email events) with timestamps and admin user ID; dedicated audit viewer UI out of prototype scope.` |

---

## Change 2.7 — §2.4 Operating environment, Email Services line

| | |
|---|---|
| **FIND** | `Email Services: Gmail SMTP via JavaMail (order confirmations, seller approvals); EmailJS HTTP API (password reset OTP)` |
| **REPLACE WITH** | `Email Services: EmailJS HTTP API (order confirmations via Cloud Functions on order create and optional Android checkout trigger; seller approval/rejection via Cloud Functions from admin dashboard; password reset OTP via Android client)` |

---

## Change 2.8 — §2.4 Web Admin — INSERT after browsers line

**INSERT after** `Browsers: Chrome, Firefox, Edge, Safari`:

```
Build tool: Vite
UI framework: React 18.x, Material UI (MUI) 5.x
Charts: Recharts 2.x
Routing: React Router 6.x
Notifications: react-hot-toast
```

---

## Change 2.9 — §2.6 Assumptions, item 7

| | |
|---|---|
| **FIND** | `Gmail SMTP and EmailJS services remain operational for transactional email notifications.` |
| **REPLACE WITH** | `EmailJS services remain operational for transactional email notifications.` |

---

# SECTION 3 — EXTERNAL INTERFACE REQUIREMENTS

## Change 3.1 — §3.1 Web Admin, Design Tools bullet (charts)

| | |
|---|---|
| **FIND** | `Data visualization using Chart.js or Recharts.` |
| **REPLACE WITH** | `Data visualization using Recharts.` |

---

## Change 3.2 — §3.1 Web Admin, DELETE Light/Dark mode bullet

| | |
|---|---|
| **DELETE entire bullet** | `Includes Light/Dark Mode options for accessibility.` |

**INSERT in Section 9 Future Work table:**

| Enhancement | Description |
|-------------|-------------|
| Admin dark mode | Theme toggle for accessibility on the web admin dashboard |

---

## Change 3.3 — §3.1 Web Admin, Dashboard Overview bullet

| | |
|---|---|
| **FIND** | `Dashboard Overview: KPI cards (active sellers, pending approvals, total revenue, orders today), sales trend charts, quick-action buttons for pending tasks.` |
| **REPLACE WITH** | `Dashboard Overview: KPI cards (Total Users, Active Sellers, Approved Products, Total Orders), sales trend charts (Recharts), recent operational activity feed, and quick navigation to modules with pending work.` |

---

## Change 3.4 — §3.1 Web Admin, Activity Audit Log bullet

| | |
|---|---|
| **FIND** | `Activity Audit Log: Chronological admin action history with timestamps, admin user details, search and filter options.` |
| **REPLACE WITH** | `Admin activity logging: Admin actions recorded in Firestore (admin_activities, admin_audit_logs) where implemented; dashboard displays a recent operational activity feed (registrations, orders, products). A dedicated searchable audit-log screen is planned for future work.` |

---

## Change 3.5 — §3.1 Web Admin, Content Moderation bullet

| | |
|---|---|
| **FIND** | `Content Moderation: Tools for managing listings, complaints, and inappropriate content.` |
| **REPLACE WITH** | `Product flagging and Reports & Complaints: Product Management supports flag/unflag; Reports & Complaints module handles user-submitted reports.` |

---

## Change 3.6 — §3.1 INSERT — Implemented admin routes (after Main Modules list)

**INSERT heading:** `Implemented Web Admin Routes (Prototype)`

**INSERT table:**

| Module | Route |
|--------|-------|
| Login | /login |
| Dashboard | /dashboard |
| Seller Verification | /sellers (Applications + Identity Verifications tabs) |
| Product Management | /products |
| User Management | /users |
| Order Oversight | /orders (Orders + Refunds tabs) |
| Co-Seller Stores | /co-seller-stores |
| Learning Resources | /learning-resources |
| Reports & Complaints | /reports |
| Commissions | /commissions |
| Notifications | /notifications |
| Settings | /settings |

---

## Change 3.7 — §3.3 Software Interfaces, Cloud Functions row

| | |
|---|---|
| **FIND** | `Automates push notifications (order, payment, chat events) via FCM. Transactional emails are NOT handled by Cloud Functions. Negotiation logic is handled client-side.` |
| **REPLACE WITH** | `Automates push notifications (order, payment, chat events) via FCM. Sends transactional emails via EmailJS (order confirmation on order document creation; seller approval/rejection when invoked from admin dashboard). Negotiation logic is handled client-side on the Android app.` |

---

## Change 3.8 — §3.3 DELETE Gmail SMTP table row

**DELETE entire row:**

| Gmail SMTP (via JavaMail) | Android email client library | Sends order confirmation and seller approval emails directly from Android EmailService on Dispatchers.IO |

---

## Change 3.9 — §3.3 ADD EmailJS table row

**INSERT row** (after Firebase Cloud Messaging or Cloud Functions):

| EmailJS HTTP API | Email delivery service | Order confirmations (Cloud Function on order create; optional Android checkout trigger); seller approval/rejection (Cloud Functions from admin dashboard); password reset OTP (Android client) |

---

## Change 3.10 — §3.4 DELETE Gmail SMTP communication row

**DELETE:**

| Gmail SMTP ↔ Android EmailService | HTTPS / REST API | Sends order confirmation and seller approval emails via JavaMail on Dispatchers.IO |

---

## Change 3.11 — §3.4 EmailJS communication row

| | |
|---|---|
| **FIND** | `EmailJS ↔ Android EmailService` + `Sends password reset OTP emails via EmailJS HTTP API` *(full row as in your table)* |
| **REPLACE WITH** | `EmailJS ↔ Android EmailService / Firebase Cloud Functions` \| `HTTPS REST API` \| `Order confirmations (Cloud Function on order create; optional Android checkout trigger); seller approval/rejection (Cloud Functions); password reset OTP (Android)` |

---

## Change 3.12 — §3.4 Web Dashboard communication row (if present)

| | |
|---|---|
| **FIND** | `Web Dashboard ↔ Firebase` |
| **REPLACE WITH (Purpose column if needed)** | `Admin operations and live dashboard updates via Firebase Web SDK and Firestore real-time listeners` |

---

# SECTION 4 — SPECIFIC REQUIREMENTS

## Change 4.1 — FR-09 Description

| | |
|---|---|
| **FIND** | `Buyers can place orders, and sellers can update order statuses PENDING → (seller accepts) → PROCESSING → SHIPPED → DELIVERED → COMPLETED.` |
| **REPLACE WITH** | `Buyers can place orders with initial status pending. Sellers can update order statuses through pending → processing (upon seller acceptance) → shipped → delivered → completed. Cancelled orders follow a separate terminal path. The status confirmed may appear in filters for legacy data but is not written by the primary seller-accept flow.` |

---

## Change 4.2 — FR-13 Description

| | |
|---|---|
| **FIND** | `Buyer-initiated refunds go through seller/admin review. Refunds rejected once can be resubmitted. After 2 rejections, no further requests are allowed. Auto-approval occurs after 24 hours of no seller/admin response only to buyer-initiated refunds. Seller-initiated refunds require explicit admin approval and are not subject to auto-approval.` |
| **REPLACE WITH** | `Buyer-initiated refunds go through seller/admin review. Refunds rejected once can be resubmitted; after two rejections, no further requests are allowed (can_resubmit false). Buyer-initiated refunds may be auto-approved after twenty-four (24) hours without seller or admin response when the auto-approval background process is active. Seller-initiated refunds require explicit admin approval and are not subject to auto-approval. All refunds include audit trail entries; failed refund processing may be retried up to three (3) attempts.` |

---

## Change 4.3 — FR-16 Description

| | |
|---|---|
| **FIND** | `For co-seller store orders, payments are automatically distributed equally among all store members after platform commission deduction.` |
| **REPLACE WITH** | `For co-seller store orders, the system shall deduct the configured platform commission from the applicable subtotal, then distribute the remaining amount among store members in proportion to each member's share of product sales in that order. Split details shall be stored in the paymentSplits array of the payment document.` |

---

## Change 4.4 — FR-21 — full replacement

| Field | REPLACE WITH |
|-------|----------------|
| **Description** | Transactional emails are delivered via EmailJS templates and HTTP APIs as follows: (1) Order confirmation — a Firestore-triggered Cloud Function (sendOrderEmail) sends an EmailJS email when an order document is created; the Android checkout flow may additionally dispatch a confirmation email at order success without blocking order completion. (2) Seller approval and rejection — Firebase Cloud Functions invoked from the admin dashboard (sendSellerApprovalEmail, sendSellerApplicationApprovalEmail), using EmailJS. (3) Password reset — OTP sent via EmailJS HTTP API from the Android app (EmailService.sendPasswordResetOtp); after OTP verification, Firebase Authentication sends the password reset link. |
| **Dependencies** | EmailJS account and templates active. Order confirmation: Cloud Function sendOrderEmail (functions/index.js, functions/emailService.js); optional Android trigger at checkout. Seller approval/rejection: Cloud Functions from admin dashboard. Password reset OTP: Android EmailService.sendPasswordResetOtp(). |
| **Rationale** | *(keep existing)* |

---

## Change 4.5 — FR-24 Description

| | |
|---|---|
| **FIND** | `All admin actions (approvals, rejections, deletions, edits) shall be logged with timestamp, admin ID, and action details in Firestore.` |
| **REPLACE WITH** | `Auditable admin actions (including seller and product approval/rejection, learning resource changes, commission settings updates, report resolution, and transactional email events where implemented) shall be logged with timestamp, admin ID, and action details in Firestore collections (e.g., admin_activities, admin_audit_logs). User suspend/activate and some product field edits may not be written to the global admin activity collection. Refund documents shall maintain a per-refund audit_trail. A dedicated searchable audit-log user interface is out of prototype scope.` |

---

## Change 4.6 — FR-25 Description

| | |
|---|---|
| **FIND** | `Data is visualized using Chart.js or Recharts.` |
| **REPLACE WITH** | `Data is visualized using Recharts on the admin dashboard.` |

---

## Change 4.7 — FR-26 Description

| | |
|---|---|
| **FIND** | `Dashboard sidebar shall display badge counters for pending sellers, products, refunds, and orders requiring immediate action.` |
| **REPLACE WITH** | `Dashboard sidebar shall display badge counters where implemented: pending seller applications (Seller Verification); pending product approvals with per-admin viewed tracking (Product Management); new users within twenty-four hours (User Management); pending orders and recent refund requests (Order Oversight); new reports with status New (Reports & Complaints); and new co-seller stores within seven days (Co-Seller Stores).` |

---

## Change 4.8 — FR-27 Description (append sentence)

| | |
|---|---|
| **FIND** | `Different admin roles (Super Admin, Admin, Moderator) have different permission levels for dashboard features.` |
| **REPLACE WITH** | `Different admin roles (Super Admin, Admin, Moderator) have different permission levels for dashboard features. Super Admin has full access including system settings, admin user management, and learning resource create/edit/delete. Admin can manage sellers, products, orders, refunds, and commissions but cannot manage admin users or edit system configuration. Moderator has limited access: can flag products and resolve reports but cannot verify sellers, delete users, access commission settings, or create or delete learning resources.` |

---

## Change 4.9 — FR-29 Description

| | |
|---|---|
| **FIND** | `The system shall monitor internet connectivity in real time and classify it as GOOD, SLOW, or OFFLINE. It shall display appropriate user-friendly messages, preserve unsaved data (e.g., cart, listings, checkout), and automatically re-synchronize with Firebase upon reconnection. The FirebaseConnectionManager utility class implements connectivity classification. Integration with all UI screens is ongoing; checkout form data is preserved via CheckoutViewModel static cache.` |
| **REPLACE WITH** | `The system shall monitor internet connectivity and classify it as GOOD, SLOW, or OFFLINE where the connectivity utility is integrated. It shall display user-friendly messages on affected screens, preserve checkout form data via CheckoutViewModel cache, and rely on Firestore listener reconnection for data synchronization when connectivity returns. Full integration of connectivity handling across all Android UI screens is not complete in the prototype.` |
| **Priority** | Change from `High` to `Medium` *(optional but recommended)* |

---

## Change 4.10 — FR-30 Dependencies

| | |
|---|---|
| **FIND** | `Dependencies` row containing `FR-19 (Real-Time Chat)` |
| **REPLACE WITH** | `FR-06 (Co-Seller Store must exist); Firebase Firestore users collection; navigation system` |

*(Remove spurious dependency on chat.)*

---

## Change 4.11 — INSERT: RBAC matrix (after FR-30 or before §4.2)

**INSERT heading:** `Table 4-A — Admin dashboard role permissions (summary)`

| Capability | Super Admin | Admin | Moderator |
|------------|:-----------:|:-----:|:---------:|
| Approve/reject seller applications | Yes | Yes | No |
| Approve/reject products | Yes | Yes | No |
| Flag products | Yes | Yes | Yes |
| Suspend/delete users | Yes | Yes | No |
| Message users | Yes | Yes | Yes |
| Update order status | Yes | Yes | No |
| Process refunds (approve/reject) | Yes | Yes | Limited view |
| Resolve reports | Yes | Yes | No |
| Commissions page | Yes | Yes | No (route blocked) |
| Edit commission settings | Yes | Yes | View only |
| Learning resources CRUD | Yes | No | No |
| System settings / maintenance | Yes | No | View only |
| Manage admin users | Yes | No | No |

---

## Change 4.12 — INSERT: New functional requirements (optional, after FR-30)

| ID | Description |
|----|-------------|
| **FR-31** | The admin dashboard shall provide global search across users, products, and orders from the header search control. |
| **FR-32** | The admin dashboard shall allow oversight of co-seller stores including activate, deactivate, flag, and delete, with sales aggregated from orders. |
| **FR-33** | Super Admin shall configure platform settings including maintenance mode, minimum product price, and maximum negotiation discount percentage. |
| **FR-34** | Notification badge counts shall respect per-admin viewed tracking (viewed_by_admins) for pending products and similar entities. |

*Add corresponding test cases TC-68–TC-70 if you include FR-31–34.*

---

## Change 4.13 — NFR-03, verification photos bullet

| | |
|---|---|
| **FIND** | `Seller verification photos shall be stored securely and deleted from Cloudinary immediately after admin approval or rejection decision; deleted photos cannot be recovered.` |
| **REPLACE WITH** | `Seller verification photos shall be stored securely on Cloudinary. Upon admin approval or rejection, verification metadata shall be updated in Firestore; physical deletion of Cloudinary assets may rely on upload lifecycle policy or server-side cleanup (prototype).` |

---

## Change 4.14 — NFR-07, pagination bullet

| | |
|---|---|
| **FIND** | `The admin dashboard shall implement pagination for tables exceeding 100 records.` |
| **REPLACE WITH** | `The admin dashboard shall support client-side filtering on loaded datasets for prototype scale; server-side pagination for tables exceeding 100 records is planned for future releases (see Section 9).` |

---

# SECTION 5 — DIAGRAMS

## Change 5.1 — Order fulfillment / purchase sequence diagram caption

**ADD note below diagram:**

> Status transitions: pending → processing → shipped → delivered → completed. Seller acceptance writes processing, not confirmed.

---

## Change 5.2 — Payment split diagram

**ADD note:**

> Commission deducted first; remaining amount split by product-sales proportion among co-seller members.

---

## Change 5.3 — Admin component / DFD diagram

| | |
|---|---|
| **FIND** (labels) | `API Server`, `Backend API`, `REST API` *(admin paths)* |
| **REPLACE WITH** | `Firebase Web SDK (Auth + Firestore)` |

---

# SECTION 6 — MOCKUPS

## Change 6.1 — Figure captions (batch)

| Figure | ADD or REPLACE caption note |
|--------|----------------------------|
| 6.32 Dashboard | Shows four KPI cards: Total Users, Active Sellers, Approved Products, Total Orders; Recharts sales and category charts; recent activity feed. |
| 6.33 Seller Verification | Two tabs: Seller Applications and Identity Verifications (ML Kit confidence). |
| 6.36 Order Oversight | Two tabs: Orders and Refunds. |
| 6.41 Settings | Super Admin: commission rate (≤5%), settlement days, admin management; Moderator view-only. |

**Action:** Re-capture screenshots from live admin if UI differs from figures.

**DELETE** any mockup annotation claiming dark mode.

---

# SECTION 7 — TEST CASES

## Change 7.1 — RTM: ADD column

**INSERT column header:** `Implementation evidence (Y/N/Partial)`

Fill from review: most Android FR = Y; FR-23/25/audit UI = Partial if admin incomplete.

---

## Change 7.2 — TC-15 Expected Result

| | |
|---|---|
| **FIND** | `email confirmation sent asynchronously via Gmail SMTP using JavaMail; cart cleared after navigation` |
| **REPLACE WITH** | `email confirmation sent asynchronously via EmailJS (Cloud Function sendOrderEmail on order create and/or Android checkout trigger); cart cleared after navigation; order success not blocked by email failure` |

---

## Change 7.3 — TC-21 Test Data and Expected Result

| | |
|---|---|
| **FIND** | `Pending → Confirmed → Processing → Shipped → Delivered` |
| **REPLACE WITH** | `Pending → Processing → Shipped → Delivered → Completed` |

| | |
|---|---|
| **FIND** | `Each status transition (Processing, Shipped, Delivered, Completed)` |
| **REPLACE WITH** | `Each status transition (processing, shipped, delivered, completed)` |

---

## Change 7.4 — TC-23 Expected Result (full replace — removes contradiction)

| | |
|---|---|
| **REPLACE entire Expected Result cell with:** |

> A single payment document is created per order in the payments collection with paymentSplits array entries per co-seller member. Platform commission (e.g., 5%) is deducted first from the order subtotal. The remaining amount is split among co-seller members in proportion to each member's product sales in that order (not necessarily equal percentages). Each member's splitAmount and splitPercentage are stored in paymentSplits. Admin commission is recorded in admin_commissions. Each co-seller views their split on the Co-Seller Order Detail screen. Order document includes involved_seller_ids for access control.

| | |
|---|---|
| **REPLACE Test Data example with:** |

> Order Total: PKR 4000; Commission 5% = PKR 200; Remaining PKR 3800 split by sales ratio (e.g., Member A 60% = PKR 2280, Member B 40% = PKR 1520 if sales ratio is 60:40).

---

## Change 7.5 — TC-25 Title and Expected Result

| | |
|---|---|
| **FIND** | `Buyer refund request auto-approved within 24 hours` |
| **REPLACE WITH** | `Buyer-initiated refund auto-approved after 24 hours without seller/admin response` |

| | |
|---|---|
| **FIND** | `refunds submitted within 24 hours of delivery are automatically approved` |
| **REPLACE WITH** | `buyer-initiated refund in requested status for 24 hours without seller/admin response is auto-approved when background auto-approval is active` |

| **Status** | If auto-approval job not running in demo: change to `Conditional Pass` and note in Actual Result |

---

## Change 7.6 — TC-36 (all email fields)

Use **FYP_FIX_10_EMAIL_PATHS.md** §7 — TC-15, TC-36 Objective, Preconditions, Steps, Expected Results Part A & B.

---

## Change 7.7 — TC-58 Preconditions

| | |
|---|---|
| **FIND** | `Admin logged in; Learning Resources screen accessible` |
| **REPLACE WITH** | `Super Admin logged in; Learning Resources screen accessible` |

---

## Change 7.8 — TC-59 Preconditions

| | |
|---|---|
| **FIND** | `admin logged in with EDIT_LEARNING_RESOURCES and DELETE_LEARNING_RESOURCES permissions` |
| **REPLACE WITH** | `Super Admin logged in with create, edit, and delete learning resource permissions` |

---

## Change 7.9 — TC-66 Expected Result (Moderator products)

| | |
|---|---|
| **FIND** | `Moderator cannot approve or reject sellers, edit or delete products, or take action on reports` |
| **REPLACE WITH** | `Moderator cannot approve or reject sellers or take action on reports; cannot access commissions route or edit commission/system settings; cannot delete users; product approve/reject buttons hidden per RBAC; learning resources view-only` |

---

## Change 7.10 — INSERT: TC-68, TC-69, TC-70 (optional)

**TC-68:** Moderator opens Learning Resources — cannot see Add Category / Delete (view only).

**TC-69:** Super Admin enables maintenance mode in Settings — value persisted in Firestore settings.

**TC-70:** Admin flags co-seller store — store hidden or flagged per implementation; audit entry if logged.

---

# SECTION 8 — GANTT CHART

**No text replacement required** unless email/admin tasks are labeled "Gmail" — rename to EmailJS integration.

---

# SECTION 9 — FUTURE WORK

## Change 9.1 — INSERT rows (move from "implemented" claims)

| Enhancement | Description |
|-------------|-------------|
| Dedicated audit log viewer | Searchable/filterable UI for admin_activities and admin_audit_logs |
| Admin dark mode | Theme toggle on web dashboard |
| Table pagination | Server-side pagination for large admin tables |
| Android order email consolidation | Single EmailJS path from Cloud Function only (remove duplicate client send) |
| Full offline UX | Integrate FirebaseConnectionManager across all Android screens |
| Cloudinary verification purge | Server-side delete of verification images after admin decision |
| Live KPI delta calculations | Replace placeholder period-over-period percentages |

---

# ORDER OF APPLICATION (print this page)

| Step | Action | § |
|------|--------|---|
| 1 | Backup .docx | — |
| 2 | Version 1.1 + Revision history | §0 |
| 3 | Global G1–G8 | All |
| 4 | Known limitations insert | §1.8 |
| 5 | Section 1 changes 1.1–1.5 | §1 |
| 6 | Section 2 changes 2.1–2.9 | §2 |
| 7 | Section 3 changes 3.1–3.12 | §3 |
| 8 | Section 4 FR/NFR 4.1–4.14 | §4 |
| 9 | Section 5 diagram notes | §5 |
| 10 | Section 6 mockup captions | §6 |
| 11 | Section 7 test cases 7.1–7.10 | §7 |
| 12 | Section 9 future work | §9 |
| 13 | Ctrl+F: Gmail, JavaMail, Chart.js, equally among, secure backend APIs | All |
| 14 | Read FR-21 + §2.4 + TC-36 together | §4, §7 |

---

# FINAL SIGN-OFF CHECKLIST

- [ ] Document Version 1.1
- [ ] Revision history table present
- [ ] Zero occurrences: Gmail, JavaMail, Chart.js
- [ ] FR-21 matches §2.4 and §3.3/3.4
- [ ] FR-16 proportional split; TC-23 consistent
- [ ] FR-09 / TC-21 no "Confirmed" in primary path
- [ ] Admin: Firebase SDK not "backend APIs"
- [ ] No dark mode in scope
- [ ] FR-24 / TC-63 aligned (partial logging)
- [ ] TC-58/59 Super Admin
- [ ] TC-66 Moderator wording fixed
- [ ] Mockups re-captured or captioned as prototype
- [ ] PDF export matches .docx after edits

---

*Generated for Craftoria FYP — Government College University, Faisalabad. Session 2022–2026.*
