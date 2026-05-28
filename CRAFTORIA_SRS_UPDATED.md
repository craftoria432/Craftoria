# Craftoria – A Digital Marketplace for Women Handicraft Entrepreneurs
## Software Requirements Specification (SRS) - UPDATED VERSION 2.1

**Document Version:** 2.1 (Updated with System Diagrams)  
**IEEE Standard:** IEEE Std 830-1998 — Software Requirements Specification  
**Last Updated:** March 2026

---

## Table of Contents

1. Introduction
2. Overall Description
3. External Interface Requirements
4. Specific Requirements
5. System Architecture & Data Models
6. System Diagrams and Models
7. Appendix

---

## 1. Introduction

### 1.1 Purpose

This Software Requirements Specification (SRS) document describes the complete functional and non-functional requirements of "Craftoria – A Digital Marketplace for Women Handicraft Entrepreneurs." This updated version (2.0) reflects the actual implementation and features deployed in the production system.

### 1.2 Product Scope

Craftoria is an Android-based marketplace platform designed to empower women artisans by providing a digital platform to showcase and sell handmade crafts. The platform bridges traditional craft-making with modern e-commerce through secure, accessible, and user-friendly technology.

**Core Components:**
1. Android Mobile Application – for sellers (women artisans) and buyers
2. Web-Based Admin Dashboard – for administrators to manage, verify, and monitor activities
3. Firebase Backend Infrastructure – for data management, authentication, and automation

**In Scope (Implemented Features)**

**Android Mobile Application**
- ✅ User registration and login (email/password, Google OAuth)
- ✅ Seller verification workflow with photo upload
- ✅ Product listing, browsing, search, and filtering
- ✅ Shopping cart and checkout
- ✅ Order placement and tracking with real-time updates
- ✅ Price negotiation system (buyers make offers, sellers auto-accept)
- ✅ Co-seller store collaboration with payment splits
- ✅ Store ratings and reviews
- ✅ Real-time chat between buyers and sellers
- ✅ Refund management system
- ✅ Payment tracking and history
- ✅ Wishlist functionality
- ✅ Learning resources and tutorials
- ✅ Theme preference system (light/dark mode)
- ✅ Seller dashboard with analytics
- ✅ Product approval workflow
- ✅ Account management (ban/suspend/delete)

**Web Admin Dashboard**
- ✅ Seller verification and approval management
- ✅ Product approval workflow
- ✅ User and account management
- ✅ Order oversight and tracking
- ✅ Refund processing and management
- ✅ Commission tracking and earnings
- ✅ Co-seller store management
- ✅ Learning resources management
- ✅ Real-time notifications
- ✅ User reports and moderation

**Backend (Firebase)**
- ✅ Authentication (email/password, Google OAuth, OTP)
- ✅ Real-time Firestore Database
- ✅ Cloud Storage for media (via Cloudinary)
- ✅ Cloud Functions for automation
- ✅ Firebase Cloud Messaging (FCM) for notifications
- ✅ Email notifications (EmailJS/SendGrid)
- ✅ Role-based access control (RBAC)

**Out of Scope**

- ❌ Real payment gateway integration (Cash on Delivery only)
- ❌ ML Kit Face Detection (manual verification only)
- ❌ GPS-based courier tracking
- ❌ iOS version of the mobile app
- ❌ Advanced financial reports or seller balance sheets
- ❌ Video hosting and LMS functionalities
- ❌ Bulk product upload
- ❌ Inventory low-stock alerts

### 1.3 Goals and Objectives

**Goals**
1. Empower women artisans to earn sustainable income through digital entrepreneurship
2. Preserve and promote local handmade crafts and cultural heritage
3. Ensure a safe women-only seller environment with secure verification
4. Provide a simple, intuitive, and accessible user interface
5. Facilitate self-learning through embedded resources and tutorials
6. Enable collaborative selling through co-seller stores
7. Maintain platform integrity through admin oversight and moderation

