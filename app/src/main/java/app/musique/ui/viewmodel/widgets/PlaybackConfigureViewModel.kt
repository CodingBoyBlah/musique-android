package app.musique.ui.viewmodel.widgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musique.core.model.PlayableAudio
import app.musique.data.repository.InterfaceSettings
import app.musique.data.repository.SettingsRepository
import app.musique.playback.PlaybackStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PlaybackConfigureViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    playbackStateHolder: PlaybackStateHolder,
) : ViewModel() {
    val interfaceSettings: Flow<InterfaceSettings> =
        settingsRepository.interfaceSettings

    val currentAudio: StateFlow<PlayableAudio?> = playbackStateHolder.state
        .map { it.currentAudio }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
}