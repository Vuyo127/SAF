@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.saf2.ui.theme.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.saf2.R
import com.example.saf2.ui.theme.components.ConfirmDialog
import com.example.saf2.ui.theme.components.PropertyImage
import com.example.saf2.ui.theme.components.isValidEmail
import com.example.saf2.ui.theme.components.isValidSaPhone
import com.example.saf2.ui.theme.components.showToast
import com.example.saf2.viewmodel.AuthViewModel
import com.example.saf2.viewmodel.ListingsViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    listingsViewModel: ListingsViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Applications", "Reviews", "Reports", "Favorites")

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Fetch user profile & user data on screen launch
    LaunchedEffect(Unit) {
        authViewModel.getProfile()
        listingsViewModel.getApplications()
        listingsViewModel.getReviews("")
        listingsViewModel.getFavorites()
    }
    // TODO BACKEND: backend/Database must fill in GET /api/users/me, GET /api/applications?studentId=me, GET /api/reviews?studentId=me, and GET /api/favorites?studentId=me

    val currentUser by authViewModel.currentUser.collectAsState()
    val studentApplications by listingsViewModel.studentApplications.collectAsState()
    val userReviews by listingsViewModel.reviews.collectAsState()
    val userReports by listingsViewModel.reports.collectAsState()
    val userFavorites by listingsViewModel.favorites.collectAsState()

    // Temporary Edit Buffer State
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }

    var showLogoutDialog by remember { mutableStateOf(false) }

    val isEditNameValid = editName.trim().length >= 2
    val isEditEmailValid = isValidEmail(editEmail)
    val isEditPhoneValid = isValidSaPhone(editPhone)

    ConfirmDialog(
        show = showLogoutDialog,
        title = "Log Out",
        message = "Are you sure you want to log out?",
        confirmText = "Log Out",
        dismissText = "Cancel",
        isDestructive = true,
        onConfirm = {
            showToast(context, "Logged out")
            navController.navigate("login") {
                popUpTo(0)
            }
        },
        onDismiss = { showLogoutDialog = false }
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Student Menu",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Column(modifier = Modifier.fillMaxSize().padding(vertical = 16.dp)) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("Home / Find Accommodation") },
                        selected = false,
                        onClick = {
                            navController.navigate("accommodation")
                            scope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                        label = { Text("Log Out") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showLogoutDialog = true
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Profile", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (!isEditing) {
                            IconButton(onClick = {
                                editName = currentUser?.name ?: ""
                                editEmail = currentUser?.email ?: ""
                                editPhone = currentUser?.phone ?: ""
                                isEditing = true
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .fillMaxSize()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box {
                            Image(
                                painter = painterResource(id = R.drawable.profile),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.background)
                            )
                            if (isEditing) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(32.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondary
                                ) {
                                    Icon(
                                        Icons.Filled.CameraAlt,
                                        contentDescription = "Edit Photo",
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .fillMaxSize(),
                                        tint = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isEditing) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Name") },
                                singleLine = true,
                                isError = !isEditNameValid,
                                supportingText = { if (!isEditNameValid) Text("Name is required", color = MaterialTheme.colorScheme.error) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editEmail,
                                onValueChange = { editEmail = it },
                                label = { Text("Email") },
                                singleLine = true,
                                isError = !isEditEmailValid,
                                supportingText = { if (!isEditEmailValid) Text("Valid email required", color = MaterialTheme.colorScheme.error) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                label = { Text("Phone (+27)") },
                                singleLine = true,
                                isError = !isEditPhoneValid,
                                supportingText = { if (!isEditPhoneValid) Text("Enter valid SA phone (+27...)", color = MaterialTheme.colorScheme.error) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Save and Cancel buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { isEditing = false },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        if (isEditNameValid && isEditEmailValid && isEditPhoneValid) {
                                            authViewModel.updateProfile(editName, editEmail, editPhone)
                                            // TODO BACKEND: backend/Database must fill in PUT /api/users/me
                                            isEditing = false
                                            showToast(context, "Profile saved successfully")
                                        } else {
                                            showToast(context, "Please fix profile validation errors")
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Save Changes", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text(
                                currentUser?.name ?: "Student User",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(currentUser?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(currentUser?.phone ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            ProfileStat("Applications", studentApplications.size.toString())
                            Divider(modifier = Modifier.height(40.dp).width(1.dp).align(Alignment.CenterVertically), color = MaterialTheme.colorScheme.outline)
                            ProfileStat("Reviews", userReviews.size.toString())
                            Divider(modifier = Modifier.height(40.dp).width(1.dp).align(Alignment.CenterVertically), color = MaterialTheme.colorScheme.outline)
                            ProfileStat("Reports", userReports.size.toString())
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = TabRowDefaults.run { Modifier.tabIndicatorOffset(tabPositions[selectedTab]) },
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.heightIn(min = 250.dp, max = 400.dp)) {
                    when (selectedTab) {
                        0 -> ApplicationsPage(studentApplications)
                        1 -> ReviewsPage(userReviews)
                        2 -> ReportsPage(userReports)
                        3 -> FavoritesPage(userFavorites)
                    }
                }

                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Log Out", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ApplicationsPage(applications: List<ProfileApplication>) {
    // TODO BACKEND: backend/Database must fill in GET /api/applications?studentId=me to return student's applications.
    var filterStatus by remember { mutableStateOf("All") }

    val filteredApps = if (filterStatus == "All") applications else applications.filter { it.status == filterStatus }

    Column {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            listOf("All", "Pending", "Accepted", "Rejected").forEach { status ->
                ProfileFilterChip(
                    label = status,
                    selected = filterStatus == status,
                    onClick = { filterStatus = status }
                )
            }
        }

        if (filteredApps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("No applications submitted yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                items(filteredApps) { app ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PropertyImage(
                                imageData = R.drawable.img,
                                contentDescription = "Property Image",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                                Text(app.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("R${app.price}/month", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                                Text("Applied: ${app.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            ProfileStatusBadge(app.status)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewsPage(reviews: List<ProfileReview>) {
    // TODO BACKEND: backend/Database must fill in GET /api/reviews?studentId=me to return student's reviews.
    val context = LocalContext.current
    var reviewToDelete by remember { mutableStateOf<ProfileReview?>(null) }

    ConfirmDialog(
        show = reviewToDelete != null,
        title = "Delete Review",
        message = "Are you sure you want to delete this review?",
        confirmText = "Delete",
        dismissText = "Cancel",
        isDestructive = true,
        onConfirm = {
            // TODO BACKEND: backend/Database must fill in DELETE /api/reviews/{id}
            showToast(context, "Review deleted")
            reviewToDelete = null
        },
        onDismiss = { reviewToDelete = null }
    )

    if (reviews.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No reviews written yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn {
            items(reviews) { review ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(review.property, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("⭐ ${review.stars}", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(review.comment, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Text("Date: ${review.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { reviewToDelete = review }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsPage(reports: List<DashboardReport>) {
    // TODO BACKEND: backend/Database must fill in GET /api/reports?studentId=me to return student's submitted reports.
    if (reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No reports submitted yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn {
            items(reports) { report ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(report.property, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Reason: ${report.reason}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Text("Date: ${report.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.weight(1f))
                            ProfileStatusBadge(report.status)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesPage(favorites: List<ProfileFavoriteProperty>) {
    // TODO BACKEND: backend/Database must fill in GET /api/favorites?studentId=me to return student's saved favorite properties.
    val context = LocalContext.current

    if (favorites.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No favorites saved.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn {
            items(favorites) { fav ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        PropertyImage(
                            imageData = R.drawable.img,
                            contentDescription = fav.title,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(fav.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("R${fav.price}/month", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row { fav.funding.forEach { ProfileFundingBadge(it) } }
                        }
                        IconButton(onClick = {
                            // TODO BACKEND: backend/Database must fill in DELETE /api/favorites/{fav.id}
                            showToast(context, "Removed from favorites")
                        }) {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = "Unfavorite",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatusBadge(status: String) {
    val color = when (status) {
        "Approved", "Accepted", "Resolved" -> MaterialTheme.colorScheme.primary
        "Pending" -> Color(0xFFF59E0B) // Amber/Orange
        "Rejected" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            status,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProfileFundingBadge(label: String) {
    val badgeColor = if (label == "NSFAS") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
    Surface(
        modifier = Modifier.padding(end = 6.dp),
        shape = CircleShape,
        color = badgeColor.copy(alpha = 0.15f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ProfileFilterChip(label: String, selected: Boolean = false, onClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier.padding(end = 8.dp).clickable { onClick() },
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

data class ProfileApplication(val title: String, val price: Int, val date: String, val status: String)
data class ProfileReview(val property: String, val stars: Int, val comment: String, val date: String)
data class ProfileReport(val property: String, val reason: String, val description: String, val status: String, val date: String)
data class ProfileFavoriteProperty(val id: String, val title: String, val price: Int, val status: String, val funding: List<String>)
