# ML Kit Integration - Deployment Checklist

## Pre-Deployment Review

### Code Review
- [ ] SellerVerificationViewModel.kt reviewed
  - [ ] `saveVerificationResultToFirestore()` method correct
  - [ ] Firebase imports added
  - [ ] Error handling implemented
  - [ ] No compilation errors

- [ ] SellerVerificationScreen.kt reviewed
  - [ ] `handleSubmitVerification` lambda added
  - [ ] Calls save method before submission
  - [ ] Error handling for failed saves
  - [ ] No compilation errors

- [ ] SellerVerificationDashboard.jsx reviewed
  - [ ] ML Kit data display enhanced
  - [ ] Progress bar implemented
  - [ ] Color coding correct
  - [ ] Approve/reject dialogs updated
  - [ ] No linting errors

### Testing
- [ ] Mobile app compiles without errors
- [ ] Web dashboard compiles without errors
- [ ] No TypeScript/Kotlin type errors
- [ ] No runtime errors in console

---

## Mobile App Deployment

### Build & Test
- [ ] Build mobile app: `./gradlew build`
- [ ] Run unit tests: `./gradlew test`
- [ ] Run instrumented tests: `./gradlew connectedAndroidTest`
- [ ] Test on physical device
- [ ] Test on emulator

### Functionality Testing
- [ ] Open Seller Verification screen
- [ ] Take selfie with camera
- [ ] ML Kit analyzes face
- [ ] Confidence score displays
- [ ] Face count displays
- [ ] Validation status displays
- [ ] Click "Submit Verification"
- [ ] Check Firestore: `seller_verifications/{userId}` created
- [ ] Verify `mlKitResult` field exists
- [ ] Verify data structure matches spec

### Edge Cases
- [ ] Test with poor lighting
- [ ] Test with multiple faces
- [ ] Test with no face detected
- [ ] Test with glasses/hat
- [ ] Test with rotated head
- [ ] Test with eyes closed
- [ ] Test with blurry image

### Deployment
- [ ] Create release build
- [ ] Sign APK/AAB
- [ ] Upload to Play Store (internal testing first)
- [ ] Test on multiple devices
- [ ] Monitor crash reports

---

## Web Dashboard Deployment

### Build & Test
- [ ] Build web app: `npm run build`
- [ ] Run linter: `npm run lint`
- [ ] Run tests: `npm run test`
- [ ] No build errors
- [ ] No linting warnings

### Functionality Testing
- [ ] Open Seller Verification Dashboard
- [ ] Dashboard loads without errors
- [ ] Verifications display correctly
- [ ] ML Kit confidence shows with progress bar
- [ ] Face count displays
- [ ] ML Kit status shows (Valid/Invalid)
- [ ] Color coding works (green/orange/red)
- [ ] Filter by status works
- [ ] Search by name/email works
- [ ] Click "View Photo" works
- [ ] Click "Approve" opens dialog
- [ ] Click "Reject" opens dialog

### Approve Dialog Testing
- [ ] Dialog shows ML Kit confidence
- [ ] Dialog shows face count
- [ ] Can add welcome message
- [ ] Can submit approval
- [ ] Firestore updates correctly
- [ ] Notification sent to seller

### Reject Dialog Testing
- [ ] Dialog shows rejection reason field
- [ ] Dialog shows message field
- [ ] Can submit rejection
- [ ] Firestore updates correctly
- [ ] Notification sent to seller

### Deployment
- [ ] Build production bundle
- [ ] Deploy to hosting
- [ ] Test on production URL
- [ ] Monitor error logs
- [ ] Check performance metrics

---

## Firestore Configuration

### Rules Deployment
- [ ] Review firestore.rules
- [ ] Add seller_verifications rules
- [ ] Test rules in simulator
- [ ] Deploy rules: `firebase deploy --only firestore:rules`
- [ ] Verify rules deployed

### Admin Setup
- [ ] Identify admin users
- [ ] Set admin custom claims for each admin
- [ ] Verify claims set correctly
- [ ] Test admin access to verifications

### Collection Setup
- [ ] Verify `seller_verifications` collection exists
- [ ] Check document structure
- [ ] Verify indexes created if needed
- [ ] Test queries work correctly

---

## Integration Testing

### End-to-End Flow
- [ ] Create test seller account
- [ ] Mobile: Open verification screen
- [ ] Mobile: Take selfie
- [ ] Mobile: Submit verification
- [ ] Firestore: Verify data saved
- [ ] Web: Refresh dashboard
- [ ] Web: See new verification
- [ ] Web: See ML Kit metrics
- [ ] Web: Approve verification
- [ ] Mobile: Check notification
- [ ] Mobile: See "Approved" status

