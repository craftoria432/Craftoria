

## 7.3 Misalignments to Verify Against SRS (continued)

| Area | Actual Behavior | Verify in SRS |
|---|---|---|
| Commission deduction | 5% deducted from seller payout automatically; seller_payout = subtotal - commission_amount | SRS may not specify the exact deduction mechanism or default rate |
| Notification categories | 9 categories including REPORT, ADMIN_MESSAGE, PAYMENTS, STORE_RATING — beyond typical order/message | SRS may only list basic categories |
| Chat message types | 5 types: TEXT, IMAGE, PRODUCT, ORDER_UPDATE, NEGOTIATION | SRS may only describe text messaging |
| Report types | 4 types: PRODUCT, SELLER, BUYER, TECHNICAL | SRS may only mention product/seller reports |
| Refund initiation | Both buyer AND seller can initiate a refund | SRS may only describe buyer-initiated refunds |
| Order is_viewed flag | Separate flag tracks whether seller has viewed a new order (drives badge count) | Likely undocumented in SRS |
| Store "NEW" badge | Stores created within last 7 days show a "NEW" badge on home screen | Likely undocumented |
| Seller public profile | Sellers have a public profile screen viewable by other users | May be missing from SRS |
| Invoice generation | InvoiceUtils.kt exists for invoice generation | May be undocumented |
| Payment reconciliation | PaymentReconciliationRepository exists for reconciling payment records | Likely undocumented |
| DashboardDataHelper | Auto-seeds sample data for new sellers with no products/payments | Implementation detail, not a requirement |

## 7.4 Missing Non-Functional Requirements (Likely Absent from SRS)

| NFR | Detail |
|---|---|
| Backward compatibility policy | System explicitly handles legacy data formats (old timestamp types, single-product orders) — this is an architectural decision that should be documented |
| Idempotency requirement | Payment processing must be idempotent to prevent duplicate charges — critical for financial systems |
| Data anonymization on deletion | Account deletion anonymizes data rather than hard-deleting — GDPR/privacy compliance requirement |
| Firestore index management | Composite queries require pre-deployed indexes (firestore.indexes.json) — operational requirement |
| Image optimization | All images served via Cloudinary with width/height parameters — performance requirement |
| FCM token management | FCM tokens stored per user for push delivery — infrastructure requirement |
| Commission settlement period | 7-day configurable settlement window — financial operations requirement |
| Retry limits | Payment retries capped at 3 attempts; refund retries capped at 3 — reliability requirement |
| Audit trail retention | admin_activities collection logs all admin actions — compliance requirement |

## 7.5 Firestore Collections Inventory (Data Architecture)

The following Firestore collections are used by the system. Your SRS data model section should account for all of these:

| Collection | Purpose |
|---|---|
| users | All user accounts (buyers, sellers, co-sellers) |
| products | All product listings |
| orders | All orders placed |
| negotiations | Negotiation offers per product |
| cart_items | Per-user cart items |
| seller_payments | Payment records per seller per order |
| refund_requests | Refund requests |
| admin_commissions | Commission records per order |
| admin_earnings | Aggregated admin earnings document |
| commission_settings | Commission configuration |
| co_seller_stores | Collaborative store records |
| store_members | Members of each co-seller store |
| store_invitations | Pending/accepted/declined invitations |
| store_ratings | Buyer ratings for co-seller stores |
| chats | Chat session documents |
| chats/{id}/messages | Messages within each chat |
| notifications | In-app notifications per user |
| reports | User-submitted reports |
| activities | Seller activity log |
| learning_categories | Tutorial categories |
| bookmarked_tutorials | User tutorial bookmarks |
| admin_activities | Admin action audit log |
| unread_messages | Unread message counts per user |

## 7.6 SRS Validation Checklist

Use this checklist to verify your SRS against the actual system. Each item should be explicitly covered in your SRS:

### Authentication & Users
- [ ] Email/password registration and login documented
- [ ] Google OAuth login documented
- [ ] Three user roles defined: BUYER, SELLER, CO_SELLER
- [ ] Role-based routing on login documented
- [ ] Account ban (permanent) behavior documented
- [ ] Account suspension (time-limited, auto-expiry) behavior documented
- [ ] Account soft-deletion with data anonymization documented
- [ ] Password reset flow documented

### Seller Onboarding
- [ ] Two-stage onboarding clearly separated (application vs. identity verification)
- [ ] ML Kit face detection specified as the verification technology
- [ ] Cloudinary specified as photo storage
- [ ] 24–48 hour review SLA documented
- [ ] Rejection with reason and retry documented
- [ ] Seller application status transitions documented (NONE → PENDING → APPROVED/REJECTED)
- [ ] Verification status transitions documented (NOT_SUBMITTED → PENDING → APPROVED/REJECTED)

### Products
- [ ] Multi-image product listings documented
- [ ] Admin approval workflow documented (pending → approved/rejected)
- [ ] Admin remote removal documented (separate from seller deactivation)
- [ ] Draft mode documented
- [ ] Negotiation fields documented (is_negotiable, minimum_price, auto_accept_price, auto_accept_discount)
- [ ] Product specifications (key-value) documented
- [ ] Product tags documented
- [ ] Product metrics documented (view_count, sold_count, like_count, share_count)
- [ ] Co-seller store association documented

### Negotiation
- [ ] Buyer offer submission documented
- [ ] Auto-accept threshold documented
- [ ] Seller accept/decline workflow documented
- [ ] Cart price update on acceptance documented
- [ ] Negotiation status badges documented

