# Cloudinary Integration - SRS Document Updates

## Summary
This document outlines the updates needed in the SRS document (CRAFTORIA_SRS_UPDATED.md) to accurately reflect the Cloudinary integration for seller verification photos and product images.

---

## ✅ Already Correct in SRS

The following sections already correctly mention Cloudinary:

1. **Section 1.4 - Key Definitions** (Line 129)
   - ✅ Cloudinary defined as "Cloud-based image storage and management service"

2. **Section 1.5 - Constraints** (Line 141)
   - ✅ "Image Storage: Cloudinary used for product images (not Firebase Cloud Storage)"

3. **Section 2.1 - Product Perspective** (Lines 168, 174)
   - ✅ "Cloudinary for image storage"
   - ✅ "Cloudinary manages product images"

4. **Section 2.4 - Operating Environment** (Line 229)
   - ✅ "Image Storage: Cloudinary"

5. **Section 3.3 - Software Interfaces** (Line 332)
   - ✅ "Cloudinary API | Cloud image storage | Product image and media hosting"

6. **Section 5.1 - Product Model** (Line 576)
   - ✅ "imageUrls (stored in Cloudinary)"

---

## 🔧 Updates Needed

### 1. Section 1.5 - Constraints (Line 141)

**Current:**
```markdown
8. **Image Storage:** Cloudinary used for product images (not Firebase Cloud Storage)
```

**Update to:**
```markdown
8. **Image Storage:** Cloudinary used for all images including product images and seller verification photos (not Firebase Cloud Storage)
```

**Rationale:** Clarify that verification photos are also stored in Cloudinary.

---

### 2. Section 2.1 - Product Perspective (Line 174)

**Current:**
```markdown
- Cloudinary manages product images
```

**Update to:**
```markdown
- Cloudinary manages all media assets (product images, seller verification photos)
```

**Rationale:** Expand scope to include verification photos.

---

### 3. Section 3.3 - Software Interfaces (Line 332)

**Current:**
```markdown
| Cloudinary API | Cloud image storage | Product image and media hosting |
```

**Update to:**
```markdown
| Cloudinary API | Cloud image storage and CDN | Product images, seller verification photos, and media hosting with automatic optimization and transformation |
```

**Rationale:** More detailed description of Cloudinary's role and capabilities.

---

### 4. Section 4.1 - FR-02: Seller Verification

**Current (around line 380):**
```markdown
#### FR-02: Seller Verification
- **Description:** New sellers submit photo for verification; admin reviews and approves/rejects
- **Rationale:** Ensure verified women artisans; prevent fraud
- **Priority:** High
- **Implementation:** SellerVerificationScreen, AuthRepository verification workflow
```

**Update to:**
```markdown
#### FR-02: Seller Verification
- **Description:** New sellers submit photo for verification via Cloudinary upload; admin reviews verification photo on web dashboard and approves/rejects; verification photos stored temporarily in Cloudinary and deleted after admin review
- **Rationale:** Ensure verified women artisans; prevent fraud; maintain privacy by removing verification photos after review
- **Priority:** High
- **Implementation:** 
  - Mobile: SellerVerificationScreen with Cloudinary upload
  - Web: SellerVerification.jsx with image display from Cloudinary URLs
  - Backend: seller_verifications collection stores Cloudinary URLs and ML Kit analysis
  - Cleanup: verificationPhotoService.js handles photo deletion after approval/rejection
```

**Rationale:** Provide complete implementation details including photo lifecycle.

---

### 5. Section 5.1 - User Model (around line 565)

**Current:**
```markdown
**User Model**
- id, email, name, role (BUYER/SELLER/CO_SELLER/ADMIN)
- phone, address, profileImage
- storeName, storeDescription
- verified, verificationStatus, verificationPhotoUrl
- sellerApplicationStatus
- isBanned, banReason, isSuspended, suspensionUntil
- createdAt, updatedAt
```

**Update to:**
```markdown
**User Model**
- id, email, name, role (BUYER/SELLER/CO_SELLER/ADMIN)
- phone, address, profileImage
- storeName, storeDescription
- verified, verificationStatus
- verification_photo_url (Cloudinary URL - temporary, deleted after review)
- sellerApplicationStatus
- isBanned, banReason, isSuspended, suspensionUntil
- createdAt, updatedAt

**Note:** verification_photo_url is populated from seller_verifications collection during admin review and is deleted after approval/rejection for privacy.
```

