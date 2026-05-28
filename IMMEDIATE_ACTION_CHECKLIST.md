# ⚡ IMMEDIATE ACTION CHECKLIST - PRODUCTION LAUNCH

**Target Launch Date:** 3-4 weeks from now  
**Current Status:** 85% Production Ready

---

## 🔴 CRITICAL - DO BEFORE LAUNCH (Week 1)

### 1. Create Firestore Security Rules ⚠️ HIGHEST PRIORITY
**Status:** FILE NOT FOUND  
**Time:** 2-3 days  
**Impact:** CRITICAL SECURITY VULNERABILITY

**Action:**
```bash
# Create firestore.rules in project root
touch firestore.rules
```

**Required Rules:**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users collection
    match /users/{userId} {
      allow read: if request.auth != null && request.auth.uid == userId;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Seller payments - only accessible by involved sellers
    match /seller_payments/{paymentId} {
      allow read: if request.auth != null && 
        (request.auth.uid == resource.data.seller_id ||
         request.auth.uid in resource.data.involved_seller_ids);
      allow write: if false; // Only Cloud Functions can write
    }
    
    // Products - public read, authenticated write
    match /products/{productId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    // Orders - only buyer and seller can read
    match /orders/{orderId} {
      allow read: if request.auth != null && 
        (request.auth.uid == resource.data.buyer_id ||
         request.auth.uid == resource.data.seller_id);
      allow write: if request.auth != null;
    }
    
    // Notifications - only user can read their notifications
    match /notifications/{notificationId} {
      allow read: if request.auth != null && 
        request.auth.uid == resource.data.user_id;
      allow write: if false; // Only Cloud Functions can write
    }
    
    // Cart - only user can access their cart
    match /cart/{userId} {
      allow read, write: if request.auth != null && 
        request.auth.uid == userId;
    }
    
    // Chats - participants only
    match /chats/{chatId} {
      allow read, write: if request.auth != null && 
        request.auth.uid in resource.data.participants;
    }
    
    // Co-seller stores - members only
    match /co_seller_stores/{storeId} {
      allow read: if true; // Public view
      allow write: if request.auth != null && 
        request.auth.uid in resource.data.member_ids;
    }
    
    // Store ratings - public read, authenticated write
    match /store_ratings/{ratingId} {
      allow read: if true;
      allow write: if request.auth != null;
    }
    
    // Learning resources - public read
    match /learning_resources/{resourceId} {
      allow read: if true;
      allow write: if false; // Admin only via Cloud Functions
    }
    
    // Reports - only reporter can read
    match /reports/{reportId} {
      allow read: if request.auth != null && 
        request.auth.uid == resource.data.reporter_id;
      allow write: if request.auth != null;
    }
  }
}
```

**Deploy:**
```bash
firebase deploy --only firestore:rules
```

**Test:**
- Go to Firebase Console > Firestore > Rules
- Use Rules Playground to test scenarios
- Verify users can't access other users' data

---

### 2. Deploy Firestore Indexes ⚠️ CRITICAL
**Status:** Defined but not deployed  
**Time:** 1 hour  
**Impact:** Slow queries, potential query failures

**Action:**
```bash
# Deploy indexes
firebase deploy --only firestore:indexes

# Verify in Firebase Console
# Firestore > Indexes > Composite
```

**Expected Result:**
- 9 indexes for seller_payments collection
- All indexes show "Enabled" status

---

### 3. Setup Firebase Crashlytics ⚠️ CRITICAL
**Status:** Not implemented  
**Time:** 1 day  
**Impact:** Can't track production crashes

**Step 1: Add Dependencies**
```kotlin
// app/build.gradle.kts
plugins {
    id("com.google.firebase.crashlytics")
}

