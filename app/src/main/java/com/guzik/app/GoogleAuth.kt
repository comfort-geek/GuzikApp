package com.guzik.app

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

object GoogleAuth {
    fun signIn(activity: Activity, onResult: (Boolean, String) -> Unit) {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(activity, gso)
            val intent = client.signInIntent
            activity.startActivityForResult(intent, 1001)
            // Result is handled in MainActivity, this is just the starter
            onResult(false, "Sign-in started")
        } catch (e: Exception) {
            onResult(false, e.message ?: "Unknown error")
            MainActivity.logGuzikError("GOOGLE SIGN-IN FAILED", e.toString())
        }
    }
}
