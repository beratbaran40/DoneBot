package com.todoapp.mobile.ui.common.locationpicker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.location.model.PlacePrediction
import com.todoapp.uikit.components.TDEmptyState
import com.todoapp.uikit.components.TDErrorState
import com.todoapp.uikit.components.TDSkeletonBox
import com.todoapp.uikit.components.TDSkeletonText
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

private const val SKELETON_ROWS = 6

@Composable
internal fun LocationSearchResults(
    results: List<PlacePrediction>,
    onClick: (placeId: String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(results, key = { it.placeId }) { prediction ->
            LocationResultRow(prediction = prediction, onClick = { onClick(prediction.placeId) })
        }
    }
}

@Composable
private fun LocationResultRow(
    prediction: PlacePrediction,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LocationPinBadge()
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TDText(
                text = prediction.primaryText,
                style = TDTheme.typography.subheading2,
                color = TDTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (prediction.secondaryText.isNotBlank()) {
                TDText(
                    text = prediction.secondaryText,
                    style = TDTheme.typography.regularTextStyle,
                    color = TDTheme.colors.gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LocationPinBadge() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(TDTheme.colors.lightPending),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(com.example.uikit.R.drawable.ic_pin),
            contentDescription = null,
            tint = TDTheme.colors.darkPending,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun LocationSearchLoading() {
    Column(modifier = Modifier.fillMaxSize()) {
        repeat(SKELETON_ROWS) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TDSkeletonBox(modifier = Modifier.size(40.dp), shape = CircleShape)
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TDSkeletonText(width = 200.dp, height = 14.dp)
                    TDSkeletonText(width = 130.dp, height = 10.dp)
                }
            }
        }
    }
}

@Composable
internal fun LocationSearchIdle() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        TDEmptyState(
            title = stringResource(R.string.location_search_prompt),
            iconRes = com.example.uikit.R.drawable.ic_search,
        )
    }
}

@Composable
internal fun LocationSearchEmpty() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        TDEmptyState(
            title = stringResource(R.string.location_search_empty),
            iconRes = com.example.uikit.R.drawable.ic_pin,
        )
    }
}

@Composable
internal fun LocationSearchError(onRetry: () -> Unit) {
    TDErrorState(
        message = stringResource(R.string.location_search_unavailable),
        actionText = stringResource(R.string.retry),
        onActionClick = onRetry,
    )
}

@Composable
internal fun PoweredByGoogleFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(
                // Official Google attribution asset from the Places SDK. Non-transitive R → it lives
                // under the library's own namespace, not the app's. Required by the Places ToS when
                // predictions are shown off-map.
                if (TDTheme.isDark) {
                    com.google.android.libraries.places.R.drawable.places_powered_by_google_light
                } else {
                    com.google.android.libraries.places.R.drawable.places_powered_by_google_dark
                },
            ),
            contentDescription = stringResource(R.string.location_powered_by_google),
            modifier = Modifier.height(18.dp),
        )
    }
}
