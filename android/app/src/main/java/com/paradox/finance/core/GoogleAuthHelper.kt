package com.paradox.finance.core

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

object GoogleAuthHelper {
    private const val TAG = "GoogleAuthHelper"

    suspend fun signIn(
        context: Context,
        onSuccess: (idToken: String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val credentialManager = CredentialManager.create(context)

            // Generate SHA-256 hashed nonce
            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(Constants.GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                request = request,
                context = context
            )

            when (val credential = response.credential) {
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        onSuccess(googleIdTokenCredential.idToken)
                    } else {
                        onError("Unrecognized credential type")
                    }
                }
                else -> {
                    onError("Unable to retrieve Google credential")
                }
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Google Sign-In cancelled by user")
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Sign-In Credential error: ${e.message}", e)
            onError(e.message ?: "Google Sign-In failed")
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In error: ${e.message}", e)
            onError(e.localizedMessage ?: "Failed to sign in with Google")
        }
    }
}
