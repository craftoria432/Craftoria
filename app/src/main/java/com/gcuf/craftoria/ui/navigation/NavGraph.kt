package com.gcuf.craftoria.ui.navigation

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gcuf.craftoria.data.model.NegotiationStatus
import com.gcuf.craftoria.data.model.Product
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.data.model.UserRole
import com.gcuf.craftoria.data.model.VerificationStatus
import com.gcuf.craftoria.ui.screens.SplashScreen
import com.gcuf.craftoria.ui.screens.auth.LoginScreen
import com.gcuf.craftoria.ui.screens.auth.ProfileScreen
import com.gcuf.craftoria.ui.screens.auth.SettingsScreen
import com.gcuf.craftoria.ui.screens.auth.SellerVerificationScreen
import com.gcuf.craftoria.ui.screens.auth.RoleSelectionScreen
import com.gcuf.craftoria.ui.screens.buyer.MyChatsScreen
import com.gcuf.craftoria.ui.screens.buyer.*
import com.gcuf.craftoria.ui.screens.seller.AddProductScreen
import com.gcuf.craftoria.ui.screens.seller.ManageProductsScreen
import com.gcuf.craftoria.ui.screens.seller.NegotiationRequestsScreen
import com.gcuf.craftoria.ui.screens.seller.SellerDashboardScreen
import com.gcuf.craftoria.ui.screens.seller.SellerOrdersScreen
import com.gcuf.craftoria.ui.screens.seller.SellerPublicProfileScreen
import com.gcuf.craftoria.ui.screens.learning.LearningResourcesScreen
import com.gcuf.craftoria.ui.screens.notifications.NotificationsScreen
import com.gcuf.craftoria.data.model.NotificationActionType
import com.gcuf.craftoria.ui.screens.seller.SellerMessagesScreen
import com.gcuf.craftoria.ui.theme.Primary
import com.gcuf.craftoria.ui.theme.PrimaryLight
import com.gcuf.craftoria.ui.theme.TextPrimary
import com.gcuf.craftoria.ui.theme.BackgroundSecondary
import com.gcuf.craftoria.ui.theme.TextSecondary
import com.gcuf.craftoria.viewmodel.AuthViewModel
import com.gcuf.craftoria.viewmodel.CartViewModel
import com.gcuf.craftoria.viewmodel.DashboardViewModel
import com.gcuf.craftoria.ui.screens.coseller.MyCoSellerStoresScreen
import com.gcuf.craftoria.ui.screens.coseller.CreateCoSellerStoreScreen
import com.gcuf.craftoria.ui.screens.coseller.StorePublicViewScreen
import com.gcuf.craftoria.ui.screens.coseller.ManageCoSellerStoreScreen
import com.gcuf.craftoria.ui.screens.chat.ChatScreen
import com.gcuf.craftoria.ui.screens.info.HelpSupportScreen
import com.gcuf.craftoria.ui.screens.info.PrivacyPolicyScreen
import com.gcuf.craftoria.ui.screens.info.TermsConditionsScreen
import com.gcuf.craftoria.ui.screens.seller.SellerPaymentsScreen
import com.gcuf.craftoria.ui.screens.seller.PaymentDetailScreen
import com.gcuf.craftoria.ui.screens.buyer.PaymentHistoryScreen
import com.gcuf.craftoria.ui.screens.seller.SellerRefundManagementScreen
import com.gcuf.craftoria.ui.screens.seller.SellerRefundDetailScreen
import com.gcuf.craftoria.viewmodel.WishlistViewModel
import com.gcuf.craftoria.viewmodel.UnreadMessageViewModel
import com.gcuf.craftoria.data.repository.PaymentRepository
import com.gcuf.craftoria.data.model.SellerPayment
import com.gcuf.craftoria.viewmodel.CoSellerStoreViewModel
import com.gcuf.craftoria.ui.screens.coseller.CoSellerStorePaymentScreen
import com.gcuf.craftoria.ui.screens.coseller.CoSellerOrderDetailScreen
import com.gcuf.craftoria.ui.screens.coseller.RateStoreScreen
import com.gcuf.craftoria.ui.screens.coseller.StoreRatingsScreen
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/* --------------------- ROUTES --------------------- */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object RoleSelection : Screen("role_selection/{userId}/{userName}") {
        fun createRoute(userId: String, userName: String) = "role_selection/$userId/$userName"
    }
    object Verification : Screen("verification")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object Wishlist : Screen("wishlist")

    object ProductDetails : Screen("product/{productId}") {
        fun createRoute(productId: String) = "product/$productId"
        fun createSellerPreviewRoute(productId: String) = "product/$productId?sellerPreview=true"
    }

    object SellerProfile : Screen("seller_profile/{userId}") {
        fun createRoute(userId: String) = "seller_profile/$userId"
    }

    object Cart : Screen("cart")
    object Checkout : Screen("checkout")

    object OrderSuccess : Screen("order_success/{orderIds}") {
        fun createRoute(orderIds: String) = "order_success/$orderIds"
    }

    // Order routes
    object MyOrders : Screen("my_orders?highlightOrderId={highlightOrderId}") {
        fun createRoute(highlightOrderId: String = "") =
            if (highlightOrderId.isEmpty()) "my_orders" else "my_orders?highlightOrderId=$highlightOrderId"
    }

    object RefundRequest : Screen("refund_request/{orderId}") {
        fun createRoute(orderId: String) = "refund_request/$orderId"
    }

    object RefundDetails : Screen("refund_details/{refundId}") {
        fun createRoute(refundId: String) = "refund_details/$refundId"
    }

    object SellerOrders : Screen("seller_orders?highlightOrderId={highlightOrderId}") {
        fun createRoute(highlightOrderId: String = "") =
            if (highlightOrderId.isEmpty()) "seller_orders" else "seller_orders?highlightOrderId=$highlightOrderId"
    }

    object Search : Screen("search")

    // Buyer routes
    object PaymentHistory : Screen("payment_history")

    // Seller routes
    object SellerDashboard : Screen("seller_dashboard")
    object AddProduct : Screen("add_product")
    object ManageProducts : Screen("manage_products")
    object NegotiationRequests : Screen("negotiation_requests")
    object SellerPayments : Screen("seller_payments")
    object SellerPaymentDetail : Screen("seller_payment_detail/{paymentId}") {
        fun createRoute(paymentId: String) = "seller_payment_detail/$paymentId"
    }

    // Seller Refund Management
    object SellerRefundManagement : Screen("seller_refund_management")
    object SellerRefundDetail : Screen("seller_refund_detail/{refundId}") {
        fun createRoute(refundId: String) = "seller_refund_detail/$refundId"
    }
    object CoSellerStorePayments : Screen("coseller_store_payments/{storeId}/{storeName}") {
        fun createRoute(storeId: String, storeName: String) =
            "coseller_store_payments/$storeId/$storeName"
    }
    object CoSellerOrderDetail : Screen("coseller_order_detail/{paymentId}") {
        fun createRoute(paymentId: String) = "coseller_order_detail/$paymentId"
    }

    object MyCoSellerStores : Screen("my_coseller_stores")
    object CreateCoSellerStore : Screen("create_coseller_store")
    object ManageCoSellerStore : Screen("manage_coseller_store")
    object StorePublicView : Screen("store_public_view")
    object LearningResources : Screen("learning_resources")
    object Notifications : Screen("notifications")
    object AllActivity : Screen("all_activity")

    // Messages route for future chat
    object Messages : Screen("messages")
    object SellerMessages : Screen("seller_messages")

    object MyChats : Screen("my_chats")

    // Chat route for future implementation
    object Chat : Screen("chat")
    object AllStores : Screen("all_stores")

    object HelpSupport : Screen("help_support")
    object TermsConditions : Screen("terms_conditions")
    object PrivacyPolicy : Screen("privacy_policy")
}

