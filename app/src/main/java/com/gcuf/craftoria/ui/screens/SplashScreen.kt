package com.gcuf.craftoria.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalInspectionMode
import com.gcuf.craftoria.ui.theme.Primary
import kotlinx.coroutines.delay
import com.gcuf.craftoria.ui.theme.CraftoriaTheme

@Composable
fun SplashScreen(
    onNavigateToNext: () -> Unit,
    isUserLoggedIn: Boolean
) {
    val isPreview = LocalInspectionMode.current

    var startAnimation by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "logoScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 300),
        label = "contentAlpha"
    )

    val badgeScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "badgeScale"
    )

    // Prevent preview crash by skipping navigation + delay
    LaunchedEffect(Unit) {
        startAnimation = true
        if (!isPreview) {
            delay(3000)
            onNavigateToNext()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF6F8),
                        Color(0xFFFFE5EC)
                    )
                )
            )
    ) {

        // Top Badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .size(35.dp)
                .scale(badgeScale),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.9f),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "00",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
        }

        // Center content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(150.dp)
                    .scale(logoScale),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {

                    // Emoji replaced for Preview stability
                    Text(
                        text = "LOGO",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Craftoria",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Primary,
                letterSpacing = 1.5.sp,
                modifier = Modifier.alpha(contentAlpha)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Empowering Women through Handicrafts",
                fontSize = 15.sp,
                color = Color(0xFF666666),
                modifier = Modifier.alpha(contentAlpha)
            )

            Spacer(modifier = Modifier.height(80.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.alpha(contentAlpha)
            ) {
                CircularProgressIndicator(
                    color = Primary,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(40.dp)
                )

                Text(
                    text = "Loading...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Version 1.0.0",
                fontSize = 12.sp,
                color = Color(0xFFAAAAAA)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "© 2024 Craftoria. All rights reserved.",
                fontSize = 11.sp,
                color = Color(0xFFAAAAAA)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    CraftoriaTheme {
        SplashScreen(
            onNavigateToNext = {},
            isUserLoggedIn = false
        )
    }
}

