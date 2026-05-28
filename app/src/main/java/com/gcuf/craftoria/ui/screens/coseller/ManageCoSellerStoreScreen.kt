package com.gcuf.craftoria.ui.screens.coseller

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import com.gcuf.craftoria.ui.components.StandardizedOutlinedTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.*
import com.gcuf.craftoria.ui.components.StandardizedOutlinedTextFieldCompact
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.utils.CloudinaryManager
import com.gcuf.craftoria.viewmodel.CoSellerStoreState
import com.gcuf.craftoria.viewmodel.CoSellerStoreViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCoSellerStoreScreen(
    storeId: String,
    user: User,
    onBackClick: () -> Unit,
    onProductClick: (Product) -> Unit,
    onAddProductClick: () -> Unit,
    onEditProductClick: (String) -> Unit,
    onPaymentsClick: () -> Unit = {},
    onNavigateToChat: (String, String) -> Unit = { _, _ -> }, // ✅ Chat navigation callback
    onNavigateToProductPreview: (String) -> Unit = { _ -> }, // ✅ NEW: Product preview navigation
    coSellerStoreViewModel: CoSellerStoreViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by coSellerStoreViewModel.uiState.collectAsState()
    val currentStore by coSellerStoreViewModel.currentStore.collectAsState()
    val storeMembers by coSellerStoreViewModel.storeMembers.collectAsState()
    val storeProducts by coSellerStoreViewModel.storeProducts.collectAsState()
    val storeInvitations by coSellerStoreViewModel.storeInvitations.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRemoveMemberDialog by remember { mutableStateOf(false) }
    var showDeleteProductDialog by remember { mutableStateOf(false) }
    var showLeaveStoreDialog by remember { mutableStateOf(false) }
    var showSellerDirectory by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<StoreMember?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(storeId) { coSellerStoreViewModel.loadStoreDetails(storeId) }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is CoSellerStoreState.ActionSuccess -> {
                snackbarHostState.showSnackbar(state.message)
                if (state.message.contains("deleted", ignoreCase = true) || state.message.contains("left", ignoreCase = true)) {
                    onBackClick()
                }
                coSellerStoreViewModel.resetState()
            }
            is CoSellerStoreState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                coSellerStoreViewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundSecondary,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
                        Text(text = currentStore?.storeName ?: "Store", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 18.sp)
                        Text(text = "Manage store details", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 13.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Box(modifier = Modifier.size(34.dp).background(Color.White.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onPaymentsClick) {
                        Box(modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = "Store Payments", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight)))
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (uiState) {
                is CoSellerStoreState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                else -> {
                    currentStore?.let { store ->
                        StoreBanner(store = store)
                        StoreHeader(store = store)

                        // Tab row — unchanged
                        Surface(color = Color.White, shadowElevation = 0.dp) {
                            TabRow(
                                selectedTabIndex = selectedTab,
                                containerColor = Color.White,
                                contentColor = Primary,
                                divider = { HorizontalDivider(color = BorderColor, thickness = 0.5.dp) },
                                indicator = { tabPositions ->
                                    SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                        color = Primary,
                                        height = 2.5.dp
                                    )
                                }
                            ) {
                                listOf("Products", "Members", "Settings").forEachIndexed { index, label ->
                                    Tab(
                                        selected = selectedTab == index,
                                        onClick = { selectedTab = index },
                                        text = {
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        when (selectedTab) {
                            0 -> ProductsTab(
                                products = storeProducts,
                                onAddProduct = onAddProductClick,
                                onProductClick = onProductClick,
                                onEditProduct = { product -> onEditProductClick(product.id) },
                                onDeleteProduct = { product -> productToDelete = product; showDeleteProductDialog = true }
                            )
                            1 -> MembersTab(
                                store = store,
                                members = storeMembers,
                                invitations = storeInvitations,
                                currentUserId = user.id,
                                onRemoveMember = { member -> memberToRemove = member; showRemoveMemberDialog = true },
                                onSendInvitation = { email ->
                                    val invitation = StoreInvitation(
                                        storeId = store.id, storeName = store.storeName,
                                        inviterId = user.id, inviterName = user.name, inviteeEmail = email
                                    )
                                    coSellerStoreViewModel.sendInvitation(invitation)
                                },
                                onBrowseSellers = { showSellerDirectory = true }
                            )
                            2 -> SettingsTab(
                                store = store,
                                currentUserId = user.id,
                                onUpdate = { updatedStore, logoUri, bannerUri ->
                                    coSellerStoreViewModel.updateStore(context, store.id, updatedStore, logoUri, bannerUri)
                                },
                                onDelete = { showDeleteDialog = true },
                                onLeaveStore = { showLeaveStoreDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Seller Directory Navigation ───────────────────────────────────────────
    // ── Seller Directory Navigation ───────────────────────────────────────────
    if (showSellerDirectory && currentStore != null) {
        SellerDirectoryScreen(
            currentStoreId = storeId,
            currentUserId = user.id,
            onSellerSelected = { seller ->
                val invitation = StoreInvitation(
                    storeId = currentStore!!.id,
                    storeName = currentStore!!.storeName,
                    inviterId = user.id,
                    inviterName = user.name,
                    inviteeEmail = seller.email
                )
                coSellerStoreViewModel.sendInvitation(invitation)
                showSellerDirectory = false
            },
            onBackClick = { showSellerDirectory = false },
            onNavigateToChat = { sellerId, sellerName ->
                showSellerDirectory = false
                onNavigateToChat(sellerId, sellerName) // ✅ Pass to parent
            },
            onNavigateToProductPreview = { productId ->
                // ✅ NEW: Navigate to product preview in seller preview mode
                onNavigateToProductPreview(productId)
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(modifier = Modifier.size(56.dp).background(Error.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = Error, modifier = Modifier.size(28.dp))
                }
            },
            title = { Text("Delete Store?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete this store?\n\nThis action cannot be undone.\n\nAll products will be removed from the store but will remain in individual seller accounts.",
                    fontSize = 13.sp, lineHeight = 19.sp, color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { coSellerStoreViewModel.deleteStore(storeId); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) { Text("Delete Store", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) { Text("Cancel", color = TextSecondary, fontSize = 13.sp) }
            }
        )
    }

    // ── Remove Member Dialog ──────────────────────────────────────────────────
    if (showRemoveMemberDialog && memberToRemove != null) {
        AlertDialog(
            onDismissRequest = { showRemoveMemberDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(modifier = Modifier.size(56.dp).background(Error.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = Error, modifier = Modifier.size(28.dp))
                }
            },
            title = { Text("Remove Member?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text("Are you sure you want to remove ${memberToRemove?.userName} from this store?", fontSize = 13.sp, color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        memberToRemove?.let { coSellerStoreViewModel.removeMember(storeId, it.userId) }
                        showRemoveMemberDialog = false; memberToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) { Text("Remove", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRemoveMemberDialog = false; memberToRemove = null },
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) { Text("Cancel", color = TextSecondary, fontSize = 13.sp) }
            }
        )
    }

    // ── Delete Product Dialog ─────────────────────────────────────────────────
    if (showDeleteProductDialog && productToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteProductDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(modifier = Modifier.size(56.dp).background(Error.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = Error, modifier = Modifier.size(28.dp))
                }
            },
            title = { Text("Delete Product?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text("Are you sure you want to delete ${productToDelete?.title}?", fontSize = 13.sp, color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        productToDelete?.let { coSellerStoreViewModel.deleteProduct(it.id, storeId) }
                        showDeleteProductDialog = false; productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) { Text("Delete", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteProductDialog = false; productToDelete = null },
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) { Text("Cancel", color = TextSecondary, fontSize = 13.sp) }
            }
        )
    }

    // ── Leave Store Dialog ────────────────────────────────────────────────────
    if (showLeaveStoreDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveStoreDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(modifier = Modifier.size(56.dp).background(Error.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = Error, modifier = Modifier.size(28.dp))
                }
            },
            title = { Text("Leave Store?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text("Are you sure you want to leave ${currentStore?.storeName}? You can rejoin if invited again.", fontSize = 13.sp, color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = { coSellerStoreViewModel.leaveStore(storeId, user.id); showLeaveStoreDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) { Text("Leave", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLeaveStoreDialog = false },
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp)
                ) { Text("Cancel", color = TextSecondary, fontSize = 13.sp) }
            }
        )
    }
}

// ── Store Banner ──────────────────────────────────────────────────────────────

@Composable
fun StoreBanner(store: CoSellerStore) {
    Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        if (store.storeBanner.isNotEmpty()) {
            AsyncImage(
                model = CloudinaryManager.getOptimizedUrl(store.storeBanner, 600, 110),
                contentDescription = "Store Banner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Primary, PrimaryLight))))
        }
    }
}

// ── Store Header ──────────────────────────────────────────────────────────────

@Composable
fun StoreHeader(store: CoSellerStore) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        Box(
            modifier = Modifier.size(62.dp).clip(RoundedCornerShape(12.dp))
                .background(Color.White).padding(3.dp)
        ) {
            if (store.storeLogo.isNotEmpty()) {
                AsyncImage(
                    model = CloudinaryManager.getOptimizedUrl(store.storeLogo, 150, 80),
                    contentDescription = "Store Logo",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                        .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = store.storeName.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = store.storeName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        if (store.storeDescription.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = store.storeDescription, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

// ── Products Tab ──────────────────────────────────────────────────────────────

@Composable
fun ProductsTab(
    products: List<Product>,
    onAddProduct: () -> Unit,
    onProductClick: (Product) -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Add product — gradient fill
            Button(
                onClick = onAddProduct,
                modifier = Modifier.fillMaxWidth().height(44.dp),
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Add Product to Store", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        if (products.isEmpty()) {
            item {
                // Primary tinted circle — consistent with all empty states
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Primary.copy(alpha = 0.08f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = Primary.copy(alpha = 0.50f),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("No products yet", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Add products to your store", fontSize = 12.sp, color = TextSecondary)
                }
            }
        } else {
            items(products) { product ->
                StoreProductCard(
                    product = product,
                    onClick = { onProductClick(product) },
                    onEdit = { onEditProduct(product) },
                    onDelete = { onDeleteProduct(product) }
                )
            }
        }
    }
}

// ── Store Product Card ────────────────────────────────────────────────────────

@Composable
fun StoreProductCard(product: Product, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(BackgroundSecondary),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrls.isNotEmpty()) {
                    AsyncImage(
                        model = CloudinaryManager.getOptimizedUrl(product.imageUrls.first(), 150, 80),
                        contentDescription = product.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(imageVector = Icons.Outlined.Inventory2, contentDescription = null, tint = TextLight, modifier = Modifier.size(26.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = "PKR ${String.format("%,.0f", product.price)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Added by: ${product.sellerName}", fontSize = 10.sp, color = TextSecondary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier.size(28.dp).background(BackgroundSecondary, RoundedCornerShape(7.dp)).clip(RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
                Box(
                    modifier = Modifier.size(28.dp).background(Error.copy(alpha = 0.08f), RoundedCornerShape(7.dp)).clip(RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Error, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

// ── Members Tab ───────────────────────────────────────────────────────────────

@Composable
fun MembersTab(
    store: CoSellerStore,
    members: List<StoreMember>,
    invitations: List<StoreInvitation>,
    currentUserId: String,
    onRemoveMember: (StoreMember) -> Unit,
    onSendInvitation: (String) -> Unit,
    onBrowseSellers: () -> Unit = {}
) {
    var inviteEmail by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(text = "STORE MEMBERS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary, letterSpacing = 0.5.sp)
        }
        items(members) { member ->
            MemberCard(
                member = member,
                isOwner = member.userId == store.ownerId,
                canRemove = store.ownerId == currentUserId && member.userId != currentUserId,
                onRemove = { onRemoveMember(member) }
            )
        }
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "INVITE NEW MEMBER", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary, letterSpacing = 0.5.sp)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StandardizedOutlinedTextFieldCompact(
                    value = inviteEmail,
                    onValueChange = { inviteEmail = it },
                    placeholder = "Enter seller's email",
                    singleLine = true,
                    minHeight = 48,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (inviteEmail.isNotEmpty() && inviteEmail.contains("@")) {
                            onSendInvitation(inviteEmail)
                            inviteEmail = ""
                        }
                    },
                    enabled = inviteEmail.isNotEmpty() && inviteEmail.contains("@"),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("Invite", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            OutlinedButton(
                onClick = onBrowseSellers,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Browse Sellers", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
        if (invitations.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "PENDING INVITATIONS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary, letterSpacing = 0.5.sp)
            }
            items(invitations) { invitation -> InvitationCard(invitation = invitation) }
        }
    }
}

// ── Member Card ───────────────────────────────────────────────────────────────

@Composable
fun MemberCard(member: StoreMember, isOwner: Boolean, canRemove: Boolean, onRemove: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight))),
                contentAlignment = Alignment.Center
            ) {
                Text(text = member.userName.take(1).uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    // ✅ NEW: Display member name with real-time updates
                    com.gcuf.craftoria.ui.components.RealtimeNameDisplay(
                        userId = member.userId,
                        fallbackName = member.userName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    if (isOwner) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Primary.copy(alpha = 0.10f)) {
                            Text(text = "Owner", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Text(text = formatJoinedDate(member.joinedAt), fontSize = 11.sp, color = TextSecondary)
            }
            if (canRemove) {
                Box(
                    modifier = Modifier.size(28.dp).background(Error.copy(alpha = 0.08f), RoundedCornerShape(7.dp)).clip(RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = Error, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

// ── Invitation Card — unified badge styling ──────────────────────────────────

@Composable
fun InvitationCard(invitation: StoreInvitation) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = BackgroundSecondary,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = invitation.inviteeEmail,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )

            // ✅ Unified badge styling — exactly matching My Orders screen
            val (bgColor, textColor) = when (invitation.status) {
                InvitationStatus.PENDING  -> Color(0xFFFFF3CD) to Color(0xFF856404)  // Warning yellow
                InvitationStatus.ACCEPTED -> Color(0xFFD4EDDA) to Color(0xFF155724)  // Success green
                InvitationStatus.DECLINED -> Color(0xFFF8D7DA) to Color(0xFF721C24)  // Error red
            }
            Surface(shape = RoundedCornerShape(20.dp), color = bgColor) {
                Text(
                    text = invitation.status.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

// ── Settings Tab ──────────────────────────────────────────────────────────────

@Composable
fun SettingsTab(
    store: CoSellerStore,
    currentUserId: String,
    onUpdate: (CoSellerStore, Uri?, Uri?) -> Unit,
    onDelete: () -> Unit,
    onLeaveStore: () -> Unit = {}
) {
    var storeName by remember { mutableStateOf(store.storeName) }
    var storeDescription by remember { mutableStateOf(store.storeDescription) }
    var logoUri by remember { mutableStateOf<Uri?>(null) }
    var bannerUri by remember { mutableStateOf<Uri?>(null) }

    val isOwner = store.ownerId == currentUserId

    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> logoUri = uri }
    val bannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> bannerUri = uri }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                StandardizedOutlinedTextFieldCompact(
                    value = storeName,
                    onValueChange = { storeName = it },
                    placeholder = "",
                    singleLine = true,
                    minHeight = 48
                )
            }
        }
        item {
            Column {
                StandardizedOutlinedTextField(
                    value = storeDescription,
                    onValueChange = { storeDescription = it },
                    label = "Store Description",
                    placeholder = "",
                    minLines = 4,
                    maxLines = 6,
                    minHeight = 120
                )
            }
        }
        item {
            Column {
                Text(text = "Store Logo", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                ImageUploadBox(uri = logoUri, iconVector = Icons.Outlined.AddAPhoto, label = "Tap to change logo", onClick = { logoLauncher.launch("image/*") })
            }
        }
        item {
            Column {
                Text(text = "Store Banner", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                ImageUploadBox(uri = bannerUri, iconVector = Icons.Outlined.AddPhotoAlternate, label = "Tap to change banner", onClick = { bannerLauncher.launch("image/*") })
            }
        }

        // Save Changes — gradient fill consistent with all primary CTAs
        item {
            Button(
                onClick = {
                    val updatedStore = store.copy(storeName = storeName, storeDescription = storeDescription)
                    onUpdate(updatedStore, logoUri, bannerUri)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)), RoundedCornerShape(10.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
        }

        // Delete / Leave — 0.5.dp border (was 2.dp throughout)
        if (isOwner) {
            item {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete Store", fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            item {
                OutlinedButton(
                    onClick = onLeaveStore,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Leave Store", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}