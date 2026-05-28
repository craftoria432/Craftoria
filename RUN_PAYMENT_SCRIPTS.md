# 🚀 Quick Start - Payment Migration Scripts

## ⚡ TL;DR

```bash
# 1. Get service account key from Firebase Console
# 2. Save as serviceAccountKey.json in project root
# 3. Run:

node check-payments.mjs      # Check status
node migrate-buyer-ids.mjs   # Fix missing buyer_id
node check-payments.mjs      # Verify fix
```

## 📥 Get Service Account Key (One-Time Setup)

1. **Open:** https://console.firebase.google.com/
2. **Select:** Your Craftoria project
3. **Navigate:** ⚙️ Settings > Service Accounts
4. **Click:** "Generate New Private Key" button
5. **Save:** Downloaded file as `serviceAccountKey.json` in project root

```
Craftoria/
├── serviceAccountKey.json  ← Put it here!
├── check-payments.mjs
└── migrate-buyer-ids.mjs
```

## 🔍 What Was Fixed

### Before (Errors):
```
❌ require is not defined in ES module scope
❌ Cannot find package 'firebase-admin'
```

### After (Working):
```
✅ Scripts use ES modules (.mjs)
✅ firebase-admin installed
✅ Helpful error messages
✅ Auto-checks multiple locations for service account key
```

## 📝 Commands

### Check Payment Status
```bash
node check-payments.mjs
```
Shows which payments have/don't have `buyer_id`

### Migrate Missing Data
```bash
node migrate-buyer-ids.mjs
```
Adds `buyer_id` to payments that are missing it

### Verify Results
```bash
node check-payments.mjs
```
Confirm all payments now have `buyer_id`

## 🔒 Security

- `serviceAccountKey.json` is already in `.gitignore`
- Never commit or share this file
- Keep it local only

## 📖 Full Documentation

See `PAYMENT_MIGRATION_SETUP.md` for detailed instructions and troubleshooting.
