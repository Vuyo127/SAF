package com.example.saf2.ui.theme.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.saf2.ui.theme.components.isValidMinChars
import com.example.saf2.ui.theme.components.showToast
import com.example.saf2.viewmodel.AuthViewModel
import com.example.saf2.viewmodel.ListingsViewModel

val GoldStarColor = Color(0xFFFFC107)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    navController: NavController,
    listingId: String,
    propertyTitle: String,
    viewModel: ListingsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()

    var overallRating by remember { mutableStateOf(0) }
    var cleanliness by remember { mutableStateOf(0) }
    var security by remember { mutableStateOf(0) }
    var value by remember { mutableStateOf(0) }
    var response by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var hasSubmitted by remember { mutableStateOf(false) }

    val isRatingValid = overallRating > 0
    val isCommentValid = isValidMinChars(comment, 20)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Write Review", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Rate your stay at",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = propertyTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Overall Rating", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Bigger 38dp Gold Stars
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { index ->
                            val isSelected = index < overallRating
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Star ${index + 1}",
                                tint = if (isSelected) GoldStarColor else MaterialTheme.colorScheme.outline,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable { overallRating = index + 1 }
                                    .padding(2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (overallRating) {
                                1 -> "Poor"
                                2 -> "Fair"
                                3 -> "Good"
                                4 -> "Very Good"
                                5 -> "Excellent"
                                else -> "Select Rating"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (overallRating > 0) GoldStarColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (hasSubmitted && !isRatingValid) {
                        Text(
                            "Please select an overall star rating",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Category Ratings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    RatingRow("Cleanliness", cleanliness) { cleanliness = it }
                    RatingRow("Security", security) { security = it }
                    RatingRow("Value for Money", value) { value = it }
                    RatingRow("Owner Response", response) { response = it }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Share your experience (min 20 chars)") },
                        isError = hasSubmitted && !isCommentValid,
                        supportingText = {
                            if (hasSubmitted && !isCommentValid) {
                                Text("Review must be at least 20 characters (currently ${comment.trim().length})", color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("${comment.trim().length}/500 chars (min 20 required)", style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            hasSubmitted = true
                            if (isRatingValid && isCommentValid) {
                                isLoading = true
                                // Saved to Firestore with the listing's owner, so it appears on the owner's
                                // dashboard/profile, updates the listing's star rating, and shows in the admin console.
                                viewModel.postReview(
                                    listingId = listingId,
                                    propertyTitle = propertyTitle,
                                    studentName = currentUser?.name ?: "",
                                    overall = overallRating,
                                    cleanliness = cleanliness,
                                    security = security,
                                    valueForMoney = value,
                                    ownerResponse = response,
                                    comment = comment.trim()
                                ) { success, error ->
                                    isLoading = false
                                    if (success) {
                                        showToast(context, "Review posted")
                                        navController.popBackStack()
                                    } else {
                                        showToast(context, error ?: "Could not post your review. Please try again.")
                                    }
                                }
                            } else {
                                showToast(context, "Comment must be at least 20 characters & rating selected")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text("Post Review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingRow(label: String, rating: Int, onRatingChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Row {
            repeat(5) { index ->
                val isSelected = index < rating
                Icon(
                    imageVector = if (isSelected) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$label star ${index + 1}",
                    tint = if (isSelected) GoldStarColor else MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onRatingChange(index + 1) }
                        .padding(2.dp)
                )
            }
        }
    }
}