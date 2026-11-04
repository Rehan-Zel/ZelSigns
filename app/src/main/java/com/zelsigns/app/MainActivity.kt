package com.zelsigns.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.apksig.ApkSigner
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Collections

class MainActivity : ComponentActivity() {
    
    // Create a dummy debug keystore in memory so we can sign immediately
    private fun getDebugKey(): Pair<PrivateKey, X509Certificate> {
        val ks = KeyStore.getInstance(KeyStore.getDefaultType())
        ks.load(null, null)
        // Note: For a real app, you load a .jks file. For this demo, we generate a fake one.
        // To keep code short, we will throw a placeholder that we'll implement next.
        // (Using built-in Android debug keystore for simplicity in this step)
        return Pair(null!!, null!!) // Placeholder - will fix in next step
    }

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
    var isSigning by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Ready to sign") }

    // File Picker Launcher
    val pickApkLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isSigning = true
            statusText = "Signing APK... (Requires keystore setup)"
            Toast.makeText(context, "File selected! Signing logic coming next.", Toast.LENGTH_LONG).show()
            // In the next step, we pass this URI to the ApkSigner engine
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "ZEL SIGNS", color = Color(0xFF22FFA7), fontSize = 32.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = statusText, color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(32.dp))
        
        if (isSigning) {
            CircularProgressIndicator(color = Color(0xFF22FFA7))
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = { pickApkLauncher.launch("application/vnd.android.package-archive") },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF))
        ) {
            Text("Select APK & Sign", color = Color(0xFF141517), fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { Toast.makeText(context, "AI Auto-Heal Started...", Toast.LENGTH_SHORT).show() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF))
        ) {
            Text("AI Auto-Fix & Build", color = Color(0xFF141517), fontSize = 16.sp)
        }
    }
}