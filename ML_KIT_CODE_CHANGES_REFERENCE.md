# ML Kit Integration - Code Changes Reference

## Summary of Changes

### 1. Mobile ViewModel - Save ML Kit Results to Firestore

**File:** `app/src/main/java/com/gcuf/craftoria/viewmodel/SellerVerificationViewModel.kt`

**Added Imports:**
```kotlin
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
```

**Added Properties:**
```kotlin
private val firestore = FirebaseFirestore.getInstance()
private val auth = FirebaseAuth.getInstance()
```

**New Method:**
```kotlin
/**
 * Save ML Kit verification result to Firestore
 */
suspend fun saveVerificationResultToFirestore(
    imageUrl: String,
    result: FaceVerificationResult
): Boolean {
    return try {
        val userId = auth.currentUser?.uid ?: return false
        
        val verificationData = mapOf(
            "userId" to userId,
            "imageUrl" to imageUrl,
            "verificationStatus" to "pending",
            "timestamp" to Timestamp.now(),
            "mlKitResult" to mapOf(
                "isValid" to result.isValid,
                "confidence" to result.confidence,
                "faceCount" to result.faceCount,
                "message" to result.message
            ),
            "submittedAt" to Timestamp.now(),
            "submittedBy" to (auth.currentUser?.email ?: "unknown")
        )
        
        firestore.collection("seller_verifications")
            .document(userId)
            .set(verificationData)
            .await()
        
        true
    } catch (e: Exception) {
        false
    }
}
```

---

### 2. Mobile Screen - Call Save Method on Submit

**File:** `app/src/main/java/com/gcuf/craftoria/ui/screens/auth/SellerVerificationScreen.kt`

**Added Lambda:**
```kotlin
// Handle verification submission with ML Kit results
val handleSubmitVerification: suspend (Uri) -> Unit = { uri ->
    mlKitVerificationResult?.let { result ->
        // Save ML Kit result to Firestore before submitting
        val saved = mlKitViewModel.saveVerificationResultToFirestore(
            imageUrl = uri.toString(),
            result = result
        )
        if (saved) {
            verificationState = VerificationStatus.PENDING
            onSubmitVerification(uri)
        }
    }
}
```

**Usage:**
```kotlin
Button(
    onClick = {
        imageUri?.let { uri ->
            verificationState = VerificationStatus.PENDING
            onSubmitVerification(uri)
        }
    },
    // ... button styling
) {
    Text("Submit Verification")
}
```

---

### 3. Web Dashboard - Display ML Kit Data

**File:** `src/pages/SellerVerificationDashboard.jsx`

**Added Imports:**
```javascript
import {
  LinearProgress,
  Verified as VerifiedIcon,
} from '@mui/material';
```

**Removed Unused Imports:**
```javascript
// Removed: Tabs, Tab (not used)
```

**Added Helper Function:**
```javascript
// Helper function to get ML Kit confidence color
const getConfidenceColor = (confidence) => {
  if (!confidence) return '#9E9E9E';
  if (confidence >= 80) return '#4CAF50'; // Green
  if (confidence >= 60) return '#FF9800'; // Orange
  return '#F44336'; // Red
};
```

**Enhanced Data Fetching:**
```javascript
// Ensure ML Kit result is properly structured
mlKitResult: verification.mlKitResult || {
  confidence: 0,
  faceCount: 0,
  isValid: false,
  message: 'No ML Kit data available',
},
```

