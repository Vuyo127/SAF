@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.saf2.ui.theme.screen

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.saf2.R
import com.example.saf2.ui.theme.components.showToast
import com.example.saf2.viewmodel.ListingsViewModel

@Composable
fun DetailScreen(
    navController: NavController,
    listingId: String,
    viewModel: ListingsViewModel = viewModel()
) {
    val context = LocalContext.current

    // Trigger ViewModel
    LaunchedEffect(listingId) {
        viewModel.getListingDetails(listingId)
    }
    // TODO BACKEND: backend/Database must fill in GET /api/listings/{listingId} to return real property details, imageUrls List<String>, price, title, location, roomType, rating, amenities, and ownerId.

    val listings by viewModel.approvedListings.collectAsState()
    val selectedProperty by viewModel.selectedProperty.collectAsState()

    // Find property from state or fallback
    val property = selectedProperty ?: listings.find { it.id == listingId }

    var isFavorite by remember { mutableStateOf(false) }
    var showApplyDialog by remember { mutableStateOf(false) }

    val propertyImages = if (property?.imageUrl != null && property.imageUrl.isNotBlank()) {
        listOf(property.imageUrl, "https://via.placeholder.com/300", "https://via.placeholder.com/300")
    } else {
        listOf("https://via.placeholder.com/300", "https://via.placeholder.com/300", "https://via.placeholder.com/300")
    }
    
    val pagerState = rememberPagerState(pageCount = { propertyImages.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accommodation Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isFavorite = !isFavorite
                        showToast(context, if (isFavorite) "Added to favorites" else "Removed from favorites")
                    }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimary
                        )
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
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.navigate("report/$listingId") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Report", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = { showApplyDialog = true },
                        modifier = Modifier.weight(2f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (property == null) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Loading accommodation details...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .fillMaxSize()
            ) {
                // Image Carousel Box (250dp height with dots indicator)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(propertyImages[page])
                                .crossfade(true)
                                .placeholder(R.drawable.img)
                                .error(R.drawable.img)
                                .build(),
                            contentDescription = "Property Image ${page + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Page count badge e.g. 1 / 3
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        Text(
                            "${pagerState.currentPage + 1} / ${propertyImages.size}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Dots indicator below image carousel
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(propertyImages.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 10.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "R${property.price}/month",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(property.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(property.location, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(12.dp))

                Row {
                    property.fundingTypes.forEach { type ->
                        DetailFundingBadge(type)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DetailInfoItem("Room", property.roomType)
                    DetailInfoItem("Gender", "Mixed")
                    DetailInfoItem("Distance", "2km")
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("Amenities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                val amenitiesWithIcons = listOf(
                    Pair("WiFi", Icons.Filled.Wifi),
                    Pair("Water", Icons.Filled.WaterDrop),
                    Pair("Electricity", Icons.Filled.Bolt),
                    Pair("Kitchen", Icons.Filled.Kitchen),
                    Pair("Parking", Icons.Filled.DirectionsCar),
                    Pair("Security", Icons.Filled.Security),
                    Pair("Furnished", Icons.Filled.Bed)
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    amenitiesWithIcons.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { (amenity, icon) ->
                                Surface(
                                    modifier = Modifier.weight(1f).padding(bottom = 8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            amenity,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            if (row.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "This modern flat offers comfortable living with all amenities included. Perfect for students looking for a safe and convenient stay close to campus.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Reviews Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Reviews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("⭐ ${property.rating}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate("review/$listingId/${Uri.encode(property.title)}") },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "View All Reviews / Write Review",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        if (showApplyDialog) {
            AlertDialog(
                onDismissRequest = { showApplyDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        "Apply Now",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Do you want to submit an application for this accommodation?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showApplyDialog = false
                            viewModel.applyForAccommodation(listingId, property?.ownerId ?: "")
                            // TODO BACKEND: backend/Database must fill in POST /api/applications { listingId, studentId, ownerId = property.ownerId, status=PENDING }
                            showToast(context, "Application sent to owner")
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Confirm", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showApplyDialog = false },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            )
        }
    }
}

@Composable
private fun DetailInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DetailFundingBadge(label: String) {
    val badgeColor = if (label.contains("NSFAS")) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
    Surface(
        modifier = Modifier.padding(end = 6.dp),
        shape = RoundedCornerShape(8.dp),
        color = badgeColor.copy(alpha = 0.15f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = badgeColor,
            fontWeight = FontWeight.Medium
        )
    }
}
