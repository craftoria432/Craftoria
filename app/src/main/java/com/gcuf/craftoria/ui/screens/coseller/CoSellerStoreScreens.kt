package com.gcuf.craftoria.ui.screens.coseller

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.*
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.utils.CloudinaryManager
import com.gcuf.craftoria.viewmodel.CoSellerStoreState
import com.gcuf.craftoria.viewmodel.CoSellerStoreViewModel
import androidx.compose.ui.unit.Dp
import java.text.SimpleDateFormat
import java.util.*

fun Modifier.dashedBorder(
    strokeWidth: Dp = 2.dp,
    color: Color = Color(0xFFE0E0E0),
    cornerRadius: Dp = 8.dp
) = this.drawBehind {
    drawRoundRect(
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = strokeWidth.toPx(),
            pathEffect = PathEffect.dashPathEffect(intervals = floatArrayOf(10f, 10f), phase = 0f)
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx())
    )
}

// ── Screen 13.1: My Co-Seller Stores List ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCoSellerStoresScreen(
    user: User,
    onBackClick: () -> Unit,
    onCreateStoreClick: () -> Unit,
    onStoreClick: (CoSellerStore) -> Unit,
    coSellerStoreViewModel: CoSellerStoreViewModel = viewModel()
) {
    val uiState by coSellerStoreViewModel.uiState.collectAsState()
    val stores by coSellerStoreViewModel.stores.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            coSellerStoreViewModel.loadUserStores(user.id)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is CoSellerStoreState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as CoSellerStoreState.Error).message,
                duration = SnackbarDuration.Short
            )
            coSellerStoreViewModel.resetState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundSecondary,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxHeight()) {
                        Text(text = "My Co-Seller Stores", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White, lineHeight = 16.sp)
                        Text(text = "Your collaborative stores", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 12.sp)
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
                    if (uiState != CoSellerStoreState.Empty && stores.isNotEmpty()) {
                        Surface(
                            onClick = onCreateStoreClick,
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(end = 12.dp).height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text(text = "Create", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight)))
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is CoSellerStoreState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            is CoSellerStoreState.Empty -> {
                EmptyStoresState(onCreateClick = onCreateStoreClick, modifier = Modifier.padding(paddingValues))
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(paddingValues)
                ) {
                    items(stores) { store ->
                        StoreCard(store = store, onClick = { onStoreClick(store) })
                    }
                }
            }
        }
    }
}

@Composable
fun StoreCard(store: CoSellerStore, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().background(BackgroundSecondary).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (store.storeLogo.isNotEmpty()) {
                    AsyncImage(
                        model = CloudinaryManager.getOptimizedUrl(store.storeLogo, 100, 80),
                        contentDescription = store.storeName,
                        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(BackgroundSecondary),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp))
                            .background(Brush.horizontalGradient(listOf(Primary, PrimaryLight))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = store.storeName.take(1).uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = store.storeName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Outlined.Group, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                            Text("${store.memberCount} Members", fontSize = 11.sp, color = TextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Outlined.ShoppingBag, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                            Text("${store.productCount} Products", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Manage Store", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun EmptyStoresState(onCreateClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).background(Primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Outlined.Store, contentDescription = null, tint = Primary.copy(alpha = 0.50f), modifier = Modifier.size(38.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("No Co-Seller Stores Yet", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Create a collaborative store and invite other sellers to join", fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 19.sp)
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.height(46.dp).width(200.dp).background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)), RoundedCornerShape(12.dp))
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Store", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Screen 13.2: Create Co-Seller Store ──────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCoSellerStoreScreen(
    user: User,
    onBackClick: () -> Unit,
    onStoreCreated: (String) -> Unit,
    coSellerStoreViewModel: CoSellerStoreViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by coSellerStoreViewModel.uiState.collectAsState()
    val isCreating = uiState is CoSellerStoreState.Loading

    var storeName by remember { mutableStateOf("") }
    var storeDescription by remember { mutableStateOf("") }
    var logoUri by remember { mutableStateOf<Uri?>(null) }
    var bannerUri by remember { mutableStateOf<Uri?>(null) }
    var inviteEmail by remember { mutableStateOf("") }
    var invitedEmails by remember { mutableStateOf<List<String>>(emptyList()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> logoUri = uri }
    val bannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> bannerUri = uri }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is CoSellerStoreState.ActionSuccess -> { snackbarHostState.showSnackbar(state.message); onStoreCreated(state.storeId) }
            is CoSellerStoreState.Error -> { snackbarHostState.showSnackbar(state.message); coSellerStoreViewModel.resetState() }
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
                        Text(text = "Create New Store", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White, lineHeight = 16.sp)
                        Text(text = "Set up your collaborative store", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Box(modifier = Modifier.size(34.dp).background(Color.White.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight)))
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(paddingValues)
        ) {
            item {
                CreateSectionCard(title = "Store Information", icon = Icons.Outlined.Store) {
                    Text(text = "Store Name *", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(value = storeName, onValueChange = { storeName = it }, placeholder = { Text("Enter store name", fontSize = 13.sp, color = TextSecondary) }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BorderColor, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Store Description", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(value = storeDescription, onValueChange = { storeDescription = it }, placeholder = { Text("Describe your collaborative store", fontSize = 13.sp, color = TextSecondary) }, minLines = 3, maxLines = 5, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BorderColor, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                CreateSectionCard(title = "Store Media", icon = Icons.Outlined.AddPhotoAlternate) {
                    Text(text = "Store Logo", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                    ImageUploadBox(uri = logoUri, iconVector = Icons.Outlined.AddAPhoto, label = "Tap to upload logo", onClick = { logoLauncher.launch("image/*") })
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Store Banner", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                    ImageUploadBox(uri = bannerUri, iconVector = Icons.Outlined.AddPhotoAlternate, label = "Tap to upload banner", onClick = { bannerLauncher.launch("image/*") })
                }
            }
            item {
                CreateSectionCard(title = "Invite Sellers", icon = Icons.Outlined.Group) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = inviteEmail, onValueChange = { inviteEmail = it }, placeholder = { Text("Enter seller's email", fontSize = 13.sp, color = TextSecondary) }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BorderColor, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f))
                        Button(onClick = { val email = inviteEmail.trim(); if (email.isNotEmpty() && email.contains("@") && !invitedEmails.contains(email)) { invitedEmails = invitedEmails + email; inviteEmail = "" } }, enabled = inviteEmail.isNotEmpty() && inviteEmail.contains("@"), colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = RoundedCornerShape(10.dp), modifier = Modifier.height(52.dp)) {
                            Text("Add", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(7.dp), color = Primary.copy(alpha = 0.05f), border = androidx.compose.foundation.BorderStroke(0.5.dp, Primary.copy(alpha = 0.15f)), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Primary, modifier = Modifier.size(13.dp))
                            Text(text = "Only registered Craftoria sellers can be invited", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }
            items(invitedEmails) { email ->
                InvitedEmailCard(email = email, status = "Pending", onRemove = { invitedEmails = invitedEmails - email })
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onBackClick, enabled = !isCreating, modifier = Modifier.weight(1f).height(48.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary), border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor), shape = RoundedCornerShape(12.dp)) {
                        Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = {
                            if (storeName.isNotEmpty()) {
                                val store = CoSellerStore(storeName = storeName, storeDescription = storeDescription, ownerId = user.id, ownerName = user.name)
                                coSellerStoreViewModel.createStore(context = context, store = store, logoUri = logoUri, bannerUri = bannerUri, invitedEmails = invitedEmails)
                            }
                        },
                        enabled = storeName.isNotEmpty() && !isCreating,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Primary.copy(alpha = 0.4f)),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(if (storeName.isNotEmpty() && !isCreating) Brush.horizontalGradient(listOf(Primary, PrimaryLight)) else Brush.horizontalGradient(listOf(Primary.copy(0.4f), PrimaryLight.copy(0.4f))), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCreating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Create Store", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(10.dp)) }
        }
    }
}

// ── Section Card — tinted gradient header ─────────────────────────────────────

@Composable
fun CreateSectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Tinted gradient header — consistent with every section card in the project
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Primary.copy(alpha = 0.06f), Primary.copy(alpha = 0.02f))
                    )
                )
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier.size(28.dp).background(Primary.copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
            }
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), content = content)
    }
}

// ── Image Upload Box ──────────────────────────────────────────────────────────

@Composable
fun ImageUploadBox(uri: Uri?, iconVector: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth().height(130.dp)
            .dashedBorder(strokeWidth = 1.5.dp, color = Primary.copy(alpha = 0.25f), cornerRadius = 10.dp)
    ) {
        if (uri != null) {
            AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(48.dp).background(Primary.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = iconVector, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = label, fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Invited Email Card — Warning token badge ──────────────────────────────────

@Composable
fun InvitedEmailCard(email: String, status: String, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = email, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Text(text = "Will receive invitation after store creation", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Warning.copy(0.10f) token replacing hardcoded Color(0xFFFFF9E6)
                Surface(shape = RoundedCornerShape(6.dp), color = Warning.copy(alpha = 0.10f)) {
                    Text(text = "Pending", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Warning, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = TextLight, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

// ── Date helper ───────────────────────────────────────────────────────────────

fun formatJoinedDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    return "Joined: ${sdf.format(Date(timestamp))}"
}