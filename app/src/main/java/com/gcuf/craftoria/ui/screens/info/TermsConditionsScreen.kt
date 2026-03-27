package com.gcuf.craftoria.ui.screens.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
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
fun TermsConditionsScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Terms & Conditions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight= 16.sp
                        )
                        Text(
                            text = "Last updated: January 30, 2026",
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
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "By using Craftoria, you agree to these terms. Please read them carefully. These terms govern your use of the platform as a buyer or seller.",
                        fontSize = 13.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }

            // ── Sections ──────────────────────────────────────────────────────
            TermsSection("1. About Craftoria",
                "Craftoria is a digital marketplace exclusively for women handicraft entrepreneurs, developed as a Final Year Project at Government College University Faisalabad (GCUF). The platform includes an Android App for sellers and buyers, and a Web Admin Dashboard for administrators.")
            TermsSection("2. Women-Only Seller Policy",
                "Craftoria is a women-only selling platform. Only verified women artisans may list and sell products. Any attempt to bypass this policy will result in immediate account termination. Buyers of any gender may use the platform to purchase products.")
            TermsSection("3. Seller Verification",
                "All sellers must complete identity verification before listing products. Verification requires a live selfie captured using Google ML Kit Face Detection. The selfie is reviewed manually by our admin team within 24–48 hours to visually confirm the seller's identity and gender. Craftoria reserves the right to reject any verification that does not meet our criteria.")
            TermsSection("4. Buyer Responsibilities",
                "Buyers agree to provide accurate delivery information and make payments in good faith. Buyers must inspect products upon receipt and report any issues within 3 days of delivery. Fraudulent orders or chargebacks may result in account suspension.")
            TermsSection("5. Seller Responsibilities",
                "Sellers must maintain accurate product listings with honest descriptions and real photos. Sellers are responsible for fulfilling orders on time. Sellers must not list counterfeit, illegal, or mass-produced goods. Commission fees of 5% or less apply to each transaction.")
            TermsSection("6. Smart Negotiation Bot",
                "Craftoria includes a Rule-Based Smart Negotiation Bot. Offers above the seller's defined threshold are automatically accepted. Offers below the threshold are forwarded to the seller for manual review. Buyers and sellers agree to respect negotiation outcomes facilitated by the platform.")
            TermsSection("7. Co-Seller Stores",
                "Sellers may create or join Co-Seller Stores for collaborative selling under a shared brand. All co-sellers are equally responsible for the store's listings and conduct. Craftoria may remove a Co-Seller Store that violates platform policies.")
            TermsSection("8. Payments & Commission",
                "Craftoria charges a platform commission of 5% or less per successful transaction. This fee supports platform maintenance and women empowerment initiatives. All prices are listed in Pakistani Rupees (PKR). Real payment gateway integration is in sandbox mode only in the current version.")
            TermsSection("9. Returns & Refunds",
                "Buyers may request a return within 3 days of delivery for items that are damaged, defective, or significantly different from the listing. Refund decisions are made by the admin team after reviewing the case. Approved refunds are processed within 5–7 business days.")
            TermsSection("10. Prohibited Activities",
                "Users must not: sell non-handmade or mass-produced goods, impersonate other users, post fake reviews, attempt to bypass seller verification, harass other users, or engage in any form of financial fraud. Violations result in permanent account removal.")
            TermsSection("11. Data & Privacy",
                "All user data is stored securely on Firebase. Verification selfies and product images are stored on Cloudinary. Craftoria does not sell user data to third parties. Please refer to our Privacy Policy for full details.")
            TermsSection("12. Platform Limitations",
                "Craftoria does not currently support: real-time GPS order tracking, live payment gateway, iOS app, automated gender detection, video hosting, multi-language support, or individual seller analytics dashboards.")
            TermsSection("13. Governing Law",
                "These terms are governed by the laws of Pakistan. Any disputes shall be resolved under the jurisdiction of the courts of Faisalabad, Punjab, Pakistan.")
            TermsSection("14. Changes to Terms",
                "Craftoria may update these Terms from time to time. Users will be notified via in-app notification. Continued use of the platform after changes constitutes acceptance of the updated terms.")

            // ── Contact card ──────────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "For questions about these terms, contact us at legal@craftoria.pk",
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
private fun TermsSection(title: String, body: String) {
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
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}