**Objectives**
1. Implement secure seller verification with photo upload and admin approval
2. Develop a rule-based negotiation system for smart pricing interactions
3. Introduce co-seller stores for shared selling and collaboration with payment splits
4. Incorporate a learning resource center for women entrepreneurs
5. Enable complete product listing, order management, and payment tracking
6. Create an admin dashboard for approvals, moderation, and analytics
7. Implement refund management and commission tracking systems
8. Provide real-time notifications and communication channels
9. Support theme preferences for improved user experience

### 1.4 Key Definitions

| Term | Definition |
|------|-----------|
| SRS | Software Requirements Specification |
| FYP | Final Year Project |
| Firebase | Google's cloud-based Backend-as-a-Service platform |
| Firestore | Cloud-hosted NoSQL database service |
| OTP | One-Time Password |
| UI/UX | User Interface / User Experience |
| Admin | System Administrator |
| Co-Seller Store | Shared store managed by multiple sellers collaboratively |
| Payment Split | Distribution of order payment among co-seller members |
| Seller Verification | Process of confirming seller identity via photo upload |
| Product Approval | Admin review and approval before product listing |
| RBAC | Role-Based Access Control |
| FCM | Firebase Cloud Messaging |
| Cloudinary | Cloud-based image storage and management service |
| EmailJS/SendGrid | Email notification services |

### 1.5 Constraints

1. **Internet Connectivity:** System requires stable internet for data synchronization
2. **Android Platform:** Mobile app limited to Android 5.0+ (API Level 21+)
3. **Third-Party Dependencies:** Firebase, Cloudinary, EmailJS services may change
4. **Payment Mode:** Cash on Delivery only; no real payment gateway
5. **Manual Verification:** Final seller approval requires admin confirmation
6. **Time Constraint:** Development completed within academic semester
7. **Resource Limitation:** Uses free-tier and academic development tools
8. **Image Storage:** Cloudinary used for product images (not Firebase Cloud Storage)

---

## 2. Overall Description

### 2.1 Product Perspective

Craftoria is a two-part integrated platform connecting women artisans with buyers through a secure digital marketplace.

**Components:**

1. **Android Mobile Application**
   - Designed for women sellers and buyers
   - Sellers: register, verify, list products, manage orders, track payments, collaborate via co-seller stores
   - Buyers: browse, search, negotiate, purchase, track orders, rate stores

2. **Web Admin Dashboard (React.js)**
   - Administrators verify sellers, manage users, approve products, process refunds
   - Real-time analytics and order oversight
   - Commission tracking and earnings management

3. **Backend Services (Firebase)**
   - Firestore Database for real-time data storage
   - Firebase Authentication for secure login
   - Cloud Functions for automation (notifications, email, payment processing)
   - Firebase Cloud Messaging for push notifications
   - Cloudinary for image storage

**System Architecture:** Client-server with real-time synchronization
- Android app communicates directly with Firebase
- Admin dashboard oversees operations via Firebase SDKs
- Cloud Functions handle backend automation
- Cloudinary manages product images

### 2.2 Product Functions

| Module | Description |
|--------|-------------|
| User Registration & Login | Email/password, Google OAuth, OTP-based authentication |
| Seller Verification | Photo upload, admin approval workflow |
| Product Listing & Management | Upload, edit, delete products with images, prices, descriptions |
| Product Approval | Admin review before product goes live |
| Product Search & Filtering | Search by keywords, category, price range, seller name |
| Smart Negotiation Bot | Buyers propose prices; sellers set auto-accept rules |
| Co-Seller Stores | Multiple sellers collaborate under shared store identity with payment splits |
| Order Management | Place orders, track status, manage delivery information |
| Payment Processing | Process payments, create seller payment records, handle splits |
| Refund Management | Full/partial/return refunds with approval workflow |
| Store Ratings | Buyers rate sellers; ratings displayed on store profiles |
| Real-Time Chat | Messaging between buyers and sellers with product sharing |
| Notifications | Real-time alerts for orders, messages, approvals, payments |
| Learning Resources | External tutorials and guides for seller development |
| Commission System | Track admin commissions from orders (5% default rate) |
| Theme Preference | Light/dark mode toggle with persistence |
| Account Management | Ban, suspend, or delete user accounts |
| Seller Dashboard | Analytics, sales tracking, order management |
| Wishlist | Save favorite products for later |

