# SRS Update Guide - Adding Diagram Specifications

## Overview
This guide explains how to integrate the diagram specifications from `CRAFTORIA_DIAGRAMS_CONCISE.md` into the main SRS document `CRAFTORIA_SRS_UPDATED.md`.

## Where to Add Diagrams in SRS

The diagram specifications should be added as a new section in the SRS document. Based on IEEE Std 830-1998 standards, diagrams typically appear in:

**Option 1: As a new section after "System Architecture & Data Models" (Section 5)**
- Add as Section 6: "System Diagrams and Models"
- Move current Section 6 (Appendix) to Section 7

**Option 2: Within Section 5 "System Architecture & Data Models"**
- Add as subsection 5.4: "System Diagrams"

## Recommended Approach

Add diagrams as **Section 6** to maintain clear separation and professional structure.

## Updated Table of Contents

```markdown
## Table of Contents

1. Introduction
2. Overall Description
3. External Interface Requirements
4. Specific Requirements
5. System Architecture & Data Models
6. System Diagrams and Models  ← NEW SECTION
7. Appendix
```

## Section 6 Content

Insert the following section after Section 5.3 (Security Rules) and before the current Section 6 (Appendix):

---

## 6. System Diagrams and Models

This section provides comprehensive diagram specifications for visualizing the Craftoria system architecture, data flows, and interactions. These diagrams follow UML standards and can be created using tools like Lucidchart, Draw.io, PlantUML, or Visual Paradigm.

### 6.1 Use Case Diagram

**Actors:** Buyer, Seller, Co-Seller, Admin

**Buyer Use Cases:**
- Authentication (Sign Up, Login, Google OAuth)
- Browse Products, Search, View Details
- Cart Management (Add, Update, Remove)
- Checkout & Place Order
- View Orders, Track Status
- Chat with Seller
- Wishlist Management
- View Notifications

**Seller Use Cases:**
- Apply to Become Seller
- Upload Verification Photo
- Manage Products (Add, Edit, Delete)
- View & Process Orders
- Update Order Status, Add Tracking
- View Payments & Payment Splits
- Manage Co-Seller Stores
- Chat with Buyers

**Admin Use Cases:**
- Review Seller Applications
- Verify Seller Photos
- Approve/Reject Products
- Manage Users (Ban, Suspend)
- View System Reports

### 6.2 Data Flow Diagram (DFD)

**Level 0 - Context:**
- External: Buyer, Seller, Admin, Firebase, Cloudinary, EmailJS
- Central Process: Craftoria System
- Flows: User Data, Product Data, Orders, Payments, Notifications

**Level 1 - Major Processes:**
1. Authentication (Login/Register)
2. Product Management (CRUD)
3. Order Processing (Cart → Checkout → Order)
4. Payment Management (Process, Split)
5. Notification System (Push, Email)
6. Chat System (Real-time messaging)

### 6.3 Class Diagram

**Core Classes:**

```
User
- id, email, name, role, phone, profileImage
- verified, verificationStatus, themePreference
+ signUp(), signIn(), updateProfile()

Product
- id, sellerId, title, price, category, images, stock, status
+ create(), update(), delete()

Order
- id, buyerId, sellerId, items[], totalPrice, status
- deliveryInfo, trackingNumber
+ placeOrder(), updateStatus(), cancel()

Cart
- userId, items[], totalAmount
+ addItem(), removeItem(), calculateTotal()

Payment
- id, orderId, amount, status, splitDetails[]
+ processPayment(), splitPayment()

Notification
- id, userId, title, message, type, isRead
+ send(), markAsRead()
```

**Relationships:**
- User (1) → (N) Product
- User (1) → (N) Order
- Order (1) → (N) OrderItem
- Order (1) → (1) Payment
- Payment (1) → (N) PaymentSplit

### 6.4 Sequence Diagrams

**6.4.1 User Login:**
User → System → Firebase Auth → Firestore → Return User Data

**6.4.2 Product Purchase:**
Buyer → Add to Cart → Checkout → Create Order → Process Payment → Send Notifications (Buyer + Seller)

