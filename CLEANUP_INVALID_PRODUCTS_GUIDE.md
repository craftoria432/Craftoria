# Cleanup Invalid Products Guide

## Problem

Your Firestore `products` collection contains invalid entries with missing or empty IDs, causing errors in the web dashboard.

---

## Solution Options

You have **2 methods** to clean up invalid products:

### Method 1: Firebase Console (Manual) ⭐ RECOMMENDED
### Method 2: Automated Script (Terminal)

---

## Method 1: Firebase Console (Manual)

### Step-by-Step Instructions

#### 1. Open Firebase Console
- Go to: https://console.firebase.google.com/
- Select your project: **craftoria432**
- Navigate to: **Firestore Database**

#### 2. Access Query Builder
- Click on the **"Query"** tab at the top
- Or click **"Start collection"** if you see it

#### 3. Configure Query
```
Collection: products
```

#### 4. Find Invalid Products

**Option A: Visual Inspection**
- Scroll through the products collection
- Look for entries with:
  - Missing `title` field
  - Empty `title` field
  - Title = "No ID"
  - Unusual document IDs

**Option B: Filter by Field**
- Click **"+ Add filter"**
- Set:
  ```
  Field: title
  Operator: ==
  Value: (leave empty or type "No ID")
  ```
- Click **"Run"**

#### 5. Delete Invalid Products
- Select the checkbox next to each invalid product
- Click the **trash icon** (🗑️) at the top
- Confirm deletion

#### 6. Verify Cleanup
- Refresh your web dashboard
- Check if errors are gone
- Verify product count is correct

---

## Method 2: Automated Script (Terminal)

### Prerequisites

1. **Node.js installed** (v16 or higher)
   ```bash
   node --version
   ```

2. **Firebase credentials** from your `.env` file

### Step 1: Get Firebase Configuration

Open your `.env` file and copy these values:

```env
VITE_FIREBASE_API_KEY=your_api_key_here
VITE_FIREBASE_AUTH_DOMAIN=your_auth_domain_here
VITE_FIREBASE_PROJECT_ID=craftoria432
VITE_FIREBASE_STORAGE_BUCKET=your_storage_bucket_here
VITE_FIREBASE_MESSAGING_SENDER_ID=your_sender_id_here
VITE_FIREBASE_APP_ID=your_app_id_here
```

### Step 2: Update Cleanup Script

Open `cleanup-invalid-products.js` and replace the config:

```javascript
const firebaseConfig = {
  apiKey: "YOUR_API_KEY",              // ← Replace with VITE_FIREBASE_API_KEY
  authDomain: "YOUR_AUTH_DOMAIN",      // ← Replace with VITE_FIREBASE_AUTH_DOMAIN
  projectId: "craftoria432",           // ✅ Already correct
  storageBucket: "YOUR_STORAGE_BUCKET", // ← Replace with VITE_FIREBASE_STORAGE_BUCKET
  messagingSenderId: "YOUR_MESSAGING_SENDER_ID", // ← Replace
  appId: "YOUR_APP_ID"                 // ← Replace with VITE_FIREBASE_APP_ID
};
```

### Step 3: Install Dependencies

```bash
npm install firebase
```

### Step 4: Run Cleanup Script

```bash
node cleanup-invalid-products.js
```

### Step 5: Review Output

The script will:
1. Scan all products
2. Identify invalid entries
3. Display a list of products to be deleted
4. Ask for confirmation
5. Delete invalid products
6. Show summary

**Example Output:**
```
═══════════════════════════════════════════════════
  Craftoria - Invalid Products Cleanup Script
═══════════════════════════════════════════════════

🔍 Scanning products collection for invalid entries...

📊 Scan Results:
   Total products: 25
   Valid products: 22
   Invalid products: 3

⚠️  Invalid products found:

  1. "No ID"
     - ID: abc123xyz
     - Seller: Unknown
     - Price: PKR 0
     - Category: N/A
     - Status: N/A

  2. ""
     - ID: def456uvw
     - Seller: Unknown
     - Price: PKR 0
     - Category: N/A
     - Status: N/A

  3. "Untitled"
     - ID: ghi789rst
     - Seller: Unknown
     - Price: PKR 0
     - Category: N/A
     - Status: N/A

❓ Do you want to delete these 3 invalid product(s)? (yes/no): yes

🗑️  Deleting invalid products...

  ✓ Deleted: "No ID" (abc123xyz)
  ✓ Deleted: "" (def456uvw)
  ✓ Deleted: "Untitled" (ghi789rst)

✅ Cleanup complete!
   Successfully deleted: 3
   Errors: 0
   Remaining valid products: 22
```

---

## What the Script Detects

The cleanup script identifies products with:

