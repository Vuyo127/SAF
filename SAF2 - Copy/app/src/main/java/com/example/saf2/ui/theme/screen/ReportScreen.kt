package com.example.saf2.ui.theme.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.saf2.data.remote.ImageUploader
import com.example.saf2.ui.theme.components.isValidMinChars
import com.example.saf2.ui.theme.components.showToast
import com.example.saf2.viewmodel.AuthViewModel
import com.example.saf2.viewmodel.ListingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    navController: NavController,
    listingId: String,
    viewModel: ListingsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by authViewModel.currentUser.collectAsState()

    // Real listing title (countView = false so opening the report screen isn't counted as a view).
    LaunchedEffect(listingId) { viewModel.getListingDetails(listingId, countView = false) }
    val property by viewModel.selectedProperty.collectAsState()
    val listingTitle = property?.takeIf { it.id == listingId }?.title ?: "this listing"

    // Single state variable for selected reason ensuring only one radio button can ever be selected
    var selectedReason by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var proofImages: List<Uri> by remember { mutableStateOf(emptyList()) }
    var anonymous by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    var hasSubmitted by remember { mutableStateOf(false) }

    val isReasonValid = selectedReason.isNotEmpty()
    val isDescValid = isValidMinChars(description, 20)

    // Pick up to 3 proof photos from the gallery
    val proofPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3)
    ) { picked ->
        if (picked.isNotEmpty()) {
            proofImages = (proofImages + picked).distinct().take(3)
            showToast(context, "${picked.size} photo(s) attached")
        }
    }

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
                        text = "Reporting: $listingTitle",
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

                    // Proof photos (optional, up to 3)
                    OutlinedButton(
                        onClick = {
                            if (proofImages.size >= 3) {
                                showToast(context, "Maximum 3 proof photos")
                            } else {
                                proofPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Upload Proof (${proofImages.size}/3 attached)", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (proofImages.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(proofImages) { index, uri ->
                                Box {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Proof photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                    IconButton(
                                        onClick = {
                                            proofImages = proofImages.toMutableList().apply { removeAt(index) }
                                            showToast(context, "Proof photo removed")
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

                    Button(
                        onClick = {
                            hasSubmitted = true
                            if (isReasonValid && isDescValid) {
                                isLoading = true
                                scope.launch {
                                    try {
                                        val proofUrls = mutableListOf<String>()
                                        if (proofImages.isNotEmpty()) {
                                            if (ImageUploader.isConfigured) {
                                                proofImages.forEachIndexed { index, uri ->
                                                    uploadStatus = "Uploading proof ${index + 1} of ${proofImages.size}..."
                                                    proofUrls.add(ImageUploader.upload(context, uri, "reports"))
                                                }
                                            } else {
                                                showToast(context, "Photo hosting isn't set up yet, so proof photos were not sent.")
                                            }
                                        }
                                        uploadStatus = ""

                                        // Saved to Firestore with the listing's owner: it appears on the owner's
                                        // Reports tab and in the admin console's Reviews & reports page.
                                        viewModel.submitReport(
                                            listingId = listingId,
                                            reason = selectedReason,
                                            description = description.trim(),
                                            anonymous = anonymous,
                                            reporterName = currentUser?.name ?: "",
                                            proofImageUrls = proofUrls
                                        ) { success, error ->
                                            isLoading = false
                                            if (success) {
                                                showSuccess = true
                                            } else {
                                                showToast(context, error ?: "Could not submit the report. Please try again.")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        isLoading = false
                                        uploadStatus = ""
                                        showToast(context, e.message ?: "Could not upload the proof photos.")
                                    }
                                }
                            } else {
                                showToast(context, "Please choose a reason and write at least 20 characters")
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
                            Text("Submit Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (uploadStatus.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            uploadStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
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