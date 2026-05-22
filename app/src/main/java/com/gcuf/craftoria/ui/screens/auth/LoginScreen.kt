package com.gcuf.craftoria.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import com.gcuf.craftoria.data.model.VerificationStatus
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.gcuf.craftoria.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.ui.components.*
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.AuthState
import com.gcuf.craftoria.viewmodel.AuthViewModel
import com.gcuf.craftoria.data.model.UserRole
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToVerification: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToRoleSelection: (userId: String, userName: String) -> Unit,
    onNavigateToLoginTab: () -> Unit,
    onNavigateToSignUpTab: () -> Unit,
    viewModel: AuthViewModel? = null
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val vm = if (isPreview) null else viewModel ?: viewModel()

    var selectedTab by remember { mutableIntStateOf(0) }
    val authState = vm?.authState?.collectAsState()?.value ?: AuthState.Idle
    var isNewUser by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                vm?.signInWithGoogle(idToken) { newUser ->
                    isNewUser = newUser
                }
            }
        } catch (e: ApiException) {
            vm?.setAuthError("Google Sign-In failed: ${e.message}")
        }
    }

    if (!isPreview && vm != null) {
        LaunchedEffect(authState) {
            when (authState) {
                is AuthState.Success -> {
                    val user = vm.currentUser.value
                    if (user != null) {
                        vm.resetAuthState()
                        
                        // ✅ NEW: If this is a new Google user, show role selection
                        if (isNewUser) {
                            isNewUser = false  // Reset flag
                            onNavigateToRoleSelection(user.id, user.name)
                        } else if (user.role == UserRole.SELLER &&
                            user.verificationStatus != VerificationStatus.APPROVED
                        ) {
                            onNavigateToVerification()
                        } else {
                            onNavigateToHome()
                        }
                    }
                }
                else -> {}
            }
        }
        DisposableEffect(Unit) { onDispose { vm.resetAuthState() } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSecondary)
    ) {
        // ── Gradient brand header ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)))
                .padding(top = 36.dp, bottom = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    // Outer glow ring
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    )
                    // White circle border
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .background(Color.White.copy(alpha = 0.25f), CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.handmade1_logo),
                            contentDescription = "Craftoria Logo",
                            modifier = Modifier.size(72.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Craftoria",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Empowering Women through Handicrafts",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.80f),
                    letterSpacing = 0.3.sp
                )
            }
        }

        // ── Tab row ───────────────────────────────────────────────────────────
        Surface(color = Color.White, shadowElevation = 0.dp) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Primary,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .height(2.5.dp)
                                .background(Primary, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        )
                    }
                },
                divider = { HorizontalDivider(color = BorderColor, thickness = 0.5.dp) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; onNavigateToSignUpTab() },
                    text = {
                        Text(
                            text = "Sign Up",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) Primary else TextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; onNavigateToLoginTab() },
                    text = {
                        Text(
                            text = "Login",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) Primary else TextSecondary
                        )
                    }
                )
            }
        }

        // ── Form body ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            when (selectedTab) {
                0 -> SignUpForm(
                    viewModel = vm,
                    authState = authState,
                    switchToLogin = { selectedTab = 1; onNavigateToLoginTab() }
                )
                1 -> LoginForm(
                    viewModel = vm,
                    authState = authState,
                    onGoogleSignIn = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken("303478520606-cs6fu2kbpa8vv15msgsgvnjqk95qlf3k.apps.googleusercontent.com")
                            .requestEmail().build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    },
                    switchToSignUp = { selectedTab = 0; onNavigateToSignUpTab() }
                )
            }
        }
    }
}

