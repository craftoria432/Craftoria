package com.gcuf.craftoria.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

data class BannerItem(
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val accentColor: Color
)

@Composable
fun BannerCarousel(
    modifier: Modifier = Modifier,
    autoScrollDuration: Long = 4000L
) {
    val banners = listOf(
        BannerItem(
            title = "Featured\nProducts",
            subtitle = "Discover handcrafted treasures from top artisans",
            badge = "✦ TOP PICKS",
            icon = Icons.Default.Star,
            gradient = listOf(Primary, PrimaryLight, Color(0xFFF48FB1)),
            accentColor = Color.White
        ),
        BannerItem(
            title = "New\nArrivals",
            subtitle = "Fresh handmade designs just landed",
            badge = "✦ JUST IN",
            icon = Icons.Default.NewReleases,
            gradient = listOf(Primary.copy(alpha = 0.9f), PrimaryLight, Color(0xFFF06292)),
            accentColor = Color.White
        ),
        BannerItem(
            title = "Special\nOffers",
            subtitle = "Limited time deals on premium crafts",
            badge = "✦ SAVE NOW",
            icon = Icons.Default.LocalOffer,
            gradient = listOf(Primary, Color(0xFFEC407A), PrimaryLight),
            accentColor = Color.White
        )
    )

    var currentPage by remember { mutableStateOf(0) }
    var isAnimating by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val transitionProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(autoScrollDuration)
            if (!isAnimating) {
                isAnimating = true
                try {
                    transitionProgress.animateTo(
                        targetValue = -1f,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    )
                    currentPage = (currentPage + 1) % banners.size
                    transitionProgress.snapTo(1f)
                    transitionProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
                } finally {
                    isAnimating = false
                }
            }
        }
    }

    fun navigateTo(index: Int) {
        if (isAnimating || index == currentPage) return
        coroutineScope.launch {
            isAnimating = true
            try {
                val direction = if (index > currentPage) -1f else 1f
                transitionProgress.animateTo(targetValue = direction, animationSpec = tween(400, easing = FastOutSlowInEasing))
                currentPage = index
                transitionProgress.snapTo(-direction)
                transitionProgress.animateTo(targetValue = 0f, animationSpec = tween(350, easing = FastOutSlowInEasing))
            } finally {
                isAnimating = false
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(animation = tween(1800, easing = EaseInOutCubic), repeatMode = RepeatMode.Reverse),
        label = "glowAlpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(animation = tween(2200, easing = EaseInOutCubic), repeatMode = RepeatMode.Reverse),
        label = "glowScale"
    )
    val iconFloat by infiniteTransition.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(animation = tween(1600, easing = EaseInOutCubic), repeatMode = RepeatMode.Reverse),
        label = "iconFloat"
    )

    val slideOffset = transitionProgress.value * 180f
    val contentAlpha = 1f - transitionProgress.value.absoluteValue.coerceIn(0f, 1f) * 0.4f

    Column(modifier = modifier.fillMaxWidth().padding(top = 12.dp)) {

        // ── Banner Card — flat (0.dp elevation) + 0.5.dp BorderColor border ──
        // Changed: elevation 8.dp → 0.dp + explicit BorderStroke for system consistency
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .pointerInput(currentPage) {
                    var dragTotal = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onDragEnd = {
                            when {
                                dragTotal < -60f -> navigateTo((currentPage + 1) % banners.size)
                                dragTotal > 60f  -> navigateTo((currentPage - 1 + banners.size) % banners.size)
                            }
                        },
                        onHorizontalDrag = { _, delta -> dragTotal += delta }
                    )
                },
            shape = RoundedCornerShape(16.dp),
            // Flat card — consistent with entire project; no shadow, 0.5.dp border
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = banners[currentPage].gradient,
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
            ) {
                // ── Decorative blurred orb — top right ────────────────────────
                // Tightened: size 120.dp → 100.dp, alpha reduced for subtlety
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 32.dp, y = (-32).dp)
                        .scale(glowScale)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = glowAlpha * 0.22f))
                )

                // ── Decorative blurred orb — bottom left ──────────────────────
                // Tightened: size 80.dp → 70.dp, alpha reduced
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (-20).dp, y = 20.dp)
                        .scale(1f / glowScale)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = glowAlpha * 0.15f))
                )

                // ── Animated content ──────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .graphicsLayer { translationX = slideOffset; alpha = contentAlpha },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.wrapContentSize()
                        ) {
                            Text(
                                text = banners[currentPage].badge,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = banners[currentPage].title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            lineHeight = 26.sp,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = banners[currentPage].subtitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.95f),
                            lineHeight = 14.sp
                        )
                    }

                    // ── Floating icon bubble ───────────────────────────────────
                    Box(
                        modifier = Modifier.size(65.dp).graphicsLayer { translationY = iconFloat },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(65.dp)
                                .scale(glowScale)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f))
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(50.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color.White.copy(alpha = 0.30f))
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(imageVector = banners[currentPage].icon, contentDescription = banners[currentPage].title, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }

                // Page counter — bottom right
                Text(
                    text = "${currentPage + 1}/${banners.size}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ── Page indicators ───────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            banners.forEachIndexed { index, _ ->
                val isSelected = currentPage == index
                val dotWidth by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 6.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "dotWidth"
                )
                val dotAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.3f,
                    animationSpec = tween(300),
                    label = "dotAlpha"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = dotWidth, height = 6.dp)
                        .clip(CircleShape)
                        .alpha(dotAlpha)
                        .background(if (isSelected) Primary else Primary.copy(alpha = 0.30f))
                )
            }
        }
    }
}

private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)