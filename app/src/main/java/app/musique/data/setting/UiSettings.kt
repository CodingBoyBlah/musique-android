package app.musique.data.setting

import androidx.compose.runtime.compositionLocalOf
import app.musique.data.repository.InterfaceSettings

val LocalUiSettings = compositionLocalOf {
    InterfaceSettings()
}