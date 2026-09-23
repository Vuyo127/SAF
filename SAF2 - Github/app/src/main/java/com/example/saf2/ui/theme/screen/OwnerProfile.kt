@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.saf2.ui.theme.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
fun OwnerProfile(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    listingsViewModel: ListingsViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("My Properties", "Applications", "Reviews", "Reports")

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        authViewModel.getProfile()
        listingsViewModel.getMyListings()
        listingsViewModel.getApplications()
        listingsViewModel.getReviews("")
    }
    // TODO BACKEND: backend/Database must fill in GET /api/users/me for owner profile details and businessName
    // TODO BACKEND: backend/Database must fill in GET /api/listings?ownerId=me
    // TODO BACKEND: backend/Database must fill in GET /api/applications?ownerId=me
    // TODO BACKEND: backend/Database must fill in GET /api/reviews?ownerId=me
    // TODO BACKEND: backend/Database must fill in GET /api/reports?ownerId=me

    val currentUser by authViewModel.currentUser.collectAsState()
    val myListings by listingsViewModel.myListings.collectAsState()
    val applications by listingsViewModel.applications.collectAsState()
    val reviews by listingsViewModel.reviews.collectAsState()
    val reports by listingsViewModel.reports.collectAsState()

    // Temporary Edit Buffer State
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editBusinessName by remember { mutableStateOf("") }
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
                        text = "Owner Menu",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Column(modifier = Modifier.fillMaxSize().padding(vertical = 16.dp)) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Dashboard, contentDescription = null) },
                        label = { Text("Dashboard") },
                        selected = false,
                        onClick = {
                            navController.navigate("ownerDashboard")
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        label = { Text("My Profile") },
                        selected = true,
                        onClick = { scope.launch { drawerState.close() } },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        label = { Text("Add Listing") },
                        selected = false,
                        onClick = {
                            navController.navigate("addListing")
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
                    title = { Text("Owner Profile", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (!isEditing) {
                            IconButton(onClick = {
                                editName = currentUser?.name ?: ""
                                editBusinessName = "Owner Properties PTY LTD"
                                editEmail = currentUser?.email ?: ""
                                editPhone = currentUser?.phone ?: ""
                                isEditing = true
                            }) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
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
                                label = { Text("Full Name") },
                                singleLine = true,
                                isError = !isEditNameValid,
                                supportingText = { if (!isEditNameValid) Text("Name is required", color = MaterialTheme.colorScheme.error) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editBusinessName,
                                onValueChange = { editBusinessName = it },
                                label = { Text("Business Name") },
                                singleLine = true,
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
                                supportingText = { if (!isEditEmailValid) Text("Enter a valid email", color = MaterialTheme.colorScheme.error) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                label = { Text("Contact Phone (+27)") },
                                singleLine = true,
                                isError = !isEditPhoneValid,
                                supportingText = { if (!isEditPhoneValid) Text("Enter valid SA phone (+27...)", color = MaterialTheme.colorScheme.error) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Explicit Save and Cancel Buttons
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
                                            // TODO BACKEND: backend/Database must fill in PUT /api/users/me for owner profile
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
                            Text(currentUser?.name ?: "Property Owner", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Registered Property Owner", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
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


                        // Dynamic owner average rating calculated from received reviews
                        val averageRating = remember(reviews) {
                            if (reviews.isEmpty()) {
                                "0.0"
                            } else {
                                val avg = reviews.map { it.stars }.average()
                                String.format(java.util.Locale.US, "%.1f", avg)
                            }
                        }
                        // TODO BACKEND: backend/Database must fill in GET /api/reviews/average?ownerId=me or calculate average rating from real DB reviews table.

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            ProfileStat("Properties", myListings.size.toString())
                            Divider(modifier = Modifier.height(40.dp).width(1.dp).align(Alignment.CenterVertically), color = MaterialTheme.colorScheme.outline)
                            ProfileStat("Rating", averageRating)
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

                Box(modifier = Modifier.heightIn(min = 250.dp, max = 400.dp).padding(top = 8.dp)) {
                    when (selectedTab) {
                        0 -> OwnerPropertiesPage(myListings)
                        1 -> OwnerApplicationsPage(applications)
                        2 -> OwnerReviewsPage(reviews)
                        3 -> OwnerReportsPage(reports)
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
fun OwnerPropertiesPage(properties: List<DashboardProperty>) {
    // TODO BACKEND: backend/Database must fill in GET /api/listings?ownerId=me
    if (properties.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No properties listed yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn {
            items(properties) { prop ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        PropertyImage(
                            imageData = R.drawable.img,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                            Text(prop.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("R${prop.price}/month", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                        }
                        OwnerProfileStatusBadge(prop.status)
                    }
                }
            }
        }
    }
}

@Composable
fun OwnerApplicationsPage(applications: List<DashboardApplication>) {
    // TODO BACKEND: backend/Database must fill in GET /api/applications?ownerId=me
    if (applications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No applications received yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn {
            items(applications) { app ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Applicant: ${app.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Property: ${app.property}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Date: ${app.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        OwnerProfileStatusBadge(app.status)
                    }
                }
            }
        }
    }
}

@Composable
fun OwnerReviewsPage(reviews: List<ProfileReview>) {
    // TODO BACKEND: backend/Database must fill in GET /api/reviews?ownerId=me
    if (reviews.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No reviews yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(review.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun OwnerReportsPage(reports: List<DashboardReport>) {
    // TODO BACKEND: backend/Database must fill in GET /api/reports?ownerId=me
    if (reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No reports yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(report.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        OwnerProfileStatusBadge(report.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerVerificationBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OwnerProfileStatusBadge(status: String) {
    val color = when (status) {
        "Approved", "Accepted", "Resolved" -> MaterialTheme.colorScheme.secondary
        "Pending" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
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
