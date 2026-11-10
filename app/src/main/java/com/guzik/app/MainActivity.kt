package com.guzik.app

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 200, 60, 60)
        }

        statusText = TextView(this).apply {
            text = "Guzik App - Not logged in"
            textSize = 18f
        }

        val loginButton = Button(this).apply {
            text = "Sign in with Google"
            setOnClickListener {
                statusText.text = "Signing in..."
                GoogleAuth.signIn(this@MainActivity) { success, message ->
                    if (success) {
                        statusText.text = "Logged in as: $message"
                        Toast.makeText(this@MainActivity, "Welcome $message", Toast.LENGTH_SHORT).show()
                    } else {
                        statusText.text = "Login failed: $message"
                        Toast.makeText(this@MainActivity, "Failed: $message", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        layout.addView(statusText)
        layout.addView(loginButton)
        setContentView(layout)
    }
}
