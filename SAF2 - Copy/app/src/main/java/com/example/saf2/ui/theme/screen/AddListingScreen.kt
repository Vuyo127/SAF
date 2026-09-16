@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.saf2.ui.theme.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.saf2.R

@Composable
fun AddListingScreen(navController: NavController) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var deposit by remember { mutableStateOf("") }
    var selectedRoomTypes by remember { mutableStateOf(setOf<String>()) }
    var images: List<Int> by remember { mutableStateOf(emptyList()) }
    var description by remember { mutableStateOf("") }
    var rules by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var nsfasAccredited by remember { mutableStateOf(false) }
    var totalBeds by remember { mutableStateOf("") }
    var acceptedFundingTypes by remember { mutableStateOf(setOf<String>()) }
    var amenities by remember { mutableStateOf(setOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Listing") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Save Draft */ }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Draft")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

            OutlinedTextField(
                value = totalBeds,
                onValueChange = { totalBeds = it },
                label = { Text("Total Number of Beds") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Price per month (R)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            OutlinedTextField(
                value = deposit,
                onValueChange = { deposit = it },
                label = { Text("Deposit Price (R)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Room Type", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth()) {
                val roomOptions = listOf("Single", "Sharing")
                roomOptions.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = selectedRoomTypes.contains(option),
                            onCheckedChange = {
                                selectedRoomTypes = if (it) selectedRoomTypes + option else selectedRoomTypes - option
                            }
                        )
                        Text(option, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Upload Images
            Button(onClick = { /* TODO: Pick Images */ }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Add Images (min 3)")
            }
            LazyRow {
                itemsIndexed(images) { index, image ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = image),
                            contentDescription = "Listing Image",
                            modifier = Modifier.size(100.dp).padding(4.dp)
                        )
                        Text("X", modifier = Modifier.clickable {
                            images = images.toMutableList().apply { removeAt(index) }
                        })
                    }
                }
            }
            Text("${images.size}/10 images selected")

            Spacer(modifier = Modifier.height(16.dp))

            // Amenities Grid
            Text("Amenities", style = MaterialTheme.typography.titleMedium)
            val amenityOptions = listOf("WiFi", "Water Included", "Electricity Included", "Kitchen", "Parking", "Security", "Furnished", "Laundry", "Study Desk")
            Column(modifier = Modifier.padding(top = 8.dp)) {
                amenityOptions.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = amenities.contains(option),
                                    onCheckedChange = {
                                        amenities = if (it) amenities + option else amenities - option
                                    }
                                )
                                Text(option, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Funding Types
            Text("Who can apply?", style = MaterialTheme.typography.titleMedium)
            FundingCheckbox("Self-Funded", acceptedFundingTypes) { toggleFunding("Self-Funded", acceptedFundingTypes) { acceptedFundingTypes = it } }
            FundingCheckbox("NSFAS", acceptedFundingTypes, enabled = nsfasAccredited) { toggleFunding("NSFAS", acceptedFundingTypes) { acceptedFundingTypes = it } }
            FundingCheckbox("Bursary", acceptedFundingTypes) { toggleFunding("Bursary", acceptedFundingTypes) { acceptedFundingTypes = it } }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = nsfasAccredited, onCheckedChange = { nsfasAccredited = it })
                Text("NSFAS Accredited")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth().height(120.dp))
            OutlinedTextField(value = rules, onValueChange = { rules = it }, label = { Text("House Rules") }, modifier = Modifier.fillMaxWidth().height(100.dp))
            OutlinedTextField(value = contactNumber, onValueChange = { contactNumber = it }, label = { Text("Contact Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    isLoading = true
                    // TODO: Firebase integration
                    isLoading = false
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary) else Text("Submit for Approval")
            }
        }
    }
}

@Composable
fun FundingCheckbox(label: String, selected: Set<String>, enabled: Boolean = true, onToggle: (Set<String>) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = selected.contains(label),
            onCheckedChange = {
                if (enabled) {
                    onToggle(if (it) selected + label else selected - label)
                }
            }
        )
        Text(label)
    }
}

fun toggleFunding(label: String, selected: Set<String>, onToggle: (Set<String>) -> Unit) {
    onToggle(if (selected.contains(label)) selected - label else selected + label)
}
