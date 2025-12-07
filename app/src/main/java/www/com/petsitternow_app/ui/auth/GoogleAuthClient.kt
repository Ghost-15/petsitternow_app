package www.com.petsitternow_app.ui.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleAuthClient(
    private val context: Context,
) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): String? {
        return try {
            val response = buildCredentialRequest()
            val credential = response.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                googleCred.idToken  // On retourne le vrai Google ID token
            } else {
                null
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("Auth", "Exception1 ${e.message}")
            null
        }
    }

    private suspend fun buildCredentialRequest(): GetCredentialResponse {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setServerClientId("756871375939-qq54gsfj3a6ch4um7f45p7qhkfum0bkp.apps.googleusercontent.com")
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build()
            ).build()

        return credentialManager.getCredential(
            request = request,
            context = context
        )
    }
}
