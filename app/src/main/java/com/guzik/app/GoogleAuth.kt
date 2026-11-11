package com.guzik.app

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

object GoogleAuth {

    private const val WEB_CLIENT_ID =
        "515882116644-09lubulqpqbc1bl8177qrk4opedub2md.apps.googleusercontent.com"

    fun signIn(
        context: Context,
        onResult: (success: Boolean, message: String?) -> Unit
    ) {
        val rawNonce = UUID.randomUUID().toString()

        val nonce = MessageDigest
            .getInstance("SHA-256")
            .digest(rawNonce.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .setNonce(nonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )

                val googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(result.credential.data)

                val googleIdToken = googleIdTokenCredential.idToken

                val firebaseCredential =
                    GoogleAuthProvider.getCredential(googleIdToken, null)

                FirebaseAuth.getInstance()
                    .signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val email = FirebaseAuth.getInstance()
                                .currentUser?.email

                            onResult(true, email)
                        } else {
                            onResult(false, task.exception?.message)
                        }
                    }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
}
