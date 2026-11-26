package app.musique

import android.app.Application
import android.system.Os
import android.util.Log
import android.widget.Toast
import app.musique.ui.theme.MusiqueDimens
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.media3.common.util.UnstableApi
import app.musique.core.spirc.SpircController
import app.musique.data.database.AppDatabase
import app.musique.data.repository.SettingsRepository
import app.musique.data.repository.UserProfileCache
import app.musique.utils.ExceptionCollector
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

const val ALBUM_COVER_URL: String = "https://i.scdn.co/image/"

/**
 * Bottom padding a scrolling page adds after its last item so it clears the floating mini player
 * and tab bar (plus a typical gesture inset). Derived from [MusiqueDimens], keep it that way.
 */
val ScreenBottomPadding = MusiqueDimens.floatingChromeHeight + MusiqueDimens.navInsetAllowance

/**
 * How far a floating button has to sit from the bottom edge to clear the floating chrome:
 * the mini player stacked on the tab bar while something plays, otherwise just the tab bar.
 */
fun fabBottomPadding(isMediaPlaying: Boolean) =
    (if (isMediaPlaying) MusiqueDimens.fabBottomWithMiniPlayer else MusiqueDimens.fabBottomWithoutMiniPlayer) +
        MusiqueDimens.navInsetAllowance

fun widgetMediaPreference(id: GlanceId) =
    stringPreferencesKey("widget_media_$id")

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ImageLoaderEntryPoint {
    fun imageLoader(): ImageLoader
}

@HiltAndroidApp
class OutifyApplication : Application(), SingletonImageLoader.Factory {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    @Inject
    lateinit var spircController: SpircController

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var userProfileCache: UserProfileCache

    @Inject
    lateinit var exceptionCollector: ExceptionCollector

    // context.imageLoader must resolve to the Hilt loader so there is one memory and disk cache.
    // An entry point works even if Coil asks before field injection has run.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        EntryPointAccessors.fromApplication(this, ImageLoaderEntryPoint::class.java).imageLoader()

    @UnstableApi
    override fun onCreate() {
        super.onCreate()

        // Android <= 12: prevent Rust temp_dir() from using /data/local/tmp.
        Os.setenv("TMPDIR", cacheDir.absolutePath, true)
        Log.i("OutifyTMP", "TMPDIR=${System.getenv("TMPDIR")}")
        exceptionCollector.install()

        val libraryLoaded = try {
            System.loadLibrary("librespot_ffi")
            true
        } catch (e: UnsatisfiedLinkError) {
            Log.e("OutifyApplication", "Failed to load librespot_ffi", e)
            false
        }

        if (!libraryLoaded) {
            Toast.makeText(this, getString(R.string.toast_librespot_failed), Toast.LENGTH_LONG).show()
        }

        appScope.launch {
            LibrespotFfi.libInit(applicationContext)
            spircController.start()
            userProfileCache.start(appScope)
        }
    }
}