**Enhanced Verification Card Display:**
```javascript
{/* ML Kit Confidence */}
<Box>
  <Typography sx={{ fontSize: '0.75rem', color: '#8b919e', mb: '4px' }}>
    ML Kit Confidence
  </Typography>
  <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
    <Typography sx={{ fontWeight: 600, color: '#1a1d23' }}>
      {verification.mlKitResult?.confidence
        ? `${verification.mlKitResult.confidence.toFixed(1)}%`
        : 'N/A'}
    </Typography>
    {verification.mlKitResult?.confidence && (
      <LinearProgress
        variant="determinate"
        value={verification.mlKitResult.confidence}
        sx={{
          width: '60px',
          height: '6px',
          borderRadius: '3px',
          backgroundColor: '#e0e0e0',
          '& .MuiLinearProgress-bar': {
            backgroundColor:
              verification.mlKitResult.confidence >= 80
                ? '#4CAF50'
                : verification.mlKitResult.confidence >= 60
                ? '#FF9800'
                : '#F44336',
          },
        }}
      />
    )}
  </Box>
</Box>

{/* Face Count */}
<Box>
  <Typography sx={{ fontSize: '0.75rem', color: '#8b919e', mb: '4px' }}>
    Faces Detected
  </Typography>
  <Typography sx={{ fontWeight: 600, color: '#1a1d23' }}>
    {verification.mlKitResult?.faceCount || 0}
  </Typography>
</Box>

{/* ML Kit Status */}
{verification.mlKitResult?.isValid && (
  <Box>
    <Typography sx={{ fontSize: '0.75rem', color: '#8b919e', mb: '4px' }}>
      ML Kit Status
    </Typography>
    <Box sx={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
      <VerifiedIcon sx={{ fontSize: '16px', color: '#4CAF50' }} />
      <Typography sx={{ fontWeight: 600, color: '#4CAF50', fontSize: '0.85rem' }}>
        Valid
      </Typography>
    </Box>
  </Box>
)}
```

**Improved Approve Dialog:**
```javascript
<Dialog open={approveDialog} onClose={() => setApproveDialog(false)} maxWidth="sm" fullWidth>
  <DialogTitle>Approve Verification</DialogTitle>
  <DialogContent>
    {selectedVerification?.mlKitResult && (
      <Alert severity="info" sx={{ mb: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <VerifiedIcon sx={{ fontSize: '18px' }} />
          <Box>
            <Typography sx={{ fontWeight: 600, fontSize: '0.9rem' }}>
              ML Kit Confidence: {selectedVerification.mlKitResult.confidence?.toFixed(1)}%
            </Typography>
            <Typography sx={{ fontSize: '0.8rem', mt: '2px' }}>
              Faces detected: {selectedVerification.mlKitResult.faceCount}
            </Typography>
          </Box>
        </Box>
      </Alert>
    )}
    {/* ... rest of dialog */}
  </DialogContent>
</Dialog>
```

**Removed Unused State:**
```javascript
// Removed: const [activeTab, setActiveTab] = useState(0);
// Removed: setActiveTab from filter useEffect dependencies
```

---

## Data Flow Example

### Mobile App Saves Data
```kotlin
// User takes selfie, ML Kit analyzes
val result = FaceVerificationResult(
    isValid = true,
    confidence = 92.5f,
    message = "Face verified successfully!",
    faceCount = 1
)

// Save to Firestore
mlKitViewModel.saveVerificationResultToFirestore(
    imageUrl = "gs://bucket/image.jpg",
    result = result
)

// Firestore document created:
// seller_verifications/user123 = {
//   userId: "user123",
//   imageUrl: "gs://bucket/image.jpg",
//   verificationStatus: "pending",
//   mlKitResult: {
//     isValid: true,
//     confidence: 92.5,
//     faceCount: 1,
//     message: "Face verified successfully!"
//   },
//   timestamp: Timestamp,
//   submittedAt: Timestamp,
//   submittedBy: "user@example.com"
// }
```

### Web Dashboard Displays Data
```javascript
// Dashboard fetches and displays
{
  id: "user123",
  userName: "Sarah Ahmed",
  userEmail: "sarah@example.com",
  verificationStatus: "pending",
  mlKitResult: {
    isValid: true,
    confidence: 92.5,
    faceCount: 1,
    message: "Face verified successfully!"
  },
  // ... other fields
}

// Renders as:
// ML Kit Confidence: 92.5% [████████░] (green progress bar)
// Faces Detected: 1
// ML Kit Status: ✓ Valid
```

---

## Testing Checklist

- [ ] Mobile app compiles without errors
- [ ] Web dashboard compiles without errors
- [ ] Mobile app saves ML Kit data to Firestore
- [ ] Web dashboard displays confidence with progress bar
- [ ] Color coding works (green/orange/red)
- [ ] Face count displays correctly
- [ ] Approve dialog shows ML Kit metrics
- [ ] Reject dialog works as before
- [ ] Admin can approve/reject with ML Kit data visible
- [ ] Notifications sent after approval/rejection

---

## Deployment Order

1. Deploy mobile app first (SellerVerificationViewModel + SellerVerificationScreen)
2. Deploy web dashboard (SellerVerificationDashboard)
3. Test with new seller verification submissions
4. Monitor Firestore for proper data structure
5. Verify admin workflow works end-to-end