### 2.3 User Classes and Characteristics

| User Class | Description | Characteristics |
|-----------|-------------|-----------------|
| **Women Sellers** | Women entrepreneurs creating/selling handmade crafts | Require smartphone + internet; may have limited technical skills; need simple UI; must complete verification; access seller dashboard |
| **Buyers** | Customers exploring and purchasing crafts | Browse, filter, negotiate prices; require secure shopping; communicate with sellers; track orders |
| **Co-Sellers** | Members of co-seller stores | Manage store products; share in payments (split commission); view store analytics |
| **Administrators** | System managers (web dashboard only) | Verify sellers, approve products, manage users, process refunds, track commissions, moderate content |

### 2.4 Operating Environment

**Android Mobile Application**
- OS: Android 5.0 (API 21) or higher
- Language: Kotlin with Jetpack Compose
- IDE: Android Studio
- Libraries: Firebase SDK, Jetpack Components, Coil (image loading), Retrofit

**Web Admin Dashboard**
- OS: Windows / macOS / Linux
- Framework: React.js with Material-UI
- IDE: Visual Studio Code
- Browsers: Chrome, Firefox, Edge (latest versions)
- Resolution: Minimum 1280×720 pixels

**Backend (Firebase Cloud)**
- Database: Cloud Firestore (NoSQL)
- Authentication: Firebase Auth
- Cloud Functions: Node.js 18
- Messaging: Firebase Cloud Messaging (FCM)
- Image Storage: Cloudinary
- Email Service: EmailJS/SendGrid
- Hosting: Firebase Hosting

### 2.5 Design and Implementation Constraints

| Constraint | Explanation |
|-----------|-------------|
| Android Only | Mobile app limited to Android; no iOS version |
| Third-Party Dependencies | Relies on Firebase, Cloudinary, EmailJS; outages may affect service |
| Performance | Real-time sync may slow on low-end devices or poor networks |
| Data Privacy | All user data and images encrypted in transit and at rest |
| Time Limitation | Project completed within 16-week FYP semester |
| Team Resources | Built by student team with limited manpower |
| Budget Constraint | Uses free/academic-tier services only |
| Manual Verification | Seller approval requires admin confirmation |
| Internet Dependency | Firebase and services require continuous internet access |
| Payment Method | Cash on Delivery only; no real payment gateway |

### 2.6 Assumptions and Dependencies

1. Users possess Android smartphones with functional cameras and stable internet
2. Firebase and related services remain available and free for educational use
3. Administrators perform timely verification and moderation
4. University FYP infrastructure remains consistent during development
5. Team members have access to required software tools
6. Cloudinary API remains available for image storage
7. EmailJS/SendGrid services remain operational for email notifications
8. Users understand basic e-commerce workflows

---

## 3. External Interface Requirements

### 3.1 User Interfaces

**Android Mobile Application**

*For Women Sellers:*
- Login/Sign-Up Screen: Firebase Authentication (email/password or Google)
- Seller Verification Screen: Photo upload with admin approval workflow
- Product Listing Screen: Add, edit, delete products with images and pricing
- Seller Dashboard: Sales analytics, order management, payment tracking
- Co-Seller Store Screen: Create/manage stores, invite members, view payment splits
- Negotiation Requests Screen: View and respond to buyer price offers
- Payment History Screen: Track earnings and payment status
- Chat Screen: Real-time messaging with buyers
- Learning Resources Screen: Access tutorials and guides
- Profile & Settings: Manage account, theme preferences, verification status

*For Buyers:*
- Home Screen: Featured stores, product categories, banner carousel
- Product Search Screen: Search, filter by category/price/seller
- Product Details Screen: Images, description, price, seller info, negotiation option
- Shopping Cart Screen: Add/remove items, view total
- Checkout Screen: Enter delivery address, select payment method
- Order Tracking Screen: Real-time order status with timeline
- Payment History Screen: View all purchases and payment status
- Store Ratings Screen: Rate sellers after purchase
- Wishlist Screen: Save favorite products
- Chat Screen: Message sellers about products
- My Orders Screen: Order history and current orders

**Web Admin Dashboard**