**6.4.3 Seller Verification:**
Seller → Apply → Admin Reviews → Approve → Upload Photo → Admin Verifies → Approved/Rejected

**6.4.4 Order Fulfillment:**
Seller → View Order → Confirm → Process → Add Tracking → Ship → Deliver → Release Payment

### 6.5 Activity Diagram

**Buyer Purchase Flow:**
Start → Login → Browse → Add to Cart → Checkout → Enter Delivery Info → Place Order → Payment → Confirmation → End

**Seller Order Processing:**
Start → Receive Order → Accept/Reject → Process → Add Tracking → Ship → Mark Delivered → End

### 6.6 Entity Relationship Diagram (ERD)

**Entities:**

**USERS** (PK: user_id)
- email, name, role, phone, profile_image, verified, verification_status

**PRODUCTS** (PK: product_id, FK: seller_id)
- title, description, price, category, images, stock, status

**ORDERS** (PK: order_id, FK: buyer_id, seller_id)
- total_price, status, payment_method, shipping_address, tracking_number

**ORDER_ITEMS** (PK: item_id, FK: order_id, product_id)
- quantity, price, is_negotiated

**PAYMENTS** (PK: payment_id, FK: order_id, seller_id)
- amount, status, payment_method

**PAYMENT_SPLITS** (PK: split_id, FK: payment_id, seller_id)
- amount, percentage

**NOTIFICATIONS** (PK: notification_id, FK: user_id)
- title, message, type, is_read

**CHATS** (PK: chat_id, FK: buyer_id, seller_id)
- last_message, last_message_time

**CO_SELLER_STORES** (PK: store_id, FK: owner_id)
- store_name, description, member_count, rating

**Relationships:**
- Users (1:N) Products, Orders, Notifications
- Orders (1:N) OrderItems
- Orders (1:1) Payments
- Payments (1:N) PaymentSplits
- Users (N:N) Chats
- CoSellerStores (1:N) Products, StoreMembers

### 6.7 State Diagram - Order Status

**States:**
NEW → PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED → COMPLETED

**Alternative:** Any State → CANCELLED

### 6.8 Component Diagram

**Layers:**
1. **Presentation:** Android UI (Jetpack Compose), ViewModels
2. **Business Logic:** Repositories, Use Cases
3. **Data:** Firebase Firestore, Firebase Auth, Firebase Storage
4. **External:** EmailJS, FCM, Cloudinary

### 6.9 Deployment Diagram

**Client:** Android App (Kotlin), Web Dashboard (React)
**Server:** Firebase Cloud Functions (Node.js)
**Database:** Firestore (NoSQL)
**Storage:** Firebase Storage, Cloudinary
**Services:** Firebase Auth, FCM, EmailJS

### 6.10 Architecture Pattern

**MVVM Architecture:**
- **View** (Composables) ↔ **ViewModel** (State Management) ↔ **Repository** (Data Access) ↔ **Data Sources** (Firebase)

---

## Implementation Steps

1. **Open** `CRAFTORIA_SRS_UPDATED.md`
2. **Locate** Section 6 (Appendix) - around line 695
3. **Insert** the new Section 6 content above
4. **Renumber** the old Section 6 to Section 7
5. **Update** Table of Contents with new section numbers
6. **Save** the document

## Files Involved

- `CRAFTORIA_SRS_UPDATED.md` - Main SRS document (to be updated)
- `CRAFTORIA_DIAGRAMS_CONCISE.md` - Source diagram specifications
- `SRS_UPDATE_GUIDE.md` - This guide

## Next Steps

After updating the SRS:
1. Use diagram specifications to create actual visual diagrams
2. Export diagrams as images (PNG/SVG)
3. Optionally embed diagram images in SRS document
4. Review complete SRS for consistency
5. Share with stakeholders for approval

---

**Note:** The diagram specifications are intentionally concise to provide just enough detail for creating professional UML diagrams without overwhelming the SRS document.
