package com.zelsigns.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.apksig.ApkSigner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.Security
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Security.addProvider(BouncyCastleProvider())
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
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Select an APK to sign, or a ZIP/HTML to build.") }
    
    // Configuration state
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var showConfigScreen by remember { mutableStateOf(false) }
    var appName by remember { mutableStateOf("MyApp") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.lowercase() ?: ""
            if (fileName.endsWith(".apk")) {
                // SIGN APK FLOW
                isProcessing = true
                statusText = "Signing APK..."
                scope.launch {
                    val result = signApk(context, uri)
                    isProcessing = false
                    statusText = result
                    Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                }
            } else if (fileName.endsWith(".zip") || fileName.endsWith(".html")) {
                // BUILD FROM HTML/ZIP FLOW
                selectedFileUri = uri
                showConfigScreen = true
            } else {
                Toast.makeText(context, "Please select an APK, ZIP, or HTML file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showConfigScreen) {
        // --- BUILD CONFIGURATION SCREEN ---
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Configure App", color = Color(0xFF22FFA7), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = appName,
                onValueChange = { appName = it },
                label = { Text("App Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = { /* Icon Picker Logic */ Toast.makeText(context, "Icon Picker coming soon", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) {
                Text("Choose Custom Icon")
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(onClick = { /* Splash Picker Logic */ Toast.makeText(context, "Splash Screen Picker coming soon", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) {
                Text("Choose Starting Screen")
            }
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    // Here you would trigger the GitHub Action with the selected file and settings
                    Toast.makeText(context, "Sending to GitHub Actions to build...", Toast.LENGTH_LONG).show()
                    showConfigScreen = false
                    isProcessing = true
                    statusText = "Building via GitHub..."
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22FFA7))
            ) {
                Text("Build APK (GitHub)", color = Color(0xFF141517), fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { showConfigScreen = false }) {
                Text("Cancel", color = Color.Gray)
            }
        }
    } else {
        // --- MAIN SCREEN ---
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("ZEL SIGNS", color = Color(0xFF22FFA7), fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(statusText, color = Color.Gray, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))
            
            if (isProcessing) {
                CircularProgressIndicator(color = Color(0xFF22FFA7))
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = { filePicker.launch("*/*") }, // Now allows APK, ZIP, HTML
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF)),
                enabled = !isProcessing
            ) {
                Text("Select File (APK / ZIP / HTML)", color = Color(0xFF141517), fontSize = 16.sp)
            }
        }
    }
}

// --- LOCAL APK SIGNING ENGINE (UNCHANGED) ---
suspend fun signApk(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    try {
        val tempApk = File(context.cacheDir, "input.apk")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempApk.outputStream().use { output -> input.copyTo(output) }
        } ?: return@withContext "Error: Could not read file"

        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keyPair = kpg.generateKeyPair()

        val now = System.currentTimeMillis()
        val dnName = X500Name("CN=ZelSigns, O=Zel, C=US")
        val certBuilder = JcaX509v3CertificateBuilder(
            dnName, BigInteger.valueOf(now), Date(now), Date(now + 31536000000L), dnName, keyPair.public
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val cert = JcaX509CertificateConverter().setProvider("BC").getCertificate(certBuilder.build(signer))

        val signerConfig = ApkSigner.SignerConfig.Builder("ZelKey", keyPair.private, listOf(cert)).build()
        val outputApk = File(context.getExternalFilesDir(null), "zel-signed.apk")

        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(tempApk)
            .setOutputApk(outputApk)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build()
            .sign()

        return@withContext "Success! Saved to:\n${outputApk.absolutePath}"
    } catch (e: Exception) {
        return@withContext "Failed: ${e.message}"
    }
}
