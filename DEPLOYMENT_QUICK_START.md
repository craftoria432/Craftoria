# ⚡ DEPLOYMENT QUICK START - 3 COMMANDS

**Time Required:** 5 minutes  
**Difficulty:** Easy  
**Platform:** Windows

---

## 🎯 THE 3 COMMANDS

### Command 1: Go to Functions Folder
```bash
cd functions
```

### Command 2: Install Dependencies
```bash
npm install
```

### Command 3: Deploy
```bash
firebase deploy --only functions:notifyOrderStatusChange
```

---

## 📋 HOW TO RUN

1. **Open Terminal** in your project root (Ctrl + ~ in VS Code)
2. **Copy Command 1** → Paste → Press Enter
3. **Copy Command 2** → Paste → Press Enter (wait 1-2 min)
4. **Copy Command 3** → Paste → Press Enter (wait 1-2 min)
5. **See "Deploy complete!"** → You're done! ✅

---

## ✅ SUCCESS INDICATOR

You'll see:
```
✔  Deploy complete!

Project Console: https://console.firebase.google.com/project/...
```

---

## ❌ IF SOMETHING GOES WRONG

| Error | Fix |
|-------|-----|
| `firebase: command not found` | `npm install -g firebase-tools` |
| `Not logged in` | `firebase login` |
| `No project selected` | `firebase use --add` |
| `npm: command not found` | Install Node.js from nodejs.org |

---

## 🎉 DONE!

Pink hover effect is now live on your app! 🚀

---

**Need help?** See `CLOUD_FUNCTIONS_DEPLOYMENT_WINDOWS.md` for detailed steps.
