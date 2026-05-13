package com.todoapp.mobile.ui.webview

import android.util.Log
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.todoapp.mobile.ui.webview.WebViewContract.UiAction
import com.todoapp.mobile.ui.webview.WebViewContract.UiEffect
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun WebViewScreen(
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
) {
    val context = LocalContext.current
    val webView = remember { WebView(context) }

    LaunchedEffect(webView) {
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
    }

    uiEffect.collectWithLifecycle {
        when (it) {
            is UiEffect.OpenWebApp -> webView.loadUrl(it.url)
        }
    }

    DisposableEffect(Unit) {
        return@DisposableEffect onDispose {
            Log.d("triggered", "1")
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
                contentDescription = "Close",
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
