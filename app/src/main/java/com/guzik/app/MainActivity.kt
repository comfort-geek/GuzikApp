package com.guzik.app

import android.os.Bundle
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GuzikApp()
            }
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
        // Bible screen with Back button
        Column(Modifier.fillMaxSize()) {
            Button(
                onClick = { showBible = false },
                modifier = Modifier.padding(16.dp)
            ) { Text("← Back to Home") }
            BibleScreen()
        }
    } else {
        // Home / Login screen
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Guzik App", fontSize = 32.sp)
            Spacer(Modifier.height(8.dp))
            Text(statusText, fontSize = 18.sp)
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    statusText = "Signing in..."
                    GoogleAuth.signIn(activity) { success, message ->
                        if (success) {
                            statusText = "Logged in as: $message"
                            Toast.makeText(activity, "Welcome $message", Toast.LENGTH_SHORT).show()
                        } else {
                            statusText = "Login failed: $message"
                            Toast.makeText(activity, "Failed: $message", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Sign in with Google") }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { showBible = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("📖 Open Bible") }
        }
    }
}
