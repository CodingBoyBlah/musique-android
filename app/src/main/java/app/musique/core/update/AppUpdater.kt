package app.musique.core.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import app.musique.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AppUpdater"
private const val REPO = "CodingBoyBlah/musique-android"
private const val LATEST_RELEASE_API = "https://api.github.com/repos/$REPO/releases/latest"
private const val RELEASES_PAGE = "https://github.com/$REPO/releases/latest"
private const val LAUNCH_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L

data class AppRelease(
    val version: String,
    val notes: String,
    val pageUrl: String,
    val apkUrl: String?,
    val apkSize: Long,
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: AppRelease) : UpdateState
    data class Downloading(val release: AppRelease, val progress: Float) : UpdateState
    data class Installing(val release: AppRelease) : UpdateState
    data class Error(val message: String?, val release: AppRelease? = null) : UpdateState
}

/**
 * Over the air updates from GitHub Releases: finds a newer release, downloads the APK built for
 * this phone's ABI and hands it to the system installer. Releases are signed with the same key as
 * every CI build, so they install over the running app and keep its data.
 */
@Singleton
class AppUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** True when a check made at launch found an update the user has not dismissed yet. */
    private val _promptVisible = MutableStateFlow(false)
    val promptVisible: StateFlow<Boolean> = _promptVisible.asStateFlow()

    private var job: Job? = null

    /** Quiet check at app start, at most every few hours; shows the prompt when it finds one. */
    fun checkOnLaunch() {
        val now = System.currentTimeMillis()
        if (now - prefs.getLong("lastCheck", 0) < LAUNCH_CHECK_INTERVAL_MS) return
        check(fromLaunch = true)
    }

    fun check(fromLaunch: Boolean = false) {
        if (job?.isActive == true) return
        if (!fromLaunch) _state.value = UpdateState.Checking
        job = scope.launch {
            val result = withContext(Dispatchers.IO) { fetchLatest() }
            prefs.edit().putLong("lastCheck", System.currentTimeMillis()).apply()
            result.onSuccess { release ->
                if (release != null && isNewer(release.version, BuildConfig.VERSION_NAME)) {
                    _state.value = UpdateState.Available(release)
                    if (!fromLaunch || prefs.getString("dismissed", null) != release.version) {
                        _promptVisible.value = fromLaunch
                    }
                } else if (!fromLaunch) {
                    _state.value = UpdateState.UpToDate
                }
            }.onFailure { error ->
                Log.w(TAG, "update check failed", error)
                if (!fromLaunch) _state.value = UpdateState.Error(error.message)
            }
        }
    }

    /** Hides the launch prompt and stops it coming back for this version. */
    fun dismissPrompt() {
        (state.value as? UpdateState.Available)?.let {
            prefs.edit().putString("dismissed", it.release.version).apply()
        }
        _promptVisible.value = false
    }

    /** Downloads and installs [release]; falls back to the release page when it has no APK. */
    fun install(release: AppRelease) {
        if (job?.isActive == true) return
        val apkUrl = release.apkUrl ?: return openPage(release.pageUrl)
        if (!context.packageManager.canRequestPackageInstalls()) {
            // The user grants "Install unknown apps" once, then taps Update again.
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        job = scope.launch {
            _state.value = UpdateState.Downloading(release, 0f)
            try {
                val apk = withContext(Dispatchers.IO) {
                    download(apkUrl, release) { progress ->
                        _state.value = UpdateState.Downloading(release, progress)
                    }
                }
                _state.value = UpdateState.Installing(release)
                withContext(Dispatchers.IO) { commit(apk) }
            } catch (e: Exception) {
                Log.w(TAG, "update install failed", e)
                _state.value = UpdateState.Error(e.message, release)
            }
        }
    }

    /** Called by [UpdateInstallReceiver] when the system installer gives up. */
    internal fun onInstallFailed(message: String?) {
        val release = when (val s = state.value) {
            is UpdateState.Installing -> s.release
            is UpdateState.Downloading -> s.release
            else -> null
        }
        _state.value = UpdateState.Error(message ?: "Install failed", release)
    }

    private fun openPage(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun fetchLatest(): Result<AppRelease?> = runCatching {
        val connection = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("User-Agent", "Musique/${BuildConfig.VERSION_NAME}")
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Unit
                HttpURLConnection.HTTP_NOT_FOUND -> return@runCatching null // no release yet
                else -> error("HTTP $code")
            }
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val assets = json.optJSONArray("assets")
            val apks = buildMap {
                for (i in 0 until (assets?.length() ?: 0)) {
                    val asset = assets!!.getJSONObject(i)
                    val name = asset.getString("name")
                    if (name.endsWith(".apk")) put(name, asset)
                }
            }
            val abis = Build.SUPPORTED_ABIS.toList() + "universal"
            val apk = abis.firstNotNullOfOrNull { abi -> apks.entries.firstOrNull { it.key.contains(abi) }?.value }
                ?: apks.values.firstOrNull()
            AppRelease(
                version = json.getString("tag_name").removePrefix("v").removePrefix("V"),
                notes = json.optString("body").trim(),
                pageUrl = json.optString("html_url").ifBlank { RELEASES_PAGE },
                apkUrl = apk?.getString("browser_download_url"),
                apkSize = apk?.optLong("size") ?: 0L,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun download(url: String, release: AppRelease, onProgress: (Float) -> Unit): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "musique-${release.version}.apk")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Musique/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${connection.responseCode}")
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: release.apkSize
            var read = 0L
            var lastReported = -1
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        if (total > 0) {
                            val percent = (read * 100 / total).toInt()
                            if (percent != lastReported) {
                                lastReported = percent
                                onProgress(percent / 100f)
                            }
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        return file
    }

    private fun commit(apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("musique.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val intent = Intent(context, UpdateInstallReceiver::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            val pending = PendingIntent.getBroadcast(context, sessionId, intent, flags)
            session.commit(pending.intentSender)
        }
    }

    companion object {
        /** Compares dotted numeric versions, ignoring a leading "v" and any "-suffix" / "+build". */
        fun isNewer(latest: String, current: String): Boolean {
            val a = parse(latest)
            val b = parse(current)
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }

        private fun parse(version: String): List<Int> =
            version.trim()
                .removePrefix("v").removePrefix("V")
                .substringBefore('-')
                .substringBefore('+')
                .split('.')
                .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
    }
}
