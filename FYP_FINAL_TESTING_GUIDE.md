# 🎓 Craftoria FYP - Final Manual Testing Guide

## 📋 Table of Contents
1. [Pre-Testing Setup](#pre-testing-setup)
2. [Phase 1: Seller Journey](#phase-1-seller-journey)
3. [Phase 2: Buyer Journey](#phase-2-buyer-journey)
4. [Phase 3: Web Admin Dashboard](#phase-3-web-admin-dashboard)
5. [Phase 4: Advanced Features](#phase-4-advanced-features)
6. [Phase 5: Integration Testing](#phase-5-integration-testing)
7. [Expected Results Checklist](#expected-results-checklist)

---

## 🔧 Pre-Testing Setup

### Required Accounts
- **3 Test Accounts Needed:**
  - Account 1: Buyer → Seller (for seller verification flow)
  - Account 2: Pure Buyer (for buyer journey)
  - Account 3: Web Admin (admin@craftoria.com)

### Environment Checklist
- [ ] Android app installed and running
- [ ] Web admin dashboard accessible (localhost:3000 or deployed URL)
- [ ] Firebase console access for verification
- [ ] Internet connection stable
- [ ] Camera permission granted (for ML Kit face detection)
- [ ] Notification permissions enabled

---

## 🏪 Phase 1: Seller Journey (30-40 minutes)

### 1.1 User Registration & Authentication
**Test Account 1 - Becoming a Seller**

#### Step 1: Sign Up
1. Open Craftoria app
2. Tap "Sign Up"
3. Enter details:
   - Name: "Test Seller One"
   - Email: "testseller1@example.com"
   - Password: "Test@123"
4. Tap "Sign Up"

**✅ Expected Results:**
- OTP sent to email
- OTP screen appears with 60-second countdown
- "Resend OTP" button disabled during countdown


#### Step 2: OTP Verification
1. Check email inbox (or spam folder)
2. Copy 6-digit OTP
3. Enter OTP in app
4. Tap "Verify"

**✅ Expected Results:**
- Email received within 30 seconds
- OTP verification successful
- Redirected to Home Screen (Buyer view)
- Welcome notification appears

---

### 1.2 Seller Application & Verification

#### Step 3: Apply to Become Seller
1. Tap Profile icon (bottom navigation)
2. Scroll down to "Become a Seller" section
3. Tap "Apply Now"
4. **Confirmation Dialog appears**

**✅ Expected Results:**
- Dialog shows: "Are you sure you want to become a seller?"
- Two options: "Cancel" and "Continue"

#### Step 4: Complete Seller Application
1. Tap "Continue" in confirmation dialog
2. Tap "Take Verification Photo" button

**✅ Expected Results:**
- Camera opens
- ML Kit face detection active
- Green indicator when face detected


#### Step 5: Face Verification (ML Kit)
1. Position face in camera frame
2. Wait for green indicator (face detected)
3. Tap capture button
4. Review photo
5. Tap "Submit Application"

**✅ Expected Results:**
- Face detection works in real-time
- Photo captured successfully
- Application submitted
- Status shows "Pending Approval"
- Notification sent to admin dashboard
- User remains in Buyer mode until approved

---

### 1.3 Admin Approval (Switch to Web Dashboard)

#### Step 6: Admin Reviews Application
1. Open web admin dashboard
2. Login with admin credentials
3. Navigate to "Seller Verification" page
4. Find "Test Seller One" application

**✅ Expected Results:**
- Application appears in pending list
- Shows all submitted details
- Verification photo visible (from Cloudinary)
- ML Kit quality score displayed
- Face detection confidence shown

#### Step 7: Approve Seller
1. Review application details
2. Check verification photo quality
3. Click "Approve" button
4. Add approval notes (optional)
5. Confirm approval

**✅ Expected Results:**
- Application status changes to "Approved"
- Real-time notification sent to mobile app
- Email sent to seller (check inbox/spam)
- Seller badge appears instantly on mobile


---

### 1.4 Seller Dashboard Access (Back to Mobile App)

#### Step 8: Verify Seller Access
1. Return to mobile app (Account 1)
2. Pull down to refresh (if needed)
3. Check notification bell - should show new notification
4. Tap notification to read approval message
5. Navigate to Profile

**✅ Expected Results:**
- Notification badge appears with animation
- "Seller Application Approved" notification visible
- Profile shows "Seller" badge
- Bottom navigation now shows seller options
- "Seller Dashboard" accessible

#### Step 9: Access Seller Dashboard
1. Tap "Seller Dashboard" from bottom nav
2. Review dashboard statistics

**✅ Expected Results:**
- Dashboard loads successfully
- Shows: Total Sales (₨0), Total Orders (0), Pending Orders (0)
- Real-time updates enabled
- Professional UI with cards and charts
- "Add Product" button visible

---

### 1.5 Product Management

#### Step 10: Add First Product
1. From Seller Dashboard, tap "Add Product"
2. Fill product details:
   - **Name:** "Handmade Clay Pot"
   - **Description:** "Traditional handcrafted clay pot"
   - **Price:** 500
   - **Stock:** 20
   - **Category:** "Home Decor"
3. Tap "Add Images" (add 2-3 product photos)
4. Tap "Add Product"

**✅ Expected Results:**
- Product created successfully
- Status: "Pending Approval" (requires admin approval)
- Product appears in "Manage Products" screen
- Badge shows "Pending" status
- Notification sent to admin


#### Step 11: Admin Approves Product (Web Dashboard)
1. Switch to web admin dashboard
2. Navigate to "Product Management"
3. Find "Handmade Clay Pot" in pending products
4. Review product details and images
5. Click "Approve Product"

**✅ Expected Results:**
- Product status changes to "Approved"
- Real-time notification sent to seller
- Product now visible to buyers
- Product appears in search results

#### Step 12: Add More Products (Repeat 2-3 times)
Add these products for testing:
- **Product 2:** "Embroidered Cushion Cover" - ₨350
- **Product 3:** "Wooden Wall Hanging" - ₨800
- **Product 4:** "Handwoven Basket" - ₨450

**✅ Expected Results:**
- All products pending approval
- Admin receives notifications for each
- Approve all products from web dashboard
- All products visible in seller's "Manage Products"

---

### 1.6 Co-Seller Store Creation

#### Step 13: Create Co-Seller Store
1. Navigate to "Co-Seller Stores" tab
2. Tap "Create New Store"
3. Fill store details:
   - **Store Name:** "Artisan Collective"
   - **Description:** "Collaborative store for local artisans"
   - **Commission Rate:** 5%
4. Tap "Create Store"

**✅ Expected Results:**
- Store created successfully
- You are the owner (admin role)
- Store appears in "My Stores" list
- Store has unique ID
- Member count shows: 1 member


#### Step 14: Invite Co-Seller (Create Account 2 as Seller First)
**Note:** You'll need a second seller account for this test.

1. Create and verify Account 2 as seller (repeat steps 1.1-1.4)
2. From Account 1, go to "Artisan Collective" store
3. Tap "Invite Members"
4. Search for Account 2 seller by name
5. Send invitation

**✅ Expected Results:**
- Invitation sent successfully
- Account 2 receives notification
- Invitation appears in Account 2's notifications
- Store shows "Pending Invitations: 1"

#### Step 15: Accept Invitation (Account 2)
1. Switch to Account 2
2. Check notifications
3. Tap invitation notification
4. Review store details
5. Tap "Accept Invitation"

**✅ Expected Results:**
- Invitation accepted
- Account 2 now member of store
- Member count updates to 2 (real-time)
- Both sellers can add products to store

---

## 🛒 Phase 2: Buyer Journey (25-35 minutes)

### 2.1 Product Discovery

#### Step 16: Browse as Buyer (Account 2 or New Account)
**Use Account 2 or create new buyer account**

1. Open app and login as buyer
2. View Home Screen

**✅ Expected Results:**
- Animated banner carousel at top (auto-scrolls every 3 seconds)
- Featured stores section with ratings
- Product grid showing approved products
- Search bar at top
- Categories tabs visible
- Bottom navigation: Home, Search, Cart, Messages, Profile


#### Step 17: Search Products
1. Tap search bar or Search tab
2. Enter "clay pot"
3. View search results

**✅ Expected Results:**
- Search results appear instantly
- Shows "Handmade Clay Pot" product
- Product card shows: image, name, price, seller name, rating
- Can filter by category
- Can sort by price/rating

#### Step 18: View Product Details
1. Tap on "Handmade Clay Pot" product card
2. Review product details screen

**✅ Expected Results:**
- Full product information displayed
- Image gallery (swipeable)
- Price, stock availability shown
- Seller name with real-time updates
- Seller rating visible
- "Add to Cart" button
- "Add to Wishlist" heart icon
- "Negotiate Price" button
- "Chat with Seller" button
- "View Seller Profile" link

---

### 2.2 Wishlist & Cart Management

#### Step 19: Add to Wishlist
1. Tap heart icon on product details
2. Navigate to Profile → Wishlist

**✅ Expected Results:**
- Heart icon fills with color
- Toast message: "Added to wishlist"
- Wishlist badge appears on bottom nav (if not already visible)
- Product appears in wishlist screen
- Badge count updates in real-time

#### Step 20: Add to Cart
1. Return to product details
2. Select quantity: 2
3. Tap "Add to Cart"

**✅ Expected Results:**
- Success message appears
- Cart badge appears/updates on bottom nav
- Badge shows count with animation
- Product added to cart with selected quantity


#### Step 21: View Cart
1. Tap Cart icon (bottom nav)
2. Review cart contents

**✅ Expected Results:**
- Shows "Handmade Clay Pot" × 2
- Unit price: ₨500
- Subtotal: ₨1,000
- Shipping fee displayed
- Total amount calculated
- Quantity adjustment buttons (+/-)
- Remove item option
- "Proceed to Checkout" button

---

### 2.3 Price Negotiation

#### Step 22: Initiate Negotiation
1. Go back to product details
2. Tap "Negotiate Price"
3. Enter proposed price: 450
4. Add message: "Can you offer a discount for bulk order?"
5. Tap "Send Negotiation Request"

**✅ Expected Results:**
- Negotiation request sent
- Status badge shows "Pending"
- Seller receives real-time notification
- Request appears in seller's "Negotiation Requests" screen

#### Step 23: Seller Responds (Switch to Account 1)
1. Switch to seller account (Account 1)
2. Check notification bell - new badge appears
3. Navigate to "Negotiation Requests"
4. Find buyer's negotiation request
5. Review details
6. Choose action:
   - **Option A:** Accept (₨450)
   - **Option B:** Counter-offer (₨475)
   - **Option C:** Reject

**For this test, select "Accept"**

**✅ Expected Results:**
- Seller can see buyer's proposed price
- Three action buttons visible
- After accepting:
  - Buyer receives real-time notification
  - Negotiated price updates in buyer's cart
  - Badge shows "Accepted" status


#### Step 24: Verify Negotiated Price (Back to Buyer)
1. Return to buyer account
2. Check notification - "Negotiation Accepted"
3. Go to Cart
4. Verify price updated

**✅ Expected Results:**
- Notification received with animation
- Cart shows negotiated price: ₨450 (was ₨500)
- Subtotal recalculated: ₨900 (2 × ₨450)
- Badge shows "Negotiated Price" indicator
- Total amount updated

---

### 2.4 Chat with Seller

#### Step 25: Start Chat
1. From product details, tap "Chat with Seller"
2. Send message: "Hello, when can you ship this?"

**✅ Expected Results:**
- Chat screen opens instantly (no delay)
- Seller name displayed in header (real-time)
- Seller profile picture shown
- Message sent successfully
- Timestamp displayed
- Product context shown at top of chat

#### Step 26: Seller Responds (Account 1)
1. Switch to seller account
2. Check Messages tab - badge appears
3. Tap Messages
4. Open chat with buyer
5. Reply: "I can ship within 2 days!"

**✅ Expected Results:**
- Seller receives real-time notification
- Chat badge updates
- Message appears in chat list
- Buyer receives message in real-time
- Read receipts work
- Profile pictures display correctly


---

### 2.5 Checkout & Payment

#### Step 27: Proceed to Checkout
1. Return to buyer account
2. Go to Cart
3. Tap "Proceed to Checkout"
4. Fill shipping details:
   - **Full Name:** "Test Buyer"
   - **Phone:** "+923001234567"
   - **Address:** "456 Buyer Street, Lahore"
   - **City:** "Lahore"
   - **Postal Code:** "54000"

**✅ Expected Results:**
- Checkout form appears
- All fields required
- Form data persists if you navigate away
- Order summary displayed
- Payment method selection available

#### Step 28: Select Payment Method
1. Choose payment method: "Cash on Delivery"
2. Review order summary:
   - Items: 2 × Handmade Clay Pot
   - Subtotal: ₨900
   - Shipping: ₨150
   - Total: ₨1,050
3. Tap "Place Order"

**✅ Expected Results:**
- Order processing indicator
- Order created successfully
- Redirected to Order Success Screen
- Order confirmation details shown
- Email sent to buyer (check inbox/spam)
- Cart cleared automatically
- Cart badge disappears

---

### 2.6 Order Tracking

#### Step 29: Track Order
1. From Order Success Screen, tap "Track Order"
   OR
2. Navigate to Profile → My Orders
3. Find recent order
4. Tap to view details

**✅ Expected Results:**
- Order details displayed
- Status: "Pending" (yellow badge)
- Order timeline shown
- Delivery date estimate displayed
- "Track Order" button visible
- Seller information shown
- Payment status: "Pending"


---

## 🖥️ Phase 3: Web Admin Dashboard (20-30 minutes)

### 3.1 Dashboard Overview

#### Step 30: Admin Login
1. Open web admin dashboard
2. Login with admin credentials
3. View main dashboard

**✅ Expected Results:**
- Dashboard loads with statistics
- Shows: Total Users, Total Orders, Total Revenue, Pending Verifications
- Real-time updates enabled
- Charts and graphs displayed
- Recent activity feed
- Quick action buttons

---

### 3.2 Order Management

#### Step 31: View All Orders
1. Navigate to "Order Oversight" page
2. Find the recent order from buyer

**✅ Expected Results:**
- All orders listed in table
- Shows: Order ID, Buyer, Seller, Amount, Status, Date
- Can filter by status
- Can search by order ID or user
- Real-time updates when status changes
- Export functionality available

#### Step 32: View Order Details
1. Click on the recent order
2. Review order details modal/page

**✅ Expected Results:**
- Complete order information
- Buyer and seller details
- Product list with quantities
- Payment information
- Shipping address
- Order timeline
- Status history
- Admin can add notes

---

### 3.3 Product Management

#### Step 33: Manage Products
1. Navigate to "Product Management"
2. View all products

**✅ Expected Results:**
- All products listed (approved and pending)
- Filter options: Status, Category, Seller
- Search functionality
- Product images displayed
- Quick approve/reject buttons
- Bulk actions available


#### Step 34: Reject a Product (Test)
1. Find any pending product
2. Click "Reject"
3. Add rejection reason: "Image quality too low"
4. Confirm rejection

**✅ Expected Results:**
- Product status changes to "Rejected"
- Seller receives real-time notification
- Email sent to seller with reason
- Product removed from buyer view
- Rejection reason logged

---

### 3.4 User Management

#### Step 35: View All Users
1. Navigate to "Users" section
2. View user list

**✅ Expected Results:**
- All registered users displayed
- Shows: Name, Email, Role (Buyer/Seller), Status, Join Date
- Filter by role
- Search by name/email
- User statistics visible
- Can view user details

#### Step 36: View User Profile
1. Click on "Test Seller One"
2. Review user profile

**✅ Expected Results:**
- Complete user information
- Role and status
- Verification status
- Order history
- Product list (if seller)
- Activity log
- Can suspend/activate user

---

### 3.5 Co-Seller Store Management

#### Step 37: View Co-Seller Stores
1. Navigate to "Co-Seller Stores"
2. View all stores

**✅ Expected Results:**
- All co-seller stores listed
- Shows: Store Name, Owner, Members, Products, Revenue, Status
- Store ratings displayed
- Can view store details
- Member count accurate and real-time
- Commission rates shown


#### Step 38: View Store Details
1. Click on "Artisan Collective" store
2. Review store information

**✅ Expected Results:**
- Store details displayed
- Member list with roles
- Products from store
- Revenue breakdown
- Payment split information
- Store ratings and reviews
- Activity timeline

---

### 3.6 Notifications & Reports

#### Step 39: Check Admin Notifications
1. Click notification bell icon
2. View notifications

**✅ Expected Results:**
- Real-time notifications displayed
- Shows: New orders, seller applications, product submissions
- Unread count badge
- Can filter by type
- Click to navigate to relevant page
- Mark as read functionality

#### Step 40: Generate Reports
1. Navigate to "Reports" section
2. Select report type: "Sales Report"
3. Choose date range: Last 7 days
4. Generate report

**✅ Expected Results:**
- Report generated successfully
- Shows sales statistics
- Charts and visualizations
- Export to PDF/Excel options
- Can filter by seller, product, category
- Revenue breakdown displayed

---

## 🚀 Phase 4: Advanced Features (15-20 minutes)

### 4.1 Order Fulfillment Flow

#### Step 41: Seller Processes Order (Account 1)
1. Switch to seller account
2. Check notification - "New Order Received"
3. Navigate to "Orders" tab
4. Find the pending order
5. Tap on order to view details
6. Tap "Accept Order"

**✅ Expected Results:**
- Order status changes to "Processing"
- Buyer receives notification
- Badge updates in real-time
- Order appears in "Active Orders"
- Estimated delivery date set


#### Step 42: Mark as Shipped
1. From order details, tap "Mark as Shipped"
2. Enter tracking details (optional):
   - Courier: "TCS"
   - Tracking Number: "TCS123456789"
3. Confirm shipping

**✅ Expected Results:**
- Order status changes to "Shipped"
- Buyer receives notification with tracking info
- Email sent to buyer
- Delivery date updated
- Timeline updated

#### Step 43: Buyer Tracks Shipment (Account 2)
1. Switch to buyer account
2. Check notification - "Order Shipped"
3. Tap notification or go to My Orders
4. View order details
5. See tracking information

**✅ Expected Results:**
- Status shows "Shipped" (blue badge)
- Tracking number displayed
- Courier information shown
- Estimated delivery date visible
- Order timeline updated
- "Track Order" button highlighted

#### Step 44: Mark as Delivered
1. Switch back to seller account
2. Open the order
3. Tap "Mark as Delivered"
4. Confirm delivery

**✅ Expected Results:**
- Order status changes to "Delivered"
- Buyer receives notification
- Email sent to buyer
- Payment status updates
- Order moves to "Completed Orders"
- Seller dashboard statistics update in real-time

---

### 4.2 Payment System

#### Step 45: Verify Payment Split (Co-Seller Store Order)
**Note:** This requires an order from co-seller store product

1. Admin views order in dashboard
2. Check payment breakdown

**✅ Expected Results:**
- Total amount shown
- Platform commission calculated (5%)
- Seller share calculated
- Co-seller shares calculated (if applicable)
- Payment split accurate
- All amounts add up correctly


#### Step 46: Seller Views Payment History
1. Seller navigates to "Payments" tab
2. Views payment history

**✅ Expected Results:**
- All payments listed
- Shows: Order ID, Amount, Commission, Net Amount, Status, Date
- Real-time updates
- Filter by status and date
- Can view payment details
- Export functionality

#### Step 47: Buyer Views Payment History
1. Buyer navigates to Profile → Payment History
2. Views all payments

**✅ Expected Results:**
- All buyer payments listed
- Shows: Order ID, Amount, Payment Method, Status, Date
- Can view order details from payment
- Real-time updates for new orders
- Retrospective and prospective orders shown

---

### 4.3 Store Rating System

#### Step 48: Rate Seller Store (After Delivery)
1. Buyer goes to delivered order
2. Tap "Rate Store" button
3. Rate store dialog appears:
   - Select stars: 5 stars
   - Add review: "Excellent quality and fast shipping!"
4. Submit rating

**✅ Expected Results:**
- Rating submitted successfully
- Store rating updates immediately
- Rating appears on store profile
- Seller receives notification
- Average rating recalculated
- Rating syncs between Android and Web

#### Step 49: View Store Ratings
1. Navigate to "All Stores" screen
2. Find "Test Handicrafts Store"
3. View store card

**✅ Expected Results:**
- Store rating displayed (5.0 ⭐)
- Rating count shown
- Can tap to view all reviews
- Rating updates in real-time across all screens
- Web dashboard shows same rating

---

### 4.4 Theme System

#### Step 50: Test Theme Switching
1. Go to Profile → Settings
2. Tap "Theme Preference"
3. Select "Dark Mode"

**✅ Expected Results:**
- Theme changes immediately
- Smooth transition animation
- All screens update
- Theme persists after app restart
- No black background issues
- Icons update appropriately


#### Step 51: Test System Theme
1. Go back to Settings
2. Select "System Default"
3. Change device theme (Android settings)

**✅ Expected Results:**
- App follows system theme
- Updates automatically when system theme changes
- Preference saved correctly

---

### 4.5 Learning Resources (Seller Feature)

#### Step 52: Access Learning Resources
1. Seller navigates to "Learning" tab
2. Browse available resources

**✅ Expected Results:**
- Learning resources displayed
- Categories: Business Tips, Marketing, Product Photography, etc.
- Can search resources
- Can filter by category
- Resources load from Firebase
- Professional content displayed

#### Step 53: View Resource Details
1. Tap on a learning resource
2. Read content

**✅ Expected Results:**
- Full resource content displayed
- Images and formatting preserved
- Can bookmark resource
- Can share resource
- Related resources suggested

---

## 🔄 Phase 5: Integration Testing (15-20 minutes)

### 5.1 Real-Time Updates

#### Step 54: Test Real-Time Notifications
**Use 2 devices or accounts simultaneously**

1. **Device A (Seller):** Add new product
2. **Device B (Admin Dashboard):** Watch for notification

**✅ Expected Results:**
- Admin receives notification within 2 seconds
- Notification badge appears with animation
- Product appears in pending list immediately
- No page refresh needed

#### Step 55: Test Real-Time Name Updates
1. **Account 1:** Change profile name to "Updated Seller Name"
2. **Account 2:** View products from this seller

**✅ Expected Results:**
- Seller name updates across all screens
- Product cards show new name
- Chat headers update
- Order screens update
- No app restart needed


#### Step 56: Test Real-Time Dashboard Updates
1. **Buyer:** Place new order
2. **Seller Dashboard:** Watch statistics

**✅ Expected Results:**
- Total Orders count increases immediately
- Total Sales amount updates
- Pending Orders count increases
- Charts update in real-time
- No manual refresh needed

---

### 5.2 Badge System

#### Step 57: Test All Badge Types
Verify badges appear correctly:

1. **Notification Badge:**
   - Appears when new notification arrives
   - Shows correct count
   - Animates on appearance
   - Clears when notifications read

2. **Cart Badge:**
   - Shows item count
   - Updates when items added/removed
   - Animates on change
   - Disappears when cart empty

3. **Message Badge:**
   - Shows unread message count
   - Updates in real-time
   - Clears when messages read
   - Works for both buyer and seller

4. **Order Badge (Seller):**
   - Shows pending order count
   - Updates when new order arrives
   - Animates on new order
   - Clears when orders processed

**✅ Expected Results:**
- All badges work correctly
- Counts accurate
- Animations smooth
- Real-time updates
- No duplicate badges

---

### 5.3 Email System

#### Step 58: Verify All Email Types
Check email inbox for these emails:

1. **OTP Verification Email**
   - Subject: "Your Craftoria Verification Code"
   - Contains 6-digit OTP
   - Professional template
   - Craftoria branding

2. **Seller Approval Email**
   - Subject: "Seller Application Approved"
   - Congratulations message
   - Next steps included

3. **Order Confirmation Email**
   - Subject: "Order Confirmation #[OrderID]"
   - Order details
   - Shipping information
   - Total amount

4. **Order Shipped Email**
   - Subject: "Your Order Has Been Shipped"
   - Tracking information
   - Estimated delivery date

5. **Order Delivered Email**
   - Subject: "Your Order Has Been Delivered"
   - Request for rating
   - Support information

**✅ Expected Results:**
- All emails received (check spam folder)
- Professional templates
- Correct information
- Links work correctly
- Branding consistent


---

### 5.4 Error Handling & Edge Cases

#### Step 59: Test Offline Handling
1. Turn off internet connection
2. Try to perform actions (add to cart, send message, etc.)

**✅ Expected Results:**
- Appropriate error messages shown
- "No internet connection" alert
- Actions queued when possible
- Graceful degradation
- No app crashes

#### Step 60: Test Empty States
1. View cart when empty
2. View wishlist when empty
3. View orders when no orders
4. View messages when no chats

**✅ Expected Results:**
- Professional empty state messages
- Helpful illustrations
- Call-to-action buttons
- No blank screens
- User-friendly guidance

#### Step 61: Test Form Validation
1. Try to submit forms with invalid data:
   - Empty required fields
   - Invalid email format
   - Short passwords
   - Invalid phone numbers

**✅ Expected Results:**
- Validation errors shown
- Clear error messages
- Fields highlighted
- Cannot submit invalid forms
- Helpful error text

---

### 5.5 Performance Testing

#### Step 62: Test App Performance
1. Navigate between screens rapidly
2. Scroll through long product lists
3. Load images in product gallery
4. Switch between tabs quickly

**✅ Expected Results:**
- Smooth transitions
- No lag or stuttering
- Images load efficiently
- No memory leaks
- Responsive UI

#### Step 63: Test Search Performance
1. Search for products with various queries
2. Apply filters
3. Sort results

**✅ Expected Results:**
- Search results appear quickly (<1 second)
- Filters apply instantly
- Sorting works correctly
- No delays or freezing
- Accurate results

---

## ✅ Expected Results Checklist

### Authentication & User Management
- [ ] Sign up with OTP verification works
- [ ] OTP countdown timer functions correctly
- [ ] Login/logout works smoothly
- [ ] Password reset functional
- [ ] Profile updates save correctly
- [ ] Theme preferences persist

### Seller Features
- [ ] Seller application with ML Kit face detection works
- [ ] Admin can approve/reject sellers
- [ ] Seller dashboard shows accurate statistics
- [ ] Product management (add/edit/delete) works
- [ ] Product approval workflow functions
- [ ] Order management complete
- [ ] Payment history accurate
- [ ] Negotiation system works
- [ ] Co-seller store creation and management works

### Buyer Features
- [ ] Product browsing and search works
- [ ] Cart management functional
- [ ] Wishlist works correctly
- [ ] Checkout process smooth
- [ ] Order tracking accurate
- [ ] Payment history displays correctly
- [ ] Chat with sellers works
- [ ] Store rating system functional
- [ ] Negotiation requests work

### Web Admin Dashboard
- [ ] Dashboard statistics accurate
- [ ] Order oversight complete
- [ ] Product management works
- [ ] User management functional
- [ ] Seller verification workflow complete
- [ ] Co-seller store management works
- [ ] Reports generation works
- [ ] Real-time notifications function

### Real-Time Features
- [ ] Notifications appear instantly
- [ ] Badge counts update in real-time
- [ ] Dashboard statistics update live
- [ ] Name changes reflect immediately
- [ ] Order status updates sync
- [ ] Payment updates real-time
- [ ] Chat messages instant
- [ ] Member counts update live

### Email System
- [ ] OTP emails delivered
- [ ] Order confirmation emails sent
- [ ] Seller approval emails sent
- [ ] Order status emails sent
- [ ] All emails professional and branded

### UI/UX
- [ ] Animated banners work
- [ ] Badge animations smooth
- [ ] Theme switching works
- [ ] Navigation smooth
- [ ] Forms validate correctly
- [ ] Empty states display properly
- [ ] Error messages clear
- [ ] Loading indicators shown

### Performance
- [ ] App loads quickly
- [ ] Smooth scrolling
- [ ] Fast search results
- [ ] Images load efficiently
- [ ] No crashes or freezes
- [ ] Offline handling graceful

---

## 📊 Testing Summary Report Template

After completing all tests, document results:

### Test Execution Summary
- **Date:** [Date]
- **Tester:** [Your Name]
- **App Version:** [Version]
- **Device:** [Device Model]
- **Android Version:** [Version]

### Results
- **Total Test Cases:** 63
- **Passed:** ___
- **Failed:** ___
- **Blocked:** ___
- **Not Tested:** ___

### Critical Issues Found
1. [Issue description]
2. [Issue description]

### Minor Issues Found
1. [Issue description]
2. [Issue description]

### Recommendations
1. [Recommendation]
2. [Recommendation]

### Overall Assessment
- [ ] Ready for submission
- [ ] Minor fixes needed
- [ ] Major fixes required

---

## 🎯 Quick Test Scenarios (For Demo)

### Scenario 1: Complete Seller Journey (10 minutes)
1. Sign up → Apply as seller → Get approved → Add product → Receive order → Ship order

### Scenario 2: Complete Buyer Journey (8 minutes)
2. Browse products → Add to cart → Negotiate price → Checkout → Track order → Rate store

### Scenario 3: Admin Management (5 minutes)
3. Approve seller → Approve product → Monitor orders → View reports

### Scenario 4: Real-Time Features (5 minutes)
4. Demonstrate live notifications → Badge updates → Dashboard statistics → Name updates

---

## 📝 Notes for FYP Presentation

### Key Features to Highlight:
1. **ML Kit Face Detection** - Unique seller verification
2. **Real-Time Updates** - Instant notifications and data sync
3. **Co-Seller Stores** - Collaborative selling platform
4. **Payment Split System** - Automated commission distribution
5. **Negotiation System** - Interactive price negotiation
6. **Comprehensive Admin Dashboard** - Full platform oversight
7. **Professional Email System** - Automated email notifications
8. **Theme System** - User preference management
9. **Badge System** - Real-time count indicators
10. **Store Rating System** - Android-Web synchronization

### Technical Achievements:
- Firebase real-time database integration
- Cloud Functions for backend logic
- ML Kit for face detection
- EmailJS/SendGrid integration
- Cloudinary for image storage
- Professional UI/UX design
- Comprehensive error handling
- Performance optimization

---

## 🚀 Final Submission Checklist

- [ ] All test cases passed
- [ ] No critical bugs
- [ ] Email system configured and working
- [ ] Firebase project properly set up
- [ ] Web dashboard deployed
- [ ] APK generated and tested
- [ ] Documentation complete
- [ ] Screenshots/videos prepared
- [ ] Demo account credentials ready
- [ ] Presentation slides prepared

---

**Good luck with your FYP submission! 🎓**

*This testing guide covers all major features and workflows in your Craftoria app. Follow it systematically for comprehensive testing.*
