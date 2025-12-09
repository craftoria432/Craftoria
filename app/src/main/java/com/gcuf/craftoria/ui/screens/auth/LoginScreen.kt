package com.gcuf.craftoria.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.ui.components.*
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.AuthState
import com.gcuf.craftoria.viewmodel.AuthViewModel
import com.gcuf.craftoria.data.model.UserRole

@Composable
fun LoginScreen(
    onNavigateToVerification: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToLoginTab: () -> Unit,
    onNavigateToSignUpTab: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = SignUp, 1 = Login

    val authState by viewModel.authState.collectAsState()

    // Navigation after login/signup success
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                val user = viewModel.currentUser.value

                if (user != null) {
                    if (user.role == UserRole.SELLER &&
                        user.verificationStatus.name != "APPROVED"
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


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        CraftoriaTopBar(
            screenNumber = "01",
            showLogo = true
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            /* ----------------------------- TAB ROW ----------------------------- */
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = BackgroundSecondary,
                contentColor = Primary,
                indicator = @Composable { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedTab])
                                .height(3.dp)
                                .background(Primary)
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        onNavigateToSignUpTab()
                    },
                    text = {
                        Text(
                            text = "Sign Up",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onNavigateToLoginTab()
                    },
                    text = {
                        Text(
                            text = "Login",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            }

            Box(modifier = Modifier.padding(20.dp)) {
                when (selectedTab) {
                    0 -> SignUpForm(
                        viewModel = viewModel,
                        authState = authState,
                        switchToLogin = {
                            selectedTab = 1
                            onNavigateToLoginTab()
                        }
                    )

                    1 -> LoginForm(
                        viewModel = viewModel,
                        authState = authState,
                        switchToSignUp = {
                            selectedTab = 0
                            onNavigateToSignUpTab()
                        }
                    )
                }
            }
        }
    }
}

/* ---------------------------------------------------------------------------
   SIGN UP FORM
--------------------------------------------------------------------------- */
@Composable
fun SignUpForm(
    viewModel: AuthViewModel,
    authState: AuthState,
    switchToLogin: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSeller by remember { mutableStateOf(false) }
    var agreeToTerms by remember { mutableStateOf(false) }

    Column {
        // Message Cards
        when (authState) {
            is AuthState.Success -> MessageCard(
                message = authState.message,
                type = MessageType.SUCCESS
            )

            is AuthState.Error -> MessageCard(
                message = authState.message,
                type = MessageType.ERROR
            )
            else -> {}
        }

        Spacer(modifier = Modifier.height(14.dp))

        CraftoriaTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = "Full Name",
            required = true,
            placeholder = "Enter your full name"
        )

        Spacer(modifier = Modifier.height(14.dp))

        CraftoriaTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            required = true,
            placeholder = "Enter email",
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(14.dp))

        CraftoriaTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = "Phone Number",
            required = true,
            placeholder = "+92 300 1234567",
            keyboardType = KeyboardType.Phone
        )

        Spacer(modifier = Modifier.height(14.dp))

        CraftoriaTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            required = true,
            isPassword = true,
            placeholder = "Enter password"
        )

        Spacer(modifier = Modifier.height(14.dp))

        CraftoriaTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirm Password",
            required = true,
            isPassword = true,
            placeholder = "Re-enter password"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Seller Checkbox
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isSeller,
                onCheckedChange = { isSeller = it },
                colors = CheckboxDefaults.colors(checkedColor = Primary)
            )
            Text("Register as a Seller", color = TextPrimary)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Terms checkbox
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = agreeToTerms,
                onCheckedChange = { agreeToTerms = it },
                colors = CheckboxDefaults.colors(checkedColor = Primary)
            )

            Text(
                text = buildAnnotatedString {
                    append("I agree to the ")
                    withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) {
                        append("Terms & Conditions")
                    }
                    append(" and ")
                    withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) {
                        append("Privacy Policy")
                    }
                },
                fontSize = 13.sp,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        CraftoriaButton(
            text = "Create Account",
            onClick = {
                if (password == confirmPassword && agreeToTerms) {
                    viewModel.signUp(
                        email = email,
                        password = password,
                        name = fullName,
                        phone = phoneNumber,
                        role = if (isSeller) UserRole.SELLER else UserRole.BUYER
                    )
                }
            },
            enabled = fullName.isNotBlank() &&
                    email.isNotBlank() &&
                    phoneNumber.isNotBlank() &&
                    password.isNotBlank() &&
                    confirmPassword.isNotBlank() &&
                    agreeToTerms,
            isLoading = authState is AuthState.Loading
        )

        Spacer(modifier = Modifier.height(20.dp))

        /* ---------------- CLICK: Already have account → Login ---------------- */
        Text(
            text = buildAnnotatedString {
                append("Already have an account? ")
                withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) {
                    append("Login")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { switchToLogin() },
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
    }
}

/* ---------------------------------------------------------------------------
   LOGIN FORM
--------------------------------------------------------------------------- */
@Composable
fun LoginForm(
    viewModel: AuthViewModel,
    authState: AuthState,
    switchToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }

    Column {

        when (authState) {
            is AuthState.Success -> MessageCard(authState.message, MessageType.SUCCESS)
            is AuthState.Error -> MessageCard(authState.message, MessageType.ERROR)
            else -> {}
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Welcome Back 👋",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        CraftoriaTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email Address",
            required = true,
            keyboardType = KeyboardType.Email,
            placeholder = "Enter your email"
        )

        Spacer(modifier = Modifier.height(14.dp))

        CraftoriaTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            required = true,
            isPassword = true,
            placeholder = "Enter your password"
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(checkedColor = Primary)
                )
                Text("Remember me", color = TextPrimary)
            }

            Text(
                text = "Forgot Password?",
                fontSize = 12.sp,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        CraftoriaButton(
            text = "Login",
            onClick = {
                viewModel.login(email = email, password = password)
            },
            enabled = email.isNotBlank() && password.isNotBlank(),
            isLoading = authState is AuthState.Loading
        )

        Spacer(modifier = Modifier.height(20.dp))

        /* ---------------- CLICK: Don't have account → Sign Up ---------------- */
        Text(
            text = buildAnnotatedString {
                append("Don't have an account? ")
                withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) {
                    append("Sign Up")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { switchToSignUp() },
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
    }
}

/* ---------------------------------------------------------------------------
   MESSAGE CARD
--------------------------------------------------------------------------- */
enum class MessageType { SUCCESS, ERROR, INFO }

@Composable
fun MessageCard(message: String, type: MessageType) {
    val color = when (type) {
        MessageType.SUCCESS -> Color(0xFFE9F8E9)
        MessageType.ERROR -> Color(0xFFFFE5E7)
        else -> Color(0xFFE0F2FF)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            fontSize = 13.sp
        )
    }
}
