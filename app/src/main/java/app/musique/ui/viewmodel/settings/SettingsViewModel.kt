package app.musique.ui.viewmodel.settings

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.musique.core.AuthCallbackServerManager
import app.musique.core.AuthManager
import app.musique.core.AuthStateEventBus
import app.musique.core.Session
import app.musique.core.SpClient
import app.musique.core.spirc.SpircController
import app.musique.core.spirc.SpircWrapper
import app.musique.data.metadata.NativeErrorHandler
import app.musique.data.repository.CollectionSyncCoordinator
import app.musique.data.repository.SettingsRepository
import app.musique.services.OAuthService
import app.musique.ui.GlobalPopupController
import app.musique.ui.PopupSpec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val spClient: SpClient,
    val authManager: AuthManager,
    private val settingsRepository: SettingsRepository,
    private val serverManager: AuthCallbackServerManager,
    private val spircController: SpircController,
    private val collectionSync: CollectionSyncCoordinator,
) : ViewModel() {
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    val username: StateFlow<String?> = settingsRepository.username
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val profilePicture: StateFlow<String?> = settingsRepository.userImageUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        checkAuthState()
    }

    fun checkAuthState() {
        viewModelScope.launch {
            _isLoggedIn.value = withContext(Dispatchers.IO) {
                authManager.hasCachedCredentials()
            }
        }
    }

    fun startSpircAuth(context: Context) {
        OAuthService.start(context)

        serverManager.start(onCodeReceived = { code, state ->
            OAuthService.stop(context)
            val result = authManager.handleOAuthCode(code, state)
            val isSuccess = result.contains("\"success\":true")
            val errorDetails = if (!isSuccess) parseErrorMessage(result) else null
            if (!isSuccess) {
                NativeErrorHandler.handleErrorJson(result, "spirc oauth")
            }
            GlobalPopupController.show(PopupSpec.AuthResult(isSuccess, errorDetails = errorDetails))
            if (isSuccess) {
                _isLoggedIn.value = authManager.hasCachedCredentials()
                checkAuthState()
                viewModelScope.launch { AuthStateEventBus.emitPlaybackLoggedIn() }
                spircController.restart()
            }
        })

        val url = authManager.getAuthorizationURL()

        context.startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri())
        )
    }

    fun logoutPlayback() {
        collectionSync.pause()
        authManager.logout()
        _isLoggedIn.value = false
        viewModelScope.launch {
            settingsRepository.removeUserProfile()
            AuthStateEventBus.emitPlaybackLoggedOut()
        }
    }

    private fun parseErrorMessage(json: String): String? {
        return try {
            val obj = org.json.JSONObject(json)
            if (obj.has("error")) {
                val err = obj.getJSONObject("error")
                err.optString("message", null)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}