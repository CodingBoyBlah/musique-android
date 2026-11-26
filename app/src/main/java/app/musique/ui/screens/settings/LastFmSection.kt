package app.musique.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.musique.core.lastfm.LastFmClient
import app.musique.core.lastfm.LastFmStatus
import app.musique.ui.components.PreferenceEntry
import app.musique.ui.components.SettingsActionRow
import app.musique.ui.components.SettingsDefaults
import app.musique.ui.components.SettingsSection
import app.musique.ui.components.TextInputPreferenceEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LastFmViewModel @Inject constructor(
    private val client: LastFmClient,
) : ViewModel() {
    val status: StateFlow<LastFmStatus> = client.status
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val savedKey get() = client.apiKey
    val savedSecret get() = client.secret

    fun saveApi(key: String, secret: String) = client.saveApi(key, secret)

    fun connect(open: (String) -> Unit) {
        viewModelScope.launch {
            client.startAuth()
                .onSuccess { _message.value = null; open(it) }
                .onFailure { _message.value = it.message }
        }
    }

    /** Called when the user comes back from the browser; quiet until they have approved. */
    fun finish(quiet: Boolean) {
        if (!client.status.value.awaitingAuth) return
        viewModelScope.launch {
            client.finishAuth()
                .onSuccess { _message.value = null }
                .onFailure { if (!quiet) _message.value = it.message }
        }
    }

    fun disconnect() = client.disconnect()
}

/** Last.fm scrobbling, set up the same way as in the desktop app. */
@Composable
fun LastFmSection() {
    val viewModel: LastFmViewModel = hiltViewModel()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var key by remember { mutableStateOf(viewModel.savedKey) }
    var secret by remember { mutableStateOf(viewModel.savedSecret) }
    LaunchedEffect(key, secret) {
        kotlinx.coroutines.delay(500)
        if (key != viewModel.savedKey || secret != viewModel.savedSecret) viewModel.saveApi(key, secret)
    }

    // returning from the approval page finishes the connection without another tap
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.finish(quiet = true) }

    val footer = when {
        message != null -> message
        status.connected -> "Songs you play on this phone are scrobbled once you've heard half of them, or 4 minutes."
        status.awaitingAuth -> "Approve Musique on the Last.fm page, then come back here."
        else -> "Create an API account at last.fm/api to get a key and secret, the same ones the desktop app uses."
    }

    SettingsSection(
        title = "Last.fm",
        icon = Icons.Rounded.GraphicEq,
        iconTint = SettingsDefaults.tintRed,
        footer = footer,
    ) {
        if (status.connected) {
            entry {
                PreferenceEntry(
                    title = { Text("Scrobbling as ${status.username.orEmpty()}") },
                    icon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
                )
            }
            entry {
                SettingsActionRow(text = "Disconnect", onClick = viewModel::disconnect, destructive = true)
            }
        } else {
            entry {
                TextInputPreferenceEntry(
                    title = { Text("API key") },
                    value = key,
                    onValueChange = { key = it },
                    placeholder = "Required",
                    icon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                )
            }
            entry {
                TextInputPreferenceEntry(
                    title = { Text("Shared secret") },
                    value = secret,
                    onValueChange = { secret = it },
                    placeholder = "Required",
                    icon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            }
            entry {
                SettingsActionRow(
                    text = if (status.awaitingAuth) "I've approved it" else "Connect Last.fm",
                    icon = Icons.Rounded.OpenInNew,
                    enabled = key.isNotBlank() && secret.isNotBlank(),
                    onClick = {
                        viewModel.saveApi(key, secret)
                        if (status.awaitingAuth) {
                            viewModel.finish(quiet = false)
                        } else {
                            viewModel.connect { url ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        }
                    },
                )
            }
        }
    }
}
