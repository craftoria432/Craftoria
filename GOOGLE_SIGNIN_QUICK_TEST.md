# Google Sign-In Role Selection - Quick Test Guide

## 🎯 What Was Fixed

When users log in for the first time using "Continue with Google," the app now shows the **Role Selection Screen** instead of automatically navigating away.

---

## 🧪 Quick Test (5 minutes)

### Test 1: New Google User (Buyer)
```
1. Uninstall app or clear data
2. Open app → Login tab
3. Click "Sign in with Google"
4. Sign in with a NEW Google account
5. ✅ EXPECT: Role Selection Screen appears
6. Click "Buyer" card
7. ✅ EXPECT: Navigate to Home Screen
```

### Test 2: New Google User (Seller)
```
1. Uninstall app or clear data
2. Open app → Login tab
3. Click "Sign in with Google"
4. Sign in with a NEW Google account
5. ✅ EXPECT: Role Selection Screen appears
6. Click "Seller" card
7. ✅ EXPECT: Navigate to Seller Verification Screen
```

### Test 3: Existing Google User
```
1. Sign in with Google account used before
2. ✅ EXPECT: Role Selection Screen does NOT appear
3. ✅ EXPECT: Navigate directly to Home/Dashboard
```

---

## 🔍 What to Check

| Scenario | Expected Behavior |
|----------|-------------------|
| New Google user | Role Selection Screen appears |
| Existing Google user (Buyer) | Direct to Home Screen |
| Existing Google user (Seller, not verified) | Direct to Verification Screen |
| Existing Google user (Seller, verified) | Direct to Seller Dashboard |
| Email/password sign-up | No change (works as before) |
| Email/password login | No change (works as before) |

---

## 📱 Firebase Verification

After testing, check Firestore:
1. Go to Firebase Console → Firestore
2. Check `users` collection
3. Find new user document
4. Verify `role` field is set to `buyer` or `seller`

---

## ✅ Success Criteria

- [ ] New Google users see Role Selection Screen
- [ ] Existing Google users skip Role Selection Screen
- [ ] Role is correctly saved in Firestore
- [ ] Navigation works correctly after role selection
- [ ] No crashes or errors
- [ ] Email/password flows unchanged

---

## 🚀 Ready to Deploy

Once all tests pass, the fix is ready for production!
