# 📁 How to Save serviceAccountKey.json in Project Root

## 🎯 What is "Project Root"?

Your project root is: `C:\Users\mehar\AndroidStudioProjects\Craftoria\`

This is the main folder where you see files like:
- `package.json`
- `check-payments.mjs`
- `migrate-buyer-ids.mjs`
- `.gitignore`
- `firestore.rules`

## 📥 Step-by-Step Instructions

### Step 1: Download the Service Account Key

1. Open your browser and go to: https://console.firebase.google.com/
2. Click on your **Craftoria** project
3. Click the **⚙️ gear icon** (top left) → **Project Settings**
4. Click the **Service Accounts** tab
5. Click the **Generate New Private Key** button
6. Click **Generate Key** in the confirmation dialog
7. A file will download (usually named something like `craftoria-xxxxx-firebase-adminsdk-xxxxx.json`)

### Step 2: Rename the Downloaded File

1. Find the downloaded file (usually in your `Downloads` folder)
2. **Right-click** the file → **Rename**
3. Change the name to exactly: `serviceAccountKey.json`
4. Press Enter

### Step 3: Move to Project Root

**Option A: Using File Explorer (Easiest)**

1. Open File Explorer
2. Navigate to your Downloads folder
3. Find `serviceAccountKey.json`
4. **Right-click** → **Cut** (or press Ctrl+X)
5. Navigate to: `C:\Users\mehar\AndroidStudioProjects\Craftoria\`
6. **Right-click** in empty space → **Paste** (or press Ctrl+V)

**Option B: Using Drag and Drop**

1. Open File Explorer
2. Open TWO windows:
   - Window 1: Your Downloads folder
   - Window 2: `C:\Users\mehar\AndroidStudioProjects\Craftoria\`
3. Drag `serviceAccountKey.json` from Downloads to Craftoria folder

**Option C: Using PowerShell (Current Terminal)**

```powershell
# If file is in Downloads folder:
Move-Item "$env:USERPROFILE\Downloads\serviceAccountKey.json" -Destination "C:\Users\mehar\AndroidStudioProjects\Craftoria\"

# Or if you renamed it but haven't moved it yet:
Move-Item "$env:USERPROFILE\Downloads\craftoria-*-firebase-adminsdk-*.json" -Destination "C:\Users\mehar\AndroidStudioProjects\Craftoria\serviceAccountKey.json"
```

### Step 4: Verify the File is in the Right Place

Run this command in PowerShell:

```powershell
Test-Path ".\serviceAccountKey.json"
```

**Expected output:** `True`

Or list files to see it:

```powershell
ls serviceAccountKey.json
```

## ✅ Final File Structure

After saving, your project should look like this:

```
C:\Users\mehar\AndroidStudioProjects\Craftoria\
├── app/
├── functions/
├── src/
├── .gitignore
├── package.json
├── check-payments.mjs
├── migrate-buyer-ids.mjs
├── serviceAccountKey.json  ← NEW FILE HERE!
└── ... other files
```

## 🧪 Test It Works

Run the check script again:

```powershell
node check-payments.mjs
```

**Before (Error):**
```
❌ ERROR: serviceAccountKey.json not found!
```

**After (Success):**
```
🔍 Checking payment records...
📊 Checking first 10 payments:
...
```

## 🔒 Security Reminder

The file `serviceAccountKey.json` contains sensitive credentials!

✅ **Already protected:**
- It's in `.gitignore` (won't be committed to Git)

❌ **Never:**
- Share this file with anyone
- Upload it to GitHub/GitLab
- Post it in Discord/Slack
- Email it

✅ **Keep it:**
- Only on your local computer
- In the project root folder
- Backed up securely (optional)

## 🐛 Troubleshooting

### "I can't find the downloaded file"

Check these locations:
1. `C:\Users\mehar\Downloads\`
2. Desktop
3. Browser's download location (check browser settings)

### "The file has a different name"

The downloaded file might be named:
- `craftoria-12345-firebase-adminsdk-abcde.json`
- `your-project-firebase-adminsdk-xxxxx.json`

Just rename it to exactly: `serviceAccountKey.json`

### "Permission denied when moving file"

1. Close any programs that might be using the file
2. Make sure you have write permissions to the Craftoria folder
3. Try running PowerShell as Administrator

### "Test-Path returns False"

Make sure:
1. You're in the correct directory: `C:\Users\mehar\AndroidStudioProjects\Craftoria\`
2. The file is named exactly `serviceAccountKey.json` (case-sensitive)
3. The file is in the root folder, not in a subfolder

## 🚀 Next Steps

Once the file is saved correctly:

```powershell
# 1. Check payment status
node check-payments.mjs

# 2. Migrate missing buyer_id
node migrate-buyer-ids.mjs

# 3. Verify migration worked
node check-payments.mjs
```

## 📞 Still Need Help?

If you're still having trouble, run these commands and share the output:

```powershell
# Show current directory
pwd

# List files in current directory
ls

# Check if file exists
Test-Path ".\serviceAccountKey.json"

# Show Downloads folder contents
ls "$env:USERPROFILE\Downloads\*firebase*.json"
```
