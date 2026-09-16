package com.example.saf2.ui.theme.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun SignUpScreen(navController: NavController) {
    var role by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var fundingType by remember { mutableStateOf("") }
    var bursaryName by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create Account", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // Role Selector
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { role = "student" }) { Text("Student") }
            Button(onClick = { role = "owner" }) { Text("Owner") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Common Fields
        OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone (+27)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = "Toggle password")
                }
            }
        )

        // Confirm Password
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = "Toggle confirm password")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Student-specific fields
        if (role == "student") {
            DropdownMenuDemo(fundingType) { fundingType = it }
            if (fundingType == "Bursary") {
                OutlinedTextField(value = bursaryName, onValueChange = { bursaryName = it }, label = { Text("Bursary Name") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        }

        // Owner-specific fields
        if (role == "owner") {
            OutlinedTextField(value = businessName, onValueChange = { businessName = it }, label = { Text("Business / Property Name") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = termsAccepted, onCheckedChange = { termsAccepted = it })
            Text("I accept the Terms and Conditions", modifier = Modifier.clickable { /* TODO: Navigate to Terms */ })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // TODO: FIREBASE - FOR SIGN UP
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Sign Up")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Text("Already have an account?")
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Login",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { navController.navigate("login") }
            )
        }
    }
}

@Composable
fun DropdownMenuDemo(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Self-Funded", "NSFAS", "Bursary", "Scholarship", "Other")

    Box {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            label = { Text("Funding Type") },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            readOnly = true
        )
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
