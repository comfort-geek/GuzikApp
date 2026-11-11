package com.guzik.app

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object GoogleAuth {
    // Put your Web Client ID from Google Cloud Console / google-services.json here
    // It is the one that ends with .apps.googleusercontent.com
    // You can also read it from strings if you have it in google-services.json
    private const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"

    fun signIn(activity: Activity, onResult: (Boolean, String) -> Unit) {
        if (WEB_CLIENT_ID.startsWith("YOUR_")) {
            val msg = "Missing WEB_CLIENT_ID in GoogleAuth.kt - paste your Web Client ID from Google Cloud Console"
            MainActivity.logGuzikError("GOOGLE SIGN-IN FAILED", msg)
            onResult(false, msg)
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(WEB_CLIENT_ID)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val credentialManager = CredentialManager.create(activity)
                val result = credentialManager.getCredential(activity, request)
                val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
                val email = credential.id
                MainActivity.logGuzikError("GOOGLE SIGN-IN SUCCESS", "Logged in as: $email")
                onResult(true, email)
            } catch (e: GetCredentialException) {
                val msg = "Google sign-in failed: ${e.message}"
                MainActivity.logGuzikError("GOOGLE SIGN-IN FAILED", msg + "\n" + Log.getStackTraceString(e))
                onResult(false, msg)
            } catch (e: Exception) {
                MainActivity.logGuzikError("GOOGLE SIGN-IN FAILED", Log.getStackTraceString(e))
                onResult(false, e.message ?: "Unknown sign-in error")
            }
        }
    }
}
