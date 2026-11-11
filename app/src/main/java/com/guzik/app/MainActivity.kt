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

        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            logGuzikError("OPEN BIBLE CRASH", Log.getStackTraceString(e))
        }

        logGuzikError("APP STARTED", "Guzik App opened, logger is working")

        setContent {
            MaterialTheme {
                GuzikApp()
            }
        }
    }

    companion object {
        fun logGuzikError(tag: String, msg: String) {
            try {
                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "guzik_crash.txt"
                )
                file.appendText("\n\n=== $tag ===\n$msg\n")
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun GuzikApp() {
    var showBible by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Guzik App - Not logged in") }

    val context = LocalContext.current
    val activity = context as MainActivity

    if (showBible) {
        Column(modifier = Modifier.fillMaxSize()) {
            Button(
                onClick = { showBible = false },
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "← Back to Home")
            }
            BibleScreen()
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Guzik App", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = statusText, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    statusText = "Signing in..."
                    MainActivity.logGuzikError("GOOGLE SIGN-IN ATTEMPT", "User tapped Sign in with Google")
                    GoogleAuth.signIn(activity) { success, message ->
                        if (success) {
                            statusText = "Logged in as: $message"
                            MainActivity.logGuzikError("GOOGLE SIGN-IN SUCCESS", "Logged in as: $message")
                            Toast.makeText(activity, "Welcome $message", Toast.LENGTH_SHORT).show()
                        } else {
                            statusText = "Login failed: $message"
                            MainActivity.logGuzikError("GOOGLE SIGN-IN FAILED", "Message: $message")
                            Toast.makeText(activity, "Failed: $message", Toast.LENGTH_LONG).show()
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
                    MainActivity.logGuzikError("OPEN BIBLE TAPPED", "User tapped Open Bible")
                    showBible = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "📖 Open Bible")
            }
        }
    }
}