### Cart & Checkout
- [ ] Firebase-backed real-time cart documented
- [ ] Multi-seller cart with per-seller shipping documented
- [ ] Checkout form fields and validation rules documented
- [ ] 4 payment methods documented
- [ ] Terms & Conditions requirement documented
- [ ] Order confirmation email documented

### Orders
- [ ] All 8 order statuses documented with transitions
- [ ] Per-status timestamps documented
- [ ] Order timeline display documented
- [ ] Courier/tracking info documented
- [ ] Buyer cancellation documented
- [ ] Seller rejection with reason documented
- [ ] Reorder functionality documented
- [ ] Notification triggers per status change documented

### Payments
- [ ] Automatic payment record creation on order placement documented
- [ ] Multi-seller payment splitting documented
- [ ] Co-seller percentage-based payment split documented
- [ ] Payment completion on order delivery documented
- [ ] Buyer payment history documented
- [ ] Seller payment history and earnings documented
- [ ] Idempotency key requirement documented
- [ ] Payment access control (seller sees only own payments) documented
- [ ] Payment retry mechanism documented

### Refunds
- [ ] Refund types documented (FULL, PARTIAL, RETURN)
- [ ] All refund statuses documented
- [ ] Buyer and seller initiation documented
- [ ] Auto-approval within 24 hours documented
- [ ] Retry limit (3 attempts) documented
- [ ] Audit trail requirement documented
- [ ] Co-seller refund split documented

### Commission
- [ ] Default 5% commission rate documented
- [ ] Configurable commission rate documented
- [ ] Commission applies to negotiated prices (configurable) documented
- [ ] Settlement period (7 days, configurable) documented
- [ ] Admin earnings aggregation documented

### Co-Seller Stores
- [ ] Store creation flow documented
- [ ] Email-based invitation system documented
- [ ] Payment split configuration documented
- [ ] Store public view documented
- [ ] Store rating system documented
- [ ] Admin store management (flag, deactivate) documented
- [ ] Member count tracking documented

### Chat
- [ ] Real-time messaging documented
- [ ] Product context in chat initiation documented
- [ ] Message types documented (TEXT, IMAGE, PRODUCT, ORDER_UPDATE, NEGOTIATION)
- [ ] Unread count tracking documented
- [ ] Chat blocking documented
- [ ] Message delivery and read receipts documented

### Notifications
- [ ] In-app notification system documented
- [ ] FCM push notifications documented
- [ ] All 9 notification categories documented
- [ ] All 14 action types documented
- [ ] Deep-link navigation from notifications documented
- [ ] Bulk notification management documented
- [ ] All notification trigger events documented (minimum 15 triggers)

### Reporting
- [ ] 4 report types documented
- [ ] Admin moderation workflow documented
- [ ] User suspension (30 days) documented
- [ ] Permanent ban documented
- [ ] Reporter notification on resolution documented
- [ ] Evidence attachment documented

### Learning Resources
- [ ] Categorized tutorial system documented
- [ ] Tutorial search documented
- [ ] Bookmark functionality documented
- [ ] External link confirmation documented

### Theme System
- [ ] 3 themes documented (Rose, Light, Dark)
- [ ] Per-user Firestore persistence documented
- [ ] Animated transitions documented

### Non-Functional
- [ ] Firebase as backend platform documented
- [ ] Cloudinary as image CDN documented
- [ ] EmailJS as email service documented
- [ ] Firestore security rules requirement documented
- [ ] Payment access control requirement documented
- [ ] Backward compatibility for legacy data documented
- [ ] Data anonymization on account deletion documented
- [ ] Audit logging requirement documented
- [ ] Real-time data sync requirement documented
- [ ] Offline/connection state handling documented

---

## 7.7 Critical Items Your SRS Must Not Miss

These are the highest-risk gaps based on the reverse-engineering analysis. If any of these are absent from your SRS, the document is materially incomplete:

1. Two-stage seller onboarding — the application approval and identity verification are distinct processes with separate state machines. Documenting them as one step is incorrect.

2. ML Kit face detection — this is a specific technology requirement, not just "photo upload." The on-device processing, face validation, and result storage to Firestore before submission are all distinct requirements.

3. Auto-accept negotiation — the auto_accept_price threshold creates a fully automated negotiation path that bypasses seller action entirely. This is a core business rule.

4. Co-seller payment split — the percentage-based split configuration, the PaymentSplit records, and the retroactive migration for legacy orders are all distinct requirements that must be specified.

5. Payment idempotency — for a financial system, the requirement that payment processing must be idempotent (no duplicate charges on retry) is a critical non-functional requirement.

6. Account ban vs. suspension distinction — these are two different enforcement mechanisms with different durations, different Firestore fields, and different user-facing messages. They must be specified separately.

7. Product dual-flag removal — is_active (seller-controlled) and is_removed (admin-controlled) are independent flags. A product can be active but removed by admin, or inactive but not removed. This distinction must be documented.

8. Per-seller shipping calculation — shipping cost = flat rate × number of unique sellers in cart. This is a non-obvious business rule that directly affects order totals.

9. Real payment gateway absence — the system currently uses test mode with no real payment gateway SDK. If your SRS specifies Stripe, PayPal, or any gateway integration, this is a gap between SRS and implementation.

10. Discount system removal — a discount system was built and then removed (DISCOUNT_SYSTEM_REMOVED.md). If your SRS still documents a discount/coupon system, this is a direct misalignment.

---

*This benchmark document was generated by reverse-engineering the Craftoria Android application (Kotlin/Compose), web admin dashboard (React), and Firebase Cloud Functions backend. It covers all data models, repositories, screens, navigation routes, and backend triggers as of the analyzed codebase state.*