// ── Sign Up Form ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpForm(
    viewModel: AuthViewModel?,
    authState: AuthState,
    switchToLogin: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("") }
    var agreeToTerms by remember { mutableStateOf(false) }
    var showTermsSheet by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {

        when (authState) {
            is AuthState.Success -> MessageCard(message = authState.message, type = UIMessageType.SUCCESS)
            is AuthState.Error -> MessageCard(message = authState.message, type = UIMessageType.ERROR)
            else -> {}
        }

        Text(
            text = "Create Account",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 18.dp)
        )

        CraftoriaTextField(value = fullName, onValueChange = { fullName = it }, label = "Full Name", required = true, placeholder = "Enter your full name", leadingIconVector = Icons.Default.Person)
        Spacer(modifier = Modifier.height(14.dp))
        CraftoriaTextField(value = email, onValueChange = { email = it }, label = "Email Address", required = true, placeholder = "Enter your email", keyboardType = KeyboardType.Email, leadingIconVector = Icons.Default.Email)
        Spacer(modifier = Modifier.height(14.dp))
        CraftoriaTextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = "Phone Number", required = true, placeholder = "+92 300 1234567", keyboardType = KeyboardType.Phone, leadingIconVector = Icons.Default.Phone)
        Spacer(modifier = Modifier.height(14.dp))

        Column {
            CraftoriaTextField(value = password, onValueChange = { password = it }, label = "Password", required = true, isPassword = true, placeholder = "Create a password", leadingIconVector = Icons.Default.Lock)
            Text(
                text = "Min. 8 characters, letters & numbers",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 5.dp, start = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        CraftoriaTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm Password", required = true, isPassword = true, placeholder = "Re-enter your password", leadingIconVector = Icons.Default.Lock)
        Spacer(modifier = Modifier.height(14.dp))

        // Role dropdown
        Column {
            Text(
                text = buildAnnotatedString {
                    append("Select Role ")
                    withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) { append("*") }
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = selectedRole,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Choose your role", fontSize = 13.sp, color = TextSecondary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = BorderColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = Color.White
                ) {
                    DropdownMenuItem(
                        text = { Text("Buyer", fontSize = 13.sp) },
                        onClick = { selectedRole = "Buyer"; expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Seller", fontSize = 13.sp) },
                        onClick = { selectedRole = "Seller"; expanded = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Terms checkbox
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = agreeToTerms,
                onCheckedChange = { agreeToTerms = it },
                colors = CheckboxDefaults.colors(checkedColor = Primary)
            )
            Text(
                text = buildAnnotatedString {
                    append("I agree to the ")
                    withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) { append("Terms & Conditions") }
                },
                fontSize = 13.sp,
                color = TextPrimary,
                modifier = Modifier.clickable { showTermsSheet = true }
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        CraftoriaButton(
            text = "Create Account",
            onClick = {
                if (password == confirmPassword && agreeToTerms) {
                    val role = if (selectedRole == "Seller") UserRole.SELLER else UserRole.BUYER
                    viewModel?.signUp(email = email, password = password, name = fullName, phone = phoneNumber, role = role)
                }
            },
            enabled = fullName.isNotBlank() && email.isNotBlank() && phoneNumber.isNotBlank() &&
                    password.isNotBlank() && confirmPassword.isNotBlank() && selectedRole.isNotBlank() && agreeToTerms,
            isLoading = authState is AuthState.Loading
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = buildAnnotatedString {
                append("Already have an account? ")
                withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) { append("Log in") }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { switchToLogin() },
            textAlign = TextAlign.Center,
            fontSize = 13.sp
        )
    }

    if (showTermsSheet) {
        TermsBottomSheet(onDismiss = { showTermsSheet = false })
    }
}

// ── Terms Bottom Sheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
        ) {
            // Header band
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Primary.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📋", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Terms & Conditions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Last updated: January 30, 2026",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            TermsSheetSection("1. About Craftoria", "Craftoria is a digital marketplace exclusively for women handicraft entrepreneurs, developed as a Final Year Project at GCUF Faisalabad.")
            TermsSheetSection("2. Women-Only Seller Policy", "Only verified women artisans may list and sell products. Buyers of any gender may purchase. Any attempt to bypass this policy results in account termination.")
            TermsSheetSection("3. Seller Verification", "Sellers must submit a live selfie via Google ML Kit Face Detection. Our admin team reviews it within 24–48 hours to confirm identity and gender.")
            TermsSheetSection("4. Buyer Responsibilities", "Provide accurate delivery info and report issues within 3 days of delivery. Fraudulent orders may result in account suspension.")
            TermsSheetSection("5. Seller Responsibilities", "Maintain accurate product listings with honest descriptions. Fulfill orders on time. Commission fees of 5% or less apply per transaction.")
            TermsSheetSection("6. Smart Negotiation Bot", "Offers above seller's threshold are auto-accepted. Lower offers go to the seller for manual review.")
            TermsSheetSection("7. Payments", "Cash on Delivery (COD) is currently supported. All prices are in PKR.")
            TermsSheetSection("8. Returns & Refunds", "Request a return within 3 days of delivery for damaged or incorrect items. Approved refunds are processed within 5–7 business days.")
            TermsSheetSection("9. Prohibited Activities", "Do not sell counterfeit goods, post fake reviews, harass users, or attempt to bypass seller verification.")
            TermsSheetSection("10. Governing Law", "These terms are governed by the laws of Pakistan. Disputes are resolved in courts of Faisalabad, Punjab.")

            Spacer(modifier = Modifier.height(16.dp))

            // Gradient "I Understand" button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "I Understand",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TermsSheetSection(title: String, body: String) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(body, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
    }
}

// ── Login Form ────────────────────────────────────────────────────────────────

@Composable
fun LoginForm(
    viewModel: AuthViewModel?,
    authState: AuthState,
    onGoogleSignIn: () -> Unit,
    switchToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {

        when (authState) {
            is AuthState.Success -> {
                MessageCard(authState.message, UIMessageType.SUCCESS)
                Spacer(modifier = Modifier.height(12.dp))
            }
            is AuthState.Error -> {
                MessageCard(authState.message, UIMessageType.ERROR)
                Spacer(modifier = Modifier.height(12.dp))
            }
            else -> {}
        }

        Text(
            text = "Welcome Back!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "Sign in to continue to Craftoria",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        CraftoriaTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email Address",
            required = false,
            keyboardType = KeyboardType.Email,
            placeholder = "Enter your email",
            leadingIconVector = Icons.Default.Email
        )

        Spacer(modifier = Modifier.height(14.dp))

        Column {
            CraftoriaTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                required = false,
                isPassword = true,
                placeholder = "Enter your password",
                leadingIconVector = Icons.Default.Lock
            )
            Text(
                text = "Forgot Password?",
                fontSize = 12.sp,
                color = Primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp, end = 2.dp)
                    .clickable { showForgotPasswordDialog = true },
                textAlign = TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        CraftoriaButton(
            text = "Login",
            onClick = { viewModel?.login(email = email, password = password) },
            enabled = email.isNotBlank() && password.isNotBlank(),
            isLoading = authState is AuthState.Loading
        )

        Spacer(modifier = Modifier.height(18.dp))

        // OR divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor, thickness = 0.5.dp)
            Text(
                text = "OR",
                modifier = Modifier.padding(horizontal = 12.dp),
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor, thickness = 0.5.dp)
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Google Sign In
        OutlinedButton(
            onClick = onGoogleSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = BorderStroke(0.5.dp, BorderColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Google",
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Sign in with Google",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = buildAnnotatedString {
                append("Don't have an account? ")
                withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) { append("Sign up here") }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { switchToSignUp() },
            textAlign = TextAlign.Center,
            fontSize = 13.sp
        )
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(viewModel = viewModel, onDismiss = { showForgotPasswordDialog = false })
    }
}

// ── Forgot Password Dialog (3-step OTP flow) ─────────────────────────────────

@Composable
fun ForgotPasswordDialog(viewModel: AuthViewModel?, onDismiss: () -> Unit) {
    // Step 0 = enter email, Step 1 = enter OTP, Step 2 = enter new password, Step 3 = success
    var step by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resendCountdown by remember { mutableIntStateOf(0) }

    // Countdown timer for resend OTP
    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            kotlinx.coroutines.delay(1000L)
            resendCountdown--
        }
    }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Gradient header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)))
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            text = when (step) {
                                0 -> "Reset Password"
                                1 -> "Enter OTP"
                                else -> "Password Reset Complete"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = when (step) {
                                0 -> "We'll send a 6-digit OTP to your email"
                                1 -> "OTP sent to $email"
                                else -> "Check your email to complete reset"
                            },
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.80f)
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    errorMessage?.let { MessageCard(message = it, type = UIMessageType.ERROR) }

                    when (step) {
                        // ── Step 0: Email input ───────────────────────────────
                        0 -> {
                            CraftoriaTextField(
                                value = email,
                                onValueChange = { email = it; errorMessage = null },
                                label = "Email Address",
                                required = true,
                                placeholder = "Enter your registered email",
                                keyboardType = KeyboardType.Email,
                                leadingIconVector = Icons.Default.Email
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    border = BorderStroke(0.5.dp, BorderColor),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                                ) {
                                    Text("Cancel", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        if (email.isNotBlank()) {
                                            isLoading = true
                                            errorMessage = null
                                            viewModel?.sendPasswordResetOtp(email.trim()) { success, error ->
                                                isLoading = false
                                                if (success) {
                                                    step = 1
                                                    resendCountdown = 60 // Start 60s countdown
                                                } else {
                                                    errorMessage = error
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    enabled = email.isNotBlank() && !isLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                if (email.isNotBlank() && !isLoading)
                                                    Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                                                else Brush.horizontalGradient(listOf(TextLight, TextLight)),
                                                RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                        else Text("Send OTP", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    }
                                }
                            }
                        }

                        // ── Step 1: OTP input ─────────────────────────────────
                        1 -> {
                            CraftoriaTextField(
                                value = otp,
                                onValueChange = { if (it.length <= 6) { otp = it; errorMessage = null } },
                                label = "6-Digit OTP",
                                required = true,
                                placeholder = "Enter the OTP from your email",
                                keyboardType = KeyboardType.Number,
                                leadingIconVector = Icons.Default.Lock
                            )

                            // Resend OTP link with countdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Didn't receive it? ",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                if (resendCountdown > 0) {
                                    Text(
                                        text = "Resend in ${resendCountdown}s",
                                        fontSize = 12.sp,
                                        color = TextLight,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Text(
                                        text = "Resend OTP",
                                        fontSize = 12.sp,
                                        color = Primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clickable(enabled = !isLoading) {
                                                isLoading = true
                                                errorMessage = null
                                                otp = ""
                                                viewModel?.sendPasswordResetOtp(email.trim()) { success, error ->
                                                    isLoading = false
                                                    if (success) {
                                                        resendCountdown = 60 // Restart countdown
                                                        errorMessage = "✓ OTP resent successfully"
                                                    } else {
                                                        errorMessage = error
                                                    }
                                                }
                                            }
                                            .padding(4.dp),
                                        textDecoration = TextDecoration.Underline
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        step = 0
                                        otp = ""
                                        errorMessage = null
                                        resendCountdown = 0
                                    },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    border = BorderStroke(0.5.dp, BorderColor),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                                ) {
                                    Text("Back", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        if (otp.length == 6) {
                                            isLoading = true
                                            errorMessage = null
                                            viewModel?.verifyOtpOnly(email.trim(), otp) { success, error ->
                                                isLoading = false
                                                if (success) {
                                                    // OTP verified — now send Firebase reset email
                                                    viewModel.sendFirebaseResetEmail(email.trim()) { sent, sendError ->
                                                        isLoading = false
                                                        if (sent) step = 3
                                                        else errorMessage = sendError
                                                    }
                                                } else {
                                                    errorMessage = error
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    enabled = otp.length == 6 && !isLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                if (otp.length == 6 && !isLoading)
                                                    Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                                                else Brush.horizontalGradient(listOf(TextLight, TextLight)),
                                                RoundedCornerShape(10.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                        else Text("Verify", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    }
                                }
                            }
                        }



                        // ── Step 3: Success ───────────────────────────────────
                        else -> {
                            MessageCard(
                                message = "✓ Identity verified! A password reset link has been sent to $email.\n\n" +
                                        "📧 Check your inbox for the reset email. If you don't see it within a few minutes, " +
                                        "please check your Spam/Junk folder.\n\n" +
                                        "Click the link in the email to set your new password, then come back here to login.",
                                type = UIMessageType.SUCCESS
                            )

                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Done", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
// ── Message Card ──────────────────────────────────────────────────────────────

enum class UIMessageType { SUCCESS, ERROR, INFO }

@Composable
fun MessageCard(message: String, type: UIMessageType) {
    val (bgColor, borderColor, textColor) = when (type) {
        UIMessageType.SUCCESS -> Triple(Color(0xFFE8F5E8), Success, Color(0xFF2E7D2E))
        UIMessageType.ERROR -> Triple(Color(0xFFF8D7DA), Error, Color(0xFF721C24))
        else -> Triple(Color(0xFFE3F2FD), Color(0xFF2196F3), Color(0xFF1976D2))
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            fontSize = 13.sp,
            color = textColor,
            lineHeight = 18.sp
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    CraftoriaTheme {
        LoginScreen(
            onNavigateToVerification = {},
            onNavigateToHome = {},
            onNavigateToRoleSelection = { _, _ -> },
            onNavigateToLoginTab = {},
            onNavigateToSignUpTab = {}
        )
    }
}