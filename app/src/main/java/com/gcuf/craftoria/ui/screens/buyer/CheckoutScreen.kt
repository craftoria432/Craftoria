package com.gcuf.craftoria.ui.screens.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.data.model.DeliveryInfo
import com.gcuf.craftoria.ui.components.CraftoriaTextField
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.CartViewModel
import com.gcuf.craftoria.viewmodel.OrderState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    userId: String,
    userName: String,
    onBackClick: () -> Unit,
    onOrderSuccess: (String) -> Unit,
    onNavigateToTerms: () -> Unit = {},
    cartViewModel: CartViewModel = viewModel(),
    checkoutViewModel: com.gcuf.craftoria.viewmodel.CheckoutViewModel = viewModel()
) {
    val context = LocalContext.current
    val cartItems by cartViewModel.cartItems.collectAsState()
    val orderState by cartViewModel.orderState.collectAsState()

    val fullName by checkoutViewModel.fullName.collectAsState()
    val phoneNumber by checkoutViewModel.phoneNumber.collectAsState()
    val email by checkoutViewModel.email.collectAsState()
    val address by checkoutViewModel.address.collectAsState()
    val city by checkoutViewModel.city.collectAsState()
    val postalCode by checkoutViewModel.postalCode.collectAsState()
    val selectedPaymentMethod by checkoutViewModel.selectedPaymentMethod.collectAsState()
    val agreeToTerms by checkoutViewModel.agreeToTerms.collectAsState()

    val subtotal = remember(cartItems) { cartViewModel.getSubtotal() }
    val shipping = CartViewModel.SHIPPING_COST
    val total = remember(cartItems) { cartViewModel.getTotal() }
    val itemCount = cartItems.sumOf { it.quantity }

    LaunchedEffect(orderState) {
        when (val state = orderState) {
            is OrderState.Success -> {
                // ✅ Navigate IMMEDIATELY to prevent showing empty cart data
                onOrderSuccess(state.orderId)
                
                // Then clear data and send email in background
                checkoutViewModel.clearCheckoutData()
                cartViewModel.resetOrderState()
                
                // Clear cart after navigation
                cartViewModel.clearCartAfterOrder()
                
                // Send confirmation email (non-blocking)
                try {
                    com.gcuf.craftoria.services.EmailService.sendOrderConfirmationEmail(
                        buyerEmail = email,
                        buyerName = fullName,
                        orderId = state.orderId,
                        totalPrice = total.toInt().toString(),
                        paymentMethod = selectedPaymentMethod,
                        deliveryAddress = "$address, $city $postalCode"
                    )
                } catch (e: Exception) {
                    android.util.Log.e("Email", "Failed to send email: ${e.message}")
                }
            }
            is OrderState.Error -> { android.widget.Toast.makeText(context, "Error: ${state.message}", android.widget.Toast.LENGTH_LONG).show() }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(text = "Checkout", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White, lineHeight = 16.sp)
                        Text(text = "Almost there! Review your order", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 12.sp)
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
        Box(modifier = Modifier.fillMaxSize().background(BackgroundSecondary).padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {

                CheckoutProgressBar()

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Delivery Information — 0.5.dp BorderColor border
                    CheckoutSectionCard(icon = { Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp)) }, title = "Delivery Information") {
                        CraftoriaTextField(value = fullName, onValueChange = { checkoutViewModel.updateFullName(it) }, label = "Full Name", placeholder = "Enter your full name", modifier = Modifier.padding(bottom = 12.dp))
                        CraftoriaTextField(value = phoneNumber, onValueChange = { checkoutViewModel.updatePhoneNumber(it) }, label = "Phone Number", placeholder = "+92 300 1234567", keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone, modifier = Modifier.padding(bottom = 12.dp))
                        CraftoriaTextField(value = email, onValueChange = { checkoutViewModel.updateEmail(it) }, label = "Email Address", placeholder = "your.email@example.com", keyboardType = androidx.compose.ui.text.input.KeyboardType.Email, modifier = Modifier.padding(bottom = 12.dp))
                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                            Text(text = "Complete Address", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary, letterSpacing = 0.4.sp, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedTextField(value = address, onValueChange = { checkoutViewModel.updateAddress(it) }, placeholder = { Text(text = "House/Street/Area", fontSize = 14.sp, color = TextLight) }, minLines = 3, maxLines = 3, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BorderColor), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CraftoriaTextField(value = city, onValueChange = { checkoutViewModel.updateCity(it) }, label = "City", placeholder = "City", modifier = Modifier.weight(1f))
                            Column(modifier = Modifier.width(100.dp)) {
                                Text(text = "Postal Code", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary, letterSpacing = 0.4.sp, modifier = Modifier.padding(bottom = 8.dp))
                                OutlinedTextField(value = postalCode, onValueChange = { newValue -> if (newValue.all { it.isDigit() } && newValue.length <= 5) checkoutViewModel.updatePostalCode(newValue) }, placeholder = { Text(text = "51310", fontSize = 14.sp, color = TextLight) }, singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = BorderColor), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }

                    // Payment Method — 0.5.dp BorderColor border
                    CheckoutSectionCard(icon = { Icon(imageVector = Icons.Default.Payment, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp)) }, title = "Payment Method") {
                        val paymentMethods = listOf(PaymentMethodOption("Debit/Credit Card", Icons.Default.CreditCard), PaymentMethodOption("Easypaisa", Icons.Default.AccountBalance), PaymentMethodOption("JazzCash", Icons.Default.AccountBalance), PaymentMethodOption("Cash on Delivery", Icons.Default.Money))
                        paymentMethods.forEach { method -> PaymentOptionWithIcon(text = method.name, icon = method.icon, isSelected = selectedPaymentMethod == method.name, onClick = { checkoutViewModel.updatePaymentMethod(method.name) }) }
                        Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(16.dp))
                                Text(text = "Payment in test mode for FYP project", fontSize = 12.sp, color = Color(0xFF1976D2), lineHeight = 18.sp)
                            }
                        }
                    }

                    // Order Summary — tinted, 0.5.dp Primary.copy(0.15f) border
                    CheckoutSectionCard(icon = { Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp)) }, title = "Order Summary", contentBg = Primary.copy(alpha = 0.03f), tinted = true) {
                        val uniqueSellers = cartItems.map { it.product.sellerId }.distinct().size
                        val totalShipping = CartViewModel.SHIPPING_COST * uniqueSellers
                        CheckoutSummaryRow("Items ($itemCount)", "PKR ${subtotal.toInt()}")
                        Spacer(modifier = Modifier.height(10.dp))
                        if (uniqueSellers > 1) {
                            repeat(uniqueSellers) { index ->
                                CheckoutSummaryRow(label = "Shipping — Seller ${index + 1}", value = "PKR ${CartViewModel.SHIPPING_COST.toInt()}")
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        } else {
                            CheckoutSummaryRow("Shipping", "PKR ${shipping.toInt()}")
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Primary.copy(alpha = 0.15f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Total", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "PKR ${(subtotal + totalShipping).toInt()}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary)
                        }
                    }

                    // Terms
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                        Checkbox(checked = agreeToTerms, onCheckedChange = { checkoutViewModel.updateAgreeToTerms(it) }, colors = CheckboxDefaults.colors(checkedColor = Primary, uncheckedColor = BorderColor))
                        Text(
                            text = androidx.compose.ui.text.buildAnnotatedString {
                                append("I agree to the ")
                                withStyle(style = androidx.compose.ui.text.SpanStyle(color = Primary, fontWeight = FontWeight.SemiBold)) { append("terms and conditions") }
                            },
                            fontSize = 14.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 2.dp).clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onNavigateToTerms() }
                        )
                    }
                }
            }

            // Fixed checkout button
            Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), color = Color.White, shadowElevation = 16.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = {
                            val isValid = validateForm(fullName, phoneNumber, email, address, city, postalCode, agreeToTerms)
                            if (!isValid) { android.widget.Toast.makeText(context, "Please fill all fields correctly. Postal code must be 5 digits.", android.widget.Toast.LENGTH_SHORT).show(); return@Button }
                            val deliveryInfo = checkoutViewModel.getDeliveryInfo()
                            cartViewModel.placeOrder(userId, userName, deliveryInfo, selectedPaymentMethod)
                        },
                        enabled = orderState !is OrderState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp).background(brush = Brush.horizontalGradient(listOf(Primary, PrimaryLight)), shape = RoundedCornerShape(14.dp))
                    ) {
                        if (orderState is OrderState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing...", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        } else {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Confirm & Place Order", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White, letterSpacing = 0.3.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckoutProgressBar() {
    Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 32.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        ProgressStep(label = "Cart", number = 1, isDone = true, isActive = false)
        ProgressLine(filled = true)
        ProgressStep(label = "Checkout", number = 2, isDone = false, isActive = true)
        ProgressLine(filled = false)
        ProgressStep(label = "Confirm", number = 3, isDone = false, isActive = false)
    }
}

@Composable
private fun ProgressStep(label: String, number: Int, isDone: Boolean, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(28.dp).background(color = when { isDone || isActive -> Primary; else -> BackgroundSecondary }, shape = CircleShape).then(if (!isDone && !isActive) Modifier.clip(CircleShape) else Modifier), contentAlignment = Alignment.Center) {
            if (isDone) Text("✓", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
            else Text(text = number.toString(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (isActive) Color.White else TextLight)
        }
        Text(text = label, fontSize = 10.sp, fontWeight = if (isActive || isDone) FontWeight.SemiBold else FontWeight.Normal, color = if (isActive || isDone) Primary else TextLight)
    }
}

@Composable
private fun ProgressLine(filled: Boolean) {
    Box(modifier = Modifier.width(48.dp).height(2.dp).padding(bottom = 14.dp).background(if (filled) Primary else BorderColor))
}

@Composable
private fun CheckoutSectionCard(icon: @Composable () -> Unit, title: String, iconBg: Color = Primary.copy(alpha = 0.10f), contentBg: Color = Color.White, tinted: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (tinted) androidx.compose.foundation.BorderStroke(0.5.dp, Primary.copy(alpha = 0.15f))
        else androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().background(if (tinted) Primary.copy(alpha = 0.03f) else Color.White).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(32.dp).background(iconBg, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { icon() }
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
        Column(modifier = Modifier.fillMaxWidth().background(contentBg).padding(16.dp), content = content)
    }
}

@Composable
private fun CheckoutSummaryRow(label: String, value: String, isDiscount: Boolean = false, color: Color = TextPrimary, fontWeight: FontWeight = FontWeight.Normal) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, fontSize = 14.sp, color = TextSecondary)
        Text(text = value, fontSize = 14.sp, fontWeight = fontWeight, color = if (isDiscount) Success else color)
    }
}

@Composable
fun PaymentOption(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFFFF5F8) else Color.White), border = androidx.compose.foundation.BorderStroke(width = if (isSelected) 1.5.dp else 0.5.dp, color = if (isSelected) Primary else BorderColor), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(11.dp)) {
            RadioButton(selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = Primary))
            Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isSelected) Primary else TextPrimary)
            if (isSelected) {
                Spacer(modifier = Modifier.weight(1f))
                Surface(color = Primary, shape = RoundedCornerShape(6.dp)) { Text(text = "Selected", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)) }
            }
        }
    }
}

@Composable
fun PaymentOptionWithIcon(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFFFF5F8) else Color.White), border = androidx.compose.foundation.BorderStroke(width = if (isSelected) 1.5.dp else 0.5.dp, color = if (isSelected) Primary else BorderColor), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(12.dp)) {
            RadioButton(selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = Primary))
            Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) Primary else TextSecondary, modifier = Modifier.size(20.dp))
            Text(text = text, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium, color = if (isSelected) Primary else TextPrimary)
            if (isSelected) {
                Spacer(modifier = Modifier.weight(1f))
                Surface(color = Primary, shape = RoundedCornerShape(6.dp)) { Text(text = "Selected", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) }
            }
        }
    }
}

data class PaymentMethodOption(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

fun validateForm(fullName: String, phoneNumber: String, email: String, address: String, city: String, postalCode: String, agreeToTerms: Boolean): Boolean {
    when { fullName.isBlank() -> return false; phoneNumber.isBlank() -> return false; email.isBlank() -> return false; address.isBlank() -> return false; city.isBlank() -> return false; postalCode.length != 5 -> return false; !agreeToTerms -> return false }
    return true
}