### Data Validation
- [ ] ML Kit confidence is 0-100
- [ ] Face count is integer
- [ ] isValid is boolean
- [ ] Message is string
- [ ] Timestamp is valid
- [ ] User ID matches auth user

### Notification Testing
- [ ] Approval notification sent
- [ ] Rejection notification sent
- [ ] Notification contains correct info
- [ ] Seller receives notification

---

## Performance Testing

### Load Testing
- [ ] Dashboard loads with 10 verifications
- [ ] Dashboard loads with 100 verifications
- [ ] Dashboard loads with 1000 verifications
- [ ] Queries complete in <2 seconds
- [ ] No memory leaks

### Mobile Testing
- [ ] ML Kit analysis completes in <3 seconds
- [ ] Save to Firestore completes in <2 seconds
- [ ] No battery drain
- [ ] No excessive data usage

---

## Security Testing

### Authentication
- [ ] Unauthenticated users can't save verification
- [ ] Users can only access their own verification
- [ ] Admins can access all verifications
- [ ] Non-admins can't update verification status

### Authorization
- [ ] Users can't delete verifications
- [ ] Users can't modify verification status
- [ ] Admins can update verification status
- [ ] Admins can't delete verifications

### Data Validation
- [ ] Invalid status values rejected
- [ ] Invalid confidence values rejected
- [ ] Invalid face count values rejected
- [ ] Malformed data rejected

---

## Monitoring & Logging

### Firestore Monitoring
- [ ] Monitor read/write operations
- [ ] Check for errors in logs
- [ ] Verify data structure consistency
- [ ] Monitor query performance

### Mobile Monitoring
- [ ] Monitor crash reports
- [ ] Check error logs
- [ ] Monitor ML Kit performance
- [ ] Check Firebase integration

### Web Monitoring
- [ ] Monitor error logs
- [ ] Check performance metrics
- [ ] Monitor API calls
- [ ] Check user interactions

---

## Rollback Plan

### If Issues Found
- [ ] Identify issue
- [ ] Document issue
- [ ] Revert mobile app to previous version
- [ ] Revert web dashboard to previous version
- [ ] Revert Firestore rules to previous version
- [ ] Notify users of issue
- [ ] Fix issue
- [ ] Re-deploy

### Rollback Steps
1. Mobile: Revert to previous APK/AAB
2. Web: Revert to previous build
3. Firestore: Revert rules
4. Test rollback works
5. Verify data integrity

---

## Post-Deployment

### Monitoring (First 24 Hours)
- [ ] Monitor error rates
- [ ] Monitor performance metrics
- [ ] Check user feedback
- [ ] Monitor Firestore usage
- [ ] Check notification delivery

### Monitoring (First Week)
- [ ] Verify data quality
- [ ] Check admin workflow
- [ ] Monitor seller feedback
- [ ] Analyze verification metrics
- [ ] Check for edge cases

### Documentation
- [ ] Update deployment docs
- [ ] Document any issues found
- [ ] Update troubleshooting guide
- [ ] Create runbook for admins

---

## Sign-Off

### Development Team
- [ ] Code reviewed and approved
- [ ] Tests passed
- [ ] Ready for deployment

### QA Team
- [ ] All tests passed
- [ ] No critical issues
- [ ] Ready for production

### Product Team
- [ ] Feature meets requirements
- [ ] User experience acceptable
- [ ] Ready for launch

### DevOps Team
- [ ] Infrastructure ready
- [ ] Monitoring configured
- [ ] Rollback plan ready
- [ ] Ready for deployment

---

## Deployment Timeline

| Phase | Duration | Owner |
|-------|----------|-------|
| Code Review | 1-2 hours | Dev Lead |
| Testing | 2-4 hours | QA |
| Staging Deploy | 30 mins | DevOps |
| Staging Testing | 1-2 hours | QA |
| Production Deploy | 30 mins | DevOps |
| Post-Deploy Monitoring | 24 hours | DevOps + QA |

---

## Contact Information

### For Issues
- **Mobile Issues**: Android Team Lead
- **Web Issues**: Frontend Team Lead
- **Firestore Issues**: Backend Team Lead
- **Deployment Issues**: DevOps Lead

### Escalation
- **Critical Issues**: Engineering Manager
- **Production Down**: VP Engineering

---

## Notes

- Keep this checklist updated as you progress
- Document any issues found during testing
- Update troubleshooting guide with solutions
- Share learnings with team
- Plan for future enhancements

---

**Deployment Status**: ⏳ Ready for Deployment

**Last Updated**: 2024-03-25
**Prepared By**: [Your Name]
**Approved By**: [Manager Name]
