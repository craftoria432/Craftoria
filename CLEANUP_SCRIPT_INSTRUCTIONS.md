# Cleanup Script - Step-by-Step Instructions

## Quick Fix for "process is not defined" Error

The error occurs because Node.js needs proper configuration for ES6 modules.

---

## Solution: Run These Commands

### Step 1: Install Dependencies

```bash
npm install dotenv firebase
```

### Step 2: Verify .env File Exists

Make sure you have a `.env` file in the project root with these variables:

```env
VITE_FIREBASE_API_KEY=your_api_key_here
VITE_FIREBASE_AUTH_DOMAIN=your_auth_domain_here
VITE_FIREBASE_PROJECT_ID=craftoria432
VITE_FIREBASE_STORAGE_BUCKET=your_storage_bucket_here
VITE_FIREBASE_MESSAGING_SENDER_ID=your_sender_id_here
VITE_FIREBASE_APP_ID=your_app_id_here
```

### Step 3: Run the Cleanup Script

```bash
node cleanup-invalid-products.js
```

---

## If You Still Get Errors

### Option A: Use the Simple Version (No Dependencies)

I'll create a simpler version that doesn't need dotenv:

<parameter name="text">
</invoke>