- Login Page: Firebase Authentication
- Dashboard Overview: Real-time charts, active users, sales metrics
- Seller Verification Panel: Pending approvals with verification images
- Product Approval Panel: Review and approve/reject products
- User Management: Approve, suspend, ban, or delete accounts
- Order Oversight: Track and update order statuses
- Refund Management: Process refund requests
- Commission Tracking: View admin earnings and commission details
- Co-Seller Store Management: Manage stores and member assignments
- Learning Resources Management: Add/edit tutorials
- Reports & Analytics: User activity, sales trends, platform metrics

**Design Standards:**
- Material Design 3 for Android
- Material-UI for web dashboard
- Responsive layouts for various screen sizes
- Light/dark mode support
- Accessibility-focused (large fonts, high contrast)

### 3.2 Hardware Interfaces

| Device Type | Interface | Purpose |
|------------|-----------|---------|
| Android Smartphone/Tablet | Touchscreen | User input (tapping, swiping, typing) |
| | Camera | Photo capture for seller verification and product images |
| | Microphone | Optional for future voice features |
| Laptop/Desktop (Admin) | Keyboard & Mouse | Dashboard navigation and data management |
| | Display | Minimum 1280×720 resolution |
| Internet Router/Modem | Network Interface | Connectivity to Firebase and cloud services |

### 3.3 Software Interfaces

| Software/API | Description | Purpose |
|-------------|-------------|---------|
| Firebase Authentication | Google identity management | User registration, login, session management |
| Firebase Firestore | NoSQL cloud database | Real-time data storage and synchronization |
| Firebase Cloud Functions | Serverless backend code | Automation (notifications, email, payment processing) |
| Firebase Cloud Messaging (FCM) | Push notification service | Real-time alerts for orders, messages, approvals |
| Cloudinary API | Cloud image storage | Product image and media hosting |
| EmailJS/SendGrid | Email notification service | Transactional emails (order confirmations, refunds) |
| Google OAuth | Third-party authentication | Social login integration |
| Android SDK (Jetpack Compose) | Android development framework | Mobile UI components |
| React.js | JavaScript UI library | Web admin dashboard |
| Node.js | JavaScript runtime | Cloud Functions backend |
| Kotlin Coroutines | Async programming | Mobile app concurrency |
| Retrofit + OkHttp | HTTP client library | API communication |
| Gson | JSON serialization | Data parsing |

### 3.4 Communication Interfaces

| Communication Type | Protocol/Method | Purpose |
|------------------|-----------------|---------|
| Android App ↔ Firebase | HTTPS/REST APIs | Real-time data transfer |
| Web Dashboard ↔ Firebase | HTTPS/Firebase SDK | Admin operations and synchronization |
| Firebase Cloud Messaging | Push Notification Protocol | Real-time user notifications |
| Email Notifications | SMTP (EmailJS/SendGrid) | Transactional emails |
| Team Collaboration | GitHub Repository | Version control and code management |

**Security:** All communication uses SSL/TLS encryption (HTTPS) for data security and privacy.

---

## 4. Specific Requirements

### 4.1 Functional Requirements

#### FR-01: User Registration and Login
- **Description:** System allows users (sellers and buyers) to register and log in using Firebase Authentication with email/password, Google OAuth, or OTP
- **Rationale:** Secure user identification and authentication
- **Priority:** High
- **Implementation:** AuthRepository with email/password and Google OAuth support

#### FR-02: Seller Verification
- **Description:** New sellers submit photo for verification; admin reviews and approves/rejects
- **Rationale:** Ensure verified women artisans; prevent fraud
- **Priority:** High
- **Implementation:** SellerVerificationScreen, AuthRepository verification workflow

#### FR-03: Product Listing and Management
- **Description:** Sellers upload products with images, names, prices, descriptions, categories; can edit/delete
- **Rationale:** Allow sellers to showcase handmade items
- **Priority:** High
- **Implementation:** ProductRepository, AddProductScreen, ManageProductsScreen

#### FR-04: Product Approval Workflow
- **Description:** Admin must approve products before they appear in buyer search/browse
- **Rationale:** Maintain product quality and platform standards
- **Priority:** High
- **Implementation:** Web dashboard product approval panel, ProductRepository approval logic

