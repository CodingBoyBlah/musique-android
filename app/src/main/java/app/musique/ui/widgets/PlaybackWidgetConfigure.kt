package app.musique.ui.widgets

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.musique.MainActivity.MainActivity.LocalSharedTransitionScope
import app.musique.data.dao.PlaylistDao
import app.musique.data.repository.InterfaceSettings
import app.musique.ui.OutifyTheme
import app.musique.ui.ThemeMode
import app.musique.ui.viewmodel.widgets.PlaybackConfigureViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackWidgetConfigure : ComponentActivity() {

    @Inject
    lateinit var playlistDao: PlaylistDao

    private val viewModel: PlaybackConfigureViewModel by viewModels()

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            val interfaceSettings by viewModel
                .interfaceSettings
                .collectAsState(initial = InterfaceSettings())

            val currentTrack by viewModel.currentAudio.collectAsState()
            val themeMode =
                if (interfaceSettings.dynamicTheme) ThemeMode.DYNAMIC_ALBUM else if (interfaceSettings.dynamicSystem) ThemeMode.DYNAMIC_SYSTEM else ThemeMode.STATIC

            OutifyTheme(
                audio = currentTrack,
                themeMode = themeMode,
                staticAccentColor = interfaceSettings.accentColor,
                pureBlack = interfaceSettings.pureBlack,
                highContrastCompat = interfaceSettings.highContrastCompat,
                content = {
                    SharedTransitionLayout {
                        CompositionLocalProvider(
                            LocalSharedTransitionScope provides this,
                        ) {

                        }
                    }
                }
            )
        }
    }
}