dependencies {
    implementation("com.google.firebase:firebase-crashlytics-ktx:18.6.2")
    implementation("com.google.firebase:firebase-analytics-ktx:21.5.1")
}
```

**Step 2: Add Plugin to Root**
```kotlin
// build.gradle.kts (root)
plugins {
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
}
```

**Step 3: Initialize in MainActivity**
```kotlin
// MainActivity.kt
import com.google.firebase.crashlytics.FirebaseCrashlytics

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Enable Crashlytics
    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    
    // Set user identifier
    Firebase.auth.currentUser?.let { user ->
        FirebaseCrashlytics.getInstance().setUserId(user.uid)
    }
}
```

**Step 4: Test**
```kotlin
// Force a test crash
Button(onClick = { throw RuntimeException("Test Crash") }) {
    Text("Test Crash")
}
```

---

### 4. Configure Rate Limiting on Cloud Functions ⚠️ CRITICAL
**Status:** Not implemented  
**Time:** 1 day  
**Impact:** Vulnerable to API abuse, high costs

**Step 1: Install Dependencies**
```bash
cd functions
npm install express-rate-limit
```

**Step 2: Add Rate Limiting**
```javascript
// functions/index.js
const rateLimit = require('express-rate-limit');

// Create limiter
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  message: 'Too many requests, please try again later.'
});

// Apply to callable functions
exports.sendAdminNotification = functions
  .runWith({ memory: '256MB', timeoutSeconds: 60 })
  .https.onCall(async (data, context) => {
    // Check rate limit
    const userId = context.auth?.uid;
    if (!userId) throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
    
    // Your existing code...
  });
```

**Step 3: Deploy**
```bash
firebase deploy --only functions
```

---

### 5. Implement Deep Linking for Notifications ⚠️ HIGH PRIORITY
**Status:** Not implemented  
**Time:** 1-2 days  
**Impact:** Poor notification user experience

**Step 1: Add Intent Filters to AndroidManifest.xml**
```xml
<!-- app/src/main/AndroidManifest.xml -->
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTop">
    
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    
    <!-- Deep link for notifications -->
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="https"
            android:host="craftoria.app"
            android:pathPrefix="/notification" />
    </intent-filter>
    
    <!-- Deep link for orders -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="craftoria"
            android:host="order" />
    </intent-filter>
    
    <!-- Deep link for chat -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="craftoria"
            android:host="chat" />
    </intent-filter>
</activity>
```

**Step 2: Handle Deep Links in MainActivity**
```kotlin
// MainActivity.kt
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Handle deep link
    handleDeepLink(intent)
    
    setContent {
        CraftoriaTheme {
            val navController = rememberNavController()
            NavGraph(
                navController = navController,
                startDestination = getStartDestination()
            )
        }
    }
}

override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    intent?.let { handleDeepLink(it) }
}

