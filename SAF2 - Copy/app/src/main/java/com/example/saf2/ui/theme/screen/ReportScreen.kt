package com.example.saf2.ui.theme.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.saf2.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(navController: NavController, listingId: String) {
    var selectedReason by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var proofImages: List<Int> by remember { mutableStateOf(emptyList()) } // ✅ explicit type
    var anonymous by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Listing") },
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
                .verticalScroll(rememberScrollState())
        ) {
            Text("Why are you reporting?", style = MaterialTheme.typography.titleMedium)

            val reasons = listOf(
                "Fake listing",
                "Wrong price/info",
                "Scam/Fraud",
                "Unsafe property",
                "Discrimination",
                "Bad/Fake images",
                "Other"
            )

            reasons.forEach { reason ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedReason == reason,
                        onClick = { selectedReason = reason }
                    )
                    Text(reason)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 500) description = it },
                label = { Text("Description (min 20 chars)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
            Text("${description.length}/500")

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { /* TODO: Pick Proof Images */ }) {
                Text("Upload Proof")
            }
            LazyRow {
                itemsIndexed(proofImages) { index, image ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = image),
                            contentDescription = "Proof Image",
                            modifier = Modifier.size(100.dp).padding(4.dp)
                        )
                        Text(
                            "X",
                            modifier = Modifier.clickable {
                                proofImages = proofImages.toMutableList().apply { removeAt(index) }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = anonymous, onCheckedChange = { anonymous = it })
                Text("Submit Anonymously")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedReason.isNotEmpty() && description.length >= 20) {
                        isLoading = true
                        // TODO: Firebase integration
                        isLoading = false
                        showSuccess = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Submit")
                }
            }
        }
    }

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { showSuccess = false },
            title = { Text("Report Submitted") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text("Your report has been submitted successfully.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSuccess = false
                    navController.popBackStack()
                }) {
                    Text("OK")
                }
            }
        )
    }
}
