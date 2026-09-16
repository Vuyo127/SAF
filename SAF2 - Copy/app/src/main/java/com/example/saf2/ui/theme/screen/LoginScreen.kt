package com.example.saf2.ui.theme.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.saf2.R


@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("student") } // default role
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "SAF Logo",
            modifier = Modifier.size(100.dp)
        )
        Text("SAF Finder", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(8.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        // Password with eye toggle
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = icon, contentDescription = "Toggle password visibility")
                }
            }
        )

        // Forgot Password
        Text(
            text = "Forgot Password?",
            modifier = Modifier
                .align(Alignment.End)
                .clickable { navController.navigate("forgotPassword") }
                .padding(top = 4.dp, bottom = 16.dp)
        )

        // Role Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RadioButton(
                selected = selectedRole == "student",
                onClick = { selectedRole = "student" }
            )
            Text("Student")
            RadioButton(
                selected = selectedRole == "owner",
                onClick = { selectedRole = "owner" }
            )
            Text("Owner")
        }

        // Error message
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, modifier = Modifier.padding(8.dp))
        }

        // Login Button
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Email and password cannot be empty"
                } else {
                    errorMessage = ""
                    // TODO: Integrate Firebase Authentication here

                    if (selectedRole == "student") {
                        navController.navigate("accommodation")
                    } else {
                        navController.navigate("ownerDashboard")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Login")
        }

        // Signup Redirect
        Row(modifier = Modifier.padding(top = 16.dp)) {
            Text("Don't have an account?")
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Sign up",
                modifier = Modifier.clickable { navController.navigate("signup") }
            )
        }
    }
}
