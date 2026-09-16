@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.saf2.ui.theme.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.saf2.R
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun DetailScreen(navController: NavController, listingId: String) {
    var isFavorite by remember { mutableStateOf(false) }
    var showApplyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accommodation Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = { navController.navigate("report/$listingId") }) {
                    Text("Report")
                }
                Button(onClick = { showApplyDialog = true }) {
                    Text("Apply Now")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            val images = listOf(R.drawable.img, R.drawable.img, R.drawable.img)
            var currentIndex by remember { mutableStateOf(0) }

            LazyRow {
                itemsIndexed(images) { index, image ->
                    Image(
                        painter = painterResource(id = image),
                        contentDescription = "Property Image",
                        modifier = Modifier
                            .size(250.dp)
                            .padding(end = 8.dp)
                            .clickable { currentIndex = index }
                    )
                }
            }
            Text("${currentIndex + 1}/${images.size}", modifier = Modifier.align(Alignment.CenterHorizontally))

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("R2500/month", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Modern Student Flat", style = MaterialTheme.typography.titleLarge)
            Text("Pretoria", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(8.dp))

            // Funding Accepted
            Row {
                DetailFundingBadge("Self-Funded")
                DetailFundingBadge("NSFAS Accredited")
                DetailFundingBadge("Bursary")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Room: Single")
                Text("Gender: Mixed")
                Text("Distance: 2km from campus")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amenities Part
            Text("Amenities", style = MaterialTheme.typography.titleMedium)
            val amenities = listOf("WiFi", "Water", "Electricity", "Kitchen", "Parking", "Security", "Furnished")
            Column {
                amenities.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { amenity ->
                            Surface(
                                modifier = Modifier.padding(4.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text(amenity, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Description", style = MaterialTheme.typography.titleMedium)
                    Text("This modern flat offers comfortable living with all amenities included...")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reviews
            Text("Reviews", style = MaterialTheme.typography.titleMedium)
            Text("⭐ 4.5 (23 reviews)")
            Text("John: Great place to stay!")
            Text("Sarah: Very safe and close to campus.")
            Text(
                "View All Reviews",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { navController.navigate("reviews/$listingId") }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Application Sec
        if (showApplyDialog) {
            AlertDialog(
                onDismissRequest = { showApplyDialog = false },
                title = { Text("Apply Now") },
                text = { Text("Do you want to apply for this accommodation?") },
                confirmButton = {
                    Button(onClick = {
                        showApplyDialog = false
                        //TODO: FIREBASE - Create application doc in "applications" collection
                    }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showApplyDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun DetailFundingBadge(label: String) {
    Surface(
        modifier = Modifier.padding(end = 4.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}