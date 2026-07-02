package com.todoapp.mobile.ui.common

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.R
import timber.log.Timber
import java.util.Locale

/**
 * Builds a click handler that opens the user's email app pre-filled to the support address
 * ([BuildConfig.SUPPORT_EMAIL]) with a subject and an auto-appended diagnostics block
 * (app version, Android version, device, language, user). Mirrors [rememberOpenLocation].
 *
 * Uses [Context.startActivity] inside a `try/catch` rather than `resolveActivity`, which avoids the
 * Android 11+ package-visibility restriction (starting an app is allowed even when querying it is
 * not). If no email app is installed, the address is copied to the clipboard and a toast is shown.
 *
 * @param userEmail the signed-in user's email, or null/blank for a guest.
 */
@Composable
fun rememberSendFeedback(userEmail: String?): () -> Unit {
    val context = LocalContext.current
    val supportEmail = BuildConfig.SUPPORT_EMAIL
    val subject = stringResource(R.string.feedback_email_subject)
    val guestLabel = stringResource(R.string.feedback_user_guest)
    val userLine = userEmail?.takeIf { it.isNotBlank() } ?: guestLabel
    val body = stringResource(
        R.string.feedback_email_body_template,
        "DoneBot ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
        "${Build.MANUFACTURER} ${Build.MODEL}",
        Locale.getDefault().language.uppercase(Locale.ROOT),
        userLine,
    )
    val noEmailAppMsg = stringResource(R.string.feedback_no_email_app, supportEmail)

    return remember(context, supportEmail, subject, body, noEmailAppMsg) {
        {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:") // bare mailto → only email apps match
                putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Timber.w(e, "No email app available for feedback")
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("DoneBot support email", supportEmail))
                Toast.makeText(context, noEmailAppMsg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