#### FR-05: Product Search and Filtering
- **Description:** Buyers search by keywords, category, price range, seller name
- **Rationale:** Enable efficient product discovery
- **Priority:** High
- **Implementation:** SearchScreen with Firestore queries

#### FR-06: Shopping Cart
- **Description:** Buyers add/remove items, update quantities, view total
- **Rationale:** Standard e-commerce functionality
- **Priority:** High
- **Implementation:** CartRepository, CartScreen

#### FR-07: Checkout and Order Placement
- **Description:** Buyers enter delivery address, select payment method (COD), place order
- **Rationale:** Complete purchase workflow
- **Priority:** High
- **Implementation:** CheckoutScreen, CheckoutViewModel, OrderRepository

#### FR-08: Order Tracking
- **Description:** Buyers and sellers view real-time order status with timeline (new → pending → confirmed → processing → shipped → delivered → completed)
- **Rationale:** Transparency in order fulfillment
- **Priority:** High
- **Implementation:** MyOrdersScreen, TrackOrderScreen, OrderRepository

#### FR-09: Price Negotiation System
- **Description:** Buyers propose prices; sellers set auto-accept rules (minimum price, discount percentage)
- **Rationale:** Enable flexible pricing and buyer engagement
- **Priority:** Medium
- **Implementation:** NegotiationRequestsScreen, negotiation logic in ProductRepository

#### FR-10: Co-Seller Stores
- **Description:** Multiple verified sellers collaborate under shared store identity; payments split among members
- **Rationale:** Promote collaboration and increase sales reach
- **Priority:** Medium
- **Implementation:** CoSellerStoreRepository, PaymentSplitProcessor, store management screens

#### FR-11: Payment Processing and Tracking
- **Description:** Process orders, create seller payment records, track payment status (pending → processing → completed/failed/refunded)
- **Rationale:** Manage financial transactions and seller earnings
- **Priority:** High
- **Implementation:** PaymentRepository, SellerPaymentsScreen, PaymentHistoryScreen

#### FR-12: Payment Split System
- **Description:** For co-seller orders, distribute payment among involved sellers based on their items
- **Rationale:** Fair compensation in collaborative stores
- **Priority:** High
- **Implementation:** PaymentSplitProcessor, CoSellerStorePaymentRepository

#### FR-13: Refund Management
- **Description:** Process full/partial/return refunds with approval workflow
- **Rationale:** Handle customer disputes and returns
- **Priority:** High
- **Implementation:** RefundRepository, RefundViewModel, web dashboard refund panel

#### FR-14: Commission System
- **Description:** Track admin commissions from orders (5% default rate); aggregate earnings
- **Rationale:** Platform revenue tracking
- **Priority:** Medium
- **Implementation:** CommissionRepository, commission calculation in PaymentRepository

#### FR-15: Store Ratings and Reviews
- **Description:** Buyers rate sellers after purchase; ratings displayed on store profiles
- **Rationale:** Build trust and seller reputation
- **Priority:** Medium
- **Implementation:** StoreRatingRepository, RateStoreDialog, store profile screens

#### FR-16: Real-Time Chat
- **Description:** Buyers and sellers exchange messages with product/negotiation sharing
- **Rationale:** Enable direct communication
- **Priority:** High
- **Implementation:** ChatRepository, ChatScreen, real-time Firestore listeners

#### FR-17: Notifications
- **Description:** Real-time alerts for orders, messages, approvals, payments, store ratings
- **Rationale:** Keep users informed of important events
- **Priority:** High
- **Implementation:** NotificationRepository, FCM integration, NotificationsScreen

#### FR-18: Learning Resources
- **Description:** Provide links to external tutorials and guides for seller development
- **Rationale:** Support seller skill development
- **Priority:** Medium
- **Implementation:** LearningRepository, LearningResourcesScreen

#### FR-19: Wishlist
- **Description:** Buyers save favorite products for later
- **Rationale:** Improve user engagement
- **Priority:** Low
- **Implementation:** WishlistRepository, WishlistScreen

