package com.gcuf.craftoria.ui.screens.learning

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.gcuf.craftoria.ui.components.StandardizedOutlinedTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gcuf.craftoria.data.model.LearningCategory
import com.gcuf.craftoria.data.model.Tutorial
import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.ui.components.StandardizedOutlinedTextFieldCompact
import com.gcuf.craftoria.ui.theme.*
import com.gcuf.craftoria.viewmodel.LearningState
import com.gcuf.craftoria.viewmodel.LearningViewModel

data class IconEntry(
    val icon: ImageVector,
    val label: String,
    val color: Color
)

val ICON_MAP = mapOf(
    "school"    to IconEntry(Icons.Outlined.School,          "School",    Color(0xFF667EEA)),
    "palette"   to IconEntry(Icons.Outlined.Palette,         "Palette",   Color(0xFFE91E63)),
    "brush"     to IconEntry(Icons.Outlined.Brush,           "Brush",     Color(0xFFF06292)),
    "stories"   to IconEntry(Icons.Outlined.AutoStories,     "Stories",   Color(0xFF9C27B0)),
    "book"      to IconEntry(Icons.Outlined.MenuBook,        "Book",      Color(0xFF3F51B5)),
    "library"   to IconEntry(Icons.Outlined.LocalLibrary,    "Library",   Color(0xFF2196F3)),
    "idea"      to IconEntry(Icons.Outlined.EmojiObjects,    "Idea",      Color(0xFFFF9800)),
    "lightbulb" to IconEntry(Icons.Outlined.Lightbulb,       "Lightbulb", Color(0xFFFFC107)),
    "star"      to IconEntry(Icons.Outlined.Star,            "Star",      Color(0xFFFFD600)),
    "favorite"  to IconEntry(Icons.Outlined.FavoriteBorder,  "Favorite",  Color(0xFFF44336)),
    "build"     to IconEntry(Icons.Outlined.Build,           "Build",     Color(0xFF795548)),
    "code"      to IconEntry(Icons.Outlined.Code,            "Code",      Color(0xFF607D8B)),
    "design"    to IconEntry(Icons.Outlined.DesignServices,  "Design",    Color(0xFFE91E63)),
    "camera"    to IconEntry(Icons.Outlined.CameraAlt,       "Camera",    Color(0xFF00BCD4)),
    "music"     to IconEntry(Icons.Outlined.MusicNote,       "Music",     Color(0xFF9C27B0)),
    "handyman"  to IconEntry(Icons.Outlined.Handyman,        "Handyman",  Color(0xFFFF5722)),
    "layers"    to IconEntry(Icons.Outlined.Layers,          "Layers",    Color(0xFF009688)),
    "category"  to IconEntry(Icons.Outlined.Category,        "Category",  Color(0xFF673AB7)),
    "play"      to IconEntry(Icons.Outlined.PlayCircle,      "Play",      Color(0xFF4CAF50)),
    "video"     to IconEntry(Icons.Outlined.OndemandVideo,   "Video",     Color(0xFFF44336)),
    "draw"      to IconEntry(Icons.Outlined.Draw,            "Draw",      Color(0xFFFF9800)),
    "article"   to IconEntry(Icons.Outlined.Article,         "Article",   Color(0xFF2196F3))
)

const val DEFAULT_CAT_ICON = "school"
const val DEFAULT_TUT_ICON = "article"

fun getIconEntry(iconKey: String, isCategory: Boolean = true): IconEntry =
    ICON_MAP[iconKey] ?: ICON_MAP[if (isCategory) DEFAULT_CAT_ICON else DEFAULT_TUT_ICON]!!

