# CRAFTORIA E-COMMERCE PLATFORM - UML DIAGRAM SPECIFICATIONS

## 1. USE CASE DIAGRAM

### Actors
- **Buyer** (Customer)
- **Seller** (Vendor)
- **Admin** (Web Dashboard)

### Use Cases by Actor

**Buyer:**
- Register/Login
- Browse Products
- Search Products
- Add to Cart
- Place Order
- Track Order
- Chat with Seller
- Rate Store
- Manage Wishlist

**Seller:**
- Apply for Seller Account
- Face Verification (ML Kit)
- Add/Edit Products
- Manage Orders
- Update Order Status
- View Payments
- Manage Co-Seller Store
- Chat with Buyers

**Admin (Web Dashboard):**
- Approve Seller Applications
- Approve/Reject Products
- View All Orders
- Manage Co-Seller Stores
- View Reports
- Send Notifications

---

## 2. DATA FLOW DIAGRAM (DFD)

### Level 0 - Context Diagram
```
[Buyer] ──→ Craftoria System ──→ [Firebase]
[Seller] ──→ Craftoria System ──→ [EmailJS]
[Admin] ──→ Craftoria System ──→ [Cloudinary]
```

### Level 1 - Main Processes
1. **User Authentication** → Firebase Auth
2. **Product Management** → Firestore (products collection)
3. **Order Processing** → Firestore (orders collection)
4. **Payment Processing** → Firestore (payments collection)
5. **Notification System** → FCM + EmailJS
6. **Chat System** → Firestore (chats collection)

---

## 3. CLASS DIAGRAM

### Core Entities

**User**
- id: String
- email: String
- name: String
- role: String (buyer/seller/admin)
- profileImage: String
- verified: Boolean

**Product**
- id: String
- sellerId: String
- title: String
- price: Double
- category: String
- images: List<String>
- stock: Int
- status: String (pending/approved/rejected)

**Order**
- id: String
- buyerId: String
- sellerId: String
- items: List<OrderItem>
- totalPrice: Double
- status: String
- shippingAddress: String
- trackingNumber: String

**Payment**
- id: String
- orderId: String
- amount: Double
- status: String
- splits: List<PaymentSplit>

**CoSellerStore**
- id: String
- ownerId: String
- storeName: String
- members: List<String>
- rating: Double

### Relationships
- User (1) ──→ (N) Product
- User (1) ──→ (N) Order
- Order (1) ──→ (1) Payment
- Payment (1) ──→ (N) PaymentSplit
- CoSellerStore (1) ──→ (N) Product

---

## 4. SEQUENCE DIAGRAMS

### 4.1 User Login Flow
```
User → LoginScreen → AuthViewModel → AuthRepository → Firebase Auth → Return User
```

### 4.2 Place Order Flow
```
Buyer → Cart → Checkout → OrderRepository → Firestore
→ PaymentRepository → Create Payment
→ NotificationRepository → Send Email (EmailJS)
→ Send FCM to Seller
```

### 4.3 Seller Verification Flow
```
Seller → Apply → Admin Reviews (Web) → Approve
→ Seller Uploads Photo → ML Kit Face Detection
→ Admin Verifies → Account Activated
```

### 4.4 Product Approval Flow
```
Seller → Add Product → Firestore (status: pending)
→ Admin Reviews (Web) → Approve/Reject
→ Update Status → Notify Seller
```

---

## 5. ACTIVITY DIAGRAM

### Order Purchase Flow
```
START
→ Login
→ Browse Products
→ Add to Cart
→ Checkout
→ Enter Shipping Details
→ Place Order
→ Payment Processed
→ Email Sent
→ END
```

### Order Fulfillment Flow
```
START
→ Seller Receives Order
→ Confirm Order
→ Process Order
→ Add Tracking Number
→ Mark as Shipped
→ Mark as Delivered
→ Payment Released
→ END
```

---

## 6. ENTITY RELATIONSHIP DIAGRAM (ERD)

### Entities & Attributes

**users**
- user_id (PK)
- email
- name
- role
- profile_image
- verified

**products**
- product_id (PK)
- seller_id (FK → users)
- title
- price
- category
- stock
- status

**orders**
- order_id (PK)
- buyer_id (FK → users)
- seller_id (FK → users)
- total_price
- status
- shipping_address
- tracking_number

**payments**
- payment_id (PK)
- order_id (FK → orders)
- amount
- status

**payment_splits**
- split_id (PK)
- payment_id (FK → payments)
- seller_id (FK → users)
- amount
- percentage

**co_seller_stores**
- store_id (PK)
- owner_id (FK → users)
- store_name
- member_count
- rating

**notifications**
- notification_id (PK)
- user_id (FK → users)
- title
- message
- type
- is_read

**chats**
- chat_id (PK)
- buyer_id (FK → users)
- seller_id (FK → users)
- last_message
- timestamp

### Relationships
- users (1:N) products
- users (1:N) orders (as buyer)
- users (1:N) orders (as seller)
- orders (1:1) payments
- payments (1:N) payment_splits
- users (N:N) chats

---

## 7. STATE DIAGRAM

### Order Status States
```
NEW → PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED → COMPLETED

Any State → CANCELLED (if cancelled)
```

### Product Status States
```
DRAFT → PENDING → APPROVED → ACTIVE

PENDING → REJECTED (if admin rejects)
```

---

## 8. COMPONENT DIAGRAM

### Android App Architecture
```
┌─────────────────────────────────┐
│   UI Layer (Jetpack Compose)    │
├─────────────────────────────────┤
│   ViewModel Layer                │
├─────────────────────────────────┤
│   Repository Layer               │
├─────────────────────────────────┤
│   Data Sources (Firebase)        │
└─────────────────────────────────┘
```

### Web Admin Dashboard
```
┌─────────────────────────────────┐
│   React Components               │
├─────────────────────────────────┤
│   Services Layer                 │
├─────────────────────────────────┤
│   Firebase SDK                   │
└─────────────────────────────────┘
```

---

## 9. DEPLOYMENT DIAGRAM

### System Architecture
```
┌──────────────┐         ┌──────────────┐
│ Android App  │────────→│   Firebase   │
│  (Kotlin)    │         │  Firestore   │
└──────────────┘         └──────────────┘
                                ↑
┌──────────────┐                │
│ Web Admin    │────────────────┘
│  (React)     │
└──────────────┘

External Services:
- Firebase Auth
- Firebase Storage
- Cloudinary (Images)
- EmailJS (Emails)
- FCM (Push Notifications)
- ML Kit (Face Detection)
```

---

## 10. PACKAGE DIAGRAM

### Android App Structure
```
com.gcuf.craftoria
├── data
│   ├── model (User, Product, Order, Payment)
│   └── repository (AuthRepository, ProductRepository, OrderRepository)
├── ui
│   ├── screens (buyer, seller, auth)
│   ├── components (ProductCard, TopBar)
│   └── theme (Color, Theme)
├── viewmodel (AuthViewModel, ProductViewModel, OrderViewModel)
├── services (EmailService, FCMService, MLKitService)
└── utils (BadgeManager, PaymentSplitProcessor)
```

---

**END OF SPECIFICATIONS**