private fun handleDeepLink(intent: Intent) {
    val data = intent.data ?: return
    
    when (data.scheme) {
        "craftoria" -> {
            when (data.host) {
                "order" -> {
                    val orderId = data.getQueryParameter("id")
                    // Navigate to order details
                }
                "chat" -> {
                    val chatId = data.getQueryParameter("id")
                    // Navigate to chat
                }
            }
        }
        "https" -> {
            if (data.host == "craftoria.app") {
                when (data.pathSegments.firstOrNull()) {
                    "notification" -> {
                        val notificationId = data.getQueryParameter("id")
                        // Navigate based on notification type
                    }
                }
            }
        }
    }
}
```

**Step 3: Update FCMService to Include Deep Links**
```kotlin
// FCMService.kt
private fun handleOrderUpdate(data: Map<String, String>) {
    val orderId = data["order_id"] ?: return
    val deepLink = "craftoria://order?id=$orderId"
    
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
    val pendingIntent = PendingIntent.getActivity(
        this, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    showNotification(
        title = data["title"] ?: "Order Update",
        message = data["message"] ?: "",
        channelId = CHANNEL_ID_ORDERS,
        pendingIntent = pendingIntent
    )
}
```

---

## 🟡 HIGH PRIORITY - DO IN WEEK 2

### 6. Security Audit
**Time:** 2-3 days

**Checklist:**
- [ ] Test Firestore security rules
- [ ] Verify authentication flows
- [ ] Check for exposed API keys
- [ ] Test payment access control
- [ ] Verify co-seller access restrictions
- [ ] Test admin function authorization
- [ ] Check for SQL injection (N/A - using Firestore)
- [ ] Test XSS vulnerabilities
- [ ] Verify HTTPS everywhere
- [ ] Check for sensitive data in logs

---

### 7. Load Testing
**Time:** 2-3 days

**Test Scenarios:**
- 100 concurrent users browsing products
- 50 concurrent users placing orders
- 20 concurrent users chatting
- 10 concurrent sellers uploading products
- Notification delivery to 1000 users

**Tools:**
- Firebase Test Lab
- JMeter for API testing
- Manual testing with multiple devices

---

### 8. User Acceptance Testing (UAT)
**Time:** 3-4 days

**Test Cases:**
- [ ] User registration and login
- [ ] Product browsing and search
- [ ] Add to cart and checkout
- [ ] Order placement and tracking
- [ ] Seller product management
- [ ] Co-seller store creation
- [ ] Payment tracking
- [ ] Notifications
- [ ] Chat functionality
- [ ] Rating and reviews

---

## 🟢 MEDIUM PRIORITY - DO IN WEEK 3

### 9. Build Missing Web Admin Pages
**Time:** 1 week

**Pages Needed:**
- Dashboard with analytics
- User management (suspend/ban users)
- Product management (approve/reject)
- Order oversight
- Reports management
- Settings

---

### 10. Setup Monitoring
**Time:** 1 day

**Firebase Console:**
- Enable Performance Monitoring
- Setup Alerts for errors
- Configure budget alerts
- Enable Analytics

**Monitoring Checklist:**
- [ ] Crashlytics enabled
- [ ] Performance monitoring enabled
- [ ] Analytics enabled
- [ ] Budget alerts configured
- [ ] Error alerts configured
- [ ] Daily reports enabled

---

## 📋 PRE-LAUNCH CHECKLIST

### Technical
- [ ] Firestore security rules deployed
- [ ] Firestore indexes deployed
- [ ] Crashlytics enabled
- [ ] Rate limiting configured
- [ ] Deep linking implemented
- [ ] Security audit completed
- [ ] Load testing completed
- [ ] UAT completed
- [ ] All critical bugs fixed

### Business
- [ ] Terms of Service written
- [ ] Privacy Policy written
- [ ] Help/Support documentation
- [ ] FAQ created
- [ ] Customer support plan
- [ ] Marketing materials ready
- [ ] App store listing prepared
- [ ] Beta testers recruited

### Legal
- [ ] Terms of Service reviewed by lawyer
- [ ] Privacy Policy compliant with GDPR/CCPA
- [ ] Data retention policy defined
- [ ] User data deletion process
- [ ] Cookie policy (if web app)

---

## 🚀 LAUNCH PLAN

### Week 1: Critical Fixes
- Days 1-2: Firestore security rules
- Day 3: Deploy indexes
- Days 4-5: Deep linking
- Day 6: Crashlytics
- Day 7: Rate limiting

### Week 2: Testing
- Days 1-2: Security audit
- Days 3-4: Load testing
- Days 5-7: UAT and bug fixes

### Week 3: Soft Launch
- Day 1: Deploy to beta (50-100 users)
- Days 2-5: Monitor and fix issues
- Days 6-7: Prepare for full launch

### Week 4: Full Launch
- Day 1: Deploy to production
- Days 2-7: Monitor closely, respond to issues

---

## 📊 SUCCESS METRICS

### Week 1 (Beta)
- 0 critical crashes
- < 5% error rate
- < 2s average response time
- 100% notification delivery

### Month 1 (Production)
- 1000+ active users
- < 1% crash rate
- 4.0+ app store rating
- < 5% churn rate

---

## 🆘 EMERGENCY CONTACTS

**Firebase Support:** https://firebase.google.com/support  
**SendGrid Support:** https://support.sendgrid.com  
**Cloudinary Support:** https://support.cloudinary.com

---

## 📝 NOTES

- Keep this checklist updated as you complete tasks
- Mark items as done with [x]
- Add notes for any blockers
- Review daily with team

---

**Created:** March 18, 2026  
**Last Updated:** March 18, 2026  
**Status:** IN PROGRESS
