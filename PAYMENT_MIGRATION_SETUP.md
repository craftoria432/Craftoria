# Payment Migration Scripts - Setup Guide

## ✅ Fixed Issues

1. **ES Module Error** - Scripts now use `.mjs` extension with proper ES module syntax
2. **Missing Dependencies** - `firebase-admin` is installed
3. **Service Account Key** - Scripts check multiple locations and provide clear error messages

## 🔧 Setup Instructions

### Step 1: Get Firebase Service Account Key

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your Craftoria project
3. Click the gear icon ⚙️ > **Project Settings**
4. Go to **Service Accounts** tab
5. Click **Generate New Private Key**
6. Save the downloaded file as `serviceAccountKey.json` in your project root

```
Craftoria/
├── serviceAccountKey.json  ← Save here
├── check-payments.mjs
├── migrate-buyer-ids.mjs
└── package.json
```

### Step 2: Run the Scripts

#### Check Current Payment Status
```bash
node check-payments.mjs
```

This will show:
- First 10 payment records
- Which ones have `buyer_id`
- Which ones are missing `buyer_id`

#### Migrate Missing buyer_id
```bash
node migrate-buyer-ids.mjs
```

This will:
- Find all payments without `buyer_id`
- Look up the order to get buyer info
- Update payment records with `buyer_id` and `buyer_name`

#### Verify Migration
```bash
node check-payments.mjs
```

Run again to confirm all payments now have `buyer_id`.

## 📋 What These Scripts Do

### check-payments.mjs
- Reads first 10 payment records from Firestore
- Checks if each has `buyer_id` field
- Shows summary of missing data
- Provides next steps

### migrate-buyer-ids.mjs
- Scans all `seller_payments` collection
- For payments missing `buyer_id`:
  - Looks up the related order
  - Extracts `buyer_id` and `buyer_name` from order
  - Updates payment record
- Shows detailed progress and summary

## 🔒 Security Note

**IMPORTANT:** The `serviceAccountKey.json` file contains sensitive credentials!

- ✅ Already added to `.gitignore`
- ❌ Never commit this file to Git
- ❌ Never share this file publicly
- ✅ Keep it secure on your local machine only

## 🐛 Troubleshooting

### Error: "serviceAccountKey.json not found"
**Solution:** Follow Step 1 above to download and save the file

### Error: "Permission denied"
**Solution:** Make sure your Firebase service account has Firestore read/write permissions

### Error: "Order not found"
**Solution:** Some old payments may reference deleted orders - these will be logged as errors but won't stop the migration

### Payment History still shows PKR 0
After migration, if the issue persists:

1. **Check Firestore Rules:**
   ```bash
   firebase deploy --only firestore:rules
   ```

2. **Clear App Cache:**
   - Android: Settings > Apps > Craftoria > Clear Cache
   - Rebuild and reinstall the app

3. **Verify buyer_id matches:**
   - Check Firebase Console
   - Ensure `buyer_id` in payments matches the logged-in user's UID

## 📊 Expected Output

### check-payments.mjs
```
🔍 Checking payment records...

Payment 12345678...
  Order ID: order_abc
  Buyer ID: user_xyz
  Buyer Name: John Doe
  Amount: PKR 1500
  Status: completed

=============================================================
📊 SUMMARY
=============================================================
✅ With buyer_id:    8
❌ Without buyer_id: 2
=============================================================
```

### migrate-buyer-ids.mjs
```
🚀 Starting buyer_id migration...

📊 Found 50 total payment records

✅ Updated payment 12345678... with buyer_id: user_xyz...
✅ Updated payment 87654321... with buyer_id: user_abc...

=============================================================
📊 MIGRATION SUMMARY
=============================================================
✅ Updated:  15
⏭️  Skipped:  30 (already had buyer_id)
❌ Errors:   5
=============================================================
```

## 🎯 Next Steps After Migration

1. ✅ Run `node check-payments.mjs` to verify
2. ✅ Test Payment History screen in the app
3. ✅ Deploy any Firestore rule updates
4. ✅ Clear app cache and test with real users
5. ✅ Monitor Firebase Console for any issues

## 📞 Need Help?

If you encounter issues:
1. Check the error messages - they're designed to be helpful
2. Verify your Firebase service account has proper permissions
3. Check Firebase Console > Firestore to see if data is updating
4. Review the Firestore security rules
