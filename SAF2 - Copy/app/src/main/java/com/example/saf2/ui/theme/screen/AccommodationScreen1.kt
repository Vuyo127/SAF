@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.saf2.ui.theme.screen

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
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
import com.example.saf2.ui.theme.components.showToast
import com.example.saf2.viewmodel.AuthViewModel
import com.example.saf2.viewmodel.ListingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AccommodationScreen1(
    navController: NavController,
    viewModel: ListingsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var isNsfasAccredited by remember { mutableStateOf(false) }
    var selectedRoomType by remember { mutableStateOf("All") }

    val currentUser by authViewModel.currentUser.collectAsState()
    // TODO BACKEND: backend/Database must fill in GET /api/users/me to return logged in user profile, name, profileImageUrl.

    // 300ms Search Debounce
    LaunchedEffect(searchQuery) {
        delay(300L)
        debouncedQuery = searchQuery
    }

    // Frontend state ready for backend/database connection
    val listings by viewModel.approvedListings.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.getApprovedListings()
    }
    // TODO BACKEND: backend/Database must fill in GET /api/listings?status=APPROVED to return real properties from DB with imageUrl, title, price, location, roomType, rating, fundingTypes, and ownerId.

    // Filter and Search Logic
    val filteredProperties = remember(listings, debouncedQuery, isNsfasAccredited, selectedRoomType) {
        var filtered = if (debouncedQuery.isBlank()) {
            listings
        } else {
            listings.filter {
                it.title.contains(debouncedQuery, ignoreCase = true) ||
                        it.location.contains(debouncedQuery, ignoreCase = true)
            }
        }

        if (isNsfasAccredited) filtered = filtered.filter { it.isNsfasAccredited }
        if (selectedRoomType != "All") filtered = filtered.filter { it.roomType.equals(selectedRoomType, ignoreCase = true) }

        filtered
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                // Drawer Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "SAF Finder",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentUser?.name ?: "Guest User",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxSize().padding(vertical = 16.dp)) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("Home / Find Accommodation") },
                        selected = true,
                        onClick = { scope.launch { drawerState.close() } },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null) },
                        label = { Text("Log In / Sign Up") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("login")
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
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "SAF Logo",
                                modifier = Modifier.size(32.dp).clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SAF", fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        Text(
                            text = "Login",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { navController.navigate("login") }
                                .padding(horizontal = 12.dp)
                        )
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
            Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by location, title") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    AccommodationFilterChip1("All", selectedRoomType == "All" && !isNsfasAccredited) {
                        selectedRoomType = "All"; isNsfasAccredited = false
                    }
                    AccommodationFilterChip1("Single", selectedRoomType == "Single") { selectedRoomType = if (selectedRoomType == "Single") "All" else "Single" }
                    AccommodationFilterChip1("Sharing", selectedRoomType == "Sharing") { selectedRoomType = if (selectedRoomType == "Sharing") "All" else "Sharing" }
                    AccommodationFilterChip1("NSFAS Accredited", isNsfasAccredited) { isNsfasAccredited = !isNsfasAccredited }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Property List with Empty State
                if (filteredProperties.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No accommodation found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn {
                        items(filteredProperties) { property ->
                            ListingCard(
                                property = property,
                                onViewClick = { navController.navigate("detail/${property.id}") },
                                onReviewsClick = { navController.navigate("review/${property.id}/${Uri.encode(property.title)}") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccommodationFilterChip1(label: String, selected: Boolean = false, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.padding(end = 8.dp)
    )
}