**Rationale:** Clarify the temporary nature of verification photos.

---

### 6. NEW Section 5.2 - Add Seller Verification Data Model

**Add after Section 5.1 (around line 620):**

```markdown
**SellerVerification Model** (seller_verifications collection)
- id, userId, userName, userEmail
- imageUrl (Cloudinary URL for verification selfie)
- mlKitResult:
  - confidence (0-100%)
  - faceCount (number of faces detected)
  - isValid (boolean)
  - message (validation message)
- status (pending/approved/rejected)
- submittedAt, reviewedAt, reviewedBy
- createdAt, updatedAt

**Lifecycle:**
1. Seller uploads photo → stored in Cloudinary
2. ML Kit analyzes face detection → results stored in mlKitResult
3. Admin reviews on web dashboard → sees Cloudinary URL
4. Admin approves/rejects → photo deleted from Cloudinary and Firestore
5. User verification status updated in users collection

**Privacy:** Verification photos are automatically deleted after admin review to protect user privacy.
```

**Rationale:** Document the complete verification data model and lifecycle.

---

### 7. Section 5.2 - Database Collections (around line 630)

**Current:**
```markdown
### 5.2 Database Collections (Firestore)

- users
- products
- orders
- seller_payments
- notifications
- co_seller_stores
- refunds
- admin_commissions
- chats
- messages
- store_ratings
- learning_resources
- cart_items
- wishlist_items
```

**Update to:**
```markdown
### 5.3 Database Collections (Firestore)

- users
- products
- seller_verifications (temporary - deleted after review)
- orders
- seller_payments
- notifications
- co_seller_stores
- refunds
- admin_commissions
- admin_audit_logs (tracks admin actions on verifications)
- chats
- messages
- store_ratings
- learning_resources
- cart_items
- wishlist_items
```

**Rationale:** Add new collections for verification workflow.

---

### 8. Section 6.2 - Data Flow Diagram

**Add to DFD description (around line 695):**

```markdown
**Seller Verification Flow:**
1. Seller → Upload Photo → Cloudinary
2. Cloudinary → Return URL → seller_verifications collection
3. ML Kit → Analyze Photo → Store results in seller_verifications
4. Admin Dashboard → Fetch verification data → Display Cloudinary image
5. Admin → Approve/Reject → Update users collection
6. System → Delete photo from Cloudinary → Delete seller_verifications document
7. System → Log action → admin_audit_logs collection
```

**Rationale:** Document the complete verification data flow.

---

### 9. Section 7.1 - Technology Stack (around line 846)

**Current:**
```markdown
**Backend (Firebase + Cloud Functions)**
- Database: Firebase Firestore
- Authentication: Firebase Auth
- Messaging: Firebase Cloud Messaging
- Image Storage: Cloudinary
- Email: EmailJS/SendGrid
```

**Update to:**
```markdown
**Backend (Firebase + Cloud Functions)**
- Database: Firebase Firestore
- Authentication: Firebase Auth
- Messaging: Firebase Cloud Messaging
- Image Storage & CDN: Cloudinary
  - Product images (permanent)
  - Seller verification photos (temporary - auto-deleted after review)
  - Automatic image optimization and transformation
  - Secure upload with signed URLs
- Email: EmailJS/SendGrid
```

**Rationale:** Provide more detail on Cloudinary usage patterns.

---

### 10. Section 7.2 - Deployment Checklist (around line 872)

**Current:**
```markdown
- [ ] Firebase project configured with Firestore, Auth, Cloud Functions
- [ ] Cloudinary account set up for image storage
- [ ] EmailJS/SendGrid configured for email notifications
- [ ] Firestore security rules deployed
```

**Update to:**
```markdown
- [ ] Firebase project configured with Firestore, Auth, Cloud Functions
- [ ] Cloudinary account set up with:
  - [ ] Upload presets configured for products and verifications
  - [ ] Folder structure: products/, seller_verifications/
  - [ ] Auto-deletion policy for verification photos (optional)
  - [ ] API keys secured in environment variables
- [ ] EmailJS/SendGrid configured for email notifications
- [ ] Firestore security rules deployed including:
  - [ ] seller_verifications collection (admin read-only)
  - [ ] admin_audit_logs collection (admin read/write only)
```

