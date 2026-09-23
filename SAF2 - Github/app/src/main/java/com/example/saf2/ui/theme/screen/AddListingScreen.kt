@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.saf2.ui.theme.screen

import android.content.Context
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.saf2.R
import com.example.saf2.ui.theme.components.isValidMinChars
import com.example.saf2.ui.theme.components.isValidPositiveNumber
import com.example.saf2.ui.theme.components.isValidSaPhone
import com.example.saf2.ui.theme.components.isValidTitle
import com.example.saf2.ui.theme.components.showToast
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Composable
fun AddListingScreen(navController: NavController, listingId: String? = null) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var deposit by remember { mutableStateOf("") }
    var selectedRoomTypes by remember { mutableStateOf(setOf<String>()) }
    var images: List<Uri> by remember { mutableStateOf(emptyList()) }
    var description by remember { mutableStateOf("") }
    var rules by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var nsfasAccredited by remember { mutableStateOf(false) }
    var totalBeds by remember { mutableStateOf("") }
    var acceptedFundingTypes by remember { mutableStateOf(setOf<String>()) }
    var amenities by remember { mutableStateOf(setOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }

    var hasSubmitted by remember { mutableStateOf(false) }

    // Multiple visual media picker launcher (up to 10 images)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { selectedUris ->
        if (selectedUris.isNotEmpty()) {
            val combined = (images + selectedUris).distinct().take(10)
            images = combined
            showToast(context, "${selectedUris.size} image(s) selected")
        }
    }

    LaunchedEffect(listingId) {
        if (listingId != null) {
            val existing = OwnerListingsData.listings.find { it.id == listingId }
            val sampleImageUri = Uri.parse("android.resource://${context.packageName}/${R.drawable.img}")
            
            if (existing != null) {
                title = existing.title
                price = existing.price.toString()
                deposit = existing.price.toString()
                totalBeds = existing.totalBeds.toString()
                location = "West Acres, Mbombela"
                contactNumber = "+27821234567"
                description = "Comfortable student accommodation near campus with all basic amenities provided."
                acceptedFundingTypes = existing.funding.toSet()
                images = listOf(sampleImageUri, sampleImageUri, sampleImageUri)
            } else {
                when (listingId) {
                    "1" -> {
                        title = "Campus View Student Residence"
                        location = "West Acres, Mbombela"
                        price = "2500"
                        deposit = "2500"
                        totalBeds = "10"
                        contactNumber = "+27821234567"
                        description = "Comfortable student accommodation near campus with all basic amenities provided."
                        acceptedFundingTypes = setOf("NSFAS", "Bursary")
                        images = listOf(sampleImageUri, sampleImageUri, sampleImageUri)
                    }
                    "2" -> {
                        title = "Student Lodge Mbombela"
                        location = "Kamagugu, Mbombela"
                        price = "3000"
                        deposit = "3000"
                        totalBeds = "5"
                        contactNumber = "+27839876543"
                        description = "Quiet environment ideal for studying with good security and parking facilities."
                        acceptedFundingTypes = setOf("Self-Funded")
                        images = listOf(sampleImageUri, sampleImageUri, sampleImageUri)
                    }
                    "3" -> {
                        title = "City Flats Residence"
                        location = "Cape Town"
                        price = "2000"
                        deposit = "2000"
                        totalBeds = "8"
                        contactNumber = "+27845551234"
                        description = "Modern bachelor flats located close to transport hubs and student facilities."
                        acceptedFundingTypes = setOf("NSFAS")
                        images = listOf(sampleImageUri, sampleImageUri, sampleImageUri)
                    }
                }
            }
        }
    }

    // Validation computations
    val isTitleValid = isValidTitle(title)
    val isLocationValid = location.trim().isNotEmpty()
    val isBedsValid = isValidPositiveNumber(totalBeds)
    val isPriceValid = isValidPositiveNumber(price)
    val isDepositValid = isValidPositiveNumber(deposit)
    val isDescriptionValid = isValidMinChars(description, 20)
    val isPhoneValid = isValidSaPhone(contactNumber)
    val isImagesValid = images.size >= 3

    val isFormValid = isTitleValid && isLocationValid && isBedsValid && isPriceValid &&
            isDepositValid && isDescriptionValid && isPhoneValid && isImagesValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (listingId != null) "Edit Listing" else "Add New Listing", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showToast(context, "Draft saved") }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Draft")
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
                    // Title Field
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (min 5 chars)") },
                        isError = hasSubmitted && !isTitleValid,
                        supportingText = {
                            if (hasSubmitted && !isTitleValid) {
                                Text("Title must be at least 5 characters", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Location Field
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        isError = hasSubmitted && !isLocationValid,
                        supportingText = {
                            if (hasSubmitted && !isLocationValid) {
                                Text("Location is required", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Beds Field
                    OutlinedTextField(
                        value = totalBeds,
                        onValueChange = { totalBeds = it },
                        label = { Text("Total Number of Beds") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = hasSubmitted && !isBedsValid,
                        supportingText = {
                            if (hasSubmitted && !isBedsValid) {
                                Text("Beds must be a valid number > 0", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Price & Deposit Fields
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Price (R)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = hasSubmitted && !isPriceValid,
                            supportingText = {
                                if (hasSubmitted && !isPriceValid) {
                                    Text("Price must be > 0", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = deposit,
                            onValueChange = { deposit = it },
                            label = { Text("Deposit (R)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = hasSubmitted && !isDepositValid,
                            supportingText = {
                                if (hasSubmitted && !isDepositValid) {
                                    Text("Deposit must be > 0", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Room Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val roomOptions = listOf("Single", "Sharing")
                        roomOptions.forEach { option ->
                            FilterChip(
                                selected = selectedRoomTypes.contains(option),
                                onClick = {
                                    selectedRoomTypes = if (selectedRoomTypes.contains(option)) {
                                        selectedRoomTypes - option
                                    } else {
                                        selectedRoomTypes + option
                                    }
                                },
                                label = { Text(option, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelLarge) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Gallery Image Picker Section
                    Text("Images", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            if (images.size >= 10) {
                                showToast(context, "Maximum 10 images reached")
                            } else {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Add Images (min 3)", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Gallery Images with Coil AsyncImage and X to remove
                    if (images.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(images) { index, uri ->
                                Box {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Listing Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                    IconButton(
                                        onClick = {
                                            images = images.toMutableList().apply { removeAt(index) }
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

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        "${images.size}/10 images selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (hasSubmitted && !isImagesValid) {
                        Text(
                            "At least 3 images are required",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Amenities Section
                    Text("Amenities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val amenityOptions = listOf("WiFi", "Generator/Inverter", "Kitchen", "Parking", "Security", "Laundry", "Study Area")
                    Column(modifier = Modifier.fillMaxWidth()) {
                        amenityOptions.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { option ->
                                    val isSelected = amenities.contains(option)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            amenities = if (isSelected) amenities - option else amenities + option
                                        },
                                        label = { Text(option, style = MaterialTheme.typography.labelMedium) },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = "Selected",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = MaterialTheme.colorScheme.outline,
                                            selectedBorderColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                                if (row.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Who can apply?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "NSFAS students are also self-funded",
                        modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    FundingCheckboxRow("Self-Funded", acceptedFundingTypes, enabled = true) { toggleFunding("Self-Funded", acceptedFundingTypes) { acceptedFundingTypes = it } }
                    FundingCheckboxRow("NSFAS", acceptedFundingTypes, enabled = true) { toggleFunding("NSFAS", acceptedFundingTypes) { acceptedFundingTypes = it } }
                    FundingCheckboxRow("Bursary", acceptedFundingTypes, enabled = true) { toggleFunding("Bursary", acceptedFundingTypes) { acceptedFundingTypes = it } }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nsfasAccredited = !nsfasAccredited }
                            .padding(vertical = 8.dp)
                    ) {
                        Switch(
                            checked = nsfasAccredited,
                            onCheckedChange = { nsfasAccredited = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.secondary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "NSFAS Accredited Property",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Description Field
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (min 20 chars)") },
                        isError = hasSubmitted && !isDescriptionValid,
                        supportingText = {
                            if (hasSubmitted && !isDescriptionValid) {
                                Text("Description must be at least 20 characters (currently ${description.trim().length})", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = rules,
                        onValueChange = { rules = it },
                        label = { Text("House Rules") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Contact Number Field
                    OutlinedTextField(
                        value = contactNumber,
                        onValueChange = { contactNumber = it },
                        label = { Text("Contact Number (+27)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = hasSubmitted && !isPhoneValid,
                        supportingText = {
                            if (hasSubmitted && !isPhoneValid) {
                                Text("Enter a valid SA phone number (+27... or 0...)", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // TODO BACKEND: PUT /api/listings/{id} must actually save title, location, price, beds, amenities, houseRules, description to DB and return updated object. Frontend will call getMyListings() after success to refresh.
                    Button(
                        onClick = {
                            hasSubmitted = true
                            if (isFormValid) {
                                isLoading = true
                                
                                // TODO BACKEND: Convert selected Uris to MultipartBody.Part. Backend endpoint POST /api/listings must accept @RequestPart images: List<MultipartFile> and save to storage and return URLs.
                                // FRONTEND ONLY: For now just log Uris and show images, do not break if backend not ready.
                                val multipartFiles = convertUrisToMultipartFiles(context, images)
                                println("Prepared ${multipartFiles.size} multipart image file(s) for upload: ${multipartFiles.map { it.name }}")
                                
                                // Update shared state so changes (e.g. Campus View -> Maleni Accommodation) persist immediately on dashboard
                                if (listingId != null) {
                                    val idx = OwnerListingsData.listings.indexOfFirst { it.id == listingId }
                                    if (idx != -1) {
                                        val old = OwnerListingsData.listings[idx]
                                        OwnerListingsData.listings[idx] = old.copy(
                                            title = title,
                                            price = price.toIntOrNull() ?: old.price,
                                            totalBeds = totalBeds.toIntOrNull() ?: old.totalBeds,
                                            funding = acceptedFundingTypes.toList().ifEmpty { old.funding }
                                        )
                                    }
                                } else {
                                    OwnerListingsData.listings.add(
                                        DashboardProperty(
                                            id = (OwnerListingsData.listings.size + 1).toString(),
                                            title = title,
                                            price = price.toIntOrNull() ?: 2500,
                                            status = "Pending",
                                            funding = acceptedFundingTypes.toList().ifEmpty { listOf("Self-Funded") },
                                            totalBeds = totalBeds.toIntOrNull() ?: 5,
                                            availableBeds = totalBeds.toIntOrNull() ?: 5
                                        )
                                    )
                                }

                                showToast(context, if (listingId != null) "Listing updated successfully" else "Listing submitted for approval")
                                isLoading = false
                                navController.popBackStack()
                            } else {
                                showToast(context, "Please fix form validation errors")
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
                            Text(if (listingId != null) "Save Changes" else "Submit for Approval", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Converts selected image URIs into Multipart File instances ready for HTTP multipart upload.
 */
fun convertUrisToMultipartFiles(context: Context, uris: List<Uri>): List<File> {
    return uris.mapNotNull { uri ->
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "listing_upload_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@Composable
fun FundingCheckboxRow(label: String, selected: Set<String>, enabled: Boolean = true, onToggle: (Set<String>) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (enabled) onToggle(if (selected.contains(label)) selected - label else selected + label) }
            .padding(vertical = 8.dp)
    ) {
        Checkbox(
            checked = selected.contains(label),
            onCheckedChange = {
                if (enabled) {
                    onToggle(if (it) selected + label else selected - label)
                }
            },
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledCheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                disabledUncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        )
    }
}

fun toggleFunding(label: String, selected: Set<String>, onToggle: (Set<String>) -> Unit) {
    onToggle(if (selected.contains(label)) selected - label else selected + label)
}
