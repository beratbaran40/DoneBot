package com.todoapp.mobile.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.android.libraries.places.api.Places
import com.todoapp.mobile.ui.common.locationpicker.LocationSearchDialog
import timber.log.Timber

/**
 * Opens the app's custom full-screen location search ([LocationSearchDialog]) and forwards the
 * chosen place to [onPicked]. Returns a `() -> Unit` you attach to a click handler — the API is
 * unchanged from the old Google Places-widget launcher, so every call site stays the same.
 *
 * If the Places SDK isn't initialized (no `MAPS_API_KEY`), the trigger logs and no-ops so the
 * rest of the form still works in CI / local-no-key dev.
 *
 * @param onPicked called with (name, address, lat, lng) on a successful pick. Latitude /
 *  longitude may be null when the chosen result lacks coordinates.
 */
@Composable
fun rememberLocationPickerLauncher(
    onPicked: (name: String, address: String, lat: Double?, lng: Double?) -> Unit,
): () -> Unit {
    var isOpen by remember { mutableStateOf(false) }

    if (isOpen) {
        LocationSearchDialog(
            onPicked = { name, address, lat, lng ->
                isOpen = false
                onPicked(name, address, lat, lng)
            },
            onDismiss = { isOpen = false },
        )
    }

    return remember {
        {
            if (Places.isInitialized()) {
                isOpen = true
            } else {
                Timber.tag("LocationPicker").w("Places SDK not initialized — MAPS_API_KEY missing.")
            }
        }
    }
}
