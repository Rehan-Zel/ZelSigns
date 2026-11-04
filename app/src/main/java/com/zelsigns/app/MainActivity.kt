package com.zelsigns.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0F1115))) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "ZEL SIGNS", color = Color(0xFF22FFA7), fontSize = 32.sp)
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { Toast.makeText(context, "Signing APK...", Toast.LENGTH_SHORT).show() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select APK & Sign")
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { Toast.makeText(context, "AI Auto-Heal Started...", Toast.LENGTH_SHORT).show() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("AI Auto-Fix & Build")
        }
    }
}