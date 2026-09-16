@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.saf2.ui.theme.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.saf2.R
import kotlinx.coroutines.launch

@Composable
fun OwnerProfile(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("My Properties", "Applications", "Reviews", "Reports")

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var isEditing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("Tsoseletso Mashiane") }
    var businessName by remember { mutableStateOf("Mashiane Properties PTY LTD") }
    var email by remember { mutableStateOf("tsoseletso@owner.com") }
    var phone by remember { mutableStateOf("+27 123 456 789") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        "Owner Menu",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

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
                        onClick = {
                            scope.launch { drawerState.close() }
                        }
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
                    title = { Text("Owner Profile") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (isEditing) {
                            IconButton(onClick = { isEditing = false }) {
                                Icon(Icons.Filled.Check, contentDescription = "Save")
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
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isEditing) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = businessName,
                                onValueChange = { businessName = it },
                                label = { Text("Business Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Contact Phone") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(businessName, style = MaterialTheme.typography.bodyMedium)
                            Text(email, style = MaterialTheme.typography.bodySmall)
                            Text(phone, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OwnerVerificationBadge("Verified Owner")
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            ProfileStat("Properties", "3")
                            ProfileStat("Rating", "4.8")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))


                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> OwnerPropertiesPage()
                        1 -> OwnerApplicationsPage()
                        2 -> OwnerReviewsPage()
                        3 -> OwnerReportsPage()
                    }
                }

                Button(
                    onClick = { navController.navigate("login") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Log Out")
                }
            }
        }
    }
}

@Composable
fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun OwnerPropertiesPage() {
    val properties = listOf(
        OwnerProfileProperty("1", "Campus View Flat", 2500, "Approved"),
        OwnerProfileProperty("2", "Student Lodge Mbombela", 3000, "Pending"),
        OwnerProfileProperty("3", "Kamagugu Bachelor", 1800, "Approved")
    )

    LazyColumn {
        items(properties) { prop ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp)
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(prop.title, style = MaterialTheme.typography.titleSmall)
                        Text("R${prop.price}/month")
                        OwnerProfileStatusBadge(prop.status)
                    }
                }
            }
        }
    }
}

@Composable
fun OwnerApplicationsPage() {
    val applications = listOf(
        OwnerProfileApplication("John Doe", "Campus View", "2026-09-10", "Pending"),
        OwnerProfileApplication("Sarah Smith", "Student Lodge", "2026-09-12", "Accepted")
    )

    LazyColumn {
        items(applications) { app ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Applicant: ${app.name}", fontWeight = FontWeight.SemiBold)
                    Text("Property: ${app.property}")
                    Text("Date: ${app.date}")
                    OwnerProfileStatusBadge(app.status)
                }
            }
        }
    }
}

@Composable
fun OwnerReviewsPage() {
    val reviews = listOf(
        OwnerProfileReview("Alice", "Great landlord, very responsive.", 5, "2026-08-15"),
        OwnerProfileReview("Bob", "Good place, but wifi was slow.", 4, "2026-07-20")
    )

    LazyColumn {
        items(reviews) { review ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(review.tenant, fontWeight = FontWeight.Bold)
                    Text("⭐ ${review.stars}")
                    Text(review.comment)
                    Text(review.date, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun OwnerReportsPage() {
    val reports = listOf(
        OwnerProfileReport("Campus View", "Plumbing", "Tap is leaking", "Pending"),
        OwnerProfileReport("Student Lodge", "Electricity", "Light flickering", "Resolved")
    )

    LazyColumn {
        items(reports) { report ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(report.property, fontWeight = FontWeight.Bold)
                    Text("Issue: ${report.type}")
                    Text(report.desc)
                    OwnerProfileStatusBadge(report.status)
                }
            }
        }
    }
}

@Composable
private fun OwnerVerificationBadge(label: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun OwnerProfileStatusBadge(status: String) {
    val color = when (status) {
        "Approved", "Accepted", "Resolved" -> MaterialTheme.colorScheme.primary
        "Pending" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
    Surface(shape = CircleShape, color = color.copy(alpha = 0.2f)) {
        Text(status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
    }
}

data class OwnerProfileProperty(val id: String, val title: String, val price: Int, val status: String)
data class OwnerProfileApplication(val name: String, val property: String, val date: String, val status: String)
data class OwnerProfileReview(val tenant: String, val comment: String, val stars: Int, val date: String)
data class OwnerProfileReport(val property: String, val type: String, val desc: String, val status: String)
