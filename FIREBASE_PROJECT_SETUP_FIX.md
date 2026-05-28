# 🔧 FIREBASE PROJECT SETUP - FIX

**Error:** `No currently active project`  
**Status:** ⚠️ NEEDS SETUP

---

## ❌ WHAT WENT WRONG

```
Error: No currently active project.
To run this command, you need to specify a project. You have two options:
  - Run this command with --project <alias_or_project_id>.
  - Set an active project by running firebase use --add
```

---

## ✅ SOLUTION - 2 STEPS

### Step 1: List Your Firebase Projects

Run this command to see all your Firebase projects:

```bash
firebase projects:list
```

You'll see output like:
```
✔ Fetching list of Firebase projects...

┌─────────────────────────────────────────────────────────────┐
│ Project ID              │ Display Name                      │
├─────────────────────────────────────────────────────────────┤
│ craftoria-prod          │ Craftoria Production              │
│ craftoria-staging       │ Craftoria Staging                 │
└─────────────────────────────────────────────────────────────┘
```

**Copy your Project ID** (e.g., `craftoria-prod`)

---

### Step 2: Set Active Project

Run this command with your project ID:

```bash
firebase use --add
```

You'll be prompted:
```
? Which project do you want to add? (Use arrow keys)
❯ craftoria-prod
  craftoria-staging
```

**Select your project** (usually the production one)

Then you'll be asked:
```
? What alias do you want to use for this project? (e.g. staging)
```

**Type an alias** (e.g., `prod` or just press Enter for default)

---

## 🚀 NOW DEPLOY

After setting the project, run:

```bash
firebase deploy --only functions:notifyOrderStatusChange
```

---

## ⚡ QUICK COMMAND (If You Know Your Project ID)

If you know your project ID, use this one-liner:

```bash
firebase use craftoria-prod
```

Replace `craftoria-prod` with your actual project ID.

---

## 📋 COMPLETE DEPLOYMENT SEQUENCE

```bash
# 1. Go to functions folder
cd functions

# 2. Install dependencies
npm install

# 3. Set Firebase project (one-time setup)
firebase use --add

# 4. Deploy the function
firebase deploy --only functions:notifyOrderStatusChange
```

---

## ✅ SUCCESS INDICATOR

You'll see:
```
✔  Deploy complete!

Project Console: https://console.firebase.google.com/project/craftoria-prod/functions
```

---

## 🔍 TROUBLESHOOTING

### Issue: "firebase: command not found"
**Solution:** Install Firebase CLI globally
```bash
npm install -g firebase-tools
```

### Issue: "Not logged in"
**Solution:** Login to Firebase
```bash
firebase login
```

### Issue: "No projects found"
**Solution:** Create a Firebase project at https://console.firebase.google.com

### Issue: "Permission denied"
**Solution:** Make sure you have access to the Firebase project

---

## 📝 NOTES

- The `firebase use --add` command only needs to run once
- After that, your project is saved in `.firebaserc` file
- Future deployments will use the saved project automatically
- You can switch projects anytime with `firebase use <project-id>`

---

**Next Step:** Run `firebase use --add` and select your project!
