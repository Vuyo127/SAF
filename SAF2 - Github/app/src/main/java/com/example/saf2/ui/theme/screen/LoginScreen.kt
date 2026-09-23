package com.example.saf2.ui.theme.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.saf2.R
import com.example.saf2.ui.theme.components.isValidEmail
import com.example.saf2.ui.theme.components.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("student") }
    var isLoading by remember { mutableStateOf(false) }
    var hasSubmitted by remember { mutableStateOf(false) }

    val isEmailValid = isValidEmail(email)
    val isPasswordValid = password.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo and Title
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "SAF Logo",
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "SAF Finder",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        isError = hasSubmitted && !isEmailValid,
                        supportingText = {
                            if (hasSubmitted && !isEmailValid) {
                                Text("Please enter a valid email address", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        isError = hasSubmitted && !isPasswordValid,
                        supportingText = {
                            if (hasSubmitted && !isPasswordValid) {
                                Text("Password is required", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = icon, contentDescription = "Toggle password visibility")
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    // Forgot Password
                    Text(
                        text = "Forgot Password?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable { navController.navigate("forgotPassword") }
                            .padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Role Selection
                    Text(
                        text = "I am a:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Student Option
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedRole = "student" }) {
                            RadioButton(
                                selected = selectedRole == "student",
                                onClick = { selectedRole = "student" },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text("Student", style = MaterialTheme.typography.bodyLarge)
                        }
                        // Owner Option
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedRole = "owner" }) {
                            RadioButton(
                                selected = selectedRole == "owner",
                                onClick = { selectedRole = "owner" },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text("Owner", style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Login Button with Loading Spinner
                    Button(
                        onClick = {
                            hasSubmitted = true
                            if (isEmailValid && isPasswordValid) {
                                isLoading = true
                                showToast(context, "Logged in successfully")
                                isLoading = false
                                if (selectedRole == "student") {
                                    navController.navigate("accommodation")
                                } else {
                                    navController.navigate("ownerDashboard")
                                }
                            } else {
                                showToast(context, "Login failed: Please check credentials")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text("Login", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Signup Redirect
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Don't have an account?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sign up",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.clickable { navController.navigate("signup") }
                        )
                    }
                }
            }
        }
    }
}
