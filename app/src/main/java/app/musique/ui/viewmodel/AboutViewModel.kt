package app.musique.ui.viewmodel

import androidx.lifecycle.ViewModel
import app.musique.core.update.AppRelease
import app.musique.core.update.AppUpdater
import app.musique.core.update.UpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val updater: AppUpdater,
) : ViewModel() {
    val updateState: StateFlow<UpdateState> = updater.state

    fun checkForUpdates() = updater.check()

    fun install(release: AppRelease) = updater.install(release)
}
