# 🚀 CLOUD FUNCTIONS DEPLOYMENT - WINDOWS STEP-BY-STEP

**Your Project Structure (Visible in Screenshot):**
```
CRAFTORIA (Project Root)
├── functions/          ← THIS FOLDER
├── app/
├── src/
├── web-admin-updates/
└── firebase.json
```

---

## ✅ EXACT STEPS FOR WINDOWS

### Step 1: Open Terminal in Project Root

**Option A: Using VS Code (Easiest)**
- Press `Ctrl + ~` (backtick key)
- Terminal opens at bottom
- You should see your project root path

**Option B: Using File Explorer**
- Right-click on your project folder
- Select "Open in Terminal" or "Open PowerShell here"

**Option C: Using Command Prompt**
- Open Command Prompt
- Navigate to your project: `cd C:\path\to\Craftoria`

---

### Step 2: Navigate to Functions Folder

**Type this command:**
```bash
cd functions
```

**Expected result:** Terminal shows `functions>` or `...\functions>`

---

### Step 3: Install Dependencies (First Time Only)

**Type this command:**
```bash
npm install
```

**What it does:**
- Installs all packages from `package.json`
- Takes 1-2 minutes
- Creates `node_modules` folder

**Expected result:**
```
added XX packages in X.XXs
```

---

### Step 4: Deploy the Function

**Type this command:**
```bash
firebase deploy --only functions:notifyOrderStatusChange
```

**What it does:**
- Uploads the function to Firebase
- Takes 1-2 minutes
- Shows deployment status

**Expected result:**
```
✔  Deploy complete!

Project Console: https://console.firebase.google.com/project/your-project-id/overview
```

---

## 🎯 COPY-PASTE COMMANDS (For Windows)

**Just copy and paste these one by one:**

```bash
cd functions
```

Then:

```bash
npm install
```

Then:

```bash
firebase deploy --only functions:notifyOrderStatusChange
```

---

## ❌ TROUBLESHOOTING

### Error: "firebase: command not found"

**Solution:**
```bash
npm install -g firebase-tools
```

Then try deployment again.

### Error: "Not logged in"

**Solution:**
```bash
firebase login
```

Browser opens → Login with your Google account → Return to terminal

### Error: "No project selected"

**Solution:**
```bash
firebase use --add
```

Select your project from the list.

### Error: "npm: command not found"

**Solution:** Install Node.js from https://nodejs.org/

---

## ✅ VERIFY DEPLOYMENT

After you see "Deploy complete!":

1. Go to: https://console.firebase.google.com
2. Select your project
3. Click **Functions** (left sidebar)
4. Look for `notifyOrderStatusChange`
5. Should show **Status: OK** ✅

---

## 📊 WHAT HAPPENS AFTER DEPLOYMENT

✅ Cloud Function is live  
✅ Automatically triggers when order status changes  
✅ Creates notifications with `order_id` field  
✅ Seller receives "View Order" button  
✅ Pink hover effect works on Android app  

---

## 💡 TIPS FOR WINDOWS

- Use **PowerShell** (better than Command Prompt)
- Copy commands exactly as shown
- Wait for each command to complete before next one
- Don't close terminal until you see "Deploy complete!"

---

## 🎉 YOU'RE DONE!

Once you see:
```
✔  Deploy complete!
```

The pink hover effect is now live! 🎊

---

**Questions?** Check the troubleshooting section above.
