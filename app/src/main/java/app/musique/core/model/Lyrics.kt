package app.musique.core.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
data class LyricsResponse(
    val lyrics: Lyrics
)

@Serializable
data class Lyrics(
    val syncType: String,
    val lines: List<RawLyricLine>
)

@Serializable
data class RawLyricSyllable(
    val startTimeMs: String,
    val numChars: Int,
)

@Serializable
data class RawLyricLine(
    val startTimeMs: String,
    val words: String,
    val endTimeMs: String? = null,
    val syllables: List<RawLyricSyllable>? = null,
)

@Immutable
data class LyricWord(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

enum class LyricRole {
    MAIN,
    DUET,
    BG,
}

@Immutable
data class LyricLine(
    val timestampMs: Long,
    val text: String,
    val endMs: Long? = null,
    val words: List<LyricWord>? = null,
    val role: LyricRole? = null,
    val agent: String? = null,
    val isBackground: Boolean = role == LyricRole.BG,
    val synced: Boolean = timestampMs >= 0L,
    val translation: String? = null,
    val roman: String? = null,
) {
    val effectiveIsBackground: Boolean
        get() = isBackground || role == LyricRole.BG
}
