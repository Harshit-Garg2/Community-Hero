package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CommunityIssue
import com.example.data.GeminiService
import com.example.data.IssueComment
import com.example.data.UserProfile
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.location.Location
import android.location.LocationManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityHeroApp(viewModel: CommunityHeroViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

    // Handle system back press symmetrically
    androidx.activity.compose.BackHandler(enabled = isLoggedIn && currentScreen != AppScreen.Dashboard) {
        viewModel.navigateTo(AppScreen.Dashboard)
    }

    if (!isLoggedIn) {
        AuthenticationScreen(viewModel)
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (currentScreen != AppScreen.Tutorial) {
                    BottomNavigationBar(
                        currentScreen = currentScreen,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        AppScreen.Tutorial -> TutorialScreen(viewModel)
                        AppScreen.Dashboard -> DashboardScreen(viewModel)
                        AppScreen.ReportIssue -> ReportIssueScreen(viewModel)
                        is AppScreen.IssueDetail -> IssueDetailScreen(viewModel, screen.issueId)
                        AppScreen.ImpactDashboard -> ImpactDashboardScreen(viewModel)
                        AppScreen.Leaderboard -> LeaderboardScreen(viewModel)
                        AppScreen.Profile -> ProfileScreen(viewModel)
                    }
                }
            }
        }
    }
}

