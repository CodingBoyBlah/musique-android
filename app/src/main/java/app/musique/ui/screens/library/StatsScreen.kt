package app.musique.ui.screens.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.musique.ALBUM_COVER_URL
import app.musique.ScreenBottomPadding
import app.musique.core.history.PlayHistory
import app.musique.core.history.PlayRecord
import app.musique.ui.components.SmartImage
import app.musique.ui.components.musique.LargeTitleTopBar
import app.musique.ui.components.musique.SectionHeader
import app.musique.ui.components.musique.rememberCollapseFraction
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueMotion
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.pressScale
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class StatsRange(val label: String, val days: Long?) {
    Week("Week", 7), Month("Month", 30), Year("Year", 365), All("All time", null)
}

data class Ranked(
    val key: String,
    val title: String,
    val subtitle: String?,
    val cover: String?,
    val plays: Int,
    val uri: String?,
)

data class ListeningStats(
    val totalMs: Long = 0,
    val plays: Int = 0,
    val distinctTracks: Int = 0,
    val distinctArtists: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val topTracks: List<Ranked> = emptyList(),
    val topArtists: List<Ranked> = emptyList(),
    val topAlbums: List<Ranked> = emptyList(),
)

data class StatsUiState(
    val range: StatsRange = StatsRange.Month,
    val loading: Boolean = true,
    val stats: ListeningStats = ListeningStats(),
)

/** Computed on the phone from what was played in Musique, like the desktop Stats page. */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val history: PlayHistory,
) : ViewModel() {
    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()
    private var records: List<PlayRecord> = emptyList()

    init {
        viewModelScope.launch {
            records = history.all()
            recompute()
        }
    }

    fun setRange(range: StatsRange) {
        _state.update { it.copy(range = range) }
        recompute()
    }

    private fun recompute() {
        val range = _state.value.range
        _state.update { it.copy(loading = false, stats = compute(records, range)) }
    }

    companion object {
        fun compute(all: List<PlayRecord>, range: StatsRange, now: Long = System.currentTimeMillis()): ListeningStats {
            val from = range.days?.let { now - it * 86_400_000L } ?: Long.MIN_VALUE
            val plays = all.filter { it.t >= from }
            val zone = ZoneId.systemDefault()
            val days = all.map { Instant.ofEpochMilli(it.t).atZone(zone).toLocalDate() }.toSortedSet()

            fun <K> top(key: (PlayRecord) -> K?, build: (K, List<PlayRecord>) -> Ranked): List<Ranked> =
                plays.groupBy(key).filterKeys { it != null }
                    .entries.sortedWith(compareByDescending<Map.Entry<K?, List<PlayRecord>>> { it.value.size }
                        .thenByDescending { e -> e.value.sumOf { it.heardMs } })
                    .take(10)
                    .map { build(it.key!!, it.value) }

            return ListeningStats(
                totalMs = plays.sumOf { it.heardMs },
                plays = plays.size,
                distinctTracks = plays.distinctBy { it.id }.size,
                distinctArtists = plays.distinctBy { it.artistUri ?: it.artist }.size,
                currentStreak = currentStreak(days, LocalDate.now(zone)),
                longestStreak = longestStreak(days),
                topTracks = top({ it.id }) { _, list ->
                    val r = list.last()
                    Ranked(r.id, r.name, r.artist, r.cover, list.size, r.albumUri)
                },
                topArtists = top({ it.artistUri ?: it.artist.ifBlank { null } }) { k, list ->
                    val r = list.last()
                    Ranked(k, r.artist, null, r.cover, list.size, r.artistUri)
                },
                topAlbums = top({ it.albumUri }) { k, list ->
                    val r = list.last()
                    Ranked(k, r.album.orEmpty(), r.artist, r.cover, list.size, r.albumUri)
                },
            )
        }

        private fun currentStreak(days: Set<LocalDate>, today: LocalDate): Int {
            var day = if (today in days) today else today.minusDays(1)
            var n = 0
            while (day in days) {
                n++
                day = day.minusDays(1)
            }
            return n
        }

        private fun longestStreak(days: Set<LocalDate>): Int {
            var best = 0
            var run = 0
            var prev: LocalDate? = null
            for (d in days) {
                run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
                best = maxOf(best, run)
                prev = d
            }
            return best
        }
    }
}

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val collapse = rememberCollapseFraction(listState)
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val stats = state.stats

    Box(
        modifier
            .fillMaxSize()
            .background(MusiqueColors.canvas)
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = statusBar + MusiqueDimens.topBarHeight + 4.dp,
                bottom = ScreenBottomPadding + 24.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "range") {
                RangePicker(
                    selected = state.range,
                    onSelect = viewModel::setRange,
                    modifier = Modifier.padding(horizontal = MusiqueDimens.screenPadding, vertical = 8.dp),
                )
            }

            if (!state.loading && stats.plays == 0) {
                item(key = "empty") {
                    Text(
                        text = "Nothing here yet. Songs you listen to on this phone for 30 seconds or more show up here.",
                        style = MusiqueType.body,
                        color = MusiqueColors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp, vertical = 48.dp),
                    )
                }
                return@LazyColumn
            }

            item(key = "summary") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = MusiqueDimens.screenPadding, vertical = 8.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(formatListening(stats.totalMs), "listened", Modifier.weight(1f), highlight = true)
                        StatTile(stats.plays.toString(), if (stats.plays == 1) "play" else "plays", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(stats.distinctTracks.toString(), "songs", Modifier.weight(1f))
                        StatTile(stats.distinctArtists.toString(), "artists", Modifier.weight(1f))
                        StatTile(
                            "${stats.currentStreak}d",
                            if (stats.longestStreak > stats.currentStreak) "streak, best ${stats.longestStreak}d" else "streak",
                            Modifier.weight(1f),
                        )
                    }
                }
            }

            rankedSection("Top Songs", stats.topTracks, round = false) { it.uri?.let(onOpenAlbum) }
            rankedSection("Top Artists", stats.topArtists, round = true) { it.uri?.let(onOpenArtist) }
            rankedSection("Top Albums", stats.topAlbums, round = false) { it.uri?.let(onOpenAlbum) }
        }

        LargeTitleTopBar(title = "Stats", collapseFraction = collapse, onBack = onBack)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.rankedSection(
    title: String,
    items: List<Ranked>,
    round: Boolean,
    onClick: (Ranked) -> Unit,
) {
    if (items.isEmpty()) return
    item(key = "h:$title") {
        SectionHeader(title = title, modifier = Modifier.padding(top = 20.dp))
    }
    items.forEachIndexed { index, r ->
        item(key = "$title:${r.key}") { RankedRow(index + 1, r, round) { onClick(r) } }
    }
}

