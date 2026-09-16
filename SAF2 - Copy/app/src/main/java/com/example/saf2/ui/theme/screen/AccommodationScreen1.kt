@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.saf2.ui.theme.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.saf2.R
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight

@Composable
fun AccommodationScreen1(navController: NavController) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf("Newest") }
    var isNsfasAccredited by remember { mutableStateOf(false) }
    var isPriceFilterActive by remember { mutableStateOf(false) }
    var selectedRoomType by remember { mutableStateOf("All") }

    val properties = listOf(
        AccommodationProperty("1", "Maleni Student Accomodation", "West Acres, Mbombela", 3500, "Single", 4.5, true, true, listOf("NSFAS", "Bursary")),
        AccommodationProperty("2", "Luhambo Students", "Kamagugu, Mbombela", 5300, "Sharing", 4.0, false, true, listOf("NSFAS", "Self-Funded")),
        AccommodationProperty("3", "Bachelor Apartment", "Cape Town", 5000, "Single", 4.8, true, true, listOf("NSFAS", "Self-Funded"))
    )

    // Filter by search query (title or location), then sort
    val filteredProperties = remember(properties, searchQuery, sortOption, isNsfasAccredited, isPriceFilterActive, selectedRoomType) {
        var filtered = if (searchQuery.isBlank()) {
            properties
        } else {
            properties.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.location.contains(searchQuery, ignoreCase = true)
            }
        }

        if (isNsfasAccredited) {
            filtered = filtered.filter { it.isNsfasAccredited }
        }

        if (isPriceFilterActive) {
            filtered = filtered.filter { it.price in 500..5000 }
        }

        if (selectedRoomType != "All") {
            filtered = filtered.filter { it.roomType.equals(selectedRoomType, ignoreCase = true) }
        }

        when (sortOption) {
            "Price Low-High" -> filtered.sortedBy { it.price }
            "Price High-Low" -> filtered.sortedByDescending { it.price }
            "Rating" -> filtered.sortedByDescending { it.rating }
            else -> filtered
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        "SAF - Mbombela",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        label = { Text("Find Accommodation") },
                        selected = true,
                        onClick = {
                            scope.launch { drawerState.close() }
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        label = { Text("My Profile") },
                        selected = false,
                        onClick = {
                            navController.navigate("profile")
                            scope.launch { drawerState.close() }
                        }
                    )


                    Spacer(modifier = Modifier.weight(1f))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null) },
                        label = { Text("Login / Signup") },
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
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "SAF Logo",
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SAF")
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
                    }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by location, title") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { focusManager.clearFocus() }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    AccommodationFilterChip(
                        label = "All",
                        selected = selectedRoomType == "All" && !isPriceFilterActive && !isNsfasAccredited,
                        onClick = {
                            selectedRoomType = "All"
                            isPriceFilterActive = false
                            isNsfasAccredited = false
                        }
                    )
                    AccommodationFilterChip(
                        label = "Single",
                        selected = selectedRoomType == "Single",
                        onClick = { selectedRoomType = if (selectedRoomType == "Single") "All" else "Single" }
                    )
                    AccommodationFilterChip(
                        label = "Sharing",
                        selected = selectedRoomType == "Sharing",
                        onClick = { selectedRoomType = if (selectedRoomType == "Sharing") "All" else "Sharing" }
                    )
                    AccommodationFilterChip(
                        label = "NSFAS Accredited",
                        selected = isNsfasAccredited,
                        onClick = { isNsfasAccredited = !isNsfasAccredited }
                    )

                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sort Dropdown
                SortDropdown(sortOption) { sortOption = it }

                Spacer(modifier = Modifier.height(16.dp))

                // Property List
                if (filteredProperties.isEmpty()) {
                    Text("No properties found.")
                } else {
                    LazyColumn {
                        items(filteredProperties) { property ->
                            PropertyCard(property) {
                                navController.navigate("detail/${property.id}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccommodationFilterChip(label: String, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.padding(end = 8.dp).clickable { onClick?.invoke() },
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(label, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SortDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Price Low-High", "Price High-Low", "Rating", "Newest")

    Box {
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PropertyCard(property: AccommodationProperty, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Image(painter = painterResource(id = R.drawable.img), contentDescription = "Property Image")

            Spacer(modifier = Modifier.height(8.dp))

            Text("R${property.price}/month", style = MaterialTheme.typography.titleLarge)
            Text(property.title, style = MaterialTheme.typography.titleMedium)
            Text(property.location, style = MaterialTheme.typography.bodyMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⭐ ${property.rating}")
                if (property.isVerified) {
                    Text("✔ Verified", modifier = Modifier.padding(start = 8.dp))
                }
            }

            Row {
                property.fundingTypes.forEach { type ->
                    AccommodationFundingBadge(type)
                }
            }
        }
    }
}

@Composable
private fun AccommodationFundingBadge(label: String) {
    Surface(
        modifier = Modifier.padding(end = 4.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

// TODO: FIREBASE - FOR ACCOMMODATION SCREEN
// 1. Query Firestore collection "listings" where status == "Approved" and availableBeds > 0
// 2. Each listing fields: listingId, ownerId, title, location, price, roomType, totalBeds, availableBeds, imagesUrls[], rating, isVerified, isNsfasAccredited, acceptedFundingTypes[], genderAllowed, viewsCount, createdAt
// 3. Search: filter locally by title.contains(searchQuery) or location.contains(searchQuery)
// 4. Funding Filter: get current student fundingType from "users" doc, keep listings where acceptedFundingTypes contains fundingType OR contains "ALL"
// 5. On card click: increment viewsCount with FieldValue.increment(1)