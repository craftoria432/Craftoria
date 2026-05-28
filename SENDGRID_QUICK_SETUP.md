# SendGrid Quick Setup (5 Minutes)

## TL;DR - Just Do This

### 1. Get API Key (2 min)
```
1. Go to sendgrid.com → Sign Up (free)
2. Verify email
3. Settings → API Keys → Create API Key
4. Copy the key (looks like: SG.xxxxx...)
```

### 2. Verify Sender Email (1 min)
```
1. Settings → Sender Authentication
2. Verify a Single Sender
3. Email: noreply@craftoria.app
4. Click verification link in email
```

### 3. Deploy to Firebase (2 min)
```bash
# Run this command (replace YOUR_KEY with actual key)
firebase functions:config:set sendgrid.key="YOUR_KEY"

# Deploy
firebase deploy --only functions

# Done! ✅
```

### 4. Test (Optional)
```
1. Place an order in your app
2. Check email inbox
3. Should receive order confirmation
```

---

## That's It!

Emails will now be sent automatically when orders are placed.

---

## If Something Goes Wrong

### Email not sending?
```bash
# Check logs
firebase functions:log

# Look for errors in sendOrderConfirmationEmail
```

### API key error?
```bash
# Verify it was set
firebase functions:config:get

# Should show: sendgrid.key = "SG.xxxxx..."
```

### Still stuck?
See **SENDGRID_SETUP_GUIDE.md** for detailed troubleshooting

---

## Files Changed

✅ `functions/index.js` - Added SendGrid integration
✅ `functions/package.json` - Created with dependencies
✅ `functions/.env.example` - Created as reference

---

## Status: ✅ READY

Code is ready. Just need to:
1. Get SendGrid API key
2. Run firebase command
3. Deploy

That's all!
