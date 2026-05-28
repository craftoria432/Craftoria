# CRAFTORIA E-COMMERCE PLATFORM - COMPLETE DIAGRAM SPECIFICATIONS

## 📋 TABLE OF CONTENTS
1. [Use Case Diagrams](#1-use-case-diagrams)
2. [Data Flow Diagrams (DFD)](#2-data-flow-diagrams)
3. [Class Diagrams](#3-class-diagrams)
4. [Sequence Diagrams](#4-sequence-diagrams)
5. [Activity Diagrams](#5-activity-diagrams)
6. [Entity Relationship Diagrams (ERD)](#6-entity-relationship-diagrams)
7. [Additional Professional Diagrams](#7-additional-professional-diagrams)

---

## 1. USE CASE DIAGRAMS

### 1.1 ACTORS
- **Buyer** (Primary User)
- **Seller** (Verified Merchant)
- **Co-Seller** (Store Member)
- **Admin** (System Administrator)
- **System** (Automated Processes)

### 1.2 BUYER USE CASES

#### Authentication & Profile Management
- UC-B01: Sign Up / Sign In (Email/Password, Google OAuth)
- UC-B02: View Profile
- UC-B03: Edit Profile (Name, Phone, Address, Profile Picture)
- UC-B04: Change Password
- UC-B05: Delete Account
- UC-B06: Apply to Become Seller
- UC-B07: Manage Theme Preferences (Rose, Blue, Green, Purple, Orange)

#### Product Discovery & Shopping
- UC-B08: Browse Home Screen (Featured Products, Stores, Banners)
- UC-B09: Search Products (By Name, Category, Price Range)
- UC-B10: View All Stores
- UC-B11: View Product Details
- UC-B12: Add Product to Wishlist
- UC-B13: Remove from Wishlist
- UC-B14: View Wishlist

#### Cart & Checkout
- UC-B15: Add Product to Cart
- UC-B16: Update Cart Quantity
- UC-B17: Remove from Cart
- UC-B18: View Cart
- UC-B19: Proceed to Checkout
- UC-B20: Enter Delivery Information
- UC-B21: Select Payment Method (Cash on Delivery)
- UC-B22: Place Order
- UC-B23: View Order Success Screen

#### Order Management
- UC-B24: View My Orders
- UC-B25: Track Order Status (New, Processing, Shipped, Delivered, Cancelled)
- UC-B26: Reorder Previous Order
- UC-B27: View Payment History
- UC-B28: View Order Timeline

#### Communication
- UC-B29: View My Chats
- UC-B30: Chat with Seller
- UC-B31: View Seller Profile
- UC-B32: View Store Public Page

#### Notifications
- UC-B33: View Notifications
- UC-B34: Receive Order Status Updates
- UC-B35: Navigate from Notification to Order/Product

### 1.3 SELLER USE CASES

#### Seller Onboarding
- UC-S01: Submit Seller Application
- UC-S02: Upload Verification Photo (ML Kit Face Detection)
- UC-S03: Wait for Admin Approval
- UC-S04: Receive Verification Status Notification

#### Product Management
- UC-S05: View Seller Dashboard (Stats, Recent Orders, Activities)
- UC-S06: Add New Product (Title, Description, Price, Images, Category, Stock)
- UC-S07: Manage Products (View, Edit, Delete)
- UC-S08: Preview Product as Buyer
- UC-S09: View Product Approval Status
- UC-S10: Receive Product Approval/Rejection Notification

#### Order Management
- UC-S11: View Seller Orders
- UC-S12: Update Order Status (Confirm, Process, Ship, Deliver)
- UC-S13: Add Tracking Information
- UC-S14: Cancel Order with Reason
- UC-S15: View Order Details

#### Payment & Financial
- UC-S16: View Seller Payments Screen
- UC-S17: View Payment Details
- UC-S18: View Payment Split Details (Co-Seller Stores)
- UC-S19: View Total Sales Statistics
- UC-S20: View Payment History

#### Communication
- UC-S21: View Seller Messages
- UC-S22: Chat with Buyers
- UC-S23: View Buyer Profile

#### Co-Seller Store Management
- UC-S24: Create Co-Seller Store
- UC-S25: Manage Co-Seller Store
- UC-S26: Add Store Members
- UC-S27: Remove Store Members
- UC-S28: View Store Payment Distribution
- UC-S29: View Store Public Page

#### Learning & Resources
- UC-S30: View Learning Resources
- UC-S31: Access Educational Content

### 1.4 CO-SELLER USE CASES

#### Store Management
- UC-CS01: Join Co-Seller Store (Invitation)
- UC-CS02: View My Co-Seller Stores
- UC-CS03: View Store Dashboard
- UC-CS04: Add Products to Store
- UC-CS05: View Store Payment Distribution
- UC-CS06: Leave Store

### 1.5 ADMIN USE CASES

#### User Management
- UC-A01: View All Users
- UC-A02: Ban/Suspend Users
- UC-A03: Review Seller Applications
- UC-A04: Approve/Reject Seller Applications
- UC-A05: Review Seller Verification Photos (ML Kit)
- UC-A06: Approve/Reject Seller Verification

#### Product Management
- UC-A07: View All Products
- UC-A08: Approve/Reject Products
- UC-A09: Remove Products

#### System Management
- UC-A10: View Reports
- UC-A11: Monitor System Activity
- UC-A12: Manage Notifications

---

## 2. DATA FLOW DIAGRAMS (DFD)

### 2.1 LEVEL 0 - CONTEXT DIAGRAM

**External Entities:**
- Buyer
- Seller
- Co-Seller
- Admin
- Firebase (Database)
- Cloudinary (Image Storage)
- EmailJS (Email Service)
- ML Kit (Face Detection)

**Central Process:**
- Craftoria E-Commerce System

**Data Flows:**
- User Credentials → System
- Product Data → System
- Order Information → System
- Payment Details → System
- Notifications ← System
- Email Confirmations ← System


### 2.2 LEVEL 1 - MAJOR PROCESSES

**Process 1.0: Authentication Management**
- Input: Email, Password, Google OAuth Token
- Output: User Session, JWT Token
- Data Store: Users Collection

**Process 2.0: Product Management**
- Input: Product Details, Images, Category
- Output: Product Listing, Approval Status
- Data Store: Products Collection

**Process 3.0: Order Processing**
- Input: Cart Items, Delivery Info, Payment Method
- Output: Order Confirmation, Order ID
- Data Store: Orders Collection

**Process 4.0: Payment Management**
- Input: Order Total, Payment Method
- Output: Payment Record, Split Distribution
- Data Store: Payments Collection

**Process 5.0: Notification System**
- Input: Event Triggers (Order Status, Approval)
- Output: Push Notifications, Email Alerts
- Data Store: Notifications Collection

**Process 6.0: Chat System**
- Input: Messages, User IDs
- Output: Real-time Messages
- Data Store: Chats Collection

### 2.3 LEVEL 2 - DETAILED PROCESSES

**Process 3.1: Cart Management**
- Add to Cart
- Update Quantity
- Remove Item
- Calculate Total

**Process 3.2: Checkout Processing**
- Validate Cart
- Collect Delivery Info
- Create Order(s)
- Clear Cart

**Process 3.3: Order Fulfillment**
- Update Order Status
- Add Tracking Info
- Send Status Notifications
- Complete Delivery

---

## 3. CLASS DIAGRAMS

### 3.1 CORE ENTITIES

**User Class**
```
User
- id: String
- email: String
- name: String
- role: UserRole (BUYER, SELLER, CO_SELLER)
- phone: String
- address: String
- profileImage: String
- createdAt: Long
- verified: Boolean
- verificationStatus: VerificationStatus
- themePreference: String
+ signUp()
+ signIn()
+ updateProfile()
+ changePassword()
+ deleteAccount()
```


**Product Class**
```
Product
- id: String
- sellerId: String
- sellerName: String
- title: String
- description: String
- price: Double
- category: String
- images: List<String>
- stock: Int
- status: String (pending, approved, rejected)
- createdAt: Long
- coSellerStoreId: String
+ create()
+ update()
+ delete()
+ updateStock()
```

**Order Class**
```
Order
- id: String
- buyerId: String
- buyerName: String
- sellerId: String
- items: List<OrderItem>
- totalPrice: Double
- status: OrderStatus
- deliveryInfo: DeliveryInfo
- paymentMethod: String
- createdAt: Long
- trackingNumber: String
+ placeOrder()
+ updateStatus()
+ cancelOrder()
+ addTracking()
```

**Cart Class**
```
Cart
- userId: String
- items: List<CartItem>
- totalAmount: Double
+ addItem()
+ removeItem()
+ updateQuantity()
+ clearCart()
+ calculateTotal()
```

**Payment Class**
```
Payment
- id: String
- orderId: String
- sellerId: String
- amount: Double
- status: String
- splitDetails: List<PaymentSplit>
- createdAt: Long
+ processPayment()
+ splitPayment()
+ refund()
```

**Notification Class**
```
Notification
- id: String
- userId: String
- title: String
- message: String
- type: NotificationType
- actionType: NotificationActionType
- isRead: Boolean
- createdAt: Long
+ send()
+ markAsRead()
+ delete()
```

### 3.2 RELATIONSHIPS

- User (1) ←→ (N) Product
- User (1) ←→ (N) Order
- User (1) ←→ (1) Cart
- Order (1) ←→ (N) OrderItem
- Order (1) ←→ (1) Payment
- User (1) ←→ (N) Notification
- Product (N) ←→ (1) CoSellerStore
- Payment (1) ←→ (N) PaymentSplit

---

## 4. SEQUENCE DIAGRAMS

### 4.1 USER REGISTRATION & LOGIN

**Actors:** Buyer/Seller, System, Firebase Auth, Firestore

**Flow:**
1. User → System: Enter credentials
2. System → Firebase Auth: Authenticate
3. Firebase Auth → System: Return auth token
4. System → Firestore: Fetch user data
5. Firestore → System: Return user profile
6. System → User: Display dashboard


### 4.2 PRODUCT PURCHASE FLOW

**Actors:** Buyer, System, Cart, Order, Payment, Notification

**Flow:**
1. Buyer → System: Browse products
2. Buyer → Cart: Add product
3. Cart → System: Update cart total
4. Buyer → System: Proceed to checkout
5. System → Buyer: Request delivery info
6. Buyer → System: Submit delivery details
7. System → Order: Create order(s)
8. Order → Payment: Process payment
9. Payment → System: Confirm payment
10. System → Notification: Send confirmation
11. Notification → Buyer: Email + Push notification
12. Notification → Seller: New order alert

### 4.3 SELLER VERIFICATION FLOW

**Actors:** Seller, System, ML Kit, Admin, Notification

**Flow:**
1. Seller → System: Apply to become seller
2. System → Notification: Notify admin
3. Admin → System: Review application
4. Admin → System: Approve application
5. System → Seller: Update role to SELLER
6. Seller → System: Upload verification photo
7. System → ML Kit: Detect face
8. ML Kit → System: Return face detection result
9. System → Admin: Send for manual review
10. Admin → System: Approve/Reject verification
11. System → Notification: Send status to seller

### 4.4 ORDER FULFILLMENT FLOW

**Actors:** Seller, System, Order, Notification, Buyer

**Flow:**
1. Seller → System: View new orders
2. Seller → Order: Update status to "Processing"
3. Order → Notification: Notify buyer
4. Seller → Order: Add tracking info
5. Order → Notification: Send tracking update
6. Seller → Order: Mark as "Shipped"
7. Order → Notification: Notify buyer
8. Seller → Order: Mark as "Delivered"
9. Order → Payment: Release payment
10. Payment → Notification: Confirm payment to seller

---

## 5. ACTIVITY DIAGRAMS

### 5.1 BUYER PURCHASE ACTIVITY

**Start** → Login → Browse Products → Add to Cart → View Cart → 
**Decision: Continue Shopping?**
- Yes → Browse Products
- No → Proceed to Checkout

Checkout → Enter Delivery Info → Review Order → Place Order → 
Payment Processing → Order Confirmation → **End**

### 5.2 SELLER PRODUCT MANAGEMENT ACTIVITY

**Start** → Login → Seller Dashboard → Add Product → 
Enter Product Details → Upload Images → Submit for Approval →
**Decision: Admin Approved?**
- Yes → Product Listed → **End**
- No → Receive Rejection Reason → Edit Product → Resubmit → **End**

### 5.3 ORDER PROCESSING ACTIVITY

**Start** → Receive Order → View Order Details →
**Decision: Accept Order?**
- No → Cancel with Reason → Notify Buyer → **End**
- Yes → Confirm Order → Process Order → Add Tracking → 
Mark as Shipped → **Decision: Delivered?**
  - No → Wait for Delivery
  - Yes → Mark as Delivered → Payment Released → **End**


---

## 6. ENTITY RELATIONSHIP DIAGRAMS (ERD)

### 6.1 ENTITIES AND ATTRIBUTES

**USERS**
- PK: user_id
- email (unique)
- name
- role (buyer/seller/co_seller)
- phone
- address
- profile_image
- created_at
- verified
- verification_status
- theme_preference

**PRODUCTS**
- PK: product_id
- FK: seller_id → USERS(user_id)
- FK: co_seller_store_id → CO_SELLER_STORES(store_id)
- title
- description
- price
- category
- images (array)
- stock
- status (pending/approved/rejected)
- created_at

**ORDERS**
- PK: order_id
- FK: buyer_id → USERS(user_id)
- FK: seller_id → USERS(user_id)
- total_price
- status (new/processing/shipped/delivered/cancelled)
- payment_method
- shipping_address
- tracking_number
- created_at
- delivered_at

**ORDER_ITEMS**
- PK: item_id
- FK: order_id → ORDERS(order_id)
- FK: product_id → PRODUCTS(product_id)
- quantity
- price
- is_negotiated

**CART**
- PK: cart_id
- FK: user_id → USERS(user_id)
- created_at
- updated_at

**CART_ITEMS**
- PK: cart_item_id
- FK: cart_id → CART(cart_id)
- FK: product_id → PRODUCTS(product_id)
- quantity
- price

**PAYMENTS**
- PK: payment_id
- FK: order_id → ORDERS(order_id)
- FK: seller_id → USERS(user_id)
- amount
- status (pending/completed/refunded)
- payment_method
- created_at

**PAYMENT_SPLITS**
- PK: split_id
- FK: payment_id → PAYMENTS(payment_id)
- FK: seller_id → USERS(user_id)
- amount
- percentage
- status

**NOTIFICATIONS**
- PK: notification_id
- FK: user_id → USERS(user_id)
- title
- message
- type
- action_type
- is_read
- created_at

**CHATS**
- PK: chat_id
- FK: buyer_id → USERS(user_id)
- FK: seller_id → USERS(user_id)
- last_message
- last_message_time
- created_at

**MESSAGES**
- PK: message_id
- FK: chat_id → CHATS(chat_id)
- FK: sender_id → USERS(user_id)
- content
- timestamp
- is_read

**CO_SELLER_STORES**
- PK: store_id
- FK: owner_id → USERS(user_id)
- store_name
- description
- member_count
- rating
- created_at

**STORE_MEMBERS**
- PK: membership_id
- FK: store_id → CO_SELLER_STORES(store_id)
- FK: user_id → USERS(user_id)
- role (owner/member)
- joined_at

**WISHLIST**
- PK: wishlist_id
- FK: user_id → USERS(user_id)
- FK: product_id → PRODUCTS(product_id)
- added_at

**STORE_RATINGS**
- PK: rating_id
- FK: store_id → CO_SELLER_STORES(store_id)
- FK: user_id → USERS(user_id)
- rating (1-5)
- review
- created_at

### 6.2 RELATIONSHIPS

- USERS (1) ←→ (N) PRODUCTS (One seller has many products)
- USERS (1) ←→ (N) ORDERS (One buyer places many orders)
- ORDERS (1) ←→ (N) ORDER_ITEMS (One order contains many items)
- PRODUCTS (N) ←→ (N) ORDER_ITEMS (Many-to-many via ORDER_ITEMS)
- USERS (1) ←→ (1) CART (One user has one cart)
- CART (1) ←→ (N) CART_ITEMS (One cart has many items)
- ORDERS (1) ←→ (1) PAYMENTS (One order has one payment)
- PAYMENTS (1) ←→ (N) PAYMENT_SPLITS (One payment splits to many sellers)
- USERS (1) ←→ (N) NOTIFICATIONS (One user receives many notifications)
- USERS (N) ←→ (N) CHATS (Many-to-many: buyers and sellers)
- CHATS (1) ←→ (N) MESSAGES (One chat has many messages)
- CO_SELLER_STORES (1) ←→ (N) PRODUCTS (One store has many products)
- CO_SELLER_STORES (1) ←→ (N) STORE_MEMBERS (One store has many members)
- USERS (N) ←→ (N) WISHLIST (Many-to-many via WISHLIST)
- CO_SELLER_STORES (1) ←→ (N) STORE_RATINGS (One store has many ratings)

---

## 7. ADDITIONAL PROFESSIONAL DIAGRAMS

### 7.1 COMPONENT DIAGRAM

**Presentation Layer (Android - Jetpack Compose)**
- UI Screens (Buyer, Seller, Co-Seller, Admin)
- ViewModels (State Management)
- Navigation Component

**Business Logic Layer**
- Repositories (Data Access)
- Use Cases / Interactors
- Domain Models

**Data Layer**
- Firebase Firestore (Database)
- Firebase Auth (Authentication)
- Firebase Storage (Images)
- Cloudinary (Image CDN)

**External Services**
- EmailJS (Email Notifications)
- ML Kit (Face Detection)
- FCM (Push Notifications)

### 7.2 DEPLOYMENT DIAGRAM

**Client Side:**
- Android Mobile App (Kotlin + Jetpack Compose)
- Web Admin Dashboard (React.js)

**Server Side:**
- Firebase Cloud Functions (Node.js)
- Firebase Hosting (Web Dashboard)

**Database:**
- Firebase Firestore (NoSQL)
- Firebase Realtime Database (Chat)

**Storage:**
- Firebase Storage (User uploads)
- Cloudinary (Product images)

**Services:**
- Firebase Authentication
- Firebase Cloud Messaging
- EmailJS API
- ML Kit API


### 7.3 STATE DIAGRAM - ORDER STATUS

**States:**
1. NEW (Initial state after order placement)
2. PENDING (Awaiting seller confirmation)
3. CONFIRMED (Seller accepted order)
4. PROCESSING (Seller preparing order)
5. SHIPPED (Order dispatched with tracking)
6. DELIVERED (Order received by buyer)
7. COMPLETED (Transaction finalized)
8. CANCELLED (Order cancelled by buyer/seller)

**Transitions:**
- NEW → PENDING (Automatic)
- PENDING → CONFIRMED (Seller action)
- PENDING → CANCELLED (Seller rejection)
- CONFIRMED → PROCESSING (Seller action)
- PROCESSING → SHIPPED (Seller adds tracking)
- SHIPPED → DELIVERED (Seller confirms delivery)
- DELIVERED → COMPLETED (Automatic after 7 days)
- Any state → CANCELLED (Buyer/Seller action with reason)

### 7.4 COLLABORATION DIAGRAM - CHECKOUT PROCESS

**Objects:**
- Buyer
- CheckoutScreen
- CartViewModel
- OrderRepository
- PaymentRepository
- NotificationService
- EmailService

**Interactions:**
1. Buyer → CheckoutScreen: Click "Place Order"
2. CheckoutScreen → CartViewModel: getCartItems()
3. CartViewModel → CheckoutScreen: Return cart items
4. CheckoutScreen → OrderRepository: createOrder(items, deliveryInfo)
5. OrderRepository → PaymentRepository: processPayment(orderId, amount)
6. PaymentRepository → OrderRepository: Payment confirmed
7. OrderRepository → NotificationService: sendOrderConfirmation(buyerId, orderId)
8. NotificationService → EmailService: sendEmail(buyerEmail, orderDetails)
9. NotificationService → Seller: Push notification (new order)
10. OrderRepository → CheckoutScreen: Order created successfully
11. CheckoutScreen → Buyer: Show success screen

### 7.5 PACKAGE DIAGRAM

**com.gcuf.craftoria**
├── **data**
│   ├── model (User, Product, Order, Payment, etc.)
│   └── repository (AuthRepository, ProductRepository, etc.)
├── **ui**
│   ├── screens (buyer, seller, coseller, auth, info)
│   ├── components (Reusable UI components)
│   ├── theme (Colors, Typography, Theme)
│   └── navigation (NavGraph, Routes)
├── **viewmodel** (AuthViewModel, ProductViewModel, etc.)
├── **services** (EmailService, FCMService, MLKitService)
├── **utils** (Helpers, Managers, Validators)
└── **payment** (PaymentSystem, PaymentSystemManager)

### 7.6 TIMING DIAGRAM - REAL-TIME NOTIFICATION FLOW

**Timeline (0-5 seconds):**

**T=0s:** Seller updates order status to "Shipped"
**T=0.1s:** OrderRepository writes to Firestore
**T=0.2s:** Firestore triggers Cloud Function
**T=0.5s:** Cloud Function creates notification document
**T=0.6s:** FCM sends push notification to buyer's device
**T=1.0s:** Buyer receives push notification
**T=1.2s:** NotificationViewModel detects new notification (real-time listener)
**T=1.3s:** UI updates notification badge count
**T=2.0s:** EmailService sends order update email
**T=3.0s:** Buyer receives email notification

### 7.7 ARCHITECTURE DIAGRAM

**MVVM Architecture Pattern**

**View Layer (Composables)**
- Observes ViewModel state
- Displays UI
- Handles user interactions

↓↑ (State Flow / Events)

**ViewModel Layer**
- Manages UI state
- Handles business logic
- Exposes StateFlow/LiveData

↓↑ (Data requests / Results)

**Repository Layer**
- Single source of truth
- Coordinates data sources
- Caching strategy

↓↑ (API calls / Database queries)

**Data Sources**
- Firebase Firestore (Remote)
- Firebase Auth (Remote)
- Local Cache (if needed)

---

## 8. DIAGRAM CREATION GUIDELINES

### 8.1 TOOLS RECOMMENDED

**For Use Case Diagrams:**
- Lucidchart
- Draw.io (diagrams.net)
- Visual Paradigm
- PlantUML

**For Class Diagrams:**
- StarUML
- Visual Paradigm
- Enterprise Architect
- PlantUML

**For Sequence Diagrams:**
- SequenceDiagram.org
- PlantUML
- Lucidchart
- WebSequenceDiagrams

**For ERD:**
- dbdiagram.io
- MySQL Workbench
- Lucidchart
- ERDPlus

**For Activity/State Diagrams:**
- Draw.io
- Lucidchart
- Visual Paradigm

### 8.2 NOTATION STANDARDS

**Use Case Diagrams:**
- Actors: Stick figures
- Use Cases: Ovals
- System Boundary: Rectangle
- Relationships: Lines (include, extend, generalization)

**Class Diagrams:**
- Classes: Rectangles (3 sections: name, attributes, methods)
- Relationships: Lines with arrows
- Multiplicity: Numbers at relationship ends
- Visibility: + (public), - (private), # (protected)

**Sequence Diagrams:**
- Actors/Objects: Boxes at top
- Lifelines: Dashed vertical lines
- Messages: Horizontal arrows
- Activation: Thin rectangles on lifelines

**ERD:**
- Entities: Rectangles
- Attributes: Ovals
- Relationships: Diamonds
- Cardinality: 1, N, M notations

### 8.3 COLOR CODING SUGGESTIONS

- **Buyer-related elements:** Blue (#2196F3)
- **Seller-related elements:** Green (#4CAF50)
- **Co-Seller-related elements:** Orange (#FF9800)
- **Admin-related elements:** Red (#F44336)
- **System processes:** Gray (#9E9E9E)
- **External services:** Purple (#9C27B0)

---

## 9. IMPLEMENTATION NOTES

### 9.1 KEY FEATURES TO HIGHLIGHT

1. **Multi-Role System:** Buyer, Seller, Co-Seller, Admin
2. **Real-Time Updates:** Orders, Notifications, Chat
3. **ML Kit Integration:** Face detection for seller verification
4. **Payment Split System:** Automatic distribution for co-seller stores
5. **Theme Customization:** 5 color themes (Rose, Blue, Green, Purple, Orange)
6. **Email Notifications:** Order confirmations, status updates
7. **Product Approval Workflow:** Admin review before listing
8. **Seller Verification:** Two-step process (application + photo verification)

### 9.2 TECHNICAL STACK

**Mobile App:**
- Kotlin
- Jetpack Compose
- MVVM Architecture
- Firebase SDK
- Cloudinary SDK

**Web Dashboard:**
- React.js
- Firebase SDK
- Material-UI

**Backend:**
- Firebase Cloud Functions
- Node.js
- EmailJS API

**Database:**
- Firebase Firestore (NoSQL)

---

## 10. DIAGRAM CHECKLIST

Before finalizing diagrams, ensure:

✓ All actors/entities are clearly labeled
✓ Relationships show correct cardinality
✓ Data flows are unidirectional and clear
✓ Use case includes all major features
✓ Sequence diagrams show complete interactions
✓ Class diagrams include key attributes and methods
✓ ERD shows all entities and relationships
✓ Activity diagrams have clear start/end points
✓ State diagrams cover all possible states
✓ Diagrams are consistent with actual implementation
✓ Legend/key is provided where needed
✓ Diagrams are readable and not cluttered

---

**END OF DOCUMENT**

This specification provides complete textual descriptions 