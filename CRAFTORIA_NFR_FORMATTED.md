# CRAFTORIA E-COMMERCE - NON-FUNCTIONAL REQUIREMENTS

## 5.2 Non Functional Requirements

### 5.2.1 Performance Requirement
The system should have immediate response time.

**Identifier:** NFR-01  
**Title:** Performance Requirement  
**Requirement:**
- Each screen should load within 3-5 seconds on standard Android devices
- Real-time data synchronization (orders, messages) should occur within 10 seconds
- Product search results shall be displayed within 3 seconds
- Cart updates shall reflect in database within 5 seconds
- Order placement confirmation shall be displayed within 5 seconds after payment processing
- The system should handle at least 500 concurrent users without degradation in performance

---

### 5.2.2 Reliability Requirement
System shall be accessible 99% of the time.

**Identifier:** NFR-02  
**Title:** Reliability Requirement  
**Requirement:**
- System uptime shall be maintained at 99%, excluding scheduled maintenance
- Database should be synchronized to Firebase cloud every 5 minutes
- System recovery after failure should occur within 5 minutes of restart
- Automatic retry mechanism for failed transactions shall be implemented

---

### 5.2.3 Security Requirement
The system should be secure as it contains personal information about users.

**Identifier:** NFR-03  
**Title:** Security Requirement  
**Requirement:**
- User's personal details (passwords, CNICs, phone numbers) shall be encrypted
- All sensitive data must be encrypted both in transit and at rest
- Authentication and authorization shall be handled via Firebase Authentication with role-based access control (RBAC)
- Only admin-approved sellers can publish products
- All communication shall use HTTPS with SSL/TLS encryption
- Seller verification photos shall be stored securely and deleted after approval/rejection
- Payment transaction details shall be encrypted and logged for audit purposes

---

### 5.2.4 Usability Requirement
The system should be user-friendly for non-technical users.

**Identifier:** NFR-04  
**Title:** Usability Requirement  
**Requirement:**
- The user interface must be simple, visual, and easy to navigate for non-technical users
- Buttons, icons, and text should be large and readable on small screens
- The system shall support English language with localized date and currency formatting
- Core user actions (upload product, place order, search) should require minimal steps (maximum 3-4 steps)
- Error messages shall be displayed via toast notifications and in-app alerts
- Help and support information shall be accessible via dedicated Help & Support screen

---

### 5.2.5 Maintainability Requirement
The system should be easy to maintain and extend.

**Identifier:** NFR-05  
**Title:** Maintainability Requirement  
**Requirement:**
- The codebase follows modular architecture with separation of concerns (MVVM pattern)
- The codebase is version-controlled using Git with commit history
- New modules (e.g., payment gateway, chat features) are designed to be addable without affecting existing functionality through repository pattern and dependency injection
- System errors and logs are recorded using console logging and PaymentAuditLogger for financial operations
- Code documentation follows Kotlin/KDoc standards for critical components (ViewModels, Repositories, Utilities)
- Firebase automatic recovery and retry mechanisms handle system failures
- Large UI components (screens) may exceed 150 lines due to Jetpack Compose declarative UI requirements

---

### 5.2.6 Portability Requirement
System shall be accessible on multiple platforms and devices.

**Identifier:** NFR-06  
**Title:** Portability Requirement  
**Requirement:**
- The mobile app must support Android 5.0 (Lollipop, API Level 21) and higher
- The web dashboard must be compatible with Google Chrome, Firefox, Microsoft Edge, and Safari
- System shall be accessible on devices with minimum 2GB RAM
- The system should be extendable to iOS or Progressive Web App (PWA) platforms in future versions
- Responsive design shall adapt to screen sizes from 4.5" to 13" displays

---

### 5.2.7 Scalability Requirement
System should handle growing user base and data volume.

**Identifier:** NFR-07  
**Title:** Scalability Requirement  
**Requirement:**
- Firebase Firestore shall auto-scale with user growth up to 1 million documents
- Cloud Functions shall scale automatically under load
- Cloudinary shall handle image storage at scale (up to 10,000 images)
- System shall support up to 10,000 registered users in initial phase
- Database queries shall be optimized with proper indexing for performance at scale

---

### 5.2.8 Data Integrity Requirement
System must ensure accuracy and consistency of data.

**Identifier:** NFR-08  
**Title:** Data Integrity Requirement  
**Requirement:**
- Transactions shall ensure consistent payment processing with ACID properties
- Audit logging shall be maintained for all financial operations
- Payment validation shall prevent duplicate or invalid transactions
- Order status updates shall be atomic and consistent across all related entities
- Data backup shall be performed automatically every 24 hours
- Database constraints shall prevent orphaned records and maintain referential integrity

---

### 5.2.6 Portability Requirement
System shall be accessible on multiple platforms and devices.

**Identifier:** NFR-06  
**Title:** Portability Requirement  
**Requirement:**
- The mobile app supports Android 7.0 (Nougat, API Level 24) and higher
- The web dashboard is compatible with Google Chrome, Firefox, Microsoft Edge, and Safari (modern browsers with ES6+ support)
- System is optimized for devices with varying RAM capacities (recommended minimum 2GB RAM for smooth performance)
- The system architecture (Firebase backend, REST APIs) is designed to be extendable to iOS or Progressive Web App (PWA) platforms in future versions
- Responsive design adapts to various screen sizes through Jetpack Compose adaptive layouts and Material Design 3 responsive components

---

### 5.2.7 Scalability Requirement
System should handle growing user base and data volume.

**Identifier:** NFR-07  
**Title:** Scalability Requirement  
**Requirement:**
- Firebase Firestore automatically scales with user growth (no predefined document limit, scales based on Firebase pricing plan)
- Cloud Functions scale automatically under load through Firebase's serverless infrastructure
- Cloudinary handles image storage at scale with CDN distribution (capacity based on subscription plan)
- System architecture supports horizontal scaling through Firebase's distributed infrastructure
- Database queries are optimized with composite indexes defined in firestore.indexes.json for performance at scale
- Batch operations are used for bulk updates to improve performance and reduce API calls

---

### 5.2.8 Data Integrity Requirement
System must ensure accuracy and consistency of data.

**Identifier:** NFR-08  
**Title:** Data Integrity Requirement  
**Requirement:**
- Firestore batch operations ensure atomic updates across multiple documents for consistent data state
- PaymentAuditLogger maintains audit logs for all financial operations (payment creation, refunds, splits)
- PaymentValidator prevents duplicate and invalid transactions through validation checks and idempotency keys
- Order status updates use Firestore batch writes to maintain consistency across related entities (orders, payments, notifications)
- Firebase automatic backup and point-in-time recovery provide data protection (managed by Firebase infrastructure)
- Application-level validation prevents orphaned records through proper relationship management in repositories
- Idempotency keys in payment processing prevent duplicate transaction execution

---

**END OF DOCUMENT**
