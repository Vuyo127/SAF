@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.saf2.ui.theme.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.saf2.R
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Applications", "Reviews", "Reports", "Favorites")

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Editable profile fields
    var isEditing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("Tsoseletso Mashiane") }
    var email by remember { mutableStateOf("tsoseletso@email.com") }
    var phone by remember { mutableStateOf("+27 123 456 789") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                    NavigationDrawerItem(
                        label = { Text("Home / Find Accommodation") },
                        selected = false,
                        onClick = {
                            navController.navigate("accommodation")
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        label = { Text("My Profile") },
                        selected = true,
                        onClick = {
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        label = { Text("My Reviews") },
                        selected = false,
                        onClick = {
                            navController.navigate("reviews")
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        label = { Text("My Applications") },
                        selected = false,
                        onClick = {
                            navController.navigate("applications")
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        label = { Text("Settings") },
                        selected = false,
                        onClick = {
                            navController.navigate("settings")
                            scope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f)) // pushes logout to bottom

                    NavigationDrawerItem(
                        label = { Text("Log Out") },
                        selected = false,
                        onClick = {
                            navController.navigate("login")
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Profile") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (isEditing) {
                            IconButton(onClick = {
                                // TODO: FIREBASE - save name/email/phone to the "users" doc for the current uid
                                isEditing = false
                            }) {
                                Icon(Icons.Filled.Check, contentDescription = "Save")
                            }
                            IconButton(onClick = {
                                // Discard changes made while editing
                                name = "Tsoseletso Mashiane"
                                email = "tsoseletso@email.com"
                                phone = "+27 123 456 789"
                                isEditing = false
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel")
                            }
                        } else {
                            IconButton(onClick = { isEditing = true }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit")
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Header Card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box {
                            Image(
                                painter = painterResource(id = R.drawable.profile),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.size(90.dp)
                            )
                            if (isEditing) {
                                Icon(
                                    Icons.Filled.CameraAlt,
                                    contentDescription = "Edit Photo",
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(24.dp)
                                        .clickable {
                                            // TODO: launch an image picker and upload the result to Firebase Storage,
                                            // then save the returned URL to the user's "users" doc
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isEditing) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Phone") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(name, style = MaterialTheme.typography.titleMedium)
                            Text(email)
                            Text(phone)
                        }

                        Row {
                            ProfileFundingBadge("Student")
                            ProfileFundingBadge("NSFAS")
                            ProfileFundingBadge("Funza Lushaka")
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text("Applications: 5")
                            Text("Reviews: 3")
                            Text("Reports: 2")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> ApplicationsPage()
                    1 -> ReviewsPage()
                    2 -> ReportsPage()
                    3 -> FavoritesPage()
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { /* TODO: FirebaseAuth.signOut() */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Log Out")
                }
            }
        }
    }
}

@Composable
fun ApplicationsPage() {
    val applications = listOf(
        ProfileApplication("Campus View", 2500, "2026-09-10", "Pending"),
        ProfileApplication("Student Lodge", 3000, "2026-09-12", "Accepted")
    )

    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        ProfileFilterChip("All")
        ProfileFilterChip("Pending")
        ProfileFilterChip("Accepted")
        ProfileFilterChip("Rejected")
    }

    LazyColumn {
        items(applications) { app ->
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img),
                        contentDescription = "Property Image",
                        modifier = Modifier.size(60.dp)
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(app.title, style = MaterialTheme.typography.titleMedium)
                        Text("R${app.price}/month")
                        Text("Applied: ${app.date}")
                        ProfileStatusBadge(app.status)
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewsPage() {
    val reviews = listOf(
        ProfileReview("Campus View", 5, "Great place!", "2026-09-10"),
        ProfileReview("Student Lodge", 4, "Safe and clean.", "2026-09-12")
    )

    LazyColumn {
        items(reviews) { review ->
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(review.property, style = MaterialTheme.typography.titleMedium)
                    Text("⭐ ${review.stars}")
                    Text(review.comment)
                    Text("Date: ${review.date}")
                    Row {
                        IconButton(onClick = { /* TODO: Edit Review */ }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { /* TODO: Delete Review */ }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsPage() {
    val reports = listOf(
        ProfileReport("Campus View", "Noise", "Too loud at night", "Pending", "2026-09-14"),
        ProfileReport("Student Lodge", "Maintenance", "Broken window", "Resolved", "2026-09-13")
    )

    LazyColumn {
        items(reports) { report ->
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(report.property, style = MaterialTheme.typography.titleMedium)
                    Text("Reason: ${report.reason}")
                    Text(report.description)
                    ProfileStatusBadge(report.status)
                    Text("Date: ${report.date}")
                }
            }
        }
    }
}

@Composable
fun FavoritesPage() {
    val favorites = listOf(
        ProfileFavoriteProperty("1", "Campus View", 2500, "Approved", listOf("NSFAS")),
        ProfileFavoriteProperty("2", "Student Lodge", 3000, "Approved", listOf("Bursary"))
    )

    LazyColumn {
        items(favorites) { fav ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { /* TODO: Navigate to Detail */ }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(fav.title, style = MaterialTheme.typography.titleMedium)
                    Text("R${fav.price}/month")
                    Row { fav.funding.forEach { ProfileFundingBadge(it) } }
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Unfavorite",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStatusBadge(status: String) {
    val color = when (status) {
        "Approved", "Accepted", "Resolved" -> MaterialTheme.colorScheme.primary
        "Pending" -> MaterialTheme.colorScheme.secondary
        "Rejected", "Reviewed" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(shape = CircleShape, color = color.copy(alpha = 0.2f)) {
        Text(status, modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun ProfileFundingBadge(label: String) {
    Surface(
        modifier = Modifier.padding(end = 4.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun ProfileFilterChip(label: String) {
    Surface(
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable { /* TODO: Filter */ },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(label, modifier = Modifier.padding(8.dp))
    }
}

data class ProfileApplication(val title: String, val price: Int, val date: String, val status: String)
data class ProfileReview(val property: String, val stars: Int, val comment: String, val date: String)
data class ProfileReport(val property: String, val reason: String, val description: String, val status: String, val date: String)
data class ProfileFavoriteProperty(val id: String, val title: String, val price: Int, val status: String, val funding: List<String>)