package com.example.saf2.ui.theme.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(navController: NavController, listingId: String, propertyTitle: String) {
    var overallRating by remember { mutableStateOf(0) }
    var cleanliness by remember { mutableStateOf(0) }
    var security by remember { mutableStateOf(0) }
    var value by remember { mutableStateOf(0) }
    var response by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Write Review") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            Text("Rate your stay at $propertyTitle", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(16.dp))

            // Overall Rating
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { index ->
                    Icon(
                        imageVector = if (index < overallRating) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Star",
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { overallRating = index + 1 }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    when (overallRating) {
                        1 -> "Poor"
                        2 -> "Fair"
                        3 -> "Good"
                        4 -> "Very Good"
                        5 -> "Excellent"
                        else -> ""
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Categories
            RatingRow("Cleanliness", cleanliness) { cleanliness = it }
            RatingRow("Security", security) { security = it }
            RatingRow("Value for Money", value) { value = it }
            RatingRow("Owner Response", response) { response = it }

            Spacer(modifier = Modifier.height(16.dp))

            // Comment
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Share your experience...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (overallRating > 0) {
                        isLoading = true
                        // TODO: Firebase integration
                        isLoading = false
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Post Review")
                }
            }
        }
    }
}

@Composable
fun RatingRow(label: String, rating: Int, onRatingChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(label, modifier = Modifier.width(150.dp))
        repeat(5) { index ->
            Icon(
                imageVector = if (index < rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "Star",
                modifier = Modifier
                    .size(30.dp)
                    .clickable { onRatingChange(index + 1) }
            )
        }
    }
}
