package com.zelsigns.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
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
import kotlinx.coroutines.CoroutineScope
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
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Security.addProvider(BouncyCastleProvider()) // Initialize Security Provider
        
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
    var isSigning by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Ready to sign") }

    val pickApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isSigning = true
            statusText = "Signing APK... Please wait."
            
            scope.launch {
                val result = signApk(context, uri)
                isSigning = false
                statusText = result
                Toast.makeText(context, result, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "ZEL SIGNS", color = Color(0xFF22FFA7), fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = statusText, color = Color.Gray, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        
        if (isSigning) {
            CircularProgressIndicator(color = Color(0xFF22FFA7))
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = { pickApkLauncher.launch("application/vnd.android.package-archive") },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF)),
            enabled = !isSigning
        ) {
            Text("Select APK & Sign", color = Color(0xFF141517), fontSize = 16.sp)
        }
    }
}

// --- THE ACTUAL SIGNING ENGINE ---
suspend fun signApk(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    try {
        // 1. Copy selected APK to temporary cache file
        val tempApk = File(context.cacheDir, "input.apk")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempApk.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return@withContext "Error: Could not read file"

        // 2. Generate a fake self-signed RSA Key on the fly
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keyPair = kpg.generateKeyPair()

        val now = System.currentTimeMillis()
        val dnName = X500Name("CN=ZelSigns, O=Zel, C=US")
        val serialNumber = BigInteger.valueOf(now)
        val certBuilder = JcaX509v3CertificateBuilder(
            dnName, serialNumber, Date(now), Date(now + 31536000000L), dnName, keyPair.public
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val certHolder = certBuilder.build(signer)
        val cert = JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder)

        // 3. Sign the APK using the ApkSigner library
        val signerConfig = ApkSigner.SignerConfig.Builder(
            "ZelKey", keyPair.private, listOf(cert)
        ).build()

        val outputApk = File(context.getExternalFilesDir(null), "zel-signed.apk")

        val apkSigner = ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(tempApk)
            .setOutputApk(outputApk)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build()

        apkSigner.sign()

        return@withContext "Success! Saved to:\n${outputApk.absolutePath}"

    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext "Failed: ${e.message}"
    }
}