#### FR-20: Theme Preference
- **Description:** Users toggle between light and dark mode; preference persists
- **Rationale:** Improve user experience and accessibility
- **Priority:** Medium
- **Implementation:** ThemeRepository, ThemeViewModel, theme system

#### FR-21: Account Management
- **Description:** Admin can ban, suspend, or delete user accounts
- **Rationale:** Enforce platform policies and remove violators
- **Priority:** High
- **Implementation:** AuthRepository account status checks, web dashboard user management

#### FR-22: Seller Dashboard
- **Description:** Sellers view sales analytics, total earnings, recent orders, store ratings
- **Rationale:** Provide business insights
- **Priority:** High
- **Implementation:** SellerDashboardScreen, DashboardRepository, DashboardViewModel

#### FR-23: Admin Dashboard
- **Description:** Admins manage sellers, products, orders, refunds, commissions, learning resources
- **Rationale:** Platform oversight and control
- **Priority:** High
- **Implementation:** Web dashboard with React components

#### FR-24: Email Notifications
- **Description:** Send transactional emails for order confirmations, refunds, approvals
- **Rationale:** Keep users informed via email
- **Priority:** Medium
- **Implementation:** EmailJS/SendGrid integration, Cloud Functions

### 4.2 Non-Functional Requirements

#### NFR-01: Performance
- Each screen loads within 3-5 seconds on standard Android devices
- Real-time data sync (orders, messages) occurs within 5-10 seconds
- System handles at least 500 concurrent users without degradation
- Firestore queries optimized with proper indexing

#### NFR-02: Reliability
- System uptime maintained at 99% (excluding scheduled maintenance)
- Data automatically syncs to Firebase every 5 minutes
- System recovery after failure within 5 minutes of restart
- Idempotent payment processing prevents duplicate charges

#### NFR-03: Security
- All sensitive data (passwords, images, payment info) encrypted in transit (HTTPS) and at rest
- Firebase Authentication and role-based access control (RBAC) enforced
- Only admin-approved sellers can publish products
- Payment data accessible only to involved sellers (access control)
- Account ban/suspension system prevents policy violators
- Soft-delete for user accounts (not hard deletion)
- Firestore security rules enforce data privacy

#### NFR-04: Usability
- UI simple, visual, and easy to navigate for non-technical users
- Buttons, icons, text large and readable on small screens
- Support for English language (Urdu support optional)
- Core actions (upload, order, search) require minimal steps
- Theme preference system for accessibility

#### NFR-05: Maintainability
- Codebase modular, documented, and version-controlled (GitHub)
- New modules addable without affecting existing functionality
- System errors and logs recorded for debugging
- Clear separation of concerns (Repository, ViewModel, UI layers)

#### NFR-06: Portability
- Mobile app supports Android 5.0 (API Level 21) and higher
- Web dashboard compatible with Chrome, Firefox, Edge, Safari
- System extendable to iOS or PWA in future versions
- Cloud-based architecture enables easy scaling

#### NFR-07: Scalability
- Firestore auto-scales with user growth
- Cloud Functions scale automatically
- Cloudinary handles image storage at scale
- Real-time database supports thousands of concurrent connections

#### NFR-08: Data Integrity
- Transactions ensure consistent payment processing
- Audit logging for financial operations
- Payment validation prevents invalid transactions
- Retroactive fixes for legacy data (e.g., member count updates)

---

## 5. System Architecture & Data Models

### 5.1 Core Data Models

**User Model**
- id, email, name, role (BUYER/SELLER/CO_SELLER/ADMIN)
- phone, address, profileImage
- storeName, storeDescription
- verified, verificationStatus, verificationPhotoUrl
- sellerApplicationStatus
- isBanned, banReason, isSuspended, suspensionUntil
- createdAt, updatedAt

**Product Model**
- id, title, description, price, category
- sellerId, sellerName, sellerVerified
- imageUrls (stored in Cloudinary)
- isNegotiable, minimumPrice, autoAcceptPrice, autoAcceptDiscount
- stock, weight, specifications
- approvalStatus (pending/approved/rejected)
- createdAt, updatedAt