@Composable
private fun RangePicker(selected: StatsRange, onSelect: (StatsRange) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MusiqueColors.chrome)
            .padding(3.dp),
    ) {
        StatsRange.entries.forEach { range ->
            val active = range == selected
            val bg by animateColorAsState(
                if (active) MusiqueColors.accent else Color.Transparent,
                MusiqueMotion.press(),
                label = "rangeBg",
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .clickable { onSelect(range) },
            ) {
                Text(
                    text = range.label,
                    style = MusiqueType.navLabel,
                    color = if (active) Color.White else MusiqueColors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (highlight) MusiqueColors.accentDim else MusiqueColors.card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = value,
            style = MusiqueType.heroTitle.copy(fontFeatureSettings = "tnum"),
            color = if (highlight) MusiqueColors.accentLight else MusiqueColors.textPrimary,
            maxLines = 1,
        )
        Text(text = label, style = MusiqueType.caption, color = MusiqueColors.textTertiary, maxLines = 1)
    }
}

@Composable
private fun RankedRow(rank: Int, item: Ranked, round: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction, pressedScale = 0.98f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = MusiqueDimens.screenPadding, vertical = 6.dp),
    ) {
        Text(
            text = rank.toString(),
            style = MusiqueType.rowDuration,
            color = MusiqueColors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(24.dp),
        )
        SmartImage(
            url = item.cover?.let { ALBUM_COVER_URL + it },
            shape = if (round) CircleShape else RoundedCornerShape(8.dp),
            modifier = Modifier
                .padding(start = 6.dp, end = 12.dp)
                .size(MusiqueDimens.rowArtworkSize),
        )
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MusiqueType.rowTitle, color = MusiqueColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            item.subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MusiqueType.rowSubtitle, color = MusiqueColors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(
            text = "${item.plays} ${if (item.plays == 1) "play" else "plays"}",
            style = MusiqueType.rowDuration,
            color = MusiqueColors.textTertiary,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

private fun formatListening(ms: Long): String {
    val minutes = ms / 60_000
    return when {
        minutes < 60 -> "${minutes}m"
        minutes < 6000 -> "${minutes / 60}h ${minutes % 60}m"
        else -> "${minutes / 60}h"
    }
}
