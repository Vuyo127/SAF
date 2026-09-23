package com.example.saf2.ui.theme.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.saf2.R
import com.example.saf2.ui.theme.components.PropertyImage
import com.example.saf2.ui.theme.components.isValidMinChars
import com.example.saf2.ui.theme.components.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(navController: NavController, listingId: String) {
    val context = LocalContext.current
    val properties = listOf(
        AccommodationProperty("1", "Maleni Student Accomodation", "West Acres, Mbombela", 3500, "Single", 4.5, true, true, listOf("NSFAS", "Bursary")),
        AccommodationProperty("2", "Luhambo Students", "Kamagugu, Mbombela", 5300, "Sharing", 4.0, false, true, listOf("NSFAS", "Self-Funded")),
        AccommodationProperty("3", "Bachelor Apartment", "Cape Town", 5000, "Single", 4.8, true, true, listOf("NSFAS", "Self-Funded"))
    )
    val property = properties.find { it.id == listingId } ?: properties[0]

    // Single state variable for selected reason ensuring only one radio button can ever be selected
    var selectedReason by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var proofImages: List<Any> by remember { mutableStateOf(emptyList()) }
    var anonymous by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var hasSubmitted by remember { mutableStateOf(false) }

    val isReasonValid = selectedReason.isNotEmpty()
    val isDescValid = isValidMinChars(description, 20)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Listing", fontWeight = FontWeight.Bold) },
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
                        text = "Reporting: ${property.title}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Why are you reporting?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val reasons = listOf(
                        "Fake listing",
                        "Wrong price/info",
                        "Scam/Fraud",
                        "Unsafe property",
                        "Discrimination",
                        "Bad/Fake images",
                        "Other"
                    )

                    // Single selection radio group
                    reasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = (selectedReason == reason),
                                onClick = { selectedReason = reason },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (hasSubmitted && !isReasonValid) {
                        Text(
                            "Please select a reason for reporting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description Field with enforced min 20 chars
                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 500) description = it },
                        label = { Text("Description (min 20 chars required)") },
                        isError = hasSubmitted && !isDescValid,
                        supportingText = {
                            if (hasSubmitted && !isDescValid) {
                                Text("Description must be at least 20 characters (currently ${description.trim().length})", color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("${description.trim().length}/500 chars (min 20 required)", style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Functional Upload Proof button
                    OutlinedButton(
                        onClick = {
                            proofImages = proofImages + R.drawable.img
                            showToast(context, "Proof image attached")
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Upload Proof (${proofImages.size} attached)", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (proofImages.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(proofImages) { index, image ->
                                Box {
                                    PropertyImage(
                                        imageData = image,
                                        modifier = Modifier
                                            .size(90.dp)
                                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                    )
                                    IconButton(
                                        onClick = {
                                            proofImages = proofImages.toMutableList().apply { removeAt(index) }
                                            showToast(context, "Proof image removed")
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.onTertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { anonymous = !anonymous }.padding(vertical = 8.dp)
                    ) {
                        Checkbox(
                            checked = anonymous,
                            onCheckedChange = { anonymous = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            "Submit Anonymously",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // TODO BACKEND: POST /api/reports { listingId, reason, description, anonymous, proofImageUrl } Backend must save proof image if uploaded.
                    Button(
                        onClick = {
                            hasSubmitted = true
                            if (isReasonValid && isDescValid) {
                                isLoading = true
                                showToast(context, "Report submitted")
                                isLoading = false
                                showSuccess = true
                            } else {
                                showToast(context, "Description must be at least 20 characters")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading && (description.trim().length >= 20 || !hasSubmitted)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text("Submit Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { showSuccess = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            icon = {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(56.dp)
                )
            },
            title = {
                Text(
                    "Report Submitted",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Your report has been submitted successfully and is being reviewed by our team.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccess = false
                        navController.popBackStack()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("OK", style = MaterialTheme.typography.titleMedium)
                }
            }
        )
    }
}
