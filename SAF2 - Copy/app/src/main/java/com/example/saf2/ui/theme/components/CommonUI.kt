package com.example.saf2.ui.theme.components

import android.content.Context
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.saf2.R

/**
 * Reusable image component that handles URL, Uri, Int resource ID, or fallback.
 * Uses placeholder from https://via.placeholder.com/300 or R.drawable.img when loading fails,
 * preventing any pink/magenta image bug.
 */
@Composable
fun PropertyImage(
    imageData: Any?,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Property Image",
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    
    val targetData = when {
        imageData is Int -> imageData
        imageData is String && imageData.isNotBlank() -> imageData
        else -> "https://via.placeholder.com/300"
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(targetData)
            .crossfade(true)
            .error(R.drawable.img)
            .placeholder(R.drawable.img)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Image Unavailable",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/**
 * Reusable confirmation Alert Dialog.
 */
@Composable
fun ConfirmDialog(
    show: Boolean,
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = if (isDestructive) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(confirmText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(dismissText)
                }
            }
        )
    }
}

// --- Validation Helper Functions ---

fun isValidEmail(email: String): Boolean {
    val trimmed = email.trim()
    return trimmed.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
}

fun isValidSaPhone(phone: String): Boolean {
    val clean = phone.replace(" ", "").trim()
    return clean.matches(Regex("^(\\+27|0)[6-8][0-9]{8}$")) || clean.matches(Regex("^\\+27[0-9]{9}$"))
}

fun isValidTitle(title: String): Boolean {
    return title.trim().length >= 5
}

fun isValidMinChars(text: String, min: Int = 20): Boolean {
    return text.trim().length >= min
}

fun isValidPositiveNumber(value: String): Boolean {
    val num = value.trim().toIntOrNull()
    return num != null && num > 0
}

fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