// --- Navigation ---
@Composable
fun FrostedGlassContainer(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
    ) {
        // Backdrop Blur (API 31+) and translucent overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    clip = true
                    this.shape = shape
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        renderEffect = android.graphics.RenderEffect.createBlurEffect(
                            25f, 25f, android.graphics.Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                }
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    ),
                    shape = shape
                )
        )

        // Foreground content with transparent surface for shadow
        Surface(
            shape = shape,
            color = Color.Transparent,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
fun BottomNavigationBar(currentScreen: AppScreen, onNavigate: (AppScreen) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        FrostedGlassContainer(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                modifier = Modifier.height(64.dp)
            ) {
                NavigationBarItem(
                    selected = currentScreen is AppScreen.Dashboard || currentScreen is AppScreen.IssueDetail,
                    onClick = { onNavigate(AppScreen.Dashboard) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Feed", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_feed"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen is AppScreen.ImpactDashboard,
                    onClick = { onNavigate(AppScreen.ImpactDashboard) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_impact"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen is AppScreen.Leaderboard,
                    onClick = { onNavigate(AppScreen.Leaderboard) },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Leaderboard") },
                    label = { Text("Heroes", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_heroes"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen is AppScreen.Profile,
                    onClick = { onNavigate(AppScreen.Profile) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_profile"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
            }
        }
    }
}

// ============================================================================
// SCREEN: TUTORIAL
// ============================================================================
@Composable
fun TutorialScreen(viewModel: CommunityHeroViewModel) {
    val page by viewModel.tutorialPage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val totalPages = 3

    val pages = listOf(
        TutorialPageData(
            title = "Identify & Report",
            subtitle = "Spot public issues around your block",
            description = "Easily report pothole hazards, overflowing waste bins, broken streetlights, or general damage. Attach a photo, write a brief details description, and let Community Hero catalog the issue.",
            icon = Icons.Default.AddLocationAlt,
            color = MaterialTheme.colorScheme.primary
        ),
        TutorialPageData(
            title = "AI-Powered Intelligence",
            subtitle = "Smart categorization via Gemini API",
            description = "When you type descriptions, our advanced Gemini integration automatically analyzes descriptions to recommend categories, estimate severity, outline municipal action steps, and generate safety tips.",
            icon = Icons.Default.AutoAwesome,
            color = MaterialTheme.colorScheme.secondary
        ),
        TutorialPageData(
            title = "Earn Points & Badges",
            subtitle = "Be recognized as a neighborhood hero",
            description = "Validate neighbor reports with upvotes, participate in public discussions, and earn points to advance levels! Unlock unique badges like \"Trash Terminator\" or \"Asphalt Patrol\" to showcase your civic impact.",
            icon = Icons.Default.WorkspacePremium,
            color = MaterialTheme.colorScheme.tertiary
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper block with beautiful vector shield graphic
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "App Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Community Hero",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Active page block
        val activePage = pages[page]
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            // Visual Banner drawn in Compose
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(activePage.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                // Interactive dynamic size pulse for logo
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                Icon(
                    imageVector = activePage.icon,
                    contentDescription = null,
                    tint = activePage.color,
                    modifier = Modifier
                        .size(84.dp * scale)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = activePage.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = activePage.subtitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = activePage.color,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = activePage.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 12.dp),
                lineHeight = 22.sp
            )
        }

        // Pagination indicators & buttons
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Dots
            Row(
                modifier = Modifier.padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(totalPages) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(if (index == page) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == page) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                            .animateContentSize()
                    )
                }
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        Toast.makeText(context, "Welcome aboard, Citizen!", Toast.LENGTH_SHORT).show()
                        viewModel.skipTutorial()
                    },
                    modifier = Modifier.testTag("skip_tutorial_button")
                ) {
                    Text(
                        text = "Skip Tutorial",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        if (page < totalPages - 1) {
                            viewModel.setTutorialPage(page + 1)
                        } else {
                            viewModel.skipTutorial()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .width(130.dp)
                        .testTag("next_tutorial_button")
                ) {
                    Text(
                        text = if (page == totalPages - 1) "Get Started" else "Next",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class TutorialPageData(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

// ============================================================================
// SCREEN: MAIN DASHBOARD FEED
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: CommunityHeroViewModel) {
    val issues by viewModel.allIssues.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()

    var dropdownExpanded by remember { mutableStateOf(false) }
    var showVerificationDialogForAction by remember { mutableStateOf(false) }
    var isMapViewSelected by remember { mutableStateOf(false) }
    val isVerified = userProfile.isEmailVerified || userProfile.isPhoneVerified

    // Filter issues locally
    val filteredIssues = issues.filter { issue ->
        val matchesSearch = issue.title.contains(searchQuery, ignoreCase = true) ||
                issue.description.contains(searchQuery, ignoreCase = true) ||
                issue.address.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || issue.category == selectedCategory
        val matchesStatus = selectedStatus == "All" || issue.status == selectedStatus
        val matchesCity = issue.city.equals(selectedCity, ignoreCase = true)
        matchesSearch && matchesCategory && matchesStatus && matchesCity
    }

    if (showVerificationDialogForAction) {
        IdentityVerificationDialog(
            userProfile = userProfile,
            onVerifyEmail = { email -> viewModel.verifyUserEmail(email) },
            onVerifyPhone = { phone -> viewModel.verifyUserPhone(phone) },
            onDismiss = { showVerificationDialogForAction = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header bar
            FrostedGlassContainer(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Logo Emblem: Campaign with multi-color dynamic gradient
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFFFF5722), Color(0xFFE91E63), Color(0xFF9C27B0))
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Column {
                            val gradientColors = listOf(Color(0xFFFF5722), Color(0xFFE91E63), Color(0xFF9C27B0))
                            Text(
                                text = "CivicHero",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    brush = Brush.horizontalGradient(gradientColors)
                                )
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ) {
                                Text(
                                    text = "1.401v Community Feed",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .clickable { dropdownExpanded = true }
                                .testTag("city_dropdown_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = selectedCity,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Dehradun") },
                                onClick = {
                                    viewModel.updateSelectedCity("Dehradun")
                                    dropdownExpanded = false
                                },
                                modifier = Modifier.testTag("city_dehradun")
                            )
                            DropdownMenuItem(
                                text = { Text("Metropolis") },
                                onClick = {
                                    viewModel.updateSelectedCity("Metropolis")
                                    dropdownExpanded = false
                                },
                                modifier = Modifier.testTag("city_metropolis")
                            )
                        }
                    }
                }
            }

            // Dynamic filter panel
            FilterPanel(
                searchQuery = searchQuery,
                onSearchChanged = { viewModel.updateSearchQuery(it) },
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.updateCategoryFilter(it) },
                selectedStatus = selectedStatus,
                onStatusSelected = { viewModel.updateStatusFilter(it) }
            )

            // Segment Switcher: Feed List vs. Interactive Google Map
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { isMapViewSelected = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isMapViewSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (!isMapViewSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f).height(40.dp).testTag("list_view_tab"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Issue List", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                Button(
                    onClick = { isMapViewSelected = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMapViewSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isMapViewSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f).height(40.dp).testTag("map_view_tab"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Map", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            if (isMapViewSelected) {
                GoogleMapVisualizer(
                    issues = filteredIssues,
                    selectedCity = selectedCity,
                    onIssueSelected = { viewModel.navigateTo(com.example.ui.AppScreen.IssueDetail(it)) },
                    onUpvoteIssue = { viewModel.upvoteIssue(it) },
                    isUserVerified = isVerified,
                    onRequireVerification = { showVerificationDialogForAction = true },
                    onMapLongClick = { lat, lng, addr ->
                        viewModel.setPrefilledLocation(lat, lng, addr)
                        viewModel.navigateTo(com.example.ui.AppScreen.ReportIssue)
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            } else {
                // Issues list
                if (filteredIssues.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        EmptyIssuesState()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredIssues, key = { it.id }) { issue ->
                            IssueCard(
                                issue = issue,
                                onCardClick = { viewModel.navigateTo(com.example.ui.AppScreen.IssueDetail(issue.id)) },
                                onUpvoteClick = {
                                    if (isVerified) {
                                        viewModel.upvoteIssue(issue.id)
                                    } else {
                                        showVerificationDialogForAction = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button
        ExtendedFloatingActionButton(
            onClick = {
                if (isVerified) {
                    viewModel.navigateTo(AppScreen.ReportIssue)
                } else {
                    showVerificationDialogForAction = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("report_issue_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Default.Add, contentDescription = "Report Issue") },
            text = { Text("Report Issue", fontWeight = FontWeight.Bold) }
        )
    }
}

@Composable
fun TopDashboardHeader(userProfile: UserProfile, viewModel: CommunityHeroViewModel) {
    var showNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(userProfile.name) }
    var showVerifyDialog by remember { mutableStateOf(false) }

    if (showVerifyDialog) {
        IdentityVerificationDialog(
            userProfile = userProfile,
            onVerifyEmail = { email -> viewModel.verifyUserEmail(email) },
            onVerifyPhone = { phone -> viewModel.verifyUserPhone(phone) },
            onDismiss = { showVerifyDialog = false }
        )
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Update Name") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true,
                    label = { Text("Civic Name") },
                    modifier = Modifier.testTag("edit_name_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateProfileName(tempName)
                        showNameDialog = false
                    },
                    modifier = Modifier.testTag("confirm_name_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile Icon Bubble with visual Level Badge (Stacked above!)
            Box(
                contentAlignment = Alignment.TopEnd,
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { showNameDialog = true }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarSymbol = when (userProfile.profilePicture) {
                        "avatar_architect" -> "🏗️"
                        "avatar_eco" -> "🌿"
                        "avatar_warden" -> "🛣️"
                        "avatar_water" -> "💧"
                        "avatar_green" -> "🌳"
                        "avatar_sentry" -> "🛡️"
                        else -> ""
                    }
                    if (userProfile.profilePicture.startsWith("avatar_") || avatarSymbol.isNotEmpty()) {
                        Text(
                            text = avatarSymbol.ifBlank { "👤" },
                            fontSize = 32.sp
                        )
                    } else {
                        coil.compose.AsyncImage(
                            model = userProfile.profilePicture,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(MaterialTheme.colorScheme.secondary, CircleShape)
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userProfile.level.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = userProfile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Name",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { showNameDialog = true }
                    )
                }
                Text(
                    text = getLevelName(userProfile.level),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f)
                )
                
                // Verification Badge or Action Trigger Button
                if (userProfile.isEmailVerified || userProfile.isPhoneVerified) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Citizen",
                            tint = Color(0xFF2DD4BF),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "VERIFIED HERO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                fontSize = 9.sp
                            ),
                            color = Color(0xFF2DD4BF)
                        )
                    }
                } else {
                    Button(
                        onClick = { showVerifyDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.25f),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(22.dp)
                            .padding(top = 4.dp)
                            .testTag("verify_identity_btn")
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verify Identity (+80 Pts)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // Mini Level progression bar (Centered)
            val nextLevelPoints = getNextLevelThreshold(userProfile.level)
            val currentLevelPoints = getLevelThreshold(userProfile.level)
            val progress = if (nextLevelPoints > currentLevelPoints) {
                (userProfile.points - currentLevelPoints).toFloat() / (nextLevelPoints - currentLevelPoints)
            } else 1.0f
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(6.dp)
                    .clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f)
            )

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            // Civic Points Center Badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "CIVIC PTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = userProfile.points.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun FilterPanel(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    selectedStatus: String,
    onStatusSelected: (String) -> Unit
) {
    val categories = listOf("All", "Pothole", "Water Leakage", "Damaged Streetlight", "Waste Management", "Public Safety", "Public Infrastructure")
    val statuses = listOf("All", "Reported", "Verifying", "In Progress", "Resolved")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Search reported concerns...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag("search_bar_input"),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Categories selector row
        Text(
            text = "Categories",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (category in categories) {
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category) },
                    modifier = Modifier.testTag("chip_cat_$category")
                )
            }
        }

        // Statuses selector row
        Text(
            text = "Status",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (status in statuses) {
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = { onStatusSelected(status) },
                    label = { Text(status) },
                    modifier = Modifier.testTag("chip_status_$status")
                )
            }
        }
    }
}

@Composable
fun EmptyIssuesState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudQueue,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No matching issues found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Be the neighborhood hero and report any public safety or utility problems you see around!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun IssueProgressBar(status: String, modifier: Modifier = Modifier) {
    val steps = listOf("Reported", "Verifying", "In Progress", "Resolved")
    val currentIndex = steps.indexOf(status).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
        ) {
            val fraction = when (currentIndex) {
                0 -> 0.15f
                1 -> 0.45f
                2 -> 0.75f
                else -> 1.0f
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }

        // Labels underneath each step
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, step ->
                val isActive = index <= currentIndex
                val isCurrent = index == currentIndex
                val color = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else if (isActive) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                }
                
                val fontWeight = if (isCurrent) FontWeight.ExtraBold else if (isActive) FontWeight.Bold else FontWeight.Normal
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.secondary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                        text = step,
                        fontSize = 10.sp,
                        fontWeight = fontWeight,
                        color = color,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun IssueCard(
    issue: CommunityIssue,
    onCardClick: () -> Unit,
    onUpvoteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("issue_card_${issue.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Category & Status Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val catColor = getCategoryColor(issue.category)
                    Icon(
                        imageVector = getCategoryIcon(issue.category),
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = issue.category,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = catColor
                    )
                }

                StatusBadge(status = issue.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title & Description
            Text(
                text = issue.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = issue.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!issue.imageUrl.isNullOrBlank() && issue.imageUrl != "default_marker") {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    if (issue.imageUrl.startsWith("content://") || issue.imageUrl.startsWith("file://") || issue.imageUrl.startsWith("http")) {
                        coil.compose.AsyncImage(
                            model = issue.imageUrl,
                            contentDescription = "Evidence Photo Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(getCategoryColor(issue.category).copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(issue.category),
                                    contentDescription = null,
                                    tint = getCategoryColor(issue.category).copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Civic Evidence Attached",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = getCategoryColor(issue.category).copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Visual Progress Bar for Issue Lifecycle Journey
            IssueProgressBar(status = issue.status)

            Spacer(modifier = Modifier.height(12.dp))

            // Address & Severity Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = issue.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))
                
                // Severity label
                SeverityIndicator(severity = issue.severityLevel)
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFFE2E8F0)
            )

            // Interaction buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reporter info
                Text(
                    text = "By ${issue.reporterName}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Upvote verify button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                            .clickable(onClick = onUpvoteClick)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("upvote_btn_${issue.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Upvote",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${issue.upvotes} Verify",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Comments indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Comment,
                            contentDescription = "Comments",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "View Details",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val containerColor = when (status) {
        "Resolved" -> Color(0xFFE8F5E9)
        "In Progress" -> Color(0xFFFFF3E0)
        "Verifying" -> Color(0xFFE1F5FE)
        else -> Color(0xFFFFEBEE)
    }
    val contentColor = when (status) {
        "Resolved" -> Color(0xFF2E7D32)
        "In Progress" -> Color(0xFFE65100)
        "Verifying" -> Color(0xFF0277BD)
        else -> Color(0xFFC62828)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                fontSize = 10.sp
            ),
            color = contentColor
        )
    }
}

@Composable
fun SeverityIndicator(severity: String) {
    val color = when (severity) {
        "Critical" -> Color(0xFFDC2626)
        "High" -> Color(0xFFF59E0B)
        "Medium" -> Color(0xFF3B82F6)
        else -> Color(0xFF10B981)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$severity Severity",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ============================================================================
// SCREEN: REPORT AN ISSUE (FORM WITH GEMINI AI)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIssueScreen(viewModel: CommunityHeroViewModel) {
    val aiState by viewModel.aiAnalysisState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Form inputs
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Pothole") }
    var severity by remember { mutableStateOf("Medium") }
    
    // Custom Geo-location Simulation parameters
    var address by remember { mutableStateOf("Metropolis St, Ward 4") }
    var latitude by remember { mutableStateOf(30.3165) }
    var longitude by remember { mutableStateOf(78.0322) }
    var showMapSelector by remember { mutableStateOf(false) }

    val prefilled by viewModel.prefilledLocation.collectAsStateWithLifecycle()

    LaunchedEffect(prefilled) {
        prefilled?.let {
            address = it.address
            latitude = it.latitude
            longitude = it.longitude
            viewModel.clearPrefilledLocation()
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            locateUser(context) { lat, lng ->
                latitude = lat
                longitude = lng
                address = "IT Park Road, near Survey of India, Dehradun"
                Toast.makeText(context, "Location auto-detected successfully!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Could not auto-detect. Please pin on the map or type your address manually below.", Toast.LENGTH_LONG).show()
        }
    }

    val triggerLocationDetection = {
        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            locateUser(context) { lat, lng ->
                latitude = lat
                longitude = lng
                address = "IT Park Road, near Survey of India, Dehradun"
                Toast.makeText(context, "Location auto-detected successfully!", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    // Picture attachment simulation
    var attachedImageName by remember { mutableStateOf("") }

    val categories = listOf("Pothole", "Water Leakage", "Damaged Streetlight", "Waste Management", "Public Safety", "Public Infrastructure")
    val severities = listOf("Low", "Medium", "High", "Critical")

    // Dynamic auto-fill if AI analyzes successfully
    LaunchedEffect(aiState) {
        if (aiState is AiAnalysisState.Success) {
            val result = (aiState as AiAnalysisState.Success).result
            category = result.category
            severity = result.severity
            Toast.makeText(context, "AI Analysis auto-filled category and severity!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Back Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(AppScreen.Dashboard) },
                modifier = Modifier.testTag("back_button_report")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Report Neighborhood Issue",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General info fields
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Issue Title (e.g. Broken streetlight on Oak Ave)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_title_input"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Describe the issue in detail...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("report_description_input"),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )

            // Smart Gemini AI Analyzer Button
            SmartAiAnalyzeBlock(
                title = title,
                description = description,
                aiState = aiState,
                onAnalyzeClick = { viewModel.analyzeIssueWithAi(title, description) },
                onClearClick = { viewModel.clearAiAnalysis() }
            )

            // Category & Severity Selectors (Pre-filled or customized)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Picker Dropdown
                var categoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("dropdown_category"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        for (cat in categories) {
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                },
                                modifier = Modifier.testTag("menu_item_cat_$cat")
                            )
                        }
                    }
                }

                // Severity Picker Dropdown
                var severityExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = severityExpanded,
                    onExpandedChange = { severityExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = severity,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Severity") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = severityExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("dropdown_severity"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = severityExpanded,
                        onDismissRequest = { severityExpanded = false }
                    ) {
                        for (sev in severities) {
                            DropdownMenuItem(
                                text = { Text(sev) },
                                onClick = {
                                    severity = sev
                                    severityExpanded = false
                                },
                                modifier = Modifier.testTag("menu_item_sev_$sev")
                            )
                        }
                    }
                }
            }

            // Interactive Map Section Trigger
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Map Pinpoint & Geolocation",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tapping the map button below auto-detects your current location on the map. You can still manually enter or correct the address below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    Button(
                        onClick = {
                            showMapSelector = !showMapSelector
                            if (showMapSelector) {
                                triggerLocationDetection()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showMapSelector) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("toggle_map_selector_btn")
                    ) {
                        Icon(
                            imageVector = if (showMapSelector) Icons.Default.Map else Icons.Default.LocationOn,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (showMapSelector) "Close Interactive Map" else "📍 Detect Location & Pin Map")
                    }
                }
            }

            if (showMapSelector) {
                GeoLocationSimulatorBlock(
                    category = category,
                    address = address,
                    latitude = latitude,
                    longitude = longitude,
                    onLocationFetched = { newAddress, newLat, newLong ->
                        address = newAddress
                        latitude = newLat
                        longitude = newLong
                    }
                )
            }

            // Editable Address field - Always accessible so user can verify & manually type
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Exact Address (Editable)") },
                placeholder = { Text("e.g. Near IT Park Survey Road, Dehradun") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_address_input"),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = getCategoryColor(category)
                    )
                },
                supportingText = {
                    Text(
                        text = "Verify or rewrite the address to ensure accuracy (sometimes maps name the area wrong).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            )

            // Picture attach simulation
            CameraAttachmentSimulatorBlock(
                category = category,
                attachedImageName = attachedImageName,
                onPhotoSelected = { attachedImageName = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Submit Button
            Button(
                onClick = {
                    if (title.isBlank() || description.isBlank()) {
                        Toast.makeText(context, "Please supply a title and description.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.reportIssue(
                            title = title,
                            description = description,
                            category = category,
                            address = address,
                            latitude = latitude,
                            longitude = longitude,
                            severity = severity,
                            imageUrl = attachedImageName.ifBlank { "default_marker" }
                        )
                        Toast.makeText(context, "Issue successfully submitted! +50 Points!", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_report_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Submit Civic Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun SmartAiAnalyzeBlock(
    title: String,
    description: String,
    aiState: AiAnalysisState,
    onAnalyzeClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini AI Smart Assistant",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (aiState is AiAnalysisState.Success) {
                    TextButton(onClick = onClearClick) {
                        Text("Reset AI")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (aiState) {
                AiAnalysisState.Idle -> {
                    Text(
                        text = "Provide title and description, then tap below to let Gemini analyze the severity level, classify the category, and draft municipal action plans automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onAnalyzeClick,
                        enabled = title.isNotBlank() && description.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_analyze_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Analyze with Gemini AI", fontWeight = FontWeight.Bold)
                    }
                }
                AiAnalysisState.Analyzing -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Analyzing details via Gemini LLM...",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                is AiAnalysisState.Success -> {
                    val result = aiState.result
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Analysis Complete!", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                        
                        Divider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))

                        Text(
                            text = "Reasoning: ${result.reasoning}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Recommended Action: ${result.recommendedAction}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Safety Tip: ${result.emergencyTip}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
                is AiAnalysisState.Error -> {
                    Text(
                        text = "Could not reach Gemini AI: ${aiState.error}. Standard offline presets used.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onAnalyzeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Retry AI Connection")
                    }
                }
            }
        }
    }
}

@Composable
fun GeoLocationSimulatorBlock(
    category: String,
    address: String,
    latitude: Double,
    longitude: Double,
    onLocationFetched: (String, Double, Double) -> Unit
) {
    var isStreetView by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    
    val streetNames = listOf("Broad Street", "Lexington Ave", "Atlantic Blvd", "Civic Center Pkwy", "Fulton Street", "Grand Concourse", "Oak Avenue", "Central Park East")
    val wards = listOf("Ward 1 (Downtown)", "Ward 2 (Northside)", "Ward 3 (Industrial)", "Ward 4 (Greenwood)")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationSearching, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Interactive Mapping HUD", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                
                // Map style toggle (Map vs Street View!)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Street View", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = isStreetView,
                        onCheckedChange = { isStreetView = it },
                        modifier = Modifier.scale(0.8f).testTag("street_view_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

             // Text search input for manual override
            OutlinedTextField(
                value = searchInput,
                onValueChange = { searchInput = it },
                placeholder = { Text("Search address manually (e.g., 205 Oak Ave)") },
                modifier = Modifier.fillMaxWidth().testTag("map_search_input"),
                shape = RoundedCornerShape(10.dp),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (searchInput.isNotBlank()) {
                                val cleanInput = searchInput.trim()
                                val isDehradun = cleanInput.contains("dehradun", ignoreCase = true) || cleanInput.contains("IT park", ignoreCase = true)
                                if (isDehradun) {
                                    onLocationFetched(cleanInput, 30.3601, 78.0772)
                                } else {
                                    val randomLat = 40.7100 + (0.0150 * java.lang.Math.random())
                                    val randomLong = -74.0050 + (0.0150 * java.lang.Math.random())
                                    onLocationFetched("$cleanInput, Metropolis", randomLat, randomLong)
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search map")
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Map canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                    .background(if (isStreetView) Color(0xFF334155) else Color(0xFFE2E8F0))
            ) {
                if (isStreetView) {
                    // STYLIZED STREET VIEW RENDERER IN COMPOSE CANVAS
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Sky
                        drawRect(Color(0xFF60A5FA), Offset.Zero, size = size.copy(height = h * 0.6f))
                        // Sun
                        drawCircle(Color(0xFFFEF08A), radius = 24f, center = Offset(w * 0.8f, h * 0.2f))
                        // Road ground
                        drawRect(Color(0xFF475569), Offset(0f, h * 0.6f), size = size.copy(height = h * 0.4f))
                        // Road perspective lines
                        drawLine(Color(0xFF94A3B8), Offset(w * 0.5f, h * 0.6f), Offset(0f, h), strokeWidth = 8f)
                        drawLine(Color(0xFF94A3B8), Offset(w * 0.5f, h * 0.6f), Offset(w, h), strokeWidth = 8f)
                        // Sidewalk line
                        drawLine(Color(0xFFCBD5E1), Offset(w * 0.5f, h * 0.6f), Offset(w * 0.2f, h), strokeWidth = 4f)

                        // Stylized indicator depending on the selected issue category!
                        when (category) {
                            "Pothole" -> {
                                // Draw a big crater crack in perspective
                                drawOval(
                                    Color(0xFF1E293B),
                                    topLeft = Offset(w * 0.4f, h * 0.75f),
                                    size = androidx.compose.ui.geometry.Size(120f, 40f)
                                )
                                drawOval(
                                    Color(0xFFE2E8F0).copy(alpha = 0.5f),
                                    topLeft = Offset(w * 0.42f, h * 0.77f),
                                    size = androidx.compose.ui.geometry.Size(100f, 30f)
                                )
                            }
                            "Damaged Streetlight" -> {
                                // Draw a streetlight post
                                drawLine(Color(0xFF0F172A), Offset(w * 0.7f, h * 0.3f), Offset(w * 0.7f, h * 0.8f), strokeWidth = 10f)
                                drawCircle(Color(0xFFFFD700).copy(alpha = 0.4f), radius = 18f, center = Offset(w * 0.7f, h * 0.3f))
                            }
                            "Waste Management" -> {
                                // Draw a green waste bin container
                                drawRect(Color(0xFF15803D), Offset(w * 0.3f, h * 0.65f), size = androidx.compose.ui.geometry.Size(60f, 50f))
                                drawOval(Color(0xFFDC2626), Offset(w * 0.32f, h * 0.62f), size = androidx.compose.ui.geometry.Size(50f, 15f))
                            }
                            else -> {
                                // Draw generic issue marker box
                                drawRect(Color(0xFFDC2626).copy(alpha = 0.8f), Offset(w * 0.45f, h * 0.68f), size = androidx.compose.ui.geometry.Size(40f, 40f))
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "STREET VIEW AT PINNED SPOT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // INTERACTIVE TAP-TO-MARK GPS MAP
                    var tapOffset by remember { mutableStateOf<Offset?>(null) }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    tapOffset = offset
                                    val deltaX = (offset.x - (size.width / 2)) / size.width
                                    val deltaY = (offset.y - (size.height / 2)) / size.height
                                    
                                    val finalLat = 40.7128 - (deltaY * 0.04)
                                    val finalLong = -74.0060 + (deltaX * 0.04)
                                    
                                    val randomNum = (100..999).random()
                                    val randomStreet = streetNames.random()
                                    val randomWard = wards.random()
                                    val finalAddress = "$randomNum $randomStreet, $randomWard"
                                    
                                    onLocationFetched(finalAddress, finalLat, finalLong)
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height

                        // Map Streets grid (background lines)
                        drawLine(Color.White, Offset(w * 0.15f, 0f), Offset(w * 0.15f, h), strokeWidth = 10f)
                        drawLine(Color.White, Offset(w * 0.45f, 0f), Offset(w * 0.45f, h), strokeWidth = 10f)
                        drawLine(Color.White, Offset(w * 0.75f, 0f), Offset(w * 0.75f, h), strokeWidth = 10f)
                        drawLine(Color.White, Offset(0f, h * 0.25f), Offset(w, h * 0.25f), strokeWidth = 10f)
                        drawLine(Color.White, Offset(0f, h * 0.65f), Offset(w, h * 0.65f), strokeWidth = 10f)

                        // Central park green area
                        drawRect(Color(0xFFDCFCE7), Offset(w * 0.5f, h * 0.35f), size = androidx.compose.ui.geometry.Size(120f, 80f))

                        // Draw dropped pin location
                        val pinCenter = if (tapOffset != null) {
                            tapOffset!!
                        } else {
                            Offset(w * 0.48f, h * 0.55f)
                        }

                        // Ripple
                        drawCircle(Color(0xFF0D9488).copy(alpha = 0.25f), radius = 32f, center = pinCenter)
                        // Marker
                        drawCircle(Color(0xFF0D9488), radius = 10f, center = pinCenter)
                        drawCircle(Color.White, radius = 4f, center = pinCenter)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "INTERACTIVE GPS MAP - TAP TO MARK PIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current Coordinates & Pin Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = address,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Lat: ${String.format("%.4f", latitude)}, Long: ${String.format("%.4f", longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val num = (100..999).random()
                        val street = streetNames.random()
                        val ward = wards.random()
                        val newAddress = "$num $street, $ward"
                        val newLat = 40.7100 + (0.0150 * java.lang.Math.random())
                        val newLong = -74.0050 + (0.0150 * java.lang.Math.random())
                        onLocationFetched(newAddress, newLat, newLong)
                    },
                    modifier = Modifier.testTag("fetch_gps_location"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto GPS")
                }
            }
        }
    }
}

@Composable
fun CameraAttachmentSimulatorBlock(
    category: String,
    attachedImageName: String,
    onPhotoSelected: (String) -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            onPhotoSelected(uri.toString())
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Evidence Photo Attachment", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                if (attachedImageName.isNotEmpty()) {
                    TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Text("Change", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (attachedImageName.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (attachedImageName.startsWith("content://") || attachedImageName.startsWith("file://") || attachedImageName.startsWith("http")) {
                        coil.compose.AsyncImage(
                            model = attachedImageName,
                            contentDescription = "Selected Photo Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = getCategoryIcon(category),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Evidence: [${category.uppercase()}]",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    IconButton(
                        onClick = { onPhotoSelected("") },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = Color.White)
                    }
                }
            } else {
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("simulate_camera_capture"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Photo from Device Gallery", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================================
// SCREEN: ISSUE DETAILS (TRACKING, VERIFICATION, & AUTHORITY UPDATE)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailScreen(viewModel: CommunityHeroViewModel, issueId: Int) {
    val issue by viewModel.selectedIssue.collectAsStateWithLifecycle()
    val comments by viewModel.selectedIssueComments.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var commentText by remember { mutableStateOf("") }
    var authorityMode by remember { mutableStateOf(false) }

    if (issue == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Loading details...")
        }
        return
    }

    val activeIssue = issue!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Detail Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(AppScreen.Dashboard) },
                modifier = Modifier.testTag("back_button_detail")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Civic Concern Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val catColor = getCategoryColor(activeIssue.category)
                            Icon(getCategoryIcon(activeIssue.category), contentDescription = null, tint = catColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(activeIssue.category, fontWeight = FontWeight.Bold, color = catColor)
                        }

                        StatusBadge(status = activeIssue.status)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(activeIssue.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(activeIssue.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), lineHeight = 22.sp)

                    if (!activeIssue.imageUrl.isNullOrBlank() && activeIssue.imageUrl != "default_marker") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            if (activeIssue.imageUrl.startsWith("content://") || activeIssue.imageUrl.startsWith("file://") || activeIssue.imageUrl.startsWith("http")) {
                                coil.compose.AsyncImage(
                                    model = activeIssue.imageUrl,
                                    contentDescription = "Issue Evidence Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(getCategoryColor(activeIssue.category).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = getCategoryIcon(activeIssue.category),
                                            contentDescription = null,
                                            tint = getCategoryColor(activeIssue.category),
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Evidence: ${activeIssue.category} File Attachment",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = getCategoryColor(activeIssue.category)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Room, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(activeIssue.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reported by ${activeIssue.reporterName} on Metropolis Wards map.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }

            // Real-time tracking progress pipeline
            RealTimeTrackingPipeline(status = activeIssue.status, notes = activeIssue.resolutionNotes)

            // Upvote & Community Verification Call
            CommunityValidationRow(
                activeIssue = activeIssue,
                onUpvote = {
                    viewModel.verifyIssue(activeIssue.id, isValid = true)
                    Toast.makeText(context, "Upvote recorded! Community credibility increased! +10 Points!", Toast.LENGTH_SHORT).show()
                },
                onDownvote = {
                    viewModel.verifyIssue(activeIssue.id, isValid = false)
                    Toast.makeText(context, "Downvote recorded. Thank you for maintaining credibility.", Toast.LENGTH_SHORT).show()
                }
            )

            // Verified Consensus Resolution Voting HUD
            val userProfileState by viewModel.userProfile.collectAsStateWithLifecycle()
            VerifiedResolutionVotingHUD(
                activeIssue = activeIssue,
                userProfile = userProfileState,
                onSubmitVote = {
                    viewModel.submitResolveVote(activeIssue.id, userProfileState.name)
                    Toast.makeText(context, "Resolution sign-off cast! +25 Points!", Toast.LENGTH_SHORT).show()
                },
                onReporterResolve = { memo ->
                    viewModel.updateIssueStatus(activeIssue.id, "Resolved", "Resolved by original reporter: $memo")
                    Toast.makeText(context, "Concern resolved successfully! +100 Points!", Toast.LENGTH_SHORT).show()
                },
                onVerifyEmail = { email -> viewModel.verifyUserEmail(email) },
                onVerifyPhone = { phone -> viewModel.verifyUserPhone(phone) }
            )

            // Authority Supervisor Mode Panel
            SupervisorControlPanel(
                isActive = authorityMode,
                onToggle = { authorityMode = it },
                activeIssue = activeIssue,
                onStatusUpdated = { newStatus, notes ->
                    viewModel.updateIssueStatus(activeIssue.id, newStatus, notes)
                    Toast.makeText(context, "Status updated to: $newStatus!", Toast.LENGTH_SHORT).show()
                }
            )

            // Comments Box
            CommentsBox(
                comments = comments,
                commentText = commentText,
                onCommentTextChanged = { commentText = it },
                onAddComment = {
                    viewModel.addComment(activeIssue.id, commentText)
                    commentText = ""
                    Toast.makeText(context, "Comment submitted! +5 Points!", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun RealTimeTrackingPipeline(status: String, notes: String?) {
    val steps = listOf("Reported", "Verifying", "In Progress", "Resolved")
    val currentIndex = steps.indexOf(status).coerceAtLeast(0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Resolution Lifecycle Tracking", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in steps.indices) {
                    val isActive = i <= currentIndex
                    val isCompleted = i < currentIndex
                    val isCurrent = i == currentIndex
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.secondary
                                    else if (isCompleted) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    CircleShape
                                )
                        ) {
                            if (isCompleted) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            } else {
                                Text(
                                    text = (i + 1).toString(),
                                    color = if (isActive) Color.White else Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = steps[i],
                            fontSize = 10.sp,
                            fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                            color = if (isCurrent) MaterialTheme.colorScheme.secondary
                                    else if (isActive) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (!notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Official Resolution Memo:", fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodySmall, color = Color(0xFF047857))
                        Text(notes, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF065F46))
                    }
                }
            }
        }
    }
}

@Composable
fun CommunityValidationRow(
    activeIssue: CommunityIssue,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Community Credibility Check", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Citizens are verifying this issue. Please validate if you have witnessed this issue first-hand.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onUpvote,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("verify_yes_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm Active (${activeIssue.upvotes})")
                }

                OutlinedButton(
                    onClick = onDownvote,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("verify_no_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ThumbDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dispute (${activeIssue.downvotes})")
                }
            }
        }
    }
}

@Composable
fun SupervisorControlPanel(
    isActive: Boolean,
    onToggle: (Boolean) -> Unit,
    activeIssue: CommunityIssue,
    onStatusUpdated: (String, String?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Engineering, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("City Authority Controller", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                
                Switch(
                    checked = isActive,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("authority_mode_switch")
                )
            }

            if (isActive) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "As municipal authority supervisor, manage state progress & enter resolution reports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                var notes by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Resolution Memo / Dispatch Notes") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_notes_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val states = listOf("Verifying", "In Progress", "Resolved")
                    for (state in states) {
                        Button(
                            onClick = { onStatusUpdated(state, notes.ifBlank { null }) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_state_btn_$state"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeIssue.status == state) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = if (activeIssue.status == state) Color.White
                                               else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(state, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentsBox(
    comments: List<IssueComment>,
    commentText: String,
    onCommentTextChanged: (String) -> Unit,
    onAddComment: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Civic Forum & Feedback", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))

            if (comments.isEmpty()) {
                Text(
                    text = "No forum entries yet. Start the community conversation!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (comment in comments) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(comment.author, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                Text("Verified Citizen", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(comment.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = onCommentTextChanged,
                    placeholder = { Text("Write neighborhood advice...") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onAddComment,
                    enabled = commentText.isNotBlank(),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("send_comment_btn")
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

// ============================================================================
// SCREEN: IMPACT DASHBOARD & CHARTS
// ============================================================================
@Composable
fun ImpactDashboardScreen(viewModel: CommunityHeroViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val issues by viewModel.allIssues.collectAsStateWithLifecycle()

    val resolvedCount = issues.count { it.status == "Resolved" }
    val totalCount = issues.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Impact Banner - Renamed to Dashboard & Metrics
        Text(
            text = "Dashboard & Community Metrics",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Weekly Collective Goal Bar
        WeeklyCommunityGoalBanner(issues = issues)

        // Daily Goals & Achievements Block
        DailyGoalsAndAchievementsBlock(userProfile = userProfile, issues = issues)

        // Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Total Issues",
                value = totalCount.toString(),
                subtitle = "In your block",
                icon = Icons.Default.Map,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Resolved Issues",
                value = resolvedCount.toString(),
                subtitle = "Fixed by crew",
                icon = Icons.Default.CheckCircle,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Total Reports",
                value = userProfile.totalReports.toString(),
                subtitle = "Submissions",
                icon = Icons.Default.AddBox,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Credibility Level",
                value = "Lvl ${userProfile.level}",
                subtitle = getLevelName(userProfile.level),
                icon = Icons.Default.WorkspacePremium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        // Custom drawn Chart Card (using Canvas API!)
        CivicImpactChart(resolved = resolvedCount, active = (totalCount - resolvedCount).coerceAtLeast(0))

        // Badge Unlocks Card
        GamifiedBadgesGrid(userProfile)
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
            Text(subtitle, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

@Composable
fun CivicImpactChart(resolved: Int, active: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Municipal Resolution Rate",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Custom dynamic ring gauge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                val total = resolved + active
                val resolvedRatio = if (total > 0) resolved.toFloat() / total else 0.5f
                
                Canvas(modifier = Modifier.size(120.dp)) {
                    val stroke = 12.dp.toPx()
                    // Background Ring
                    drawCircle(
                        color = Color(0xFFE2E8F0),
                        radius = size.width / 2 - stroke,
                        style = Stroke(width = stroke)
                    )
                    // Foreground Ring
                    drawArc(
                        color = Color(0xFF0D9488), // Teal
                        startAngle = -90f,
                        sweepAngle = 360f * resolvedRatio,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val percent = if (total > 0) (resolvedRatio * 100).toInt() else 0
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0D9488)
                    )
                    Text(
                        text = "Resolved Rate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Legend labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF0D9488), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resolved ($resolved issues)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFE2E8F0), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Active ($active issues)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GamifiedBadgesGrid(userProfile: UserProfile) {
    // Badges definitions
    val badges = listOf(
        BadgeData("First Responder", "Report your first issue", Icons.Default.AddAlert, userProfile.totalReports >= 1),
        BadgeData("Eagle Eye", "Verify/Upvote 3 neighborhood reports", Icons.Default.Visibility, userProfile.verifiedCount >= 3),
        BadgeData("Civic Rookie", "Earn at least 50 community points", Icons.Default.School, userProfile.points >= 50),
        BadgeData("Pothole Hunter", "Earn at least 100 community points", Icons.Default.Search, userProfile.points >= 100),
        BadgeData("Community Guardian", "Earn at least 250 community points", Icons.Default.VerifiedUser, userProfile.points >= 250),
        BadgeData("Neighborhood Hero", "Earn at least 500 community points", Icons.Default.WorkspacePremium, userProfile.points >= 500),
        BadgeData("Local Advisor", "Write constructive comments in forum", Icons.Default.Forum, userProfile.points > 50),
        BadgeData("Asphalt Patrol", "Get points by reporting road cavities", Icons.Default.Traffic, userProfile.totalReports >= 2),
        BadgeData("Trash Terminator", "Report waste management concerns", Icons.Default.DeleteOutline, userProfile.points >= 150)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Honor Badges Catalog",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (badge in badges) {
                    val alpha = if (badge.unlocked) 1f else 0.35f
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (badge.unlocked) MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                                else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (badge.unlocked) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = badge.icon,
                                contentDescription = badge.name,
                                tint = if (badge.unlocked) MaterialTheme.colorScheme.secondary else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = badge.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                            )
                            Text(
                                text = badge.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (badge.unlocked) 0.6f else 0.35f)
                            )
                        }

                        if (badge.unlocked) {
                            Icon(Icons.Default.Verified, contentDescription = "Unlocked", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

data class BadgeData(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val unlocked: Boolean
)

// ============================================================================
// SCREEN: HEROES LEADERBOARD
// ============================================================================
@Composable
fun LeaderboardScreen(viewModel: CommunityHeroViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val citizens = listOf(
        LeaderboardEntry("Elena Carter", 4, 820, true),
        LeaderboardEntry("Alex Rivera", 3, 560, false),
        LeaderboardEntry(userProfile.name, userProfile.level, userProfile.points, false, isCurrentUser = true),
        LeaderboardEntry("Marcus Vance", 2, 280, false),
        LeaderboardEntry("Sophie Dubois", 1, 95, false),
        LeaderboardEntry("Jack Sparrow", 1, 40, false)
    ).sortedByDescending { it.points }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Neighborhood Heroes",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Citizens contributing actively to public infrastructure excellence.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(citizens) { citizen ->
                val rank = citizens.indexOf(citizen) + 1
                LeaderboardCard(citizen = citizen, rank = rank)
            }
        }
    }
}

data class LeaderboardEntry(
    val name: String,
    val level: Int,
    val points: Int,
    val isTopVerified: Boolean,
    val isCurrentUser: Boolean = false
)

@Composable
fun LeaderboardCard(citizen: LeaderboardEntry, rank: Int) {
    val cardBg = if (citizen.isCurrentUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    val borderStroke = if (citizen.isCurrentUser) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = borderStroke
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Number
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        when (rank) {
                            1 -> Color(0xFFFBBF24) // Gold
                            2 -> Color(0xFF94A3B8) // Silver
                            3 -> Color(0xFFB45309) // Bronze
                            else -> Color.Transparent
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = if (rank in 1..3) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar bubble
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = citizen.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = citizen.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (citizen.isCurrentUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("YOU", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Text(
                    text = "Lvl ${citizen.level} • ${getLevelName(citizen.level)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Points
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = citizen.points.toString(),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ============================================================================
// HELPER UTILS
// ============================================================================
fun getLevelName(level: Int): String {
    return when (level) {
        1 -> "Civic Rookie"
        2 -> "Neighborhood Watch"
        3 -> "Street Guardian"
        else -> "Legendary Community Hero"
    }
}

fun getLevelThreshold(level: Int): Int {
    return when (level) {
        1 -> 0
        2 -> 100
        3 -> 300
        else -> 600
    }
}

fun getNextLevelThreshold(level: Int): Int {
    return when (level) {
        1 -> 100
        2 -> 300
        3 -> 600
        else -> 600
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Pothole" -> Icons.Default.Traffic
        "Water Leakage" -> Icons.Default.WaterDrop
        "Damaged Streetlight" -> Icons.Default.Lightbulb
        "Waste Management" -> Icons.Default.Delete
        "Public Safety" -> Icons.Default.ReportProblem
        else -> Icons.Default.Build
    }
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Pothole" -> Color(0xFFF59E0B) // Sunset Amber
        "Water Leakage" -> Color(0xFF3B82F6) // Electric Blue
        "Damaged Streetlight" -> Color(0xFFEAB308) // Sunny Yellow
        "Waste Management" -> Color(0xFF10B981) // Emerald Green
        "Public Safety" -> Color(0xFFEF4444) // Bright Red
        "Public Infrastructure" -> Color(0xFF8B5CF6) // Royal Violet
        else -> Color(0xFF6366F1) // Indigo
    }
}

@Composable
fun DailyGoalsAndAchievementsBlock(
    userProfile: com.example.data.UserProfile,
    issues: List<CommunityIssue>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Section Title with multi-color vibrant gradient icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFE11D48), Color(0xFFC084FC))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Daily Goals & Achievements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Complete objectives to earn bonus XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Goals list
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val goals = listOf(
                    Triple(
                        "Report a Problem",
                        "Help clean or fix the city by submitting 1 report.",
                        userProfile.totalReports >= 1
                    ),
                    Triple(
                        "Verified Citizen",
                        "Secure your profile by verifying email or phone.",
                        userProfile.isEmailVerified || userProfile.isPhoneVerified
                    ),
                    Triple(
                        "Active Explorer",
                        "Check out issues pinned on the interactive map.",
                        true // Simulated always explored or active
                    )
                )

                for (goal in goals) {
                    val (title, desc, completed) = goal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (completed) Color(0xFF10B981).copy(alpha = 0.06f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (completed) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (completed) "Completed" else "Incomplete",
                            tint = if (completed) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (completed) Color(0xFF047857) else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                lineHeight = 13.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (completed) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (completed) "DONE" else "+30 XP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (completed) Color(0xFF047857) else MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

            // Achievements Badge Badges
            Text(
                text = "Milestone Achievements",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Badge 1: First Responder
                val badge1Unlocked = userProfile.totalReports >= 1
                AchievementBadgeItem(
                    title = "First Responder",
                    desc = "File your first report",
                    unlocked = badge1Unlocked,
                    gradientColors = listOf(Color(0xFFF97316), Color(0xFFFACC15)),
                    icon = Icons.Default.Campaign,
                    modifier = Modifier.weight(1f)
                )

                // Badge 2: Civic Inspector
                val badge2Unlocked = userProfile.totalReports >= 3
                AchievementBadgeItem(
                    title = "Civic Inspector",
                    desc = "File 3 total reports",
                    unlocked = badge2Unlocked,
                    gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
                    icon = Icons.Default.VerifiedUser,
                    modifier = Modifier.weight(1f)
                )

                // Badge 3: Neighborhood Legend
                val badge3Unlocked = userProfile.level >= 3
                AchievementBadgeItem(
                    title = "Local Legend",
                    desc = "Reach Credibility Lvl 3",
                    unlocked = badge3Unlocked,
                    gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF10B981)),
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AchievementBadgeItem(
    title: String,
    desc: String,
    unlocked: Boolean,
    gradientColors: List<Color>,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = if (unlocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    brush = if (unlocked) Brush.linearGradient(gradientColors) else Brush.linearGradient(listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1))),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (unlocked) Color.White else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ============================================================================
// ANIMATED COMPONENT: WEEKLY COMMUNITY GOALS BANNER
// ============================================================================
@Composable
fun WeeklyCommunityGoalBanner(issues: List<CommunityIssue>) {
    val totalResolved = issues.count { it.status == "Resolved" }
    val weeklyGoal = 5
    val percent = if (weeklyGoal > 0) (totalResolved.toFloat() / weeklyGoal).coerceIn(0f, 1f) else 1.0f

    // Breathing pulse effect for active motivation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Goal",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Weekly Collective Goal",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "+150 PTS ALL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Resolve 5 community issues this week to keep Metropolis clean and safe! Collective contributions boost everyone's civic standing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Resolutions: $totalResolved / $weeklyGoal Solved",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "${(percent * 100).toInt()}% Done",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { percent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            )
        }
    }
}

// ============================================================================
// COMPONENT: OTP EMAIL/SMS IDENTITY VERIFICATION MODAL
// ============================================================================
@Composable
fun IdentityVerificationDialog(
    userProfile: UserProfile,
    onVerifyEmail: (String) -> Unit,
    onVerifyPhone: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Select method, 2: Email verification, 3: Phone verification, 4: Success Screen
    var emailInput by remember { mutableStateOf(userProfile.emailAddress) }
    var phoneInput by remember { mutableStateOf(userProfile.phoneNumber) }
    var otpInput by remember { mutableStateOf("") }
    var otpSentCode by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Citizen Identity Board",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (step) {
                    1 -> {
                        Text(
                            text = "To eliminate bot activity and secure municipal resolution integrity, citizens must authenticate their account via SMS or Email verification. Completing verification grants voting rights!",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Email Option Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    otpSentCode = "1099"
                                    otpInput = ""
                                    otpError = ""
                                    step = 2
                                }
                                .testTag("verify_email_option"),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Verify Email Address", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("Get security code via secure Sandbox gateway (+40 pts)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }

                        // Phone Option Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    otpSentCode = "8840"
                                    otpInput = ""
                                    otpError = ""
                                    step = 3
                                }
                                .testTag("verify_phone_option"),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Verify Mobile Number", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("Get SMS code via secure Sandbox gateway (+40 pts)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }

                    2 -> { // Email entry & OTP Verification
                        Text(
                            text = "Enter your email address. A 4-digit security code will be routed to your inbox.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            placeholder = { Text("e.g. citizen@metropolis.gov") },
                            modifier = Modifier.fillMaxWidth().testTag("verify_email_input"),
                            singleLine = true,
                            label = { Text("Civic Email Address") }
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔑 Integration Sandbox Mode:\nUse the secure mock token [1099] to authorize.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = { otpInput = it },
                            placeholder = { Text("Enter 4-Digit OTP") },
                            modifier = Modifier.fillMaxWidth().testTag("verify_email_otp_input"),
                            singleLine = true,
                            label = { Text("Security Verification Token") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        if (otpError.isNotEmpty()) {
                            Text(text = otpError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    3 -> { // SMS entry & OTP Verification
                        Text(
                            text = "Enter your mobile phone number. A 4-digit security code will be sent via cellular network.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            placeholder = { Text("e.g. +1 (555) 012-3456") },
                            modifier = Modifier.fillMaxWidth().testTag("verify_phone_input"),
                            singleLine = true,
                            label = { Text("Mobile Phone Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔑 Integration Sandbox Mode:\nUse the secure mock token [8840] to authorize.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = { otpInput = it },
                            placeholder = { Text("Enter 4-Digit OTP") },
                            modifier = Modifier.fillMaxWidth().testTag("verify_phone_otp_input"),
                            singleLine = true,
                            label = { Text("Security Verification Token") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        if (otpError.isNotEmpty()) {
                            Text(text = otpError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    4 -> { // Verification Success Visual State
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF0D9488),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Citizen Identity Secured!",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Thank you! You have successfully unlocked neighborhood consensus voting capabilities. Your contributions are now safe from bot flags. +40 Civic Points credited!",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (step == 2 || step == 3) {
                Button(
                    onClick = {
                        if (otpInput == otpSentCode) {
                            if (step == 2) {
                                onVerifyEmail(emailInput.ifBlank { "citizen@metropolis.gov" })
                            } else {
                                onVerifyPhone(phoneInput.ifBlank { "+1 (555) 012-3456" })
                            }
                            otpError = ""
                            step = 4
                        } else {
                            otpError = "Invalid verification code. Please enter the Sandbox Integration token."
                        }
                    },
                    modifier = Modifier.testTag("submit_otp_button")
                ) {
                    Text("Verify Code")
                }
            } else if (step == 4) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_verify_success_btn")
                ) {
                    Text("Complete")
                }
            }
        },
        dismissButton = {
            if (step < 4) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

// ============================================================================
// COMPONENT: VERIFIED CITIZEN CONSENSUS RESOLUTION VOTING BOARD
// ============================================================================
@Composable
fun VerifiedResolutionVotingHUD(
    activeIssue: CommunityIssue,
    userProfile: UserProfile,
    onSubmitVote: () -> Unit,
    onReporterResolve: (String) -> Unit,
    onVerifyEmail: (String) -> Unit,
    onVerifyPhone: (String) -> Unit
) {
    if (activeIssue.status == "Resolved") return

    val voters = activeIssue.votedUsersListJson.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val hasAlreadyVoted = voters.contains(userProfile.name) || userProfile.signedOffIssueIds.split(",").contains(activeIssue.id.toString())
    val isReporter = activeIssue.reporterName == userProfile.name
    val isVerified = userProfile.isEmailVerified || userProfile.isPhoneVerified

    var showVerifyDialogInDetail by remember { mutableStateOf(false) }

    if (showVerifyDialogInDetail) {
        IdentityVerificationDialog(
            userProfile = userProfile,
            onVerifyEmail = onVerifyEmail,
            onVerifyPhone = onVerifyPhone,
            onDismiss = { showVerifyDialogInDetail = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Resolution Verification Board",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isReporter) {
                if (!isVerified) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Verification Required: As the reporter, please authenticate your account to sign off and resolve this civic issue.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Button(
                            onClick = { showVerifyDialogInDetail = true },
                            modifier = Modifier.fillMaxWidth().testTag("reporter_auth_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Authenticate Account Now (+40 Pts)", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Reporter Instant Close Panel
                    Text(
                        text = "You are the original reporter of this civic concern. You can instantly sign off and mark this concern as solved once repairs are complete.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    var reporterMemo by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = reporterMemo,
                        onValueChange = { reporterMemo = it },
                        placeholder = { Text("Describe how this was fixed (e.g. municipal crew patched it)...") },
                        modifier = Modifier.fillMaxWidth().testTag("reporter_memo_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onReporterResolve(reporterMemo.ifBlank { "Marked resolved by original reporter." }) },
                        modifier = Modifier.fillMaxWidth().testTag("reporter_resolve_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resolve Concern Now (+100 Pts)", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Consensus-based Verified Voting Board
                Text(
                    text = "To prevent bot spam, municipal close-out requires resolution sign-off from verified citizen witnesses.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Verified Witness Signatures",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${activeIssue.resolveVotesCount} / ${activeIssue.resolveVotesRequired} Signatures",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val progressFrac = if (activeIssue.resolveVotesRequired > 0) {
                    (activeIssue.resolveVotesCount.toFloat() / activeIssue.resolveVotesRequired)
                } else 1.0f

                LinearProgressIndicator(
                    progress = { progressFrac.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                )

                if (voters.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Signatures: " + voters.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontStyle = FontStyle.Italic
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isVerified) {
                    if (hasAlreadyVoted) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF0D9488))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Your signature has been registered! Thank you for voting.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF065F46),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onSubmitVote,
                            modifier = Modifier.fillMaxWidth().testTag("cast_resolve_vote_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.RateReview, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign-off Resolution (+25 Pts)", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.05f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Verification Required: Please authenticate your account to vote on resolution signatures.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Button(
                            onClick = { showVerifyDialogInDetail = true },
                            modifier = Modifier.fillMaxWidth().testTag("verify_now_detail_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify Identity with OTP (+80 Pts)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(viewModel: CommunityHeroViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val issues by viewModel.allIssues.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }
    var showVerifyDialog by remember { mutableStateOf(false) }

    val getAvatarSymbol = { avatarKey: String ->
        when (avatarKey) {
            "avatar_architect" -> "🏗️"
            "avatar_eco" -> "🌿"
            "avatar_warden" -> "🛣️"
            "avatar_water" -> "💧"
            "avatar_green" -> "🌳"
            "avatar_sentry" -> "🛡️"
            else -> "👤"
        }
    }

    if (showVerifyDialog) {
        IdentityVerificationDialog(
            userProfile = userProfile,
            onVerifyEmail = { email -> viewModel.verifyUserEmail(email) },
            onVerifyPhone = { phone -> viewModel.verifyUserPhone(phone) },
            onDismiss = { showVerifyDialog = false }
        )
    }

    if (showEditDialog) {
        EditProfileDialog(
            userProfile = userProfile,
            onSave = { name, phone, address, avatar ->
                viewModel.updateUserProfile(name, phone, address, avatar)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "My Citizen Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Button(
                onClick = { showEditDialog = true },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp).testTag("edit_profile_btn")
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Profile Main Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile Avatar Bubble with level badge (Stacked above!)
                Box(
                    contentAlignment = Alignment.TopEnd,
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { showEditDialog = true }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfile.profilePicture.startsWith("avatar_")) {
                            Text(
                                text = getAvatarSymbol(userProfile.profilePicture),
                                fontSize = 36.sp
                            )
                        } else {
                            coil.compose.AsyncImage(
                                model = userProfile.profilePicture,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userProfile.level.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = userProfile.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { showEditDialog = true }
                        )
                    }
                    Text(
                        text = getLevelName(userProfile.level),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    // Verification Badge or Action Trigger Button
                    if (userProfile.isEmailVerified || userProfile.isPhoneVerified) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Citizen",
                                tint = Color(0xFF2DD4BF),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "VERIFIED CITIZEN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    fontSize = 9.sp
                                ),
                                color = Color(0xFF2DD4BF)
                            )
                        }
                    } else {
                        Button(
                            onClick = { showVerifyDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.25f),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(22.dp)
                                .padding(top = 4.dp)
                                .testTag("verify_identity_btn_profile")
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verify Identity (+80 Pts)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Mini Level progression bar
                Spacer(modifier = Modifier.height(2.dp))
                val nextLevelPoints = getNextLevelThreshold(userProfile.level)
                val currentLevelPoints = getLevelThreshold(userProfile.level)
                val progress = if (nextLevelPoints > currentLevelPoints) {
                    (userProfile.points - currentLevelPoints).toFloat() / (nextLevelPoints - currentLevelPoints)
                } else 1.0f
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                }
            }
        }

        // Verification Gateway Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Anti-Bot Verification Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                if (userProfile.isEmailVerified || userProfile.isPhoneVerified) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF0D9488))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Account Fully Authenticated",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF065F46),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Your identity is validated via OTP. You have active privileges to report community issues and sign off on consensus resolutions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF0F766E)
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Gated: Anonymous Citizen",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "To maintain civic platform integrity and filter out spam bots, you must complete either Email or Phone verification before reporting issues or casting resolution votes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { showVerifyDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("profile_verify_action_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify with SMS / Email OTP (+80 Pts)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Civic Ledger & Statistics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Civic Ledger Stats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Total Civic Score Stack
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("Total Civic Score", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text("${userProfile.points} Points", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.fillMaxWidth(0.6f))

                // Current Influence Level Stack
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("Current Influence Level", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text("Level ${userProfile.level} (${getLevelName(userProfile.level)})", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.fillMaxWidth(0.6f))

                // Email Address Stack
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("Email Address", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text(
                        text = userProfile.emailAddress.ifBlank { "Not Linked" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.fillMaxWidth(0.6f))

                // Phone Number Stack
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("Phone Number", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text(
                        text = userProfile.phoneNumber.ifBlank { "Not Linked" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.fillMaxWidth(0.6f))

                // Residential Address Stack
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("Residential Address", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text(
                        text = userProfile.address.ifBlank { "Not Specified" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        // --- MY CITIZEN REPORTS ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "My Submitted Reports",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                val myIssues = issues.filter { it.reporterName == userProfile.name || it.reporterId == "current_user" }
                if (myIssues.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "You haven't submitted any civic reports yet. Submit one from the feed screen!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        myIssues.forEach { issue ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.navigateTo(AppScreen.IssueDetail(issue.id)) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = issue.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${issue.category} • ${issue.city}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatusBadge(status = issue.status)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- VERSION LABEL ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Text(
                    text = "Version 1.401v",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // --- LOGOUT ACTION ---
        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("logout_btn_profile")
        ) {
            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout & Clear Session", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationScreen(viewModel: CommunityHeroViewModel) {
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    var isSignUp by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // CivicHero App Badge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "CivicHero Community",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Version 1.401v Real-Time Ledger",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Authentication Form Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (isSignUp) "Create Civic Account" else "Sign In to CivicHero",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = if (isSignUp) {
                                "Register to report, upvote, and coordinate verified community resolutions in real-time."
                            } else {
                                "Welcome back! Enter your email and password to connect to the verified civic network."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        // Form Inputs
                        if (isSignUp) {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text("Display Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("auth_name_input")
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                        )

                        // Error message
                        authError?.let { err ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (isSignUp) {
                                    viewModel.signUpWithSupabase(email, password, displayName.ifBlank { "Citizen Hero" })
                                } else {
                                    viewModel.signInWithSupabase(email, password)
                                }
                            },
                            enabled = !isAuthenticating && email.isNotBlank() && password.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("auth_submit_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSignUp) "Register Account" else "Sign In Now",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Toggle Sign In vs Sign Up
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isSignUp) "Already have an account?" else "Don't have an account?",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            TextButton(
                                onClick = { isSignUp = !isSignUp },
                                modifier = Modifier.testTag("auth_toggle_mode_btn")
                            ) {
                                Text(
                                    text = if (isSignUp) "Sign In" else "Sign Up",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun locateUser(context: android.content.Context, onLocationFound: (Double, Double) -> Unit) {
    try {
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        var location: android.location.Location? = null
        if (isNetworkEnabled) {
            location = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
        }
        if (location == null && isGpsEnabled) {
            location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
        }
        if (location != null) {
            onLocationFound(location.latitude, location.longitude)
        } else {
            // Default coordinates for Dehradun IT Park
            onLocationFound(30.3165, 78.0322)
        }
    } catch (e: SecurityException) {
        onLocationFound(30.3165, 78.0322)
    } catch (e: Exception) {
        onLocationFound(30.3165, 78.0322)
    }
}

@Composable
fun EditProfileDialog(
    userProfile: com.example.data.UserProfile,
    onSave: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(userProfile.name) }
    var phone by remember { mutableStateOf(userProfile.phoneNumber) }
    var address by remember { mutableStateOf(userProfile.address) }
    var selectedAvatar by remember { mutableStateOf(userProfile.profilePicture) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedAvatar = uri.toString()
        }
    }

    val avatarsList = listOf(
        Pair("avatar_architect", "Architect 🏗️"),
        Pair("avatar_eco", "Eco 🌿"),
        Pair("avatar_warden", "Warden 🛣️"),
        Pair("avatar_water", "Guardian 💧"),
        Pair("avatar_green", "Activist 🌳"),
        Pair("avatar_sentry", "Sentry 🛡️")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Citizen Profile", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Interactive Preview Bubble (Symmetric)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedAvatar.startsWith("avatar_")) {
                            val emoji = when (selectedAvatar) {
                                "avatar_architect" -> "🏗️"
                                "avatar_eco" -> "🌿"
                                "avatar_warden" -> "🛣️"
                                "avatar_water" -> "💧"
                                "avatar_green" -> "🌳"
                                "avatar_sentry" -> "🛡️"
                                else -> "👤"
                            }
                            Text(emoji, fontSize = 36.sp)
                        } else {
                            coil.compose.AsyncImage(
                                model = selectedAvatar,
                                contentDescription = "Preview",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Custom Photo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            selectedAvatar = "avatar_architect"
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Reset to Preset Emoji", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Avatar Selector Title
                Text("Or Choose Civic Preset Role", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                // Avatar grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    avatarsList.take(3).forEach { (key, nameWithEmoji) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedAvatar == key) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                .border(1.dp, if (selectedAvatar == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .clickable { selectedAvatar = key }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(nameWithEmoji, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    avatarsList.takeLast(3).forEach { (key, nameWithEmoji) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedAvatar == key) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                .border(1.dp, if (selectedAvatar == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .clickable { selectedAvatar = key }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(nameWithEmoji, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Citizen Name") },
                    placeholder = { Text("e.g. John Doe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_name"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("e.g. +91 98765 43210") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_phone"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Residential Address") },
                    placeholder = { Text("e.g. EC Road, Dehradun") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_address"),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, phone, address, selectedAvatar)
                },
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Save Profile Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
