# LoginScreen.kt - Google Sign-In Update

## Updated Google Sign-In Handler

Replace your existing Google sign-in callback with this:

```kotlin
// In your LoginScreen composable or ViewModel

fun handleGoogleSignIn(idToken: String, navController: NavController) {
    authViewModel.signInWithGoogle(idToken) { isNewUser ->
        if (isNewUser) {
            // ✅ NEW USER: Navigate to role selection
            Log.d("LoginScreen", "🆕 New user detected, showing role selection")
            navController.navigate("roleSelection") {
                popUpTo("login") { inclusive = true }
            }
        } else {
            // ✅ EXISTING USER: Navigate based on role
            val currentUser = authViewModel.currentUser.value
            Log.d("LoginScreen", "👤 Existing user: ${currentUser?.name}, role: ${currentUser?.role}")
            
            when (currentUser?.role) {
                UserRole.BUYER -> {
                    navController.navigate("buyerHome") {
                        popUpTo("login") { inclusive = true }
                    }
                }
                UserRole.SELLER -> {
                    // Check if seller is verified
                    if (currentUser.verified) {
                        navController.navigate("sellerDashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        // Seller not verified yet
                        navController.navigate("sellerVerification") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
                else -> {
                    // Fallback to buyer home
                    navController.navigate("buyerHome") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
        }
    }
}
```

## Full LoginScreen Example

```kotlin
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    navController: NavController
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val authState by authViewModel.authState.collectAsState()
    
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                // Handle success
                isLoading = false
            }
            is AuthState.Error -> {
                errorMessage = (authState as AuthState.Error).message
                isLoading = false
            }
            is AuthState.Loading -> {
                isLoading = true
            }
            else -> {}
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Email/Password Login
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        
        Button(
            onClick = {
                authViewModel.signIn(email, password)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Sign In")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Google Sign-In Button
        Button(
            onClick = {
                // Trigger Google Sign-In
                // This should call your Google Sign-In library
                // and pass the idToken to handleGoogleSignIn
                triggerGoogleSignIn { idToken ->
                    handleGoogleSignIn(idToken, navController)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Continue with Google")
        }
        
        // Error Message
        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// Helper function to handle Google Sign-In
private fun handleGoogleSignIn(idToken: String, navController: NavController, authViewModel: AuthViewModel) {
    authViewModel.signInWithGoogle(idToken) { isNewUser ->
        if (isNewUser) {
            // ✅ NEW USER: Navigate to role selection
            Log.d("LoginScreen", "🆕 New user detected, showing role selection")
            navController.navigate("roleSelection") {
                popUpTo("login") { inclusive = true }
            }
        } else {
            // ✅ EXISTING USER: Navigate based on role
            val currentUser = authViewModel.currentUser.value
            Log.d("LoginScreen", "👤 Existing user: ${currentUser?.name}, role: ${currentUser?.role}")
            
            when (currentUser?.role) {
                UserRole.BUYER -> {
                    navController.navigate("buyerHome") {
                        popUpTo("login") { inclusive = true }
                    }
                }
                UserRole.SELLER -> {
                    if (currentUser.verified) {
                        navController.navigate("sellerDashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("sellerVerification") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
                else -> {
                    navController.navigate("buyerHome") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
        }
    }
}
```

## Navigation Graph Update

Add this to your NavGraph:

```kotlin
composable("roleSelection") {
    RoleSelectionScreen(
        authViewModel = authViewModel,
        navController = navController,
        userId = authViewModel.currentUser.value?.id ?: ""
    )
}
```

## Key Points

1. **isNewUser Callback**: The `onNewUser` lambda is called with `true` for new users, `false` for existing
2. **Role-Based Navigation**:
   - New Buyer → Buyer Home
   - New Seller → Role Selection Screen → Seller Verification
   - Existing Buyer → Buyer Home
   - Existing Seller (verified) → Seller Dashboard
   - Existing Seller (not verified) → Seller Verification
3. **Pop Up To**: Use `popUpTo("login") { inclusive = true }` to remove login screen from back stack
4. **Error Handling**: Errors are handled by AuthViewModel and reflected in authState

## Testing

```kotlin
// Test 1: New Google user
// Expected: isNewUser = true, navigate to roleSelection

// Test 2: Existing Google user (Buyer)
// Expected: isNewUser = false, navigate to buyerHome

// Test 3: Existing Google user (Seller, verified)
// Expected: isNewUser = false, navigate to sellerDashboard

// Test 4: Existing Google user (Seller, not verified)
// Expected: isNewUser = false, navigate to sellerVerification
```