/* --------------------- NAV GRAPH --------------------- */
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    cartViewModel: CartViewModel = viewModel(),
    wishlistViewModel: WishlistViewModel = viewModel(),
    dashboardViewModel: DashboardViewModel = viewModel(),
    unreadMessageViewModel: com.gcuf.craftoria.viewmodel.UnreadMessageViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()

    // Start listening for unread messages and initialize cart when user changes
    LaunchedEffect(currentUser?.id) {
        currentUser?.id?.let { userId ->
            if (userId.isNotEmpty()) {
                unreadMessageViewModel.startListening(userId)
                cartViewModel.initializeCart(userId)  // ✅ Initialize cart for real-time updates
                wishlistViewModel.initForUser(userId)  // ✅ Initialize wishlist for real-time updates
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {

        /* ---------------------- SPLASH ---------------------- */
        composable(Screen.Splash.route) {
            SplashScreen(
                isUserLoggedIn = currentUser != null,
                onNavigateToNext = {
                    // The 3-second splash delay ensures auth state is almost always available
                    // AuthViewModel starts its Firestore listener immediately in init
                    val destination = when {
                        currentUser == null -> Screen.Login.route

                        currentUser!!.role == UserRole.SELLER &&
                                currentUser!!.verificationStatus != VerificationStatus.APPROVED ->
                            Screen.Verification.route

                        currentUser!!.role == UserRole.SELLER -> Screen.SellerDashboard.route

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
                    val destination = if (currentUser?.role == UserRole.SELLER) {
                        Screen.SellerDashboard.route
                    } else {
                        Screen.Home.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRoleSelection = { userId, userName ->
                    navController.navigate(Screen.RoleSelection.createRoute(userId, userName)) {
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

        /* ---------------------- ROLE SELECTION (First-time Google users) ---------------------- */
        composable(
            route = Screen.RoleSelection.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("userName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val userName = backStackEntry.arguments?.getString("userName") ?: ""

            RoleSelectionScreen(
                userId = userId,
                userName = userName,
                onRoleSelected = { intendedRole ->
                    // ─────────────────────────────────────────────────────────────────────────────
                    // intendedRole = what the user originally tapped (BUYER or SELLER).
                    // For SELLER, Firestore stores SELLER + APPROVED (seller_application_status)
                    // with verification_status = NOT_SUBMITTED, and we route to Verification
                    // so the user can submit their identity verification selfie.
                    // ─────────────────────────────────────────────────────────────────────────────
                    val destination = when (intendedRole) {
                        UserRole.SELLER -> Screen.Verification.route
                        else -> Screen.Home.route
                    }
                    navController.navigate(destination) {
                        // Remove RoleSelection from back-stack so pressing back from
                        // Verification (or Home) does NOT return here.
                        popUpTo(Screen.RoleSelection.route) { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                },
                viewModel = authViewModel
            )
        }

        /* ---------------------- SELLER VERIFICATION ---------------------- */
        composable(Screen.Verification.route) {
            val user = currentUser

            // ✅ SECURITY: Prevent any navigation away from verification screen for unverified sellers
            BackHandler(enabled = user?.role == UserRole.SELLER && 
                               user.verificationStatus != VerificationStatus.APPROVED) {
                // Block back button - unverified sellers cannot leave this screen
                // They must complete verification or logout
            }

            SellerVerificationScreen(
                verificationStatus = user?.verificationStatus ?: VerificationStatus.NOT_SUBMITTED,
                rejectionReason = user?.rejectionReason,
                sellerName = user?.name ?: "",
                sellerEmail = user?.email ?: "",
                sellerPhone = user?.phone ?: "",
                onBackClick = {
                    // ✅ SECURITY: Strictly block back navigation for unverified sellers
                    val isUnverified = user?.role == UserRole.SELLER && 
                                      user.verificationStatus != VerificationStatus.APPROVED
                    
                    if (!isUnverified) {
                        // Allow approved sellers to go back (e.g., from Profile)
                        navController.popBackStack()
                    }
                    // If unverified, do nothing - back is completely blocked by BackHandler
                },
                onNavigateToSellerDashboard = {
                    navController.navigate(Screen.SellerDashboard.route) {
                        popUpTo(Screen.Verification.route) { inclusive = true }
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
                },
                viewModel = authViewModel
            )
        }

        /* ---------------------- HOME ---------------------- */
        composable(Screen.Home.route) {
            HomeScreen(
                productViewModel = viewModel(),
                cartViewModel = cartViewModel,
                wishlistViewModel = wishlistViewModel,
                unreadMessageViewModel = unreadMessageViewModel,
                currentUserId = currentUser?.id ?: "",
                onNavigateToProduct = { product ->
                    navController.navigate(Screen.ProductDetails.createRoute(product.id))
                },
                onNavigateToCart = {
                    navController.navigate(Screen.Cart.route)
                },
                onNavigateToOrders = {
                    navController.navigate(Screen.MyOrders.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToStore = { storeId ->
                    navController.navigate("${Screen.StorePublicView.route}/$storeId")
                },
                onNavigateToChats = {
                    navController.navigate(Screen.MyChats.route)
                },
                onNavigateToWishlist = {
                    navController.navigate(Screen.Wishlist.route)
                },
                onNavigateToAllStores = {
                    navController.navigate(Screen.AllStores.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                }
            )
        }

        composable(Screen.Wishlist.route) {
            WishlistScreen(
                wishlistViewModel = wishlistViewModel,
                cartViewModel = cartViewModel,
                userId = currentUser?.id ?: "",
                onBackClick = { navController.popBackStack() },
                onNavigateToProduct = { product ->
                    navController.navigate(Screen.ProductDetails.createRoute(product.id))
                }
            )
        }

        /* ---------------------- PAYMENT HISTORY ---------------------- */
        composable(Screen.PaymentHistory.route) {
            currentUser?.let { user ->
                PaymentHistoryScreen(
                    buyerId = user.id,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.AllStores.route) {
            AllStoresScreen(
                onBackClick = { navController.popBackStack() },
                onStoreClick = { storeId ->
                    navController.navigate("${Screen.StorePublicView.route}/$storeId")
                }
            )
        }

        /* ---------------------- HELP & SUPPORT ---------------------- */
        composable(Screen.HelpSupport.route) {
            HelpSupportScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        /* ---------------------- TERMS & CONDITIONS ---------------------- */
        composable(Screen.TermsConditions.route) {
            TermsConditionsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        /* ---------------------- PRIVACY POLICY ---------------------- */
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        /* ---------------------- SELLER DASHBOARD ---------------------- */
        composable(Screen.SellerDashboard.route) {
            currentUser?.let { user ->
                // ✅ SECURITY: Strictly enforce verification - redirect unverified sellers immediately
                if (user.role == UserRole.SELLER && user.verificationStatus != VerificationStatus.APPROVED) {
                    LaunchedEffect(user.verificationStatus) {
                        navController.navigate(Screen.Verification.route) {
                            popUpTo(0) { inclusive = true } // Clear entire back stack
                        }
                    }
                    // Show nothing while redirecting
                    Box(modifier = Modifier.fillMaxSize())
                    return@composable
                }
                
                SellerDashboardScreen(
                    user = user,
                    onNavigateToAddProduct = {
                        navController.navigate(Screen.AddProduct.route)
                    },
                    onNavigateToProducts = {
                        navController.navigate(Screen.ManageProducts.route)
                    },
                    onNavigateToOrders = {
                        navController.navigate(Screen.SellerOrders.createRoute())
                    },
                    onNavigateToNegotiations = {
                        navController.navigate(Screen.NegotiationRequests.route)
                    },
                    onNavigateToCoSeller = {
                        navController.navigate(Screen.MyCoSellerStores.route)
                    },
                    onNavigateToLearning = {
                        navController.navigate(Screen.LearningResources.route)
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Screen.Notifications.route)
                    },
                    onNavigateToActivity = {
                        navController.navigate(Screen.AllActivity.route)
                    },
                    onNavigateToMessages = {
                        navController.navigate(Screen.SellerMessages.route)
                    },
                    onNavigateToPayments = {  // ✅ Add payments navigation
                        navController.navigate(Screen.SellerPayments.route)
                    },
                    onNavigateToRefunds = {  // ✅ Add refunds navigation
                        navController.navigate(Screen.SellerRefundManagement.route)
                    },
                    dashboardViewModel = dashboardViewModel
                )
            }
        }

        /* ---------------------- PRODUCT DETAILS ---------------------- */
        composable(
            route = Screen.ProductDetails.route + "?sellerPreview={sellerPreview}",
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType },
                navArgument("sellerPreview") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            val isSellerPreview = backStackEntry.arguments?.getBoolean("sellerPreview") ?: false

            ProductDetailsScreenWrapper(
                productId = productId,
                currentUserId = currentUser?.id ?: "",
                isSellerPreview = isSellerPreview,
                cartViewModel = cartViewModel,
                wishlistViewModel = wishlistViewModel,
                onBackClick = { navController.popBackStack() },
                onAddToCart = { product, price, isNegotiated, negotiationStatus ->
                    cartViewModel.addToCart(
                        userId = currentUser?.id ?: "",  // ✅ FIX 1
                        product = product,
                        price = price,
                        isNegotiated = isNegotiated,
                        negotiationStatus = negotiationStatus
                    )
                    navController.navigate(Screen.Cart.route)
                },
                onNavigateToCart = {
                    navController.navigate(Screen.Cart.route)
                },
                onChatWithSeller = { sellerId, sellerName ->
                    // Pass productId to chat for product context
                    navController.navigate("${Screen.Chat.route}/$sellerId/$sellerName?productId=$productId")
                },
                onNavigateToStore = { storeId ->
                    navController.navigate("${Screen.StorePublicView.route}/$storeId")
                }
            )
        }

        /* ---------------------- CART ---------------------- */
        composable(Screen.Cart.route) {
            CartScreen(
                cartViewModel = cartViewModel,
                onBackClick = { navController.popBackStack() },
                onCheckout = { navController.navigate(Screen.Checkout.route) },
                onContinueShopping = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetails.createRoute(productId))
                }
            )
        }

        /* ---------------------- CHECKOUT ---------------------- */
        composable(Screen.Checkout.route) {
            CheckoutScreen(
                cartViewModel = cartViewModel,
                userId = currentUser?.id ?: "",
                userName = currentUser?.name ?: "",
                onBackClick = { navController.popBackStack() },
                onOrderSuccess = { orderId ->
                    navController.navigate(Screen.OrderSuccess.createRoute(orderId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onNavigateToTerms = {
                    navController.navigate(Screen.TermsConditions.route)
                }
            )
        }

        /* ---------------------- ORDER SUCCESS ---------------------- */
        composable(
            route = Screen.OrderSuccess.route,
            arguments = listOf(navArgument("orderIds") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderIds = backStackEntry.arguments?.getString("orderIds") ?: ""

            OrderSuccessScreen(
                orderIds = orderIds,
                onTrackOrder = {
                    navController.navigate(Screen.MyOrders.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onContinueShopping = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        /* ---------------------- SEARCH ---------------------- */
        composable(Screen.Search.route) {
            SearchScreen(
                onBackClick = { navController.popBackStack() },
                onProductClick = { product ->
                    navController.navigate(Screen.ProductDetails.createRoute(product.id))
                },
                cartViewModel = cartViewModel,
                wishlistViewModel = wishlistViewModel,
                currentUserId = currentUser?.id ?: ""
            )
        }

        /* ---------------------- MY CHATS (Buyer) ---------------------- */
        composable(Screen.MyChats.route) {
            MyChatsScreen(
                userId = currentUser?.id ?: "",
                onBackClick = {
                    navController.popBackStack()
                },
                onChatClick = { sellerId, sellerName ->
                    navController.navigate("${Screen.Chat.route}/$sellerId/$sellerName")
                }
            )
        }

        /* ---------------------- PROFILE ---------------------- */
        composable(Screen.Profile.route) {
            currentUser?.let { user ->
                ProfileScreen(
                    user = user,
                    onBackClick = { navController.popBackStack() },
                    onLogout = {
                        // Clear local theme cache so a different user doesn't inherit this theme
                        com.gcuf.craftoria.utils.ThemePreferenceCache.clear(context)
                        // Reset ThemeManager to default so the login screen uses Rose
                        com.gcuf.craftoria.ui.theme.ThemeManager.getInstance()
                            .setTheme(com.gcuf.craftoria.ui.theme.ThemeType.ROSE)
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateTo = { route ->
                        when (route) {
                            "verification" -> {
                                navController.navigate(Screen.Verification.route)
                            }

                            "my_orders" -> {
                                navController.navigate(Screen.MyOrders.route)
                            }

                            "orders" -> {
                                val destination = if (user.role == UserRole.SELLER) {
                                    Screen.SellerOrders.createRoute()
                                } else {
                                    Screen.MyOrders.route
                                }
                                navController.navigate(destination)
                            }

                            "manage_products" -> {
                                navController.navigate(Screen.ManageProducts.route)
                            }

                            "co_seller_stores" -> {
                                navController.navigate(Screen.MyCoSellerStores.route)
                            }

                            "messages" -> {
                                navController.navigate(Screen.SellerMessages.route)
                            }

                            "chats" -> {
                                navController.navigate(Screen.MyChats.route)
                            }

                            "notifications" -> {
                                navController.navigate(Screen.Notifications.route)
                            }

                            "help" ->
                                navController.navigate(Screen.HelpSupport.route)

                            "terms" ->
                                navController.navigate(Screen.TermsConditions.route)

                            "privacy" ->
                                navController.navigate(Screen.PrivacyPolicy.route)

                            "payment_history" ->
                                navController.navigate(Screen.PaymentHistory.route)

                            "wishlist" -> navController.navigate(Screen.Wishlist.route)

                            "settings" -> navController.navigate(Screen.Settings.route)

                            else -> {
                                Log.d("NavGraph", "Navigation not implemented for: $route")
                            }
                        }
                    }
                )
            } ?: run {
                // Fallback when user is null - show loading or redirect to login
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
                LaunchedEffect(Unit) {
                    // If user is null, redirect to login after a short delay
                    kotlinx.coroutines.delay(1000)
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        /* ---------------------- SETTINGS ---------------------- */
        composable(Screen.Settings.route) {
            currentUser?.let { user ->
                // Inject dependencies properly - use singleton ThemeManager
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                // Pass context so ThemeRepository can write to the local cache on every save
                val themeRepository = com.gcuf.craftoria.data.repository.ThemeRepository(
                    firestore = firestore,
                    context = context
                )
                val themeManager = com.gcuf.craftoria.ui.theme.ThemeManager.getInstance()

                SettingsScreen(
                    user = user,
                    onBackClick = { navController.popBackStack() },
                    themeRepository = themeRepository,
                    themeManager = themeManager
                )
            }
        }

        /* ---------------------- MY ORDERS (Buyer) ---------------------- */
        composable(
            route = Screen.MyOrders.route,
            arguments = listOf(
                androidx.navigation.navArgument("highlightOrderId") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val highlightOrderId = backStackEntry.arguments?.getString("highlightOrderId") ?: ""
            MyOrdersScreen(
                userId = currentUser?.id ?: "",
                cartViewModel = cartViewModel,
                highlightOrderId = highlightOrderId,
                onBackClick = { navController.popBackStack() },
                onNavigateToProduct = { productId ->
                    navController.navigate(Screen.ProductDetails.createRoute(productId))
                },
                onNavigateToCart = {
                    navController.navigate(Screen.Cart.route)
                },
                onNavigateToRefundRequest = { orderId ->
                    navController.navigate(Screen.RefundRequest.createRoute(orderId))
                },
                onNavigateToRefundDetails = { refundId ->
                    navController.navigate(Screen.RefundDetails.createRoute(refundId))
                }
            )
        }

        /* ---------------------- REFUND REQUEST ---------------------- */
        composable(
            route = Screen.RefundRequest.route,
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            BuyerRefundRequestScreen(
                orderId = orderId,
                onBackClick = { navController.popBackStack() }
            )
        }

        /* ---------------------- REFUND DETAILS ---------------------- */
        composable(
            route = Screen.RefundDetails.route,
            arguments = listOf(
                navArgument("refundId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val refundId = backStackEntry.arguments?.getString("refundId") ?: ""
            RefundDetailsScreen(
                refundId = refundId,
                onBackClick = { navController.popBackStack() },
                onContactSupport = {
                    navController.navigate(Screen.HelpSupport.route)
                },
                onViewOrderDetails = { /* No longer needed - dialog opens directly */ }
            )
        }

        /* ---------------------- SELLER PROFILE (View Other User's Profile) ---------------------- */
        composable(
            route = Screen.SellerProfile.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""

            SellerPublicProfileScreen(
                sellerId = userId,
                currentUserId = currentUser?.id ?: "",
                onBackClick = { navController.popBackStack() },
                onProductClick = { productId ->
                    navController.navigate(Screen.ProductDetails.createRoute(productId))
                },
                onChatWithSeller = { sellerId, sellerName ->
                    navController.navigate("${Screen.Chat.route}/$sellerId/$sellerName")
                },
                onAddToCart = { product ->
                    cartViewModel.addToCart(
                        userId = currentUser?.id ?: "",  // ✅ FIX 2
                        product = product,
                        price = product.price,
                        isNegotiated = false,
                        negotiationStatus = null
                    )
                },
                onAddToWishlist = { product ->
                    wishlistViewModel.toggleWishlist(product)
                },
                onNavigateToCart = {
                    navController.navigate(Screen.Cart.route)
                }
            )
        }

        /* ---------------------- SELLER ORDERS ---------------------- */
        composable(
            route = Screen.SellerOrders.route,
            arguments = listOf(
                navArgument("highlightOrderId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val highlightOrderId = backStackEntry.arguments?.getString("highlightOrderId") ?: ""
            currentUser?.let { user ->
                // ✅ SECURITY: Strictly enforce verification - block orders for unverified sellers
                if (user.role == UserRole.SELLER && user.verificationStatus != VerificationStatus.APPROVED) {
                    LaunchedEffect(user.verificationStatus) {
                        navController.navigate(Screen.Verification.route) {
                            popUpTo(0) { inclusive = true } // Clear entire back stack
                        }
                    }
                    // Show nothing while redirecting
                    Box(modifier = Modifier.fillMaxSize())
                    return@composable
                }
                
                if (user.role == UserRole.SELLER) {
                    SellerOrdersScreen(
                        user = user,
                        highlightOrderId = highlightOrderId,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onOrderClick = { order ->
                            Log.d("NavGraph", "Order clicked: ${order.id}")
                        },
                        navController = navController
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.MyOrders.route) {
                            popUpTo(Screen.SellerOrders.createRoute()) { inclusive = true }
                        }
                    }
                }
            }
        }

        /* ---------------------- SELLER PAYMENTS ---------------------- */
        composable(Screen.SellerPayments.route) {
            currentUser?.let { user ->
                // ✅ SECURITY: Strictly enforce verification - block payments for unverified sellers
                if (user.role == UserRole.SELLER && user.verificationStatus != VerificationStatus.APPROVED) {
                    LaunchedEffect(user.verificationStatus) {
                        navController.navigate(Screen.Verification.route) {
                            popUpTo(0) { inclusive = true } // Clear entire back stack
                        }
                    }
                    // Show nothing while redirecting
                    Box(modifier = Modifier.fillMaxSize())
                    return@composable
                }
                
                if (user.role == UserRole.SELLER) {
                    SellerPaymentsScreen(
                        sellerId = user.id,
                        onBackClick = { navController.popBackStack() },
                        onPaymentClick = { paymentId ->
                            navController.navigate(Screen.SellerPaymentDetail.createRoute(paymentId))
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.SellerPayments.route) { inclusive = true }
                        }
                    }
                }
            }
        }

        /* ---------------------- SELLER PAYMENT DETAIL ---------------------- */
        composable(
            route = Screen.SellerPaymentDetail.route,
            arguments = listOf(navArgument("paymentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val paymentId = backStackEntry.arguments?.getString("paymentId") ?: ""
            currentUser?.let { user ->
                if (user.role == UserRole.SELLER) {
                    PaymentDetailScreen(
                        paymentId = paymentId,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }

        /* ══════════════════════════════════════════════════════════════════════════════ */
        /* Seller Refund Management                                                        */
        /* ══════════════════════════════════════════════════════════════════════════════ */
        composable(Screen.SellerRefundManagement.route) {
            currentUser?.let { user ->
                // ✅ SECURITY: Strictly enforce verification - block refunds for unverified sellers
                if (user.role == UserRole.SELLER && user.verificationStatus != VerificationStatus.APPROVED) {
                    LaunchedEffect(user.verificationStatus) {
                        navController.navigate(Screen.Verification.route) {
                            popUpTo(0) { inclusive = true } // Clear entire back stack
                        }
                    }
                    // Show nothing while redirecting
                    Box(modifier = Modifier.fillMaxSize())
                    return@composable
                }
                
                // Guard: only sellers can access
                if (user.role == UserRole.SELLER) {
                    SellerRefundManagementScreen(
                        onBackClick = { navController.popBackStack() },
                        onRefundClick = { refundId ->
                            navController.navigate(Screen.SellerRefundDetail.createRoute(refundId))
                        }
                    )
                } else {
                    // Redirect unauthorized users
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }
        }

        composable(
            route = Screen.SellerRefundDetail.route,
            arguments = listOf(navArgument("refundId") { type = NavType.StringType })
        ) { backStackEntry ->
            val refundId = backStackEntry.arguments?.getString("refundId") ?: return@composable
            val scope = rememberCoroutineScope()
            
            if (currentUser?.role == UserRole.SELLER) {
                SellerRefundDetailScreen(
                    refundId = refundId,
                    onBackClick = { navController.popBackStack() },
                    onContactBuyer = { buyerId ->
                        // Fetch buyer name and navigate to chat
                        scope.launch {
                            try {
                                val userDoc = FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(buyerId)
                                    .get()
                                    .await()
                                
                                val buyerName = userDoc.getString("name") ?: "Buyer"
                                navController.navigate("${Screen.Chat.route}/$buyerId/$buyerName")
                            } catch (e: Exception) {
                                Log.e("NavGraph", "Error fetching buyer name: ${e.message}")
                                // Fallback to generic name
                                navController.navigate("${Screen.Chat.route}/$buyerId/Buyer")
                            }
                        }
                    }
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        /* ---------------------- CO-SELLER STORE PAYMENTS ---------------------- */
        composable(
            route = "coseller_store_payments/{storeId}/{storeName}",
            arguments = listOf(
                navArgument("storeId") { type = NavType.StringType },
                navArgument("storeName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
            val storeName = backStackEntry.arguments?.getString("storeName") ?: ""

            CoSellerStorePaymentScreen(
                storeId = storeId,
                storeName = storeName,
                onBackClick = { navController.popBackStack() },
                onPaymentClick = { paymentId ->
                    navController.navigate(Screen.CoSellerOrderDetail.createRoute(paymentId))
                }
            )
        }

        /* ---------------------- CO-SELLER ORDER DETAIL ---------------------- */
        composable(
            route = Screen.CoSellerOrderDetail.route,
            arguments = listOf(navArgument("paymentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val paymentId = backStackEntry.arguments?.getString("paymentId") ?: ""
            currentUser?.let { user ->
                CoSellerOrderDetailScreen(
                    paymentId = paymentId,
                    currentUserId = user.id,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        /* ---------------------- NEGOTIATION REQUESTS ---------------------- */
        composable(Screen.NegotiationRequests.route) {
            currentUser?.let { user ->
                // ✅ SECURITY: Strictly enforce verification - block negotiation access for unverified sellers
                if (user.role == UserRole.SELLER && user.verificationStatus != VerificationStatus.APPROVED) {
                    LaunchedEffect(user.verificationStatus) {
                        navController.navigate(Screen.Verification.route) {
                            popUpTo(0) { inclusive = true } // Clear entire back stack
                        }
                    }
                    // Show nothing while redirecting
                    Box(modifier = Modifier.fillMaxSize())
                    return@composable
                }
                
                if (user.role == UserRole.SELLER) {
                    NegotiationRequestsScreen(
                        user = user,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.NegotiationRequests.route) { inclusive = true }
                        }
                    }
                }
            }
        }

        /* ---------------------- SELLER MESSAGES ---------------------- */
        composable(Screen.SellerMessages.route) {
            currentUser?.let { user ->
                // ✅ SECURITY: Strictly enforce verification - block messages for unverified sellers
                if (user.role == UserRole.SELLER && user.verificationStatus != VerificationStatus.APPROVED) {
                    LaunchedEffect(user.verificationStatus) {
                        navController.navigate(Screen.Verification.route) {
                            popUpTo(0) { inclusive = true } // Clear entire back stack
                        }
                    }
                    // Show nothing while redirecting
                    Box(modifier = Modifier.fillMaxSize())
                    return@composable
                }
                
                if (user.role == UserRole.SELLER) {
                    SellerMessagesScreen(
                        user = user,
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onChatClick = { otherUserId, otherUserName ->
                            navController.navigate("${Screen.Chat.route}/$otherUserId/$otherUserName")
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.SellerMessages.route) { inclusive = true }
                        }
                    }
                }
            }
        }

        /* ---------------------- ADD PRODUCT ---------------------- */
        composable(
            route = "${Screen.AddProduct.route}?productId={productId}",
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            currentUser?.let { user ->
                // ✅ SECURITY: Strictly enforce verification - block product creation for unverified sellers
                if (user.role == UserRole.SELLER && user.verificationStatus != VerificationStatus.APPROVED) {
                    LaunchedEffect(user.verificationStatus) {
                        navController.navigate(Screen.Verification.route) {
                            popUpTo(0) { inclusive = true } // Clear entire back stack
                        }
                    }
                    // Show nothing while redirecting
                    Box(modifier = Modifier.fillMaxSize())
                    return@composable
                }
                
                AddProductScreen(
                    user = user,
                    editProductId = productId,
                    onBackClick = { navController.popBackStack() },
                    onSuccess = {
                        navController.navigate(Screen.ManageProducts.route) {
                            popUpTo(Screen.SellerDashboard.route)
                        }
                    }
                )
            }
        }

        /* ---------------------- MANAGE PRODUCTS ---------------------- */
        composable(Screen.ManageProducts.route) {
            currentUser?.let { user ->
                // ✅ SECURITY: Strictly enforce verification - block product management for unverified sellers
                if (user.role == UserRole.SELLER && user.verificationStatus != VerificationStatus.APPROVED) {
                    LaunchedEffect(user.verificationStatus) {
                        navController.navigate(Screen.Verification.route) {
                            popUpTo(0) { inclusive = true } // Clear entire back stack
                        }
                    }
                    // Show nothing while redirecting
                    Box(modifier = Modifier.fillMaxSize())
                    return@composable
                }
                
                ManageProductsScreen(
                    user = user,
                    onBackClick = { navController.popBackStack() },
                    onAddProductClick = {
                        navController.navigate(Screen.AddProduct.route)
                    },
                    onEditProductClick = { product ->
                        navController.navigate("${Screen.AddProduct.route}?productId=${product.id}")
                    },
                    onProductClick = { product ->
                        navController.navigate(
                            Screen.ProductDetails.createSellerPreviewRoute(
                                product.id
                            )
                        )
                    }
                )
            }
        }

        /* ---------------------- CO-SELLER STORES ---------------------- */
        composable(Screen.MyCoSellerStores.route) {
            currentUser?.let { user ->
                MyCoSellerStoresScreen(
                    user = user,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onCreateStoreClick = {
                        navController.navigate(Screen.CreateCoSellerStore.route)
                    },
                    onStoreClick = { store ->
                        navController.navigate("${Screen.ManageCoSellerStore.route}/${store.id}")
                    }
                )
            }
        }

        composable(Screen.CreateCoSellerStore.route) {
            currentUser?.let { user ->
                CreateCoSellerStoreScreen(
                    user = user,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onStoreCreated = { storeId ->
                        navController.navigate("${Screen.ManageCoSellerStore.route}/$storeId") {
                            popUpTo(Screen.MyCoSellerStores.route)
                        }
                    }
                )
            }
        }

        composable(
            route = "${Screen.ManageCoSellerStore.route}/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
            currentUser?.let { user ->
                ManageCoSellerStoreScreen(
                    storeId = storeId,
                    user = user,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onProductClick = { product ->
                        // ✅ Use seller preview mode for co-sellers viewing their store products
                        navController.navigate(Screen.ProductDetails.createSellerPreviewRoute(product.id)) {
                            launchSingleTop = true
                        }
                    },
                    onAddProductClick = {
                        navController.navigate(Screen.AddProduct.route)
                    },
                    onEditProductClick = { productId ->
                        navController.navigate("${Screen.AddProduct.route}?productId=$productId")
                    },
                    onPaymentsClick = {
                        // Navigate to dedicated payments screen
                        navController.navigate(
                            Screen.CoSellerStorePayments.createRoute(
                                storeId,
                                "Store Payments"
                            )
                        )
                    },
                    onNavigateToChat = { sellerId, sellerName ->
                        // ✅ FIX: Navigate to seller-to-seller chat WITHOUT product context
                        navController.navigate("${Screen.Chat.route}/$sellerId/$sellerName")
                    },
                    onNavigateToProductPreview = { productId ->
                        // Navigate to product preview in seller preview mode
                        navController.navigate(Screen.ProductDetails.createSellerPreviewRoute(productId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable(
            route = "${Screen.StorePublicView.route}/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
            StorePublicViewScreen(
                storeId = storeId,
                currentUserId = currentUser?.id ?: "",
                onBackClick = {
                    navController.popBackStack()
                },
                onProductClick = { product ->
                    navController.navigate(Screen.ProductDetails.createRoute(product.id))
                },
                onAddToCart = { product ->
                    cartViewModel.addToCart(
                        userId = currentUser?.id ?: "",  // ✅ FIX 3
                        product = product,
                        price = product.price,
                        isNegotiated = false,
                        negotiationStatus = null
                    )
                }
            )
        }

        /* ---------------------- LEARNING RESOURCES ---------------------- */
        composable(Screen.LearningResources.route) {
            currentUser?.let { user ->
                LearningResourcesScreen(
                    user = user,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }

        /* ---------------------- NOTIFICATIONS ---------------------- */
        composable(Screen.Notifications.route) {
            currentUser?.let { user ->
                val coSellerStoreViewModel: com.gcuf.craftoria.viewmodel.CoSellerStoreViewModel =
                    viewModel()
                val notificationViewModel: com.gcuf.craftoria.viewmodel.NotificationViewModel =
                    viewModel()
                val coroutineScope = rememberCoroutineScope()

                NotificationsScreen(
                    user = user,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onNotificationAction = { notification ->
                        when (notification.actionTypeEnum) {
                            NotificationActionType.VIEW_ORDER -> {
                                val orderId = notification.orderId
                                if (user.role == UserRole.SELLER) {
                                    if (orderId.isNotEmpty()) {
                                        navController.navigate(
                                            Screen.SellerOrders.createRoute(
                                                orderId
                                            )
                                        )
                                    } else {
                                        navController.navigate(Screen.SellerOrders.createRoute())
                                    }
                                } else {
                                    if (orderId.isNotEmpty()) {
                                        navController.navigate(Screen.MyOrders.createRoute(orderId))
                                    } else {
                                        navController.navigate(Screen.MyOrders.createRoute())
                                    }
                                }
                            }

                            NotificationActionType.TRACK_ORDER -> {
                                if (user.role == UserRole.SELLER) {
                                    val orderId = notification.orderId
                                    if (orderId.isNotEmpty()) {
                                        navController.navigate(
                                            Screen.SellerOrders.createRoute(
                                                orderId
                                            )
                                        )
                                    } else {
                                        navController.navigate(Screen.SellerOrders.createRoute())
                                    }
                                } else {
                                    // ✅ Pass order ID to MyOrders screen
                                    val orderId = notification.orderId
                                    if (orderId.isNotEmpty()) {
                                        navController.navigate(Screen.MyOrders.createRoute(orderId))
                                    } else {
                                        navController.navigate(Screen.MyOrders.createRoute())
                                    }
                                }
                            }

                            NotificationActionType.ACCEPT_INVITATION -> {
                                coroutineScope.launch {
                                    try {
                                        var invitationId =
                                            notification.actionData["invitation_id"] ?: ""

                                        if (invitationId.isEmpty()) {
                                            Log.d(
                                                "NavGraph",
                                                "Invitation ID not in notification, searching..."
                                            )
                                            val db =
                                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                            val invitationSnapshot =
                                                db.collection("store_invitations")
                                                    .whereEqualTo("store_id", notification.storeId)
                                                    .whereEqualTo("invitee_id", user.id)
                                                    .whereEqualTo("status", "PENDING")
                                                    .limit(1)
                                                    .get()
                                                    .await()

                                            if (invitationSnapshot.documents.isNotEmpty()) {
                                                invitationId = invitationSnapshot.documents[0].id
                                                Log.d("NavGraph", "Found invitation: $invitationId")
                                            }
                                        }

                                        if (invitationId.isNotEmpty()) {
                                            Log.d("NavGraph", "Accepting invitation: $invitationId")
                                            val result =
                                                coSellerStoreViewModel.acceptInvitationAsync(
                                                    invitationId = invitationId,
                                                    userId = user.id,
                                                    userName = user.name
                                                )

                                            if (result.isSuccess) {
                                                Log.d(
                                                    "NavGraph",
                                                    "Invitation accepted successfully"
                                                )
                                            } else {
                                                Log.e(
                                                    "NavGraph",
                                                    "Failed to accept invitation: ${result.exceptionOrNull()?.message}"
                                                )
                                            }
                                        } else {
                                            Log.e(
                                                "NavGraph",
                                                "No invitation found for store: ${notification.storeId}"
                                            )
                                        }

                                        navController.navigate(Screen.MyCoSellerStores.route) {
                                            popUpTo(Screen.Notifications.route) {
                                                inclusive = false
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("NavGraph", "Error accepting invitation", e)
                                    }
                                }
                            }

                            NotificationActionType.VIEW_STORE -> {
                                navController.navigate("${Screen.ManageCoSellerStore.route}/${notification.storeId}")
                            }

                            NotificationActionType.REPLY_MESSAGE -> {
                                navController.navigate(Screen.MyChats.route)
                            }

                            NotificationActionType.VIEW_PRODUCT -> {
                                // ✅ Check if user is the seller of this specific product to show seller preview
                                coroutineScope.launch {
                                    try {
                                        val productDoc =
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                .collection("products")
                                                .document(notification.productId)
                                                .get()
                                                .await()

                                        val productSellerId =
                                            productDoc.getString("seller_id") ?: ""

                                        if (user.role == UserRole.SELLER && productSellerId == user.id) {
                                            // User is the seller of this product - show seller preview
                                            navController.navigate(
                                                Screen.ProductDetails.createSellerPreviewRoute(
                                                    notification.productId
                                                )
                                            )
                                        } else {
                                            // User is not the seller or is a buyer - show normal view
                                            navController.navigate(
                                                Screen.ProductDetails.createRoute(
                                                    notification.productId
                                                )
                                            )
                                        }
                                    } catch (e: Exception) {
                                        Log.e("NavGraph", "Error checking product seller", e)
                                        // Fallback to normal view
                                        navController.navigate(
                                            Screen.ProductDetails.createRoute(
                                                notification.productId
                                            )
                                        )
                                    }
                                }
                            }

                            NotificationActionType.RATE_ORDER -> {
                                if (user.role == UserRole.SELLER) {
                                    navController.navigate(Screen.SellerOrders.createRoute())
                                } else {
                                    navController.navigate(Screen.MyOrders.route)
                                }
                            }

                            NotificationActionType.VIEW_PROMOTIONS -> {
                                navController.navigate(Screen.Home.route)
                            }

                            NotificationActionType.VIEW_RATING -> {
                                // Different navigation based on user role
                                if (user.role == UserRole.SELLER) {
                                    // Seller: Navigate to store ratings view
                                    // Shows all ratings received for this store
                                    val storeId = notification.storeId
                                    if (storeId.isNotEmpty()) {
                                        navController.navigate("store_ratings/$storeId")
                                    }
                                } else {
                                    // Buyer: Navigate to rate store dialog
                                    // Opens dialog to submit rating
                                    val storeId = notification.storeId
                                    val orderId = notification.orderId
                                    if (storeId.isNotEmpty() && orderId.isNotEmpty()) {
                                        navController.navigate("rate_store/$storeId/$orderId")
                                    } else if (storeId.isNotEmpty()) {
                                        navController.navigate("rate_store/$storeId/")
                                    }
                                }
                                
                                // Mark notification as read
                                if (!notification.isRead) {
                                    notificationViewModel.markAsRead(notification.id, user.id)
                                }
                            }

                            else -> {}
                        }
                    }
                )
            }
        }

        /* ---------------------- ALL ACTIVITY → Notifications ---------------------- */
        composable(Screen.AllActivity.route) {
            LaunchedEffect(Unit) {
                navController.navigate(Screen.Notifications.route) {
                    popUpTo(Screen.AllActivity.route) { inclusive = true }
                }
            }
        }

        /* ---------------------- MESSAGES (Buyer) ---------------------- */
        composable(Screen.Messages.route) {
            currentUser?.let { user ->
                MyChatsScreen(
                    userId = user.id,
                    onBackClick = { navController.popBackStack() },
                    onChatClick = { otherUserId, otherUserName ->
                        navController.navigate("${Screen.Chat.route}/$otherUserId/$otherUserName")
                    }
                )
            }
        }

        /* ---------------------- CHAT ---------------------- */
        composable(
            route = "${Screen.Chat.route}/{otherUserId}/{otherUserName}?productId={productId}",
            arguments = listOf(
                navArgument("otherUserId") { type = NavType.StringType },
                navArgument("otherUserName") { type = NavType.StringType },
                navArgument("productId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            val otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: ""
            val productId = backStackEntry.arguments?.getString("productId") ?: ""

            currentUser?.let { user ->
                ChatScreen(
                    currentUser = user,
                    otherUserId = otherUserId,
                    otherUserName = otherUserName,
                    productId = productId,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onViewProfile = { userId ->
                        navController.navigate(Screen.SellerProfile.createRoute(userId))
                    },
                    onViewProduct = { productId ->
                        navController.navigate(Screen.ProductDetails.createRoute(productId))
                    },
                    onTrackOrder = { orderId ->
                        // Order details dialog opens directly from MyOrdersScreen
                    }
                )
            }
        }

        /* ---------------------- STORE RATINGS (Seller View) ---------------------- */
        composable(
            route = "store_ratings/{storeId}",
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
            currentUser?.let { user ->
                if (user.role == UserRole.SELLER) {
                    StoreRatingsScreen(
                        storeId = storeId,
                        onBackClick = { navController.popBackStack() }
                    )
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }
        }

        /* ---------------------- RATE STORE (Buyer) ---------------------- */
        composable(
            route = "rate_store/{storeId}/{orderId}",
            arguments = listOf(
                navArgument("storeId") { type = NavType.StringType },
                navArgument("orderId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            currentUser?.let { user ->
                if (user.role != UserRole.SELLER) {
                    RateStoreScreen(
                        storeId = storeId,
                        orderId = orderId,
                        buyerId = user.id,
                        buyerName = user.name,
                        onBackClick = { navController.popBackStack() },
                        onRated = { navController.popBackStack() }
                    )
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }
        }

    }
}

/* ---------------------- PRODUCT DETAILS WRAPPER ---------------------- */
@Composable
fun ProductDetailsScreenWrapper(
    productId: String,
    currentUserId: String,
    onBackClick: () -> Unit,
    onAddToCart: (Product, Double, Boolean, NegotiationStatus?) -> Unit,
    onNavigateToCart: () -> Unit,
    isSellerPreview: Boolean = false,
    onChatWithSeller: (String, String) -> Unit,
    onNavigateToStore: (String) -> Unit,
    cartViewModel: CartViewModel? = null,
    wishlistViewModel: WishlistViewModel? = null
) {
    var product by remember { mutableStateOf<Product?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(productId) {
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("products")
                .document(productId)
                .get()
                .await()

            if (doc.exists()) {
                product = doc.toObject(Product::class.java)?.copy(id = doc.id)
            } else {
                errorMessage = "Product not found"
            }
        } catch (e: Exception) {
            errorMessage = e.message
        } finally {
            isLoading = false
        }
    }

    when {
        isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }

        errorMessage != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text(errorMessage ?: "Unknown error")
        }

        product != null -> {
            val prod = product ?: return
            ProductDetailsScreen(
                product = prod,
                currentUserId = currentUserId,
                onBackClick = onBackClick,
                onAddToCart = onAddToCart,
                onNavigateToCart = onNavigateToCart,
                onChatWithSeller = onChatWithSeller,
                onNavigateToStore = onNavigateToStore,
                isSellerPreview = isSellerPreview,
                cartViewModel = cartViewModel,
                wishlistViewModel = wishlistViewModel
            )
        }
    }
}

/* ---------------------- PLACEHOLDER SCREEN ---------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    title: String,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Primary, PrimaryLight)
                    )
                )
            )
        }
    )
    { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF5F5F5),
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Text(
                    text = "$title - Coming Soon",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "This feature is under development",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
