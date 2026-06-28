package com.todoapp.mobile.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.todoapp.mobile.R.string
import okio.IOException
import timber.log.Timber
import javax.inject.Singleton

@Singleton
object GoogleSignInManager {
    suspend fun getGoogleIdToken(activityContext: Context): Result<String> = try {
        val credentialManager = CredentialManager.create(activityContext)
        val googleIdOption =
            GetGoogleIdOption
                .Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(activityContext.getString(string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build()

        val request =
            GetCredentialRequest
                .Builder()
                .addCredentialOption(googleIdOption)
                .build()

        val result =
            credentialManager.getCredential(
                request = request,
                context = activityContext,
            )

        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    Result.success(googleCredential.idToken)
                } else {
                    Result.failure(Exception(activityContext.getString(string.google_login_error)))
                }
            }

            else -> {
                Result.failure(Exception(activityContext.getString(string.google_login_error)))
            }
        }
    } catch (e: GetCredentialException) {
        Timber.w(e, "Google Sign-In failed: type=${e.type}")
        val messageRes = when (e) {
            is NoCredentialException -> string.google_signin_no_account
            is GetCredentialCancellationException -> string.google_signin_cancelled
            is GetCredentialInterruptedException -> string.google_signin_interrupted
            is GetCredentialProviderConfigurationException,
            is GetCredentialUnsupportedException,
            -> string.google_signin_play_services_unavailable
            else -> string.google_login_error
        }
        Result.failure(Exception(activityContext.getString(messageRes)))
    } catch (e: GoogleIdTokenParsingException) {
        Timber.w(e, "Google ID token parsing failed")
        Result.failure(Exception(activityContext.getString(string.google_login_error)))
    } catch (e: IOException) {
        Timber.w(e, "Google Sign-In I/O error")
        Result.failure(Exception(activityContext.getString(string.google_login_error)))
    }
}
