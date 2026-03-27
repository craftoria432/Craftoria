package com.gcuf.craftoria.ui.screens.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.HelpOutline
import com.gcuf.craftoria.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Help & Support",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight= 16.sp
                        )
                        Text(
                            text = "Find answers or contact us",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            lineHeight= 12.sp
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
        },
        containerColor = BackgroundSecondary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Hero Card ─────────────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Primary.copy(alpha = 0.08f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "How can we help you?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Craftoria is a digital marketplace for women handicraft entrepreneurs. Find answers below or contact our team.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 19.sp
                    )
                }
            }

            // ── Buyer FAQs ────────────────────────────────────────────────────
            SectionLabel("For Buyers")

            FaqItem("How do I browse and buy products?",
                "Open the Home screen to explore authentic handicraft products from verified women sellers. Tap any product to view details, photos, and seller information. Tap 'Add to Cart' to add items, then go to Cart and tap 'Checkout' to place your order.")
            FaqItem("What payment methods are accepted?",
                "Craftoria currently supports Cash on Delivery (COD) for all orders. Payment is collected when your order is delivered. Future versions will include online payment gateways.")
            FaqItem("How do I track my order?",
                "Go to Profile → My Orders to view all your orders. Each order shows real-time status updates: Processing, Shipped, Out for Delivery, and Delivered. Tap 'Track Order' to see detailed timeline.")
            FaqItem("Can I negotiate the price?",
                "Yes! Craftoria features a Smart Negotiation Bot. On any product page, tap 'Make an Offer' and enter your desired price. If your offer meets the seller's threshold, it's auto-accepted. Otherwise, the seller reviews it manually within 24 hours.")
            FaqItem("How do I cancel an order?",
                "Orders can be cancelled within 24 hours of placement if the seller hasn't started processing. Go to My Orders, select the order, and tap 'Cancel Order'. Refunds are processed within 5-7 business days.")
            FaqItem("How do I return a product?",
                "If you receive a damaged or defective product, contact our support team within 3 days of delivery with photos. Go to Help & Support → Email Support. Our team will review and guide you through the return process.")
            FaqItem("Can I rate sellers and products?",
                "Yes! After receiving an order, you can rate the seller and leave a review. Go to My Orders, select the completed order, and tap 'Rate Seller'. Your feedback helps other buyers and supports quality sellers.")
            FaqItem("How do I add items to my Wishlist?",
                "Tap the heart icon on any product card to add it to your Wishlist. Go to Profile → Wishlist to view all saved items. You can remove items or add them to cart from your Wishlist.")

            // ── Seller FAQs ───────────────────────────────────────────────────
            SectionLabel("For Sellers")

            FaqItem("How do I become a verified seller?",
                "Craftoria is exclusively for women artisans. Register with your email, select 'Seller' role, then go to Profile → Become a Seller. Take a live selfie using your front camera. Our admin team visually reviews it within 24-48 hours to confirm your identity and approve your account.")
            FaqItem("Why is selfie verification required?",
                "Craftoria uses Google ML Kit Face Detection to capture a live selfie during registration. This ensures only real women artisans can sell on our platform. The selfie is reviewed manually by our admin team—never used for automated detection.")
            FaqItem("How do I list a product?",
                "After verification approval, go to Seller Dashboard → Add Product. Enter product title, detailed description, price in PKR, category, and upload high-quality photos (stored on Cloudinary). Products appear on the marketplace immediately after approval.")
            FaqItem("What is a Co-Seller Store?",
                "Co-Seller Stores allow women artisans to collaborate and sell under a shared brand name. Go to Profile → My Co-Seller Stores to create a new store or join an existing one. All co-sellers share revenue based on their contribution percentage.")
            FaqItem("How do I manage my Co-Seller Store?",
                "Go to Seller Dashboard → Co-Seller Stores. You can invite other sellers, manage store members, view shared revenue, and monitor store performance. Store owners can set payment split percentages for each member.")
            FaqItem("What commission does Craftoria charge?",
                "Craftoria charges a platform fee of 5% per successful transaction to support platform maintenance and women empowerment initiatives. This keeps your earnings maximized.")
            FaqItem("How do I view my earnings?",
                "Go to Seller Dashboard → Payment History to see all completed orders and earnings. View detailed payment breakdowns, transaction dates, and payment status. Co-seller earnings are automatically calculated and split.")
            FaqItem("Where can I learn to sell online?",
                "Go to Seller Dashboard → Learning Resources for curated external tutorials specifically designed for women new to online selling. Topics include product photography, descriptions, customer service, and more.")
            FaqItem("How do I handle order cancellations?",
                "If a buyer cancels within 24 hours, the order is automatically cancelled. If you need to cancel after processing has started, go to Seller Orders, select the order, and tap 'Cancel Order' with a reason.")

            // ── Contact ───────────────────────────────────────────────────────
            SectionLabel("Contact Us")

            // Email support card
            Card(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("mailto:")
                        putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("support@craftoria.pk"))
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Support Request – Craftoria")
                        putExtra(android.content.Intent.EXTRA_TEXT, "Hi Craftoria Support,\n\nI need help with:\n\n")
                    }
                    if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
                },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF90CAF9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF1976D2).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text("Email Support", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0D47A1))
                        Text("support@craftoria.pk", fontSize = 12.sp, color = Color(0xFF1976D2))
                        Text("Response within 24 hours", fontSize = 11.sp, color = Color(0xFF1976D2), fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Support hours card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Support Hours",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    SupportHoursRow("Monday – Friday", "9:00 AM – 6:00 PM PKT")
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
                    SupportHoursRow("Saturday", "10:00 AM – 4:00 PM PKT")
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
                    SupportHoursRow("Sunday", "Closed")
                }
            }

            // Footer
            Text(
                text = "Craftoria v1.0.0 – Digital Marketplace for Women Handicraft Entrepreneurs",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun FaqItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = if (expanded) Primary.copy(alpha = 0.4f) else BorderColor
        ),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (expanded) {
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                Text(
                    text = answer,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundSecondary)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun SupportHoursRow(day: String, hours: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = day, fontSize = 13.sp, color = TextSecondary)
        Text(
            text = hours,
            fontSize = 13.sp,
            color = if (hours == "Closed") Error else TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}