package com.gcuf.craftoria.ui.screens.seller

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.data.model.VerificationStatus
import com.gcuf.craftoria.ui.components.CraftoriaTextField
import com.gcuf.craftoria.ui.screens.coseller.dashedBorder
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.AddProductViewModel
import com.gcuf.craftoria.viewmodel.ProductState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    user: User,
    onBackClick: () -> Unit,
    editProductId: String? = null,
    onSuccess: () -> Unit,
    addProductViewModel: AddProductViewModel = viewModel()
) {
    val context = LocalContext.current
    val isEditMode = editProductId != null

    val productState by addProductViewModel.productState.collectAsState()
    val selectedImages by addProductViewModel.selectedImages.collectAsState()
    val userStores by addProductViewModel.userStores.collectAsState()
    val isNegotiationEnabled by addProductViewModel.isNegotiationEnabled.collectAsState()

    var title by addProductViewModel.titleState
    var description by addProductViewModel.descriptionState
    var category by addProductViewModel.categoryState
    var price by addProductViewModel.priceState
    var stock by addProductViewModel.stockState
    var weightKg by addProductViewModel.weightKgState
    var selectedStoreId by addProductViewModel.selectedStoreIdState
    var minimumPrice by addProductViewModel.minimumPriceState
    var autoAcceptDiscount by addProductViewModel.autoAcceptDiscountState

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showDraftDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(editProductId) {
        if (editProductId != null) addProductViewModel.loadProductForEditing(editProductId)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> uris.forEach { uri -> addProductViewModel.addImage(uri) } }

    LaunchedEffect(user.id) { addProductViewModel.loadUserStores(user.id) }

    LaunchedEffect(productState) {
        when (val state = productState) {
            is ProductState.Success -> showSuccessDialog = true
            is ProductState.DraftSaved -> showDraftDialog = true
            is ProductState.Error -> {
                snackbarHostState.showSnackbar(message = state.message, duration = SnackbarDuration.Short)
                addProductViewModel.resetState()
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
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(
                            text = if (isEditMode) "Edit Product" else "Add New Product",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = if (isEditMode) "Update your listing" else "Fill in the details below",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color.White.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    brush = Brush.horizontalGradient(colors = listOf(Primary, PrimaryLight))
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Image Upload ──────────────────────────────────────────────────
            ImageUploadSection(
                selectedImages = selectedImages,
                onTakePhoto = { galleryLauncher.launch("image/*") },
                onChooseGallery = { galleryLauncher.launch("image/*") },
                onRemoveImage = { uri -> addProductViewModel.removeImage(uri) }
            )

            // ── Basic Information ─────────────────────────────────────────────
            AddProductSectionCard(title = "Basic Information", icon = Icons.Outlined.Edit) {
                CraftoriaTextField(value = title, onValueChange = { title = it }, label = "Product Title *", placeholder = "e.g., Hand-Embroidered Wall Hanging")
                Spacer(modifier = Modifier.height(12.dp))
                Column {
                    Text(text = "Description *", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(
                        value = description, onValueChange = { description = it },
                        placeholder = { Text("Describe your product, materials used, dimensions, etc.", fontSize = 13.sp, color = TextSecondary) },
                        minLines = 4, maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BorderColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                CategoryDropdown(selectedCategory = category, onCategorySelected = { category = it })
            }

            // ── Pricing & Stock ───────────────────────────────────────────────
            AddProductSectionCard(title = "Pricing & Stock", icon = Icons.Outlined.PriceChange) {
                CraftoriaTextField(value = price, onValueChange = { price = it }, label = "Price (PKR) *", placeholder = "0", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                Spacer(modifier = Modifier.height(12.dp))
                CraftoriaTextField(value = stock, onValueChange = { stock = it }, label = "Stock Quantity *", placeholder = "How many units available?", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            }

            // ── Store & Shipping ──────────────────────────────────────────────
            if (userStores.isNotEmpty()) {
                AddProductSectionCard(title = "Store & Shipping", icon = Icons.Filled.Storefront) {
                    StoreDropdown(stores = userStores, selectedStoreId = selectedStoreId, onStoreSelected = { selectedStoreId = it })
                    Spacer(modifier = Modifier.height(12.dp))
                    CraftoriaTextField(value = weightKg, onValueChange = { weightKg = it }, label = "Product Weight (kg)", placeholder = "For shipping calculation", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                }
            } else {
                AddProductSectionCard(title = "Shipping", icon = Icons.Outlined.LocalShipping) {
                    CraftoriaTextField(value = weightKg, onValueChange = { weightKg = it }, label = "Product Weight (kg)", placeholder = "For shipping calculation", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                }
            }

            // ── Negotiation ───────────────────────────────────────────────────
            NegotiationCard(
                isEnabled = isNegotiationEnabled,
                onToggle = { addProductViewModel.toggleNegotiation(it) },
                minimumPrice = minimumPrice,
                onMinimumPriceChange = { minimumPrice = it },
                autoAcceptDiscount = autoAcceptDiscount,
                onAutoAcceptDiscountChange = { autoAcceptDiscount = it },
                addProductViewModel = addProductViewModel,
                price = price
            )

            // ── Specifications ────────────────────────────────────────────────
            SpecificationsSection(
                specifications = addProductViewModel.specifications.collectAsState().value,
                onAddSpecification = { key, value -> addProductViewModel.addSpecification(key, value) },
                onRemoveSpecification = { key -> addProductViewModel.removeSpecification(key) }
            )

            // ── Action Buttons ────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        addProductViewModel.saveDraft(
                            context = context, title = title, description = description, category = category,
                            price = price.toDoubleOrNull() ?: 0.0, stock = stock.toIntOrNull() ?: 0,
                            weightKg = weightKg.toDoubleOrNull() ?: 0.0, coSellerStoreId = selectedStoreId,
                            minimumPrice = minimumPrice.toDoubleOrNull() ?: 0.0,
                            autoAcceptDiscount = autoAcceptDiscount.toIntOrNull() ?: 0,
                            sellerId = user.id, sellerName = user.name,
                            sellerVerified = user.verificationStatus == VerificationStatus.APPROVED
                        )
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(text = "Save as Draft", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }

                Button(
                    onClick = {
                        if (isEditMode) {
                            addProductViewModel.updateProduct(
                                context = context, title = title, description = description, category = category,
                                price = price.toDoubleOrNull() ?: 0.0, stock = stock.toIntOrNull() ?: 0,
                                weightKg = weightKg.toDoubleOrNull() ?: 0.0, coSellerStoreId = selectedStoreId,
                                minimumPrice = minimumPrice.toDoubleOrNull() ?: 0.0,
                                autoAcceptDiscount = autoAcceptDiscount.toIntOrNull() ?: 0,
                                sellerId = user.id, sellerName = user.name
                            )
                        } else {
                            addProductViewModel.publishProduct(
                                context = context, title = title, description = description, category = category,
                                price = price.toDoubleOrNull() ?: 0.0, stock = stock.toIntOrNull() ?: 0,
                                weightKg = weightKg.toDoubleOrNull() ?: 0.0, coSellerStoreId = selectedStoreId,
                                minimumPrice = minimumPrice.toDoubleOrNull() ?: 0.0,
                                autoAcceptDiscount = autoAcceptDiscount.toIntOrNull() ?: 0,
                                sellerId = user.id, sellerName = user.name,
                                sellerVerified = user.verificationStatus == VerificationStatus.APPROVED
                            )
                        }
                    },
                    enabled = productState !is ProductState.Loading && productState !is ProductState.DraftSaved,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (productState is ProductState.Loading && productState !is ProductState.DraftSaved) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Text("Publishing...", fontSize = 13.sp, color = Color.White)
                            }
                        } else {
                            Text(text = if (isEditMode) "Update Product" else "Publish Product", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    if (showSuccessDialog) {
        SuccessDialog(onDismiss = { showSuccessDialog = false; addProductViewModel.clearForm(); onSuccess() })
    }
    if (showDraftDialog) {
        DraftSavedDialog(
            onContinueEditing = { showDraftDialog = false; addProductViewModel.resetState() },
            onViewDrafts = { showDraftDialog = false; addProductViewModel.clearForm(); onSuccess() }
        )
    }
}

// ── Section Card ──────────────────────────────────────────────────────────────

@Composable
fun AddProductSectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Primary.copy(alpha = 0.04f)).padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(modifier = Modifier.size(28.dp).background(Primary.copy(alpha = 0.10f), RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
            }
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), content = content)
    }
}

// ── Image Upload ──────────────────────────────────────────────────────────────

@Composable
fun ImageUploadSection(selectedImages: List<Uri>, onTakePhoto: () -> Unit, onChooseGallery: () -> Unit, onRemoveImage: (Uri) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().dashedBorder(1.5.dp, Primary.copy(alpha = 0.25f), 12.dp).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(54.dp).background(Primary.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = Primary, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Add Product Images", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = "(Maximum ${AddProductViewModel.MAX_IMAGES})", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(onClick = onTakePhoto, modifier = Modifier.weight(1f).height(36.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary), border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor), shape = RoundedCornerShape(20.dp)) {
                    Icon(imageVector = Icons.Outlined.CameraAlt, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Take Photo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(onClick = onChooseGallery, modifier = Modifier.weight(1f).height(36.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary), border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor), shape = RoundedCornerShape(20.dp)) {
                    Icon(imageVector = Icons.Outlined.PhotoLibrary, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Gallery", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    if (selectedImages.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(selectedImages) { uri -> ImageThumbnail(uri = uri, onRemove = { onRemoveImage(uri) }) }
        }
    }
}

@Composable
fun ImageThumbnail(uri: Uri, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(68.dp)) {
        Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(8.dp), color = BackgroundSecondary, border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)) {
            AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Surface(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-5).dp).size(18.dp), shape = CircleShape, color = Error) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
    }
}

// ── Category Dropdown ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(selectedCategory: String, onCategorySelected: (String) -> Unit) {
    val categories = com.gcuf.craftoria.utils.ProductCategories.ALL
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(text = "Category *", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = if (selectedCategory.isEmpty()) "Select Category" else selectedCategory,
                onValueChange = {}, readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BorderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Color.White) {
                categories.forEach { cat -> DropdownMenuItem(text = { Text(cat, fontSize = 13.sp) }, onClick = { onCategorySelected(cat); expanded = false }) }
            }
        }
    }
}

// ── Store Dropdown ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDropdown(stores: List<com.gcuf.craftoria.data.model.CoSellerStore>, selectedStoreId: String, onStoreSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedStore = stores.find { it.id == selectedStoreId }
    val displayText = selectedStore?.storeName ?: "None – Personal Listing Only"
    Column {
        Text(text = "Link to Co-Seller Store (Optional)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = displayText, onValueChange = {}, readOnly = true,
                leadingIcon = { Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BorderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = Color.White) {
                DropdownMenuItem(text = { Text("None – Personal Listing Only", fontSize = 13.sp) }, onClick = { onStoreSelected(""); expanded = false })
                stores.forEach { store -> DropdownMenuItem(text = { Text(store.storeName, fontSize = 13.sp) }, onClick = { onStoreSelected(store.id); expanded = false }) }
            }
        }
        Text(text = "Select a store to also list this product there", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 5.dp))
    }
}

// ── Negotiation Card ──────────────────────────────────────────────────────────

@Composable
fun NegotiationCard(
    isEnabled: Boolean, onToggle: (Boolean) -> Unit,
    minimumPrice: String, onMinimumPriceChange: (String) -> Unit,
    autoAcceptDiscount: String, onAutoAcceptDiscountChange: (String) -> Unit,
    addProductViewModel: com.gcuf.craftoria.viewmodel.AddProductViewModel, price: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isEnabled) Primary.copy(alpha = 0.30f) else BorderColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(if (isEnabled) Primary.copy(alpha = 0.04f) else Color.White).padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(modifier = Modifier.size(28.dp).background(if (isEnabled) Primary.copy(alpha = 0.10f) else BackgroundSecondary, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Outlined.Handshake, contentDescription = null, tint = if (isEnabled) Primary else TextSecondary, modifier = Modifier.size(14.dp))
                }
                Text(text = "Price Negotiation", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            Switch(checked = isEnabled, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Success, uncheckedThumbColor = Color.White, uncheckedTrackColor = BorderColor))
        }
        if (isEnabled) {
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CraftoriaTextField(value = autoAcceptDiscount, onValueChange = onAutoAcceptDiscountChange, label = "Auto-Accept Discount (%)", placeholder = "Recommended: 10-15%", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                CraftoriaTextField(value = minimumPrice, onValueChange = onMinimumPriceChange, label = "Minimum Acceptable Price (PKR)", placeholder = "Recommended: 20-25% off original price", keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NegotiationPresetChip("Conservative", "10% auto", autoAcceptDiscount == "10", { addProductViewModel.setAutoAcceptDiscount("10", price) }, Modifier.weight(1f))
                    NegotiationPresetChip("Balanced", "15% auto", autoAcceptDiscount == "15", { addProductViewModel.setAutoAcceptDiscount("15", price) }, Modifier.weight(1f))
                    NegotiationPresetChip("Flexible", "20% auto", autoAcceptDiscount == "20", { addProductViewModel.setAutoAcceptDiscount("20", price) }, Modifier.weight(1f))
                }
                Surface(shape = RoundedCornerShape(8.dp), color = Primary.copy(alpha = 0.05f), border = androidx.compose.foundation.BorderStroke(0.5.dp, Primary.copy(alpha = 0.15f)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Outlined.Lightbulb, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                            Text(text = "How Negotiation Works:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Auto-Accept: Offers within this discount are accepted instantly\n• Manual Review: Offers between auto-accept and minimum go to you\n• Below Minimum: Offers below minimum price are rejected automatically",
                            fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NegotiationPresetChip(title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick, modifier = modifier.height(48.dp), shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Primary.copy(alpha = 0.06f) else Color.White,
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.dp else 0.5.dp, if (isSelected) Primary else BorderColor)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = subtitle, fontSize = 10.sp, color = Primary)
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
fun SuccessDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(20.dp),
        icon = {
            Box(modifier = Modifier.size(60.dp).background(Success.copy(alpha = 0.10f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(30.dp))
            }
        },
        title = { Text(text = "Published!", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = { Text(text = "Your product has been published successfully and is now live on the marketplace.", fontSize = 13.sp, color = TextSecondary, lineHeight = 20.sp) },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().height(44.dp)) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Primary, PrimaryLight)), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Text("View Products", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    )
}

@Composable
fun DraftSavedDialog(onContinueEditing: () -> Unit, onViewDrafts: () -> Unit) {
    AlertDialog(
        onDismissRequest = onContinueEditing, containerColor = Color.White, shape = RoundedCornerShape(20.dp),
        icon = {
            Box(modifier = Modifier.size(60.dp).background(Primary.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Outlined.Save, contentDescription = null, tint = Primary, modifier = Modifier.size(30.dp))
            }
        },
        title = { Text(text = "Draft Saved!", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = { Text(text = "Your product has been saved as a draft. You can continue editing it later.", fontSize = 13.sp, color = TextSecondary, lineHeight = 20.sp) },
        confirmButton = { Button(onClick = onViewDrafts, colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = RoundedCornerShape(10.dp)) { Text("View Drafts", fontWeight = FontWeight.SemiBold) } },
        dismissButton = { OutlinedButton(onClick = onContinueEditing, border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor), shape = RoundedCornerShape(10.dp)) { Text("Continue Editing", color = TextSecondary) } }
    )
}

// ── Specifications ────────────────────────────────────────────────────────────

@Composable
fun SpecificationsSection(specifications: Map<String, String>, onAddSpecification: (String, String) -> Unit, onRemoveSpecification: (String) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Primary.copy(alpha = 0.04f)).padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(modifier = Modifier.size(28.dp).background(Primary.copy(alpha = 0.10f), RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Outlined.Checklist, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Product Specifications", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(text = "Dimensions, material, finish, etc.", fontSize = 11.sp, color = TextSecondary)
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (specifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().dashedBorder(1.dp, BorderColor, 8.dp).padding(20.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Outlined.Lightbulb, contentDescription = null, tint = TextLight, modifier = Modifier.size(26.dp))
                        Text(text = "No specifications added yet", fontSize = 13.sp, color = TextSecondary)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    specifications.forEach { (key, value) -> SpecificationItem(label = key, value = value, onRemove = { onRemoveSpecification(key) }) }
                }
            }
            OutlinedButton(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth().height(40.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary), border = androidx.compose.foundation.BorderStroke(0.5.dp, Primary), shape = RoundedCornerShape(10.dp)) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Add Specification", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
    if (showAddDialog) {
        AddSpecificationDialog(onDismiss = { showAddDialog = false }, onAdd = { key, value -> onAddSpecification(key, value); showAddDialog = false })
    }
}

@Composable
fun SpecificationItem(label: String, value: String, onRemove: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = BackgroundSecondary, border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(top = 1.dp))
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Error, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
fun AddSpecificationDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var specKey by remember { mutableStateOf("") }
    var specValue by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val imeVisible = ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.ime()) == true
    BackHandler(enabled = imeVisible) { keyboardController?.hide(); focusManager.clearFocus() }
    val predefinedKeys = listOf("Dimensions", "Material", "Finish", "Weight", "Color", "Handmade", "Care Instructions", "Origin", "Style", "Pattern")
    AlertDialog(
        onDismissRequest = { keyboardController?.hide(); focusManager.clearFocus(); onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        modifier = Modifier.fillMaxWidth(0.92f).imePadding().navigationBarsPadding(),
        containerColor = Color.White, shape = RoundedCornerShape(20.dp),
        icon = {
            Box(modifier = Modifier.size(52.dp).background(Primary.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Outlined.Lightbulb, contentDescription = null, tint = Primary, modifier = Modifier.size(26.dp))
            }
        },
        title = { Text(text = "Add Specification", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            CompositionLocalProvider(LocalContentColor provides TextPrimary) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text(text = "Add product details to help buyers make informed decisions", fontSize = 13.sp, color = TextSecondary)
                    Column {
                        Text(text = "Specification Name *", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                        OutlinedTextField(
                            value = specKey, onValueChange = { specKey = it },
                            placeholder = { Text("e.g., Dimensions, Material", fontSize = 13.sp, color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BorderColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = Primary),
                            singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = TextPrimary)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Quick Select:", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(28.dp)) {
                            items(predefinedKeys) { key ->
                                Surface(onClick = { specKey = key }, shape = RoundedCornerShape(6.dp), color = if (specKey == key) Primary.copy(alpha = 0.10f) else BackgroundSecondary, border = androidx.compose.foundation.BorderStroke(0.5.dp, if (specKey == key) Primary else BorderColor)) {
                                    Text(text = key, fontSize = 11.sp, color = if (specKey == key) Primary else TextSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                    Column {
                        Text(text = "Value *", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                        OutlinedTextField(
                            value = specValue, onValueChange = { specValue = it },
                            placeholder = { Text("e.g., 8 x 5 x 4 inches", fontSize = 13.sp, color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BorderColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = Primary),
                            singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = TextPrimary)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (specKey.isNotBlank() && specValue.isNotBlank()) { keyboardController?.hide(); focusManager.clearFocus(); onAdd(specKey.trim(), specValue.trim()) } }, enabled = specKey.isNotBlank() && specValue.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = RoundedCornerShape(10.dp)) { Text("Add", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            OutlinedButton(onClick = { keyboardController?.hide(); focusManager.clearFocus(); onDismiss() }, border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor), shape = RoundedCornerShape(10.dp)) { Text("Cancel", color = TextSecondary) }
        }
    )
}