package com.todoapp.mobile.ui.webview

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.webview.WebViewContract.UiAction
import com.todoapp.mobile.ui.webview.WebViewContract.UiEffect
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Hosts the in-app WebView is allowed to render. Derived from BuildConfig.BASE_URL (the backend that
 * serves the /legal pages) so there is no hardcoded host. Any navigation to another host is bounced
 * out to the external browser instead of loading inside our authenticated WebView. (§2.14)
 */
private val ALLOWED_HOSTS: Set<String> = setOfNotNull(Uri.parse(BuildConfig.BASE_URL).host)

private fun isHostAllowed(url: String?): Boolean {
    val host = url?.let { Uri.parse(it).host } ?: return false
    return host in ALLOWED_HOSTS
}

@Composable
fun WebViewScreen(
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
) {
    val context = LocalContext.current
    val webView = remember { WebView(context) }

    LaunchedEffect(webView) {
        // §2.14 hardening: static legal HTML needs no scripting or file/content access. Lock it all
        // down, then only render allowlisted hosts; anything else opens in the external browser.
        webView.settings.apply {
            javaScriptEnabled = false
            domStorageEnabled = false
            allowFileAccess = false // also makes the deprecated file-URL sub-settings moot
            allowContentAccess = false
        }
        webView.webViewClient =
            object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    val host = request.url.host
                    if (host != null && host in ALLOWED_HOSTS) return false
                    runCatching {
                        view.context.startActivity(
                            Intent(Intent.ACTION_VIEW, request.url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                    return true
                }
            }
    }

    uiEffect.collectWithLifecycle {
        when (it) {
            is UiEffect.OpenWebApp ->
                if (isHostAllowed(it.url)) {
                    webView.loadUrl(it.url)
                } else {
                    onAction(UiAction.OnCloseWebView)
                }
        }
    }

    DisposableEffect(Unit) {
        return@DisposableEffect onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    BackHandler(enabled = true) {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            onAction(UiAction.OnCloseWebView)
        }
    }

    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(TDTheme.colors.white),
    ) {
        AndroidView(
            modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            factory = { webView },
        )

        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp),
            contentAlignment = Alignment.TopEnd,
        ) {
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel),
                contentDescription = stringResource(R.string.cd_close),
                tint = TDTheme.colors.black,
                modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(TDTheme.colors.white.copy(alpha = 0.9f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onAction(UiAction.OnCloseWebView) }
                    .padding(8.dp),
            )
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun WebViewScreenPreview() {
    TDTheme {
        WebViewScreen(
            uiEffect = emptyFlow(),
            onAction = {},
        )
    }
}
