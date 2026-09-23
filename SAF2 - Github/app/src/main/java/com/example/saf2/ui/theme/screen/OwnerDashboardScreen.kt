@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.saf2.ui.theme.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.saf2.R
import com.example.saf2.ui.theme.components.ConfirmDialog
import com.example.saf2.ui.theme.components.PropertyImage
import com.example.saf2.ui.theme.components.showToast
import com.example.saf2.viewmodel.AuthViewModel
import com.example.saf2.viewmodel.ListingsViewModel
import kotlinx.coroutines.launch

// Shared state repository for owner listings so updates persist across navigation
object OwnerListingsData {
    val listings = mutableStateListOf<DashboardProperty>()
}

@Composable
fun OwnerDashboardScreen(
    navController: NavController,
    viewModel: ListingsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("My Listings", "Applications", "Reports")

    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.getMyListings()
        viewModel.getApplications()
    }
    // TODO BACKEND: backend/Database must fill in GET /api/listings?ownerId=currentUserId to fetch owner listings.
    // TODO BACKEND: backend/Database must fill in GET /api/applications?ownerId=currentUserId to fetch applications for owner's properties.
    // TODO BACKEND: backend/Database must fill in GET /api/reports?ownerId=currentUserId to fetch reports against owner's properties.

    val listingsFlow by viewModel.myListings.collectAsState()
    val applicationsFlow by viewModel.applications.collectAsState()
    val reportsFlow by viewModel.reports.collectAsState()

    // Sync listings flow to state list for interactive mutation
    val listings = remember(listingsFlow) {
        OwnerListingsData.listings.clear()
        OwnerListingsData.listings.addAll(listingsFlow)
        OwnerListingsData.listings
    }

    val applications = remember(applicationsFlow) {
        mutableStateListOf<DashboardApplication>().apply { addAll(applicationsFlow) }
    }

    val reports = remember(reportsFlow) {
        mutableStateListOf<DashboardReport>().apply { addAll(reportsFlow) }
    }

    val totalListings by remember { derivedStateOf { listings.size } }
    val occupiedStudents by remember { derivedStateOf { applications.count { it.status == "Accepted" } } }

    val pendingReportsCount by remember { derivedStateOf { reports.count { it.status == "Pending" } } }
    var forceFlagged by remember { mutableStateOf(false) }
    val isFlagged by remember { derivedStateOf { pendingReportsCount >= 10 || forceFlagged } }

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
                            scope.launch { drawerState.close() }
                            if (isFlagged) {
                                showToast(context, "Account flagged: Adding listings is disabled")
                            } else {
                                navController.navigate("addListing")
                            }
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
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "SAF Logo",
                                modifier = Modifier.size(32.dp).clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Owner Dashboard")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(
                        onClick = {
                            if (isFlagged) {
                                showToast(context, "Account flagged: Adding new listings is disabled.")
                            } else {
                                navController.navigate("addListing")
                            }
                        },
                        containerColor = if (isFlagged) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add Listing",
                            tint = if (isFlagged) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {

                if (isFlagged) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "⚠️ Your account is flagged due to 10+ reports. Team reviewing it.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Dynamic stats counters from state
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard("Total Listings", totalListings.toString())
                    StatCard("Occupied Students", occupiedStudents.toString())
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

                Spacer(modifier = Modifier.height(8.dp))

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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun MyListingsTab(navController: NavController, listings: MutableList<DashboardProperty>) {
    val context = LocalContext.current
    var listingToDelete by remember { mutableStateOf<DashboardProperty?>(null) }

    ConfirmDialog(
        show = listingToDelete != null,
        title = "Delete Listing",
        message = "Are you sure you want to delete '${listingToDelete?.title}'?",
        confirmText = "Delete",
        dismissText = "Cancel",
        isDestructive = true,
        onConfirm = {
            listingToDelete?.let {
                listings.remove(it)
                // TODO BACKEND: backend/Database must fill in DELETE /api/listings/{it.id}
                showToast(context, "Listing deleted")
            }
            listingToDelete = null
        },
        onDismiss = { listingToDelete = null }
    )

    if (listings.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No listings added yet.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn {
            items(listings) { listing ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        PropertyImage(
                            imageData = R.drawable.img,
                            contentDescription = listing.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = listing.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            softWrap = true,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("R${listing.price}/month", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                        Text("Beds: ${listing.availableBeds}/${listing.totalBeds} available", style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OwnerStatusBadge(listing.status)
                            Spacer(modifier = Modifier.width(8.dp))
                            listing.funding.forEach { OwnerFundingBadge(it) }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = { navController.navigate("editListing/${listing.id}") },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { listingToDelete = listing },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApplicationsTab(applications: MutableList<DashboardApplication>, listings: MutableList<DashboardProperty>) {
    val context = LocalContext.current
    var filterStatus by remember { mutableStateOf("All") }
    var appToAccept by remember { mutableStateOf<DashboardApplication?>(null) }
    var appToReject by remember { mutableStateOf<DashboardApplication?>(null) }

    val filteredApps = if (filterStatus == "All") applications else applications.filter { it.status == filterStatus }

    ConfirmDialog(
        show = appToAccept != null,
        title = "Accept Application",
        message = "Are you sure you want to accept the application from ${appToAccept?.name}?",
        confirmText = "Accept",
        dismissText = "Cancel",
        onConfirm = {
            appToAccept?.let { app ->
                val index = applications.indexOf(app)
                if (index != -1) {
                    applications[index] = app.copy(status = "Accepted")
                    val propIndex = listings.indexOfFirst { it.title == app.property }
                    if (propIndex != -1) {
                        val property = listings[propIndex]
                        if (property.availableBeds > 0) {
                            listings[propIndex] = property.copy(availableBeds = property.availableBeds - 1)
                        }
                    }
                    // TODO BACKEND: backend/Database must fill in PUT /api/applications/{app.id}/status { status: "ACCEPTED" }
                    showToast(context, "Application accepted")
                }
            }
            appToAccept = null
        },
        onDismiss = { appToAccept = null }
    )

    ConfirmDialog(
        show = appToReject != null,
        title = "Reject Application",
        message = "Are you sure you want to reject the application from ${appToReject?.name}?",
        confirmText = "Reject",
        dismissText = "Cancel",
        isDestructive = true,
        onConfirm = {
            appToReject?.let { app ->
                val index = applications.indexOf(app)
                if (index != -1) {
                    applications[index] = app.copy(status = "Rejected")
                    // TODO BACKEND: backend/Database must fill in PUT /api/applications/{app.id}/status { status: "REJECTED" }
                    showToast(context, "Application rejected")
                }
            }
            appToReject = null
        },
        onDismiss = { appToReject = null }
    )

    Column {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            listOf("All", "Pending", "Accepted", "Rejected").forEach { status ->
                FilterChip(
                    selected = filterStatus == status,
                    onClick = { filterStatus = status },
                    label = { Text(status, modifier = Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.padding(end = 8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No applications found.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                items(filteredApps) { app ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(app.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OwnerFundingBadge(app.funding)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Property: ${app.property}", style = MaterialTheme.typography.bodyMedium)
                            Text("Date: ${app.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Institution: ${app.institution}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            OwnerStatusBadge(app.status)
                            
                            if (app.status == "Pending") {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { appToAccept = app },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Accept") }
                                    
                                    OutlinedButton(
                                        onClick = { appToReject = app },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Reject") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsTab(reports: MutableList<DashboardReport>) {
    val context = LocalContext.current
    var reportToResolve by remember { mutableStateOf<DashboardReport?>(null) }

    ConfirmDialog(
        show = reportToResolve != null,
        title = "Resolve Report",
        message = "Are you sure you want to mark this report as resolved?",
        confirmText = "Resolve",
        dismissText = "Cancel",
        onConfirm = {
            reportToResolve?.let { report ->
                val index = reports.indexOf(report)
                if (index != -1) {
                    reports[index] = report.copy(status = "Resolved")
                    // TODO BACKEND: backend/Database must fill in PUT /api/reports/{report.id}/resolve
                    showToast(context, "Report marked as resolved")
                }
            }
            reportToResolve = null
        },
        onDismiss = { reportToResolve = null }
    )

    if (reports.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No reports yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn {
            items(reports) { report ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(report.property, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Reason: ${report.reason}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Text(report.description, style = MaterialTheme.typography.bodyMedium)
                        Text("Reporter: ${report.reporter}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Date: ${report.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        OwnerStatusBadge(report.status)
                        
                        if (report.status == "Pending") {
                            Button(
                                onClick = { reportToResolve = report },
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                            ) { Text("Mark as Resolved") }
                        }
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
        "Pending" -> Color(0xFFF59E0B)
        "Rejected" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(shape = CircleShape, color = color.copy(alpha = 0.15f)) {
        Text(
            status,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OwnerFundingBadge(label: String) {
    Surface(
        modifier = Modifier.padding(end = 4.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
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
