# Report System UI Implementation Guide

**Quick Reference for Adding Report Functionality to Screens**

---

## Step 1: Add Report Button to Your Screen

### Example: Product Details Screen

```kotlin
// In ProductDetailsScreen.kt
IconButton(onClick = { showReportDialog = true }) {
    Icon(
        imageVector = Icons.Default.Report,
        contentDescription = "Report Product",
        tint = Color.Red
    )
}
```

---

## Step 2: Create Report Dialog Composable

```kotlin
@Composable
fun ReportDialog(
    reportType: ReportType, // Pre-filled based on context
    reportedEntityId: String,
    reportedEntityName: String,
    currentUserRole: String, // "buyer" or "seller"
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit // reason, description
) {
    var reason by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report ${reportType.name}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Reason Dropdown
                var expanded by remember { mutableStateOf(false) }
                val reasons = when (reportType) {
                    ReportType.PRODUCT -> listOf(
                        "Fake/Misleading",
                        "Inappropriate Content",
                        "Copyright Violation",
                        "Other"
                    )
                    ReportType.SELLER -> listOf(
                        "Fraud",
                        "Rude Behavior",
                        "Not Responding",
                        "Other"
                    )
                    ReportType.BUYER -> listOf(
                        "Harassment",
                        "Fake Orders",
                        "Abusive Language",
                        "Other"
                    )
                    ReportType.TECHNICAL -> listOf(
                        "App Crash",
                        "Feature Not Working",
                        "Slow Performance",
                        "Other"
                    )
                }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reason") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        reasons.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    reason = item
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // Description TextField
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Please provide details...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )
                
                Text(
                    text = "Your report will be reviewed by our admin team.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isNotEmpty() && description.isNotEmpty()) {
                        onSubmit(reason, description)
                        onDismiss()
                    }
                },
                enabled = reason.isNotEmpty() && description.isNotEmpty()
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

---

## Step 3: Handle Report Submission in ViewModel

```kotlin
// In your ViewModel
class ProductViewModel(
    private val reportRepository: ReportRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    fun submitReport(
        reportType: ReportType,
        reportedEntityId: String,
        reportedEntityName: String,
        reason: String,
        description: String
    ) {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser() ?: return@launch
            val reporterRole = currentUser.role.name.lowercase()
            
            // Validate report type is allowed for this role
            if (!isReportTypeAllowed(reportType, reporterRole)) {
                _reportState.value = ReportState.Error("You cannot submit this type of report")
                return@launch
            }
            
            _reportState.value = ReportState.Loading
            
            val result = reportRepository.submitReport(
                reportType = reportType,
                reporterId = currentUser.id,
                reporterName = currentUser.name,
                reporterRole = reporterRole,
                reportedEntityId = reportedEntityId,
                reportedEntityName = reportedEntityName,
                reason = reason,
                description = description
            )
            
            _reportState.value = if (result.isSuccess) {
                ReportState.Success("Report submitted successfully")
            } else {
                ReportState.Error(result.exceptionOrNull()?.message ?: "Failed to submit report")
            }
        }
    }
}

sealed class ReportState {
    object Idle : ReportState()
    object Loading : ReportState()
    data class Success(val message: String) : ReportState()
    data class Error(val message: String) : ReportState()
}
```

---

## Step 4: Use in Screen

```kotlin
// In ProductDetailsScreen.kt
@Composable
fun ProductDetailsScreen(
    productId: String,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val reportState by viewModel.reportState.collectAsState()
    var showReportDialog by remember { mutableStateOf(false) }
    
    // ... other UI code ...
    
    // Report Button
    IconButton(onClick = { showReportDialog = true }) {
        Icon(
            imageVector = Icons.Default.Report,
            contentDescription = "Report Product",
            tint = Color.Red
        )
    }
    
    // Report Dialog
    if (showReportDialog) {
        ReportDialog(
            reportType = ReportType.PRODUCT,
            reportedEntityId = product.id,
            reportedEntityName = product.title,
            currentUserRole = currentUser?.role?.name?.lowercase() ?: "buyer",
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, description ->
                viewModel.submitReport(
                    reportType = ReportType.PRODUCT,
                    reportedEntityId = product.id,
                    reportedEntityName = product.title,
                    reason = reason,
                    description = description
                )
            }
        )
    }
    
    // Show success/error messages
    when (reportState) {
        is ReportState.Success -> {
            LaunchedEffect(Unit) {
                // Show success toast
                Toast.makeText(context, (reportState as ReportState.Success).message, Toast.LENGTH_SHORT).show()
                viewModel.resetReportState()
            }
        }
        is ReportState.Error -> {
            LaunchedEffect(Unit) {
                // Show error toast
                Toast.makeText(context, (reportState as ReportState.Error).message, Toast.LENGTH_SHORT).show()
                viewModel.resetReportState()
            }
        }
        else -> {}
    }
}
```

---

## Common Report Locations

### 1. Product Details Screen
- **Report Type**: `PRODUCT`
- **Reported Entity**: Product ID and title
- **Who Can Report**: Buyers only

### 2. Seller Profile Screen
- **Report Type**: `SELLER`
- **Reported Entity**: Seller ID and name
- **Who Can Report**: Buyers only

### 3. Chat Screen
- **Report Type**: `BUYER` or `SELLER` (depending on who is being reported)
- **Reported Entity**: User ID and name
- **Who Can Report**: Both buyers and sellers

### 4. Settings/Help Screen
- **Report Type**: `TECHNICAL`
- **Reported Entity**: "System" or specific feature
- **Who Can Report**: Anyone

---

## Role-Based UI Filtering

### Show/Hide Report Button Based on Role

```kotlin
// Only show report button if user can report this type
val currentUserRole = currentUser?.role?.name?.lowercase() ?: "buyer"
val canReport = isReportTypeAllowed(ReportType.PRODUCT, currentUserRole)

if (canReport) {
    IconButton(onClick = { showReportDialog = true }) {
        Icon(Icons.Default.Report, "Report")
    }
}
```

### Filter Report Types in Dropdown

```kotlin
// If you have a generic report dialog with type selection
val allowedTypes = getAllowedReportTypes(currentUserRole)

ExposedDropdownMenu(...) {
    allowedTypes.forEach { type ->
        DropdownMenuItem(
            text = { Text(type.name) },
            onClick = { selectedType = type }
        )
    }
}
```

---

## Testing Checklist

### As Buyer
- [ ] Can report products from product details screen
- [ ] Can report sellers from seller profile screen
- [ ] Can report technical issues from settings
- [ ] Cannot see option to report buyers

### As Seller
- [ ] Can report buyers from chat screen
- [ ] Can report technical issues from settings
- [ ] Cannot see option to report products
- [ ] Cannot see option to report sellers

---

## Quick Copy-Paste Snippets

### Report Button
```kotlin
IconButton(onClick = { showReportDialog = true }) {
    Icon(Icons.Default.Report, "Report", tint = Color.Red)
}
```

### Report State Variable
```kotlin
var showReportDialog by remember { mutableStateOf(false) }
```

### Report Submission
```kotlin
viewModel.submitReport(
    reportType = ReportType.PRODUCT,
    reportedEntityId = entityId,
    reportedEntityName = entityName,
    reason = reason,
    description = description
)
```

---

**Status**: Ready to implement in any screen that needs reporting functionality!