1. **Missing Document ID**
   ```javascript
   !docId || docId.trim() === ''
   ```

2. **Missing Title**
   ```javascript
   !data.title || data.title.trim() === ''
   ```

3. **Placeholder Title**
   ```javascript
   data.title === 'No ID'
   ```

---

## Safety Features

### Script Safety
- ✅ Shows preview before deletion
- ✅ Requires user confirmation
- ✅ Displays detailed information
- ✅ Counts valid vs invalid products
- ✅ Reports errors if any occur

### What Gets Deleted
- Products with no title
- Products with empty title
- Products with title "No ID"
- Products with missing document IDs

### What Gets Preserved
- All valid products with proper titles
- All products with valid document IDs
- All products with complete data

---

## Verification After Cleanup

### 1. Check Firebase Console
- Open Firestore Database
- Navigate to `products` collection
- Verify invalid entries are gone
- Count should match "Remaining valid products"

### 2. Check Web Dashboard
- Refresh the Product Management page
- Verify no "No ID" errors
- Check product count is correct
- Test filtering and search

### 3. Check Mobile App
- Open mobile app
- Navigate to products list
- Verify products display correctly
- Test product details

---

## Troubleshooting

### Issue: "Firebase config error"

**Solution**: Verify your Firebase credentials in the script match your `.env` file exactly.

### Issue: "Permission denied"

**Solution**: 
1. Check Firestore Security Rules
2. Ensure your Firebase user has admin permissions
3. Try using Firebase Admin SDK instead

### Issue: Script hangs or doesn't respond

**Solution**:
1. Press `Ctrl+C` to cancel
2. Check your internet connection
3. Verify Firebase project ID is correct
4. Try Method 1 (Firebase Console) instead

### Issue: Some products not detected

**Solution**:
The script only detects specific invalid patterns. For custom cleanup:
1. Modify the `isInvalid` condition in the script
2. Add your own validation logic
3. Or use Firebase Console for manual inspection

---

## Prevention: Avoid Future Invalid Products

### In Mobile App (Kotlin)

Ensure all products have required fields:

```kotlin
// ✅ Good - All required fields
val product = hashMapOf(
    "title" to title,
    "price" to price,
    "category" to category,
    "seller_name" to sellerName,
    "seller_id" to sellerId,
    "status" to "active",
    "created_at" to FieldValue.serverTimestamp()
)

// ❌ Bad - Missing required fields
val product = hashMapOf(
    "price" to price  // Missing title!
)
```

### In Web Dashboard (React)

Add validation before creating products:

```javascript
// ✅ Good - Validation before creation
const handleAddProduct = async () => {
  if (!addForm.title || !addForm.price || !addForm.category) {
    toast.error('Title, price and category are required');
    return;
  }
  
  const newProduct = {
    title: addForm.title,
    price: parseFloat(addForm.price),
    category: addForm.category,
    // ... other fields
  };
  
  await addDoc(collection(db, 'products'), newProduct);
};
```

### Firestore Security Rules

Add validation in security rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /products/{productId} {
      allow create: if request.auth != null &&
                       request.resource.data.title is string &&
                       request.resource.data.title.size() > 0 &&
                       request.resource.data.price is number &&
                       request.resource.data.category is string;
      
      allow update: if request.auth != null &&
                       request.resource.data.title is string &&
                       request.resource.data.title.size() > 0;
    }
  }
}
```

---

## Recommendation

**Use Method 1 (Firebase Console)** if:
- You have only a few invalid products
- You want visual confirmation before deletion
- You're not comfortable with terminal commands
- You want to inspect each product manually

**Use Method 2 (Automated Script)** if:
- You have many invalid products (10+)
- You want to automate the cleanup
- You're comfortable with terminal/Node.js
- You want detailed logging and confirmation

---

## After Cleanup

### 1. Verify Dashboard
- [ ] Product Management page loads without errors
- [ ] All products display correctly
- [ ] Filtering and search work
- [ ] No "No ID" entries visible

### 2. Test CRUD Operations
- [ ] Add new product works
- [ ] Edit product works
- [ ] Delete product works
- [ ] Flag/unflag product works

### 3. Check Real-Time Sync
- [ ] Add product in mobile app → appears in web dashboard
- [ ] Edit product in web dashboard → updates in mobile app
- [ ] Delete product in mobile app → disappears from web dashboard

---

## Support

If you encounter issues:

1. Check the error message carefully
2. Verify Firebase credentials are correct
3. Check Firestore Security Rules
4. Review the script output for clues
5. Try the alternative method (Console vs Script)

---

**Document Version**: 1.0  
**Last Updated**: 2026-03-09  
**Status**: Ready to Use
