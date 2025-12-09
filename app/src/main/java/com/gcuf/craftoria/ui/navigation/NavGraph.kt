package com.gcuf.craftoria.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gcuf.craftoria.data.model.UserRole
import com.gcuf.craftoria.data.model.VerificationStatus
import com.gcuf.craftoria.ui.screens.SplashScreen
import com.gcuf.craftoria.ui.screens.auth.LoginScreen
import com.gcuf.craftoria.ui.screens.auth.SellerVerificationScreen
import com.gcuf.craftoria.ui.screens.auth.ProfileScreen
import com.gcuf.craftoria.ui.screens.buyer.HomeScreen
import com.gcuf.craftoria.ui.screens.buyer.MyOrdersScreen
import com.gcuf.craftoria.viewmodel.AuthViewModel
import com.gcuf.craftoria.viewmodel.OrderViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Verification : Screen("verification")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Cart : Screen("cart")
    object Orders : Screen("orders")
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    orderViewModel: OrderViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {

        /* ---------------------- SPLASH ---------------------- */
        composable(Screen.Splash.route) {
            SplashScreen(
                isUserLoggedIn = currentUser != null,
                onNavigateToNext = {
                    val destination = when {
                        currentUser == null -> Screen.Login.route

                        // SELLER → MUST BE VERIFIED
                        currentUser!!.role == UserRole.SELLER &&
                                currentUser!!.verificationStatus != VerificationStatus.APPROVED ->
                            Screen.Verification.route

                        // BUYER OR VERIFIED SELLER → HOME
                        else -> Screen.Home.route
                    }

                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        /* ---------------------- LOGIN ---------------------- */
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToVerification = {
                    navController.navigate(Screen.Verification.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLoginTab = {
                    navController.navigate(Screen.Login.route) { launchSingleTop = true }
                },
                onNavigateToSignUpTab = {
                    navController.navigate(Screen.Login.route) { launchSingleTop = true }
                },
                viewModel = authViewModel
            )
        }

        /* ---------------- SELLER VERIFICATION ---------------- */
        composable(Screen.Verification.route) {

            val user = currentUser

            SellerVerificationScreen(
                verificationStatus = user?.verificationStatus ?: VerificationStatus.NOT_SUBMITTED,
                rejectionReason = user?.rejectionReason,
                onBackClick = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Home.route)
                    }
                },
                onSubmitVerification = { uri ->
                    if (user != null) {
                        authViewModel.uploadVerificationPhoto(
                            context = context,
                            userId = user.id,
                            imageUri = uri
                        )
                    }
                }
            )
        }

        /* ---------------------- BUYER HOME ---------------------- */
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToProduct = { product ->
                    // TODO: navigate to product details
                },
                onNavigateToCart = {
                    navController.navigate(Screen.Cart.route)
                },
                onNavigateToOrders = {
                    navController.navigate(Screen.Orders.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        /* ---------------------- MY ORDERS ---------------------- */
        composable(Screen.Orders.route) {
            MyOrdersScreen(
                userId = currentUser?.id ?: "",
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToProduct = { productId ->
                    // TODO: Navigate to product details when route is ready
                    // navController.navigate(Screen.ProductDetails.createRoute(productId))
                },
                onNavigateToCart = {
                    navController.navigate(Screen.Cart.route)
                },
                orderViewModel = orderViewModel
            )
        }

        /* ---------------------- PROFILE ---------------------- */
        composable(Screen.Profile.route) {
            val user by authViewModel.currentUser.collectAsState()

            user?.let {
                ProfileScreen(
                    user = it,
                    onBackClick = { navController.popBackStack() },
                    onLogout = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateTo = { route ->
                        if (route == "verification") {
                            navController.navigate(Screen.Verification.route)
                        }
                    }
                )
            }
        }
    }
}