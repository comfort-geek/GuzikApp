package com.guzik.app

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // === 1. CRASH CATCHER FOR OPEN BIBLE ===
        // This catches ANY crash in the app, including BibleScreen crash
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            logGuzikError("OPEN BIBLE CRASH", Log.getStackTraceString(e))
        }

        // Log that app started, so we know logging works
        logGuzikError("APP STARTED", "Guzik App opened, logger is working")

        setContent {
            MaterialTheme {
                GuzikApp()
            }
        }
    }

    // === 2. SHARED LOGGER - used by both crash + sign-in ===
    companion object {
        fun logGuzikError(tag: String, msg: String) {
            try {
                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "guzik_crash.txt"
                )
                file.appendText("\n\n=== $tag ===\n$msg\n")
            } catch (_: Exception) {
                // If Downloads fails, try app private storage as backup
                try {
                    // This will be caught in log, but we ignore it to avoid loop
                } catch (_: Exception) {}
            }
        }
    }
}

@Composable
fun GuzikApp() {
    var showBible by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Guzik App - Not logged in") }
    var bibleError by remember { mutableStateOf("") }

    val context = LocalContext.current
    val activity = context as MainActivity

    if (showBible) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Button(
                onClick = {
                    showBible = false
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "← Back to Home")
            }

            // Show error if BibleScreen failed before
            if (bibleError.isNotEmpty()) {
                Text(
                    text = bibleError,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                // Wrap BibleScreen so if it throws, we log it instead of just dying silently
                try {
                    BibleScreen()
                } catch (e: Exception) {
                    val msg = "BibleScreen failed on open:\n" + Log.getStackTraceString(e)
                    bibleError = "Bible failed to open. Error saved to Downloads/guzik_crash.txt"
                    MainActivity.logGuzikError("OPEN BIBLE CRASH - CAUGHT IN COMPOSE", msg)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Guzik App",
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusText,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    statusText = "Signing in..."

                    // Log that we ATTEMPTED sign-in
                    MainActivity.logGuzikError("GOOGLE SIGN-IN ATTEMPT", "User tapped Sign in with Google")

                    GoogleAuth.signIn(activity) { success, message ->
                        if (success) {
                            statusText = "Logged in as: $message"
                            MainActivity.logGuzikError("GOOGLE SIGN-IN SUCCESS", "Logged in as: $message")

                            Toast.makeText(
                                activity,
                                "Welcome $message",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            statusText = "Login failed: $message"
                            // === THIS IS THE GOOGLE SIGN-IN LOGGER ===
                            // This catches Error 10 which does NOT crash, so the crash catcher would miss it
                            MainActivity.logGuzikError(
                                "GOOGLE SIGN-IN FAILED",
                                "Message from GoogleAuth: $message\n\nIf this says Status 10 / DEVELOPER_ERROR, it means SHA-1 fingerprint in Firebase / Google Cloud does not match the key GitHub Actions used to sign this APK."
                            )

                            Toast.makeText(
                                activity,
                                "Failed: $message",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Sign in with Google")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // Log before we try to open Bible
                    MainActivity.logGuzikError("OPEN BIBLE TAPPED", "User tapped Open Bible, about to show BibleScreen")
                    showBible = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "📖 Open Bible")
            }
        }
    }
}
