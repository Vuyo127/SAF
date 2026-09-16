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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.saf2.R

@Composable
fun OwnerDashboardScreen(navController: NavController) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("My Listings", "Applications", "Reports")

    // Mutable state for lists to make actions functional
    val listings = remember {
        mutableStateListOf(
            DashboardProperty("1", "Campus View", 2500, "Approved", listOf("NSFAS", "Bursary"), 10, 8),
            DashboardProperty("2", "Student Lodge", 3000, "Pending", listOf("Self-Funded"), 5, 5),
            DashboardProperty("3", "City Flats", 2000, "Rejected", listOf("NSFAS"), 8, 8)
        )
    }

    val applications = remember {
        mutableStateListOf(
            DashboardApplication("1", "John Doe", "NSFAS", "Campus View", "2026-09-10", "UP", "Pending"),
            DashboardApplication("2", "Sarah Smith", "Bursary", "Student Lodge", "2026-09-12", "Wits", "Accepted")
        )
    }

    val reports = remember {
        mutableStateListOf(
            DashboardReport("1", "Campus View", "Noise", "Too loud at night", "Anonymous", "2026-09-14", "Pending"),
            DashboardReport("2", "Student Lodge", "Maintenance", "Broken window", "Jane", "2026-09-13", "Resolved")
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        "Owner Panel",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Dashboard, contentDescription = null) },
                        label = { Text("Dashboard") },
                        selected = true,
                        onClick = { scope.launch { drawerState.close() } }
                    )
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        label = { Text("Add New Listing") },
                        selected = false,
                        onClick = {
                            navController.navigate("addListing")
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        label = { Text("Profile") },
                        selected = false,
                        onClick = {
                            navController.navigate("ownerprofile")
                            scope.launch { drawerState.close() }
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                        label = { Text("Logout") },
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
                    title = { Text("Owner Dashboard") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(onClick = { navController.navigate("addListing") }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Listing")
                    }
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
                // Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard("Total Listings", listings.size.toString())
                    StatCard("Occupied Students", applications.count { it.status == "Accepted" }.toString())
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
                    0 -> MyListingsTab(navController, listings)
                    1 -> ApplicationsTab(applications, listings)
                    2 -> ReportsTab(reports)
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String) {
    Card(
        modifier = Modifier.size(150.dp).padding(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun MyListingsTab(navController: NavController, listings: MutableList<DashboardProperty>) {
    LazyColumn {
        items(listings) { listing ->
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img),
                        contentDescription = "Property Image",
                        modifier = Modifier.fillMaxWidth().height(150.dp)
                    )
                    Text(listing.title, style = MaterialTheme.typography.titleMedium)
                    Text("R${listing.price}/month")
                    Text("Beds: ${listing.availableBeds}/${listing.totalBeds} available")
                    OwnerStatusBadge(listing.status)
                    Row(modifier = Modifier.padding(vertical = 4.dp)) { 
                        listing.funding.forEach { OwnerFundingBadge(it) } 
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { navController.navigate("addListing") /* Simulating edit */ }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { listings.remove(listing) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApplicationsTab(applications: MutableList<DashboardApplication>, listings: MutableList<DashboardProperty>) {
    var filterStatus by remember { mutableStateOf("All") }
    
    val filteredApps = if (filterStatus == "All") applications else applications.filter { it.status == filterStatus }

    Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        listOf("All", "Pending", "Accepted", "Rejected").forEach { status ->
            OwnerFilterChip(
                label = status, 
                selected = filterStatus == status,
                onClick = { filterStatus = status }
            )
        }
    }

    LazyColumn {
        items(filteredApps) { app ->
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(app.name, style = MaterialTheme.typography.titleMedium)
                    OwnerFundingBadge(app.funding)
                    Text("Property: ${app.property}")
                    Text("Date: ${app.date}")
                    Text("Institution: ${app.institution}")
                    OwnerStatusBadge(app.status)
                    
                    if (app.status == "Pending") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { 
                                    val index = applications.indexOf(app)
                                    if (index != -1) {
                                        applications[index] = app.copy(status = "Accepted")
                                        // Count down available beds
                                        val propIndex = listings.indexOfFirst { it.title == app.property }
                                        if (propIndex != -1) {
                                            val property = listings[propIndex]
                                            if (property.availableBeds > 0) {
                                                listings[propIndex] = property.copy(availableBeds = property.availableBeds - 1)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Accept") }
                            
                            OutlinedButton(
                                onClick = { 
                                    val index = applications.indexOf(app)
                                    if (index != -1) applications[index] = app.copy(status = "Rejected")
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Reject") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsTab(reports: MutableList<DashboardReport>) {
    LazyColumn {
        items(reports) { report ->
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(report.property, style = MaterialTheme.typography.titleMedium)
                    Text("Reason: ${report.reason}", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text(report.description)
                    Text("Reporter: ${report.reporter}")
                    Text("Date: ${report.date}")
                    OwnerStatusBadge(report.status)
                    
                    if (report.status == "Pending") {
                        Button(
                            onClick = { 
                                val index = reports.indexOf(report)
                                if (index != -1) reports[index] = report.copy(status = "Resolved")
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) { Text("Mark as Resolved") }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerStatusBadge(status: String) {
    val color = when (status) {
        "Approved", "Accepted", "Resolved" -> MaterialTheme.colorScheme.primary
        "Pending" -> MaterialTheme.colorScheme.secondary
        "Rejected" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(shape = CircleShape, color = color.copy(alpha = 0.2f)) {
        Text(status, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun OwnerFundingBadge(label: String) {
    Surface(
        modifier = Modifier.padding(end = 4.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun OwnerFilterChip(label: String, selected: Boolean = false, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.padding(end = 8.dp).clickable { onClick() },
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

data class DashboardProperty(
    val id: String,
    val title: String,
    val price: Int,
    val status: String,
    val funding: List<String>,
    val totalBeds: Int,
    val availableBeds: Int
)
data class DashboardApplication(val id: String, val name: String, val funding: String, val property: String, val date: String, val institution: String, val status: String)
data class DashboardReport(val id: String, val property: String, val reason: String, val description: String, val reporter: String, val date: String, val status: String)
