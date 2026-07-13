package com.todoapp.mobile.ui.licenses

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

/**
 * Static open-source attribution list (§6.20). Library names and license identifiers are proper
 * nouns / standard terms that are identical in every locale, so they stay in code — only the
 * screen's own copy (title, intro, settings row) is translated. Curated from the shipped runtime
 * dependencies in gradle/libs.versions.toml; test-only deps are intentionally excluded. Keep in
 * sync when a shipped dependency is added or removed.
 */
private data class OssLicense(
    val name: String,
    val author: String,
    val license: String,
    val url: String,
)

private const val APACHE_2 = "Apache License 2.0"
private const val ANDROID_SDK_LICENSE = "Android Software Development Kit License"

@Suppress("ktlint:standard:max-line-length", "MaxLineLength")
private val ossLicenses = listOf(
    OssLicense("Jetpack Compose", "Google", APACHE_2, "https://developer.android.com/jetpack/compose"),
    OssLicense("AndroidX (Jetpack)", "Google", APACHE_2, "https://developer.android.com/jetpack"),
    OssLicense("Material Components for Android", "Google", APACHE_2, "https://github.com/material-components/material-components-android"),
    OssLicense("Room", "Google", APACHE_2, "https://developer.android.com/training/data-storage/room"),
    OssLicense("Hilt / Dagger", "Google", APACHE_2, "https://dagger.dev/hilt/"),
    OssLicense("Retrofit", "Square, Inc.", APACHE_2, "https://github.com/square/retrofit"),
    OssLicense("OkHttp", "Square, Inc.", APACHE_2, "https://github.com/square/okhttp"),
    OssLicense("Kotlin", "JetBrains", APACHE_2, "https://kotlinlang.org"),
    OssLicense("kotlinx.serialization", "JetBrains", APACHE_2, "https://github.com/Kotlin/kotlinx.serialization"),
    OssLicense("kotlinx.coroutines", "JetBrains", APACHE_2, "https://github.com/Kotlin/kotlinx.coroutines"),
    OssLicense("Coil", "Coil Contributors", APACHE_2, "https://github.com/coil-kt/coil"),
    OssLicense("Lottie for Android", "Airbnb, Inc.", APACHE_2, "https://github.com/airbnb/lottie-android"),
    OssLicense("Timber", "Jake Wharton", APACHE_2, "https://github.com/JakeWharton/timber"),
    OssLicense("Firebase Android SDK", "Google", APACHE_2, "https://github.com/firebase/firebase-android-sdk"),
    OssLicense("Google Play Services", "Google", ANDROID_SDK_LICENSE, "https://developers.google.com/android/guides/setup"),
    OssLicense("Maps Compose", "Google", APACHE_2, "https://github.com/googlemaps/android-maps-compose"),
    OssLicense("Places SDK for Android", "Google", ANDROID_SDK_LICENSE, "https://developers.google.com/maps/documentation/places/android-sdk"),
    OssLicense("Reorderable", "Calvin Liang", APACHE_2, "https://github.com/Calvin-LL/Reorderable"),
)

@Composable
fun LicensesScreen() {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TDText(
                text = stringResource(R.string.licenses_intro),
                style = TDTheme.typography.regularTextStyle,
                color = TDTheme.colors.gray,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        items(ossLicenses) { library ->
            LicenseCard(library) {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(library.url))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
        }
    }
}

@Composable
private fun LicenseCard(library: OssLicense, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TDTheme.colors.lightPending)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TDText(
            text = library.name,
            style = TDTheme.typography.heading6,
            color = TDTheme.colors.onBackground,
        )
        TDText(
            text = library.author,
            style = TDTheme.typography.regularTextStyle,
            color = TDTheme.colors.gray,
        )
        TDText(
            text = library.license,
            style = TDTheme.typography.regularTextStyle,
            color = TDTheme.colors.darkPending,
        )
    }
}

@TDPreview
@Composable
private fun LicensesScreenPreview() {
    TDTheme {
        LicensesScreen()
    }
}