**Order Model**
- id, buyerId, buyerName, buyerPhone
- sellerId, sellerName
- items (array of OrderItem)
- subtotal, shipping, discount, totalPrice
- status (new/pending/confirmed/processing/shipped/delivered/completed/cancelled)
- shippingAddress, fullAddress
- paymentMethod, transactionId
- coSellerStoreId (for co-seller orders)
- createdAt, updatedAt

**SellerPayment Model**
- id, sellerId, sellerName, orderId
- buyerId, buyerName
- amount, paymentMethod, transactionId
- status (pending/processing/completed/failed/refunded)
- paymentSplits (array for co-seller distribution)
- involvedSellerIds (for access control)
- createdAt, updatedAt, paymentDate

**Notification Model**
- id, userId, title, message
- category (orders/messages/promotions/system/payments/store_ratings)
- relatedId (orderId, messageId, etc.)
- isRead, createdAt

**CoSellerStore Model**
- id, storeName, storeDescription, storeImage
- creatorId, members (array of member objects)
- products (array of productIds)
- totalRating, ratingCount
- createdAt, updatedAt

**Refund Model**
- id, orderId, sellerId, buyerId
- amount, refundType (full/partial/return)
- reason, status (pending/approved/rejected/processed)
- createdAt, updatedAt, processedAt

**AdminCommission Model**
- id, orderId, paymentId, sellerId
- subtotal, commissionRate (5% default)
- commissionAmount, sellerPayout
- status (pending/processing/completed)
- createdAt, updatedAt, paidAt

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

### 5.3 Security Rules (RBAC)

- Users can only read/write their own data
- Products are publicly readable; only sellers can create/update
- Orders readable by buyer, seller, and admin
- Payments readable only by involved sellers and admin
- Admin operations restricted to ADMIN role

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

## 7. Appendix

### 7.1 Technology Stack

**Mobile (Android)**
- Language: Kotlin
- UI: Jetpack Compose
- Architecture: MVVM
- Database: Firebase Firestore
- Authentication: Firebase Auth
- Messaging: Firebase Cloud Messaging
- Image Storage: Cloudinary
- Image Loading: Coil
- Networking: Retrofit + OkHttp
- Serialization: Gson
- Local Storage: DataStore
- Coroutines: Kotlin Coroutines + Flow

**Web (Admin Dashboard)**
- Framework: React.js
- UI Library: Material-UI (MUI)
- Database: Firebase Firestore
- Authentication: Firebase Auth
- State Management: React Hooks
- Notifications: React Hot Toast

**Backend**
- Database: Firebase Firestore
- Authentication: Firebase Auth
- Cloud Functions: Node.js 18
- Messaging: Firebase Cloud Messaging
- Email: EmailJS/SendGrid
- Image Storage: Cloudinary

### 7.2 Deployment Checklist

- [ ] Firebase project configured with Firestore, Auth, Cloud Functions
- [ ] Cloudinary account set up for image storage
- [ ] EmailJS/SendGrid configured for email notifications
- [ ] Firestore security rules deployed
- [ ] Cloud Functions deployed
- [ ] Android app signed and ready for Play Store
- [ ] Web dashboard deployed to Firebase Hosting
- [ ] All environment variables configured
- [ ] Testing completed on multiple Android devices
- [ ] Admin dashboard tested in multiple browsers

### 7.3 Future Enhancements

- Real payment gateway integration (Stripe, JazzCash)
- ML Kit face detection for automated verification
- GPS-based courier tracking
- iOS mobile app
- Advanced analytics and financial reports
- Video tutorials and LMS
- Bulk product upload
- Inventory management with low-stock alerts
- Progressive Web App (PWA) version
- Multi-language support (Urdu, etc.)

### 7.4 Revision History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | Original | Initial SRS document |
| 2.0 | March 2026 | Updated to reflect actual implementation; added payment split system, refund management, commission tracking, store ratings, account management, theme preferences, product approval workflow |
| 2.1 | March 2026 | Added Section 6: System Diagrams and Models with comprehensive UML diagram specifications (Use Case, DFD, Class, Sequence, Activity, ERD, State, Component, Deployment diagrams) |

---

**Document End**
