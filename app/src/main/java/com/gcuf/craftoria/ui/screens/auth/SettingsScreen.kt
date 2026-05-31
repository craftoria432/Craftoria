package com.gcuf.craftoria.ui.screens.auth

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import com.gcuf.craftoria.ui.theme.ThemeType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.data.repository.ThemeRepository
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: User,
    onBackClick: () -> Unit,
    themeRepository: ThemeRepository? = null,
    themeManager: ThemeManager? = null
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Use composition locals for dynamic theme colors
    val primary = LocalPrimary.current
    val primaryLight = LocalPrimaryLight.current
    val borderColor = LocalBorderColor.current
    val textPrimary = LocalTextPrimary.current
    val backgroundSecondary = LocalBackgroundSecondary.current
    val textSecondary = LocalTextSecondary.current

    val themeViewModel = remember {
        if (themeRepository != null && themeManager != null) {
            ThemeViewModel(themeRepository, themeManager)
        } else {
            null
        }
    }

    val selectedTheme by themeViewModel?.selectedTheme?.collectAsState()
        ?: remember { mutableStateOf(ThemeType.ROSE) }
    val isLoading by themeViewModel?.isLoading?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val errorMessage by themeViewModel?.errorMessage?.collectAsState()
        ?: remember { mutableStateOf<String?>(null) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            themeViewModel?.clearError()
        }
    }

    LaunchedEffect(user.id) {
        themeViewModel?.loadUserTheme(user.id)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = backgroundSecondary,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Settings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 18.sp
                        )
                        Text(
                            text = "Personalise your experience",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 13.sp
                        )
                    }
                },
                navigationIcon = {
                    // Pill back button — consistent with every other screen in the project
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
                    brush = Brush.horizontalGradient(colors = listOf(primary, primaryLight))
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundSecondary)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Appearance / Theme section card ───────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                border = BorderStroke(0.5.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // Tinted gradient section header — matches ProfileScreen, CheckoutScreen, etc.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        primary.copy(alpha = 0.06f),
                                        primary.copy(alpha = 0.02f)
                                    )
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(primary.copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "Appearance",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                            Text(
                                text = "Choose your Craftoria theme",
                                fontSize = 10.sp,
                                color = textSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = borderColor, thickness = 0.5.dp)

                    // ── Three-column theme tile grid ──────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        // Rose tile
                        ThemeTile(
                            primaryColor   = Color(0xFFE91E63),
                            primaryLight   = Color(0xFFF06292),
                            label          = "Rose",
                            description    = "Pink theme",
                            isSelected     = selectedTheme == ThemeType.ROSE,
                            isLoading      = isLoading,
                            modifier       = Modifier.weight(1f),
                            onClick        = {
                                themeViewModel?.selectTheme(ThemeType.ROSE, user.id)
                                Log.d("SettingsScreen", "Rose theme selected")
                            }
                        )

                        // Ocean tile
                        ThemeTile(
                            primaryColor   = Color(0xFF0277BD),
                            primaryLight   = Color(0xFF29B6F6),
                            label          = "Ocean",
                            description    = "Blue theme",
                            isSelected     = selectedTheme == ThemeType.OCEAN,
                            isLoading      = isLoading,
                            modifier       = Modifier.weight(1f),
                            onClick        = {
                                themeViewModel?.selectTheme(ThemeType.OCEAN, user.id)
                                Log.d("SettingsScreen", "Ocean theme selected")
                            }
                        )

                        // Purple tile
                        ThemeTile(
                            primaryColor   = Color(0xFF9C27B0),
                            primaryLight   = Color(0xFFBA68C8),
                            label          = "Purple",
                            description    = "Purple theme",
                            isSelected     = selectedTheme == ThemeType.PURPLE,
                            isLoading      = isLoading,
                            modifier       = Modifier.weight(1f),
                            onClick        = {
                                themeViewModel?.selectTheme(ThemeType.PURPLE, user.id)
                                Log.d("SettingsScreen", "Purple theme selected")
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// ── Theme Tile ────────────────────────────────────────────────────────────────

@Composable
fun ThemeTile(
    primaryColor: Color,
    primaryLight: Color,
    label: String,
    description: String,
    isSelected: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = LocalBorderColor.current
    val textPrimary = LocalTextPrimary.current
    val textSecondary = LocalTextSecondary.current
    Surface(
        onClick = { if (!isLoading) onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) primaryColor.copy(alpha = 0.05f) else Color.White,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 0.5.dp,
            color = if (isSelected) primaryColor else borderColor
        ),
        modifier = modifier
    ) {
        Column {
            // Gradient swatch — full-width colour band at top of tile
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        Brush.linearGradient(listOf(primaryColor, primaryLight))
                    )
            ) {
                // Check indicator — white circle with tick, bottom-right of swatch
                if (isSelected && !isLoading) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
                            .size(18.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = primaryColor,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }

                // Loading spinner — bottom-right of swatch when saving
                if (isLoading && isSelected) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
            }

            // Label + description
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) primaryColor else textPrimary
                )
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = textSecondary
                )
            }
        }
    }
}

// ── ThemeOptionButton (kept for backward compat — not rendered in current UI) ─

@Composable
fun ThemeOptionButton(
    icon: @Composable () -> Unit,
    label: String,
    description: String,
    isSelected: Boolean = false,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val primary = LocalPrimary.current
    val borderColorLocal = LocalBorderColor.current
    val textPrimary = LocalTextPrimary.current
    val textSecondary = LocalTextSecondary.current
    val textLight = LocalTextLight.current
    
    val borderColor     = if (isSelected) primary else borderColorLocal
    val borderWidth     = if (isSelected) 1.5.dp  else 0.5.dp
    val backgroundColor = if (isSelected) primary.copy(alpha = 0.06f) else Color.White

    Surface(
        onClick = { if (!isLoading) onClick() },
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = textLight,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = primary
                )
            }
        }
    }
}