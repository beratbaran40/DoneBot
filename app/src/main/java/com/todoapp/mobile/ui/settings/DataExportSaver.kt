package com.todoapp.mobile.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.todoapp.mobile.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Storage Access Framework saver for the GDPR "Download my data" export. Returns a trigger that,
 * given the export JSON, opens the system "create document" picker and writes the JSON to the
 * location the user chooses — no storage permission, no FileProvider. Surfaces a result toast.
 */
@Composable
fun rememberDataExportSaver(): (String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingJson by remember { mutableStateOf<String?>(null) }
    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            val json = pendingJson
            pendingJson = null
            if (uri == null || json == null) return@rememberLauncherForActivityResult
            scope.launch {
                val saved =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                                ?: error("could not open output stream")
                        }.isSuccess
                    }
                val msg =
                    if (saved) R.string.settings_download_data_saved else R.string.settings_download_data_error
                Toast.makeText(context, context.getString(msg), Toast.LENGTH_SHORT).show()
            }
        }
    return { json ->
        pendingJson = json
        launcher.launch("donebot-data-export.json")
    }
}