fun iconGradient(iconKey: String, isCategory: Boolean = true): Brush {
    val entry = getIconEntry(iconKey, isCategory)
    return Brush.linearGradient(
        colors = listOf(entry.color.copy(alpha = 0.80f), entry.color.copy(alpha = 0.50f))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningResourcesScreen(
    user: User,
    onBackClick: () -> Unit,
    learningViewModel: LearningViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by learningViewModel.uiState.collectAsState()
    val categories by learningViewModel.categories.collectAsState()
    val expandedCategories by learningViewModel.expandedCategories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showExternalLinkDialog by remember { mutableStateOf(false) }
    var pendingUrl by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(user.id) { learningViewModel.loadCategories(user.id) }
    LaunchedEffect(uiState) {
        if (uiState is LearningState.Error) {
            snackbarHostState.showSnackbar(
                (uiState as LearningState.Error).message,
                duration = SnackbarDuration.Short
            )
            learningViewModel.resetState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(
                            text = "Learning Resources",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 18.sp
                        )
                        Text(
                            text = "Tutorials for women artisans",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 13.sp
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (uiState) {
                is LearningState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { WelcomeBanner() }
                        item {
                            LearningSearchBar(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { query ->
                                    searchQuery = query
                                    learningViewModel.searchTutorials(query, user.id)
                                }
                            )
                        }
                        if (uiState is LearningState.Empty) {
                            item { EmptySearchState() }
                        } else {
                            items(categories) { category ->
                                CategoryCard(
                                    category = category,
                                    isExpanded = expandedCategories.contains(category.id),
                                    onToggleExpand = { learningViewModel.toggleCategory(category.id) },
                                    onOpenTutorial = { url -> pendingUrl = url; showExternalLinkDialog = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExternalLinkDialog) {
        ExternalLinkDialog(
            url = pendingUrl,
            onConfirm = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pendingUrl)))
                showExternalLinkDialog = false
            },
            onDismiss = { showExternalLinkDialog = false }
        )
    }
}

// ── Welcome Banner ────────────────────────────────────────────────────────────

@Composable
fun WelcomeBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                RoundedCornerShape(14.dp)
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.20f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Learn and Grow with Craftoria!",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Free tutorials and guides to help you succeed as an online seller",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.90f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

// ── Search Bar ────────────────────────────────────────────────────────────────

@Composable
fun LearningSearchBar(searchQuery: String, onSearchQueryChange: (String) -> Unit) {
    StandardizedOutlinedTextFieldCompact(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = "Search tutorials...",
        singleLine = true,
        minHeight = 48
    )
}

// ── Category Card ─────────────────────────────────────────────────────────────

@Composable
fun CategoryCard(
    category: LearningCategory,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenTutorial: (String) -> Unit
) {
    val iconEntry = getIconEntry(category.icon, isCategory = true)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isExpanded) 0.dp else 0.5.dp,
            color = if (isExpanded) Color.Transparent else BorderColor
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Column(
            modifier = Modifier.background(
                if (isExpanded) Brush.horizontalGradient(listOf(Primary, PrimaryLight))
                else Brush.horizontalGradient(listOf(Color.White, Color.White)),
                RoundedCornerShape(12.dp)
            )
        ) {
            Surface(
                onClick = onToggleExpand,
                color = Color.Transparent,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isExpanded)
                                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.12f)))
                                    else iconGradient(category.icon, isCategory = true)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconEntry.icon,
                                contentDescription = iconEntry.label,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = category.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isExpanded) Color.White else TextPrimary
                            )
                            Text(
                                text = "${category.tutorials.size} tutorial${if (category.tutorials.size != 1) "s" else ""}",
                                fontSize = 11.sp,
                                color = if (isExpanded) Color.White.copy(alpha = 0.85f) else TextSecondary
                            )
                        }
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (isExpanded) Color.White else TextSecondary
                    )
                }
            }

            if (isExpanded) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        category.tutorials.forEach { tutorial ->
                            TutorialItem(
                                tutorial = tutorial,
                                onOpen = { onOpenTutorial(tutorial.url) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Tutorial Item ─────────────────────────────────────────────────────────────

@Composable
fun TutorialItem(tutorial: Tutorial, onOpen: () -> Unit) {
    val iconEntry = getIconEntry(tutorial.icon, isCategory = false)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = BackgroundSecondary,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    iconEntry.color.copy(alpha = 0.14f),
                                    iconEntry.color.copy(alpha = 0.07f)
                                )
                            )
                        )
                        .border(0.5.dp, iconEntry.color.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconEntry.icon,
                        contentDescription = iconEntry.label,
                        tint = iconEntry.color,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tutorial.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                }
            }

            Text(
                text = tutorial.description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(bottom = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(text = tutorial.duration, fontSize = 11.sp, color = TextSecondary)
                }

                // Open button — gradient fill consistent with all primary CTAs
                Button(
                    onClick = onOpen,
                    modifier = Modifier
                        .height(30.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                            RoundedCornerShape(20.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Open", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Empty Search State ────────────────────────────────────────────────────────

@Composable
fun EmptySearchState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Primary tinted circle — consistent with all empty states
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Primary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.SearchOff,
                contentDescription = null,
                tint = Primary.copy(alpha = 0.50f),
                modifier = Modifier.size(38.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text("No tutorials found", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Try different keywords", fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

// ── External Link Dialog ──────────────────────────────────────────────────────

@Composable
fun ExternalLinkDialog(url: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Primary.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text("External Link", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "You are about to open an external website. This content is provided by third parties.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    color = TextSecondary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BackgroundSecondary,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor)
                ) {
                    Text(
                        text = url,
                        fontSize = 11.sp,
                        color = Primary,
                        modifier = Modifier.padding(10.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(40.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Primary, PrimaryLight)),
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Text(
                    "Open Link",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("Cancel", color = TextSecondary, fontSize = 13.sp)
            }
        }
    )
}