package com.gcuf.craftoria.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.R
import androidx.compose.ui.platform.LocalInspectionMode
import com.gcuf.craftoria.ui.theme.Primary
import kotlinx.coroutines.delay
import com.gcuf.craftoria.ui.theme.CraftoriaTheme

// ─── Crosshatch pattern — matches HTML prototype ──────────────────────────────
// CSS used: repeating-linear-gradient(45deg, transparent 35px, #e91e63 1px)
//           repeating-linear-gradient(-45deg, transparent 35px, #e91e63 1px)
//           opacity: 0.12
//
// Key insight: for a 45° diagonal on a tall screen (h >> w) we must iterate
// starting well before x=0 so lines cover the full top-left & bottom-right corners.
// Range: from -(h) to +(w+h) ensures every corner is covered regardless of aspect ratio.
private fun DrawScope.drawCrosshatchPattern() {
    val lineColor = Color(0xFFE91E63).copy(alpha = 0.12f)
    val strokeWidth = 1.5f
    val step = 24.dp.toPx()   // gap between parallel diagonal lines

    val w = size.width
    val h = size.height

    // +45° lines: go from top-left to bottom-right
    // start.x ranges from -h to w+h so lines fully cover a tall screen
    var x = -h
    while (x <= w + h) {
        drawLine(
            color = lineColor,
            start = Offset(x, 0f),
            end   = Offset(x + h, h),
            strokeWidth = strokeWidth
        )
        x += step
    }

    // -45° lines: go from top-right to bottom-left
    // mirror: start at (x, 0) end at (x - h, h)
    x = -h
    while (x <= w + h) {
        drawLine(
            color = lineColor,
            start = Offset(x, 0f),
            end   = Offset(x - h, h),
            strokeWidth = strokeWidth
        )
        x += step
    }
}

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
            // Base gradient: #FFF6F8 → #FFE5EC (matches HTML)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF6F8),
                        Color(0xFFFFE5EC)
                    )
                )
            )
    ) {

        // ── Crosshatch pattern overlay ────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCrosshatchPattern()
        }

        // ── Center content — untouched from original ──────────────────────────
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
                    Image(
                        painter = painterResource(id = R.drawable.handmade_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(140.dp),
                        contentScale = ContentScale.Fit
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
                text = "© 2026 Craftoria. All rights reserved.",
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