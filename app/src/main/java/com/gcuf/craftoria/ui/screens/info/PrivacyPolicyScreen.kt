package com.gcuf.craftoria.ui.screens.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Privacy Policy",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight= 18.sp
                        )
                        Text(
                            text = "Last updated: January 30, 2026",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight= 13.sp
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Hero card ─────────────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Primary.copy(alpha = 0.20f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Primary.copy(alpha = 0.08f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Craftoria is committed to protecting your privacy. This policy explains what data we collect, why we collect it, and how it is stored securely using Firebase and Cloudinary. Your trust is essential to our mission of empowering women artisans.",
                        fontSize = 13.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }

            // ── Sections ──────────────────────────────────────────────────────
            PrivacySection("1. Information We Collect", listOf(
                "Account Information: Full name, email address, phone number, and password (hashed) when you register via Email/Password or Google Sign-In.",
                "Seller Verification: A live selfie captured using Google ML Kit Face Detection, stored on Cloudinary for admin visual review.",
                "Profile Data: Profile photo uploaded by the user, stored on Cloudinary.",
                "Product Data: Product title, description, price, category, and images uploaded by verified sellers.",
                "Order Data: Order details, delivery address, buyer and seller IDs, and order status — stored in Firebase Firestore.",
                "Device & App Info: Device type and OS version collected for debugging and app improvement purposes only."
            ))
            PrivacySection("2. How We Use Your Information", listOf(
                "To create and manage your buyer or seller account.",
                "To verify seller identity and gender through manual admin review of ML Kit-captured selfies.",
                "To process orders and facilitate secure transactions between buyers and sellers.",
                "To operate the Smart Negotiation Bot using seller-defined pricing thresholds stored in Firestore.",
                "To manage Co-Seller Stores and collaborative selling features.",
                "To send order updates and platform notifications through Firebase.",
                "To display Learning Resources for women artisans who are new to online selling.",
                "To improve the app based on anonymous usage patterns."
            ))
            PrivacySection("3. Data Storage", listOf(
                "Firebase Authentication: Manages all user login credentials securely.",
                "Firebase Firestore: Stores all user profiles, product listings, orders, negotiations, Co-Seller Store data, and notifications in real-time.",
                "Cloudinary (Free Tier – 25GB): Stores all profile photos, product images, and seller verification selfies with access controls.",
                "Client-Side Logic: All business logic runs in Kotlin ViewModels on the Android app. No separate backend server is used."
            ))
            PrivacySection("4. Information Sharing", listOf(
                "We do not sell your personal information to any third parties.",
                "Buyer name and delivery address are shared with the seller only to fulfill the order.",
                "Seller verification selfies are accessed only by Craftoria admin for approval purposes.",
                "We use Firebase (Google) and Cloudinary as trusted third-party service providers under their respective privacy policies.",
                "We may disclose information if required by Pakistani law or to protect platform safety."
            ))
            PrivacySection("5. Your Rights", listOf(
                "Access: View your personal information anytime from your Profile screen.",
                "Edit: Update your name and profile photo from Profile → Edit.",
                "Delete: Permanently delete your account and all associated data from Profile → Delete Account.",
                "Notification Control: Manage notification preferences from Profile → Notification Preferences."
            ))
            PrivacySection("6. Seller Verification & ML Kit", listOf(
                "Craftoria uses Google ML Kit Face Detection (on-device) to capture a live selfie during seller registration.",
                "The selfie is used only for admin visual review to confirm the seller is a real woman artisan.",
                "Selfies are stored securely on Cloudinary and are never used for automated gender detection.",
                "Good lighting conditions are recommended for accurate face detection during the verification process."
            ))
            PrivacySection("7. Platform Scope & Limitations", listOf(
                "Craftoria is an Android-only platform (no iOS version).",
                "Real payment gateway integration is not included — only Cash on Delivery is supported in the current version.",
                "No real-time GPS or courier tracking is implemented.",
                "The platform is currently English-only."
            ))
            PrivacySection("8. Children's Privacy", listOf(
                "Craftoria is intended for adult women artisans and general buyers aged 13 and above.",
                "We do not knowingly collect data from children under 13.",
                "If you believe a child's data has been submitted, contact us immediately."
            ))
            PrivacySection("9. Changes to This Policy", listOf(
                "This Privacy Policy may be updated as the platform evolves.",
                "Users will be notified of significant changes via in-app notification.",
                "Continued use of Craftoria after updates means you accept the revised policy."
            ))

            // ── Contact card ──────────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "For privacy-related inquiries, contact us at privacy@craftoria.pk",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun PrivacySection(title: String, points: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                points.forEach { point ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(Primary, androidx.compose.foundation.shape.CircleShape)
                                .padding(top = 6.dp)
                        )
                        Text(
                            text = point,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 19.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}