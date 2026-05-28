# PROFESSIONAL ALERT COMPONENTS - QUICK REFERENCE

## How to Use in Your Code

### Import the Components
```kotlin
import com.gcuf.craftoria.ui.components.SuccessAlert
import com.gcuf.craftoria.ui.components.InfoAlert
import com.gcuf.craftoria.ui.components.WarningAlert
```

### Basic Usage

#### ✅ Success Alert (Green)
```kotlin
SuccessAlert(message = "Your changes have been saved successfully!")
```

#### ℹ️ Info Alert (Blue)
```kotlin
InfoAlert(message = "This is an informational message for the user.")
```

#### ⚠️ Warning Alert (Yellow)
```kotlin
WarningAlert(message = "Please review this warning before proceeding.")
```

### Advanced Usage with Dismiss Button

```kotlin
var showAlert by remember { mutableStateOf(true) }

if (showAlert) {
    SuccessAlert(
        message = "Operation completed!",
        onDismiss = { showAlert = false }
    )
}
```

### Using in a Column (Stacked Layout)

```kotlin
Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SuccessAlert(message = "✓ Profile updated")
    Spacer(modifier = Modifier.height(12.dp))
    InfoAlert(message = "Changes will sync across all devices")
}
```

### Conditional Display

```kotlin
when (authState) {
    is AuthState.Success -> {
        SuccessAlert(authState.message)
        Spacer(modifier = Modifier.height(12.dp))
    }
    is AuthState.Error -> {
        WarningAlert(authState.message)
        Spacer(modifier = Modifier.height(12.dp))
    }
    else -> {}
}
```

---

## Component API Reference

### SuccessAlert
```kotlin
@Composable
fun SuccessAlert(
    message: String,                           // Required: Alert message text
    onDismiss: (() -> Unit)? = null,          // Optional: Dismiss callback
    modifier: Modifier = Modifier,             // Optional: Custom modifier
    icon: ImageVector = Icons.Default.CheckCircle  // Optional: Custom icon
)
```

### InfoAlert
```kotlin
@Composable
fun InfoAlert(
    message: String,                           // Required: Alert message text
    onDismiss: (() -> Unit)? = null,          // Optional: Dismiss callback
    modifier: Modifier = Modifier              // Optional: Custom modifier
)
```

### WarningAlert
```kotlin
@Composable
fun WarningAlert(
    message: String,                           // Required: Alert message text
    onDismiss: (() -> Unit)? = null,          // Optional: Dismiss callback
    modifier: Modifier = Modifier              // Optional: Custom modifier
)
```

---

## Visual Design Specifications

### Dimensions
- **Width**: Full width (fillMaxWidth)
- **Corner Radius**: 12.dp
- **Padding**: 16.dp (internal)
- **Icon Size**: 20.dp
- **Spacing**: 12.dp (between elements)

### Typography
- **Font Size**: 14.sp
- **Font Weight**: Medium
- **Line Height**: 18.sp (explicit)

### Colors

#### Success (Green)
```kotlin
Background: Color(0xFFF0F9FF)
Border: Color(0xFFC6F6D5)
Text/Icon: Color(0xFF22863A)
```

#### Info (Blue)
```kotlin
Background: Color(0xFFF0F9FF)
Border: Color(0xFFBDE4FF)
Text/Icon: Color(0xFF0969DA)
```

#### Warning (Yellow)
```kotlin
Background: Color(0xFFFFF8C5)
Border: Color(0xFFEAE5D9)
Text/Icon: Color(0xFF9A6700)
```

---

## Real-World Examples

### Example 1: Password Reset Success
```kotlin
SuccessAlert(
    message = "✓ Password reset link sent to your email.\n\n" +
            "Check your inbox (and spam folder) for the reset link.",
    onDismiss = { showAlert = false }
)
```

### Example 2: Form Validation Error
```kotlin
WarningAlert(message = "Please fill in all required fields before proceeding.")
```

### Example 3: System Information
```kotlin
InfoAlert(message = "Payment processing is in test mode. No charges will be made.")
```