**Rationale:** Provide detailed Cloudinary setup checklist.

---

### 11. NEW Section - Add Cloudinary Configuration Details

**Add new section after 7.2:**

```markdown
### 7.3 Cloudinary Configuration

**Account Setup:**
- Cloud Name: [Your Cloudinary cloud name]
- API Key: Stored in environment variables
- API Secret: Stored securely (never in client code)

**Upload Presets:**
1. **products_preset**
   - Folder: products/
   - Transformations: Auto-optimize, format auto
   - Max file size: 10MB
   - Allowed formats: jpg, png, webp

2. **seller_verifications_preset**
   - Folder: seller_verifications/
   - Transformations: Face detection, auto-optimize
   - Max file size: 5MB
   - Allowed formats: jpg, png
   - Auto-expiry: 30 days (optional)

**Security:**
- Signed uploads for verification photos
- Public read access for product images
- Admin-only access for verification photos via Firestore rules
- Automatic deletion after admin review

**Integration Points:**
- Android: Direct upload from SellerVerificationScreen
- Web Dashboard: Display images via Cloudinary URLs
- Backend: verificationPhotoService.js handles cleanup
- Cloud Functions: Optional - deleteVerificationPhoto function for secure deletion

**Performance:**
- CDN delivery for fast image loading
- Automatic format conversion (WebP for modern browsers)
- Responsive image transformations
- Lazy loading support
```

**Rationale:** Comprehensive Cloudinary configuration documentation.

---

## 📋 Implementation Files Reference

**Web Dashboard:**
- `src/pages/SellerVerification.jsx` - Admin verification interface
- `src/components/seller/UserCard.jsx` - Displays verification photos
- `src/components/seller/SellerModals.jsx` - Approval/rejection modals
- `src/services/verificationPhotoService.js` - Photo deletion service
- `src/services/adminAuditService.js` - Audit logging

**Mobile App:**
- `app/src/main/java/com/gcuf/craftoria/ui/screens/auth/SellerVerificationScreen.kt` - Photo upload
- `app/src/main/java/com/gcuf/craftoria/viewmodel/SellerVerificationViewModel.kt` - Upload logic
- `app/src/main/java/com/gcuf/craftoria/services/MLKitFaceDetectionService.kt` - Face detection

**Backend:**
- `functions/index.js` - Cloud Functions (optional deletion function)
- `firestore.rules` - Security rules for seller_verifications collection

---

## 🎯 Summary of Changes

1. ✅ Clarified Cloudinary stores both product images AND verification photos
2. ✅ Added SellerVerification data model with ML Kit integration
3. ✅ Documented verification photo lifecycle (upload → review → delete)
4. ✅ Added seller_verifications and admin_audit_logs collections
5. ✅ Expanded FR-02 with complete implementation details
6. ✅ Added Cloudinary configuration section with presets and security
7. ✅ Updated deployment checklist with Cloudinary setup steps
8. ✅ Added data flow diagram for verification process
9. ✅ Documented privacy measures (auto-deletion after review)
10. ✅ Referenced all implementation files

---

## 🔒 Privacy & Security Notes

**Privacy Measures:**
- Verification photos deleted immediately after admin review
- Only admins can access verification photos via Firestore rules
- Cloudinary URLs are temporary and not stored permanently
- Admin actions logged in admin_audit_logs for accountability

**Security Measures:**
- Signed uploads prevent unauthorized photo submissions
- Firestore rules restrict access to seller_verifications collection
- API secrets stored in environment variables
- HTTPS encryption for all image transfers

---

## ✅ Verification Checklist

Before marking SRS as complete, verify:
- [ ] All Cloudinary references updated in SRS
- [ ] SellerVerification model documented
- [ ] Verification photo lifecycle explained
- [ ] Privacy measures documented
- [ ] Security rules specified
- [ ] Deployment checklist includes Cloudinary setup
- [ ] Implementation files referenced
- [ ] Data flow diagrams updated
- [ ] Technology stack section expanded
- [ ] Configuration details provided

---

**Document Version:** 1.0  
**Last Updated:** March 2026  
**Status:** Ready for SRS Integration
