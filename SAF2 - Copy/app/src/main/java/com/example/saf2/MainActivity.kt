package com.example.saf2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.saf2.ui.theme.SAF2Theme
import com.example.saf2.ui.theme.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SAF2Theme {
               Surface(modifier = Modifier) {
                   AppNavHost()
               }
                }
            }
        }
    }


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SAF2Theme {
    }
}