### Example 4: Multiple Alerts Stack
```kotlin
Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    if (profileUpdated) {
        SuccessAlert("Profile photo updated successfully")
    }
    if (showWarning) {
        WarningAlert("Your verification status is pending")
    }
    if (showInfo) {
        InfoAlert("Learn more about seller verification in our help center")
    }
}
```

---

## When to Use Each Alert Type

### ✅ SuccessAlert
- Order confirmed/placed successfully
- Profile updated successfully
- File upload completed
- Password changed
- Account created
- Payment processed
- Any successful operation completion

### ℹ️ InfoAlert
- General information messages
- Feature explanations
- System status (test mode, etc.)
- Tips and helpful hints
- Important announcements
- Non-critical notifications

### ⚠️ WarningAlert
- Validation errors
- Permission warnings
- Confirmation needed
- Temporary issues
- Deprecated features
- Any action that needs user attention

---

## Best Practices

✅ **DO:**
- Keep messages concise and actionable
- Use clear, user-friendly language
- Provide a way to dismiss if important info
- Use consistent styling across similar messages
- Include emoji or icons for quick recognition

❌ **DON'T:**
- Make alerts too long (break into multiple if needed)
- Use CAPS LOCK or excessive punctuation
- Bury important info in long paragraphs
- Show multiple alerts simultaneously without spacing
- Force the user to interact with non-critical alerts

---

## Component Location
📁 File: `app/src/main/java/com/gcuf/craftoria/ui/components/CraftoriaTextField.kt`

Lines:
- SuccessAlert: ~140-190
- InfoAlert: ~200-245
- WarningAlert: ~255-300

---

## Screens Using These Components

✅ **LoginScreen** - Auth messages (success/error)
✅ **CheckoutScreen** - Payment info message

**Future Integration Opportunities:**
- Add to profile screen for profile updates
- Use in refund flows
- Add to product management screens
- Use for order confirmations

---

## Common Patterns

### Pattern 1: LaunchedEffect with State Management
```kotlin
var showSuccessAlert by remember { mutableStateOf(false) }

LaunchedEffect(someState) {
    when (someState) {
        is Success -> showSuccessAlert = true
        is Error -> showSuccessAlert = false
    }
}

if (showSuccessAlert) {
    SuccessAlert(
        message = "Operation completed!",
        onDismiss = { showSuccessAlert = false }
    )
}
```

### Pattern 2: Inline with Auth State
```kotlin
when (val state = authState) {
    is AuthState.Success -> {
        SuccessAlert(state.message)
        Spacer(modifier = Modifier.height(12.dp))
    }
    is AuthState.Error -> {
        WarningAlert(state.message)
        Spacer(modifier = Modifier.height(12.dp))
    }
    else -> {}
}
```

### Pattern 3: Multi-line Messages
```kotlin
SuccessAlert(
    message = buildString {
        append("✓ Your order has been confirmed!\n\n")
        append("Order ID: #12345\n")
        append("Expected Delivery: 3-5 business days\n\n")
        append("Track your order from My Orders")
    }
)
```

---

## Troubleshooting

**Q: Alert not showing?**
A: Verify state management and LaunchedEffect is properly configured. Check if visibility state is correct.

**Q: Text too small/large?**
A: Font size is fixed at 14sp for consistency. Adjust padding or container size instead.

**Q: Want different colors?**
A: Create a new custom alert component following the existing pattern. Don't modify existing ones to maintain consistency.

**Q: Need custom icon?**
A: SuccessAlert accepts custom icon parameter: `icon: Icons.Default.SomeIcon`

---

## Summary

These professional alert components provide:
- ✅ Modern, professional appearance
- ✅ Consistent styling across the app
- ✅ Easy to use and customize
- ✅ Accessible color combinations
- ✅ Flexible for different message types
- ✅ Optional dismiss functionality
- ✅ Full-width responsive design

Use them to replace any basic alert boxes or toast messages for a more polished user experience!
