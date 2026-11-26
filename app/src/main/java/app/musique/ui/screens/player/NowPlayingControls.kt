package app.musique.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayDisabled
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.musique.R
import app.musique.core.model.Artist
import app.musique.core.model.Episode
import app.musique.core.model.PlayableAudio
import app.musique.ui.Haptics
import app.musique.ui.components.DraggableMarquee
import app.musique.ui.components.WavyMusicSlider
import app.musique.ui.theme.LocalNowPlayingPalette
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueMotion
import app.musique.ui.theme.MusiqueShapes
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.pressScale
import java.util.Locale
import kotlin.math.abs

/** Round, borderless icon button used across Now Playing (44dp target, press scale). */
@Composable
internal fun PlayerIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MusiqueColors.textPrimary,
    background: Color = Color.Transparent,
    size: Dp = MusiqueDimens.minTouchTarget,
    iconSize: Dp = 24.dp,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .pressScale(interactionSource, pressedScale = 0.88f, pressedAlpha = 0.75f)
            .clip(CircleShape)
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                enabled = enabled,
                onClick = onClick,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.4f),
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Favorite star that pops when it flips, like Cider and Apple Music. */
@Composable
internal fun FavoriteButton(
    isLiked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = MusiqueDimens.minTouchTarget,
) {
    val palette = LocalNowPlayingPalette.current
    val pop = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(isLiked) {
        if (first) {
            first = false
            return@LaunchedEffect
        }
        pop.snapTo(if (isLiked) 1.25f else 0.85f)
        pop.animateTo(1f, MusiqueMotion.press())
    }
    PlayerIconButton(
        icon = if (isLiked) Icons.Rounded.Star else Icons.Rounded.StarBorder,
        contentDescription = stringResource(R.string.cd_favorite),
        onClick = onToggle,
        tint = if (isLiked) palette.accent else MusiqueColors.textPrimary.copy(alpha = 0.85f),
        size = size,
        modifier = modifier.graphicsLayer {
            scaleX = pop.value
            scaleY = pop.value
        },
    )
}

/**
 * Title (marquee on overflow) and clickable artists, with favorite and more on the right.
 * [compact] is the lyrics mode header variant.
 */
@Composable
internal fun NowPlayingTitleRow(
    audio: PlayableAudio?,
    isLiked: Boolean,
    isPlaying: Boolean,
    expansionFractionProvider: () -> Float,
    onArtistClick: (Artist) -> Unit,
    onShowClick: (Episode?) -> Unit,
    onToggleFavorite: (() -> Unit)?,
    onMoreClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val titleStyle = if (compact) {
        MusiqueType.heroSubtitle.copy(fontWeight = FontWeight.Bold, color = MusiqueColors.textPrimary)
    } else {
        MusiqueType.heroTitle.copy(color = MusiqueColors.textPrimary)
    }
    val subtitleStyle = if (compact) {
        MusiqueType.rowSubtitle.copy(fontWeight = FontWeight.Medium, color = MusiqueColors.textSecondary)
    } else {
        MusiqueType.heroSubtitle.copy(color = Color.White.copy(alpha = 0.74f))
    }
    val buttonSize = if (compact) 40.dp else MusiqueDimens.minTouchTarget

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            DraggableMarquee(
                contentKey = audio?.uri to audio?.name,
                expansionFractionProvider = expansionFractionProvider,
                canScroll = isPlaying,
            ) {
                Text(
                    text = audio?.name ?: stringResource(R.string.player_nothing_playing),
                    style = titleStyle,
                    maxLines = 1,
                    softWrap = false,
                )
            }

            Spacer(Modifier.height(if (compact) 0.dp else 2.dp))

            val artists = audio?.artists.orEmpty()
            val subtitle = artists.takeIf { it.isNotEmpty() }?.joinToString { it.name }
                ?: audio?.showName
                ?: stringResource(R.string.player_unknown_source)

            DraggableMarquee(
                contentKey = audio?.uri to subtitle,
                expansionFractionProvider = expansionFractionProvider,
                canScroll = isPlaying,
            ) {
                when {
                    audio?.isEpisode() ?: true -> Text(
                        text = subtitle,
                        style = subtitleStyle,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.clickable { onShowClick(audio?.sourceEpisode) },
                    )

                    artists.isEmpty() -> Text(
                        text = subtitle,
                        style = subtitleStyle,
                        maxLines = 1,
                        softWrap = false,
                    )

                    else -> artists.forEachIndexed { index, artist ->
                        Text(
                            text = artist.name + if (index < artists.lastIndex) ", " else "",
                            style = subtitleStyle,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.clickable { onArtistClick(artist) },
                        )
                    }
                }
            }
        }

        if (onToggleFavorite != null) {
            FavoriteButton(isLiked = isLiked, onToggle = onToggleFavorite, size = buttonSize)
        }
        if (onMoreClick != null) {
            PlayerIconButton(
                icon = Icons.Rounded.MoreHoriz,
                contentDescription = stringResource(R.string.player_more_options),
                onClick = onMoreClick,
                size = buttonSize,
                background = if (compact) Color.Transparent else MusiqueColors.fillOnArtwork,
                iconSize = if (compact) 24.dp else 22.dp,
            )
        }
    }
}

internal fun formatPlayerTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0L)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.ROOT, "%d:%02d", m, s)
    }
}

/**
 * Cider scrubber: wave on the elapsed part while playing, flat when paused or dragging, vertical
 * pill thumb, then elapsed / quality / -remaining.
 */
@Composable
internal fun NowPlayingScrubber(
    elapsedMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    qualityLabel: String?,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current

    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var lastTickValue by remember { mutableFloatStateOf(0f) }

    val progressTarget = if (durationMs > 0) (elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 300),
        label = "scrubberProgress",
    )
    val value = if (isDragging) dragValue else animatedProgress
    val shownMs = (value * durationMs).toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))

    Column(modifier = modifier.fillMaxWidth()) {
        WavyMusicSlider(
            value = value,
            onValueChange = {
                if (!isDragging) {
                    isDragging = true
                    lastTickValue = it
                }
                dragValue = it.coerceIn(0f, 1f)
                if (abs(dragValue - lastTickValue) >= 0.02f) {
                    lastTickValue = dragValue
                    Haptics.textHandleMove(context, view)
                }
            },
            onValueChangeFinished = {
                onSeek((dragValue * durationMs).toLong().coerceIn(0L, durationMs.coerceAtLeast(0L)))
                isDragging = false
            },
            isPlaying = isPlaying,
            semanticsLabel = stringResource(R.string.player_scrubber_cd),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
        ) {
            Text(
                text = formatPlayerTime(shownMs),
                style = MusiqueType.timestamp,
                color = Color.White.copy(alpha = 0.70f),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = qualityLabel.orEmpty(),
                style = MusiqueType.timestamp,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "-" + formatPlayerTime((durationMs - shownMs).coerceAtLeast(0L)),
                style = MusiqueType.timestamp,
                color = Color.White.copy(alpha = 0.70f),
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private enum class TransportIcon { Buffering, Playing, Paused, Unavailable }

private class TransportButton(
    val key: String,
    val baseWeight: Float,
    val primary: Boolean,
    val onClick: () -> Unit,
    val content: @Composable () -> Unit,
)

/**
 * Three M3 Expressive split pills: previous / wide white play-pause / next. Episodes get
 * rewind and forward pills as well. The pressed pill widens and its neighbours give way.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NowPlayingTransport(
    isAudioSet: Boolean,
    isBuffering: Boolean,
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = MusiqueDimens.transportHeight,
    onRewind: (() -> Unit)? = null,
    onForward: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val iconState = when {
        !isAudioSet -> TransportIcon.Unavailable
        isBuffering -> TransportIcon.Buffering
        isPlaying -> TransportIcon.Playing
        else -> TransportIcon.Paused
    }
    val sideIconSize = if (height < 72.dp) 24.dp else 28.dp
    val mainIconSize = if (height < 72.dp) 30.dp else 34.dp

    val buttons = buildList {
        val withSeek = onRewind != null && onForward != null
        val sideWeight = if (withSeek) 0.15f else 0.24f
        add(
            TransportButton("prev", sideWeight, false, {
                Haptics.textHandleMove(context, view)
                onPrevious()
            }) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.common_previous),
                    tint = MusiqueColors.textPrimary,
                    modifier = Modifier.size(sideIconSize),
                )
            }
        )
        if (withSeek) {
            add(
                TransportButton("rewind", sideWeight, false, {
                    Haptics.textHandleMove(context, view)
                    onRewind!!()
                }) {
                    Icon(
                        Icons.Rounded.FastRewind,
                        contentDescription = stringResource(R.string.player_fr),
                        tint = MusiqueColors.textPrimary,
                        modifier = Modifier.size(sideIconSize),
                    )
                }
            )
        }
        add(
            TransportButton("play", if (withSeek) 0.40f else 0.52f, true, {
                Haptics.toggle(context, view)
                onPlayPause()
            }) {
                AnimatedContent(
                    targetState = iconState,
                    transitionSpec = {
                        (scaleIn(MusiqueMotion.press(), initialScale = 0.6f) + fadeIn()) togetherWith
                            (scaleOut(targetScale = 0.6f) + fadeOut())
                    },
                    label = "playPauseIcon",
                ) { state ->
                    when (state) {
                        TransportIcon.Buffering -> LoadingIndicator(
                            color = MusiqueColors.onPillSelected,
                            modifier = Modifier.size(mainIconSize + 6.dp),
                        )

                        TransportIcon.Playing -> Icon(
                            Icons.Rounded.Pause,
                            contentDescription = stringResource(R.string.common_pause),
                            tint = MusiqueColors.onPillSelected,
                            modifier = Modifier.size(mainIconSize),
                        )

                        TransportIcon.Paused -> Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.common_play),
                            tint = MusiqueColors.onPillSelected,
                            modifier = Modifier.size(mainIconSize),
                        )

                        TransportIcon.Unavailable -> Icon(
                            Icons.Rounded.PlayDisabled,
                            contentDescription = stringResource(R.string.player_play_not_available),
                            tint = MusiqueColors.onPillSelected.copy(alpha = 0.6f),
                            modifier = Modifier.size(mainIconSize - 6.dp),
                        )
                    }
                }
            }
        )
        if (withSeek) {
            add(
                TransportButton("forward", sideWeight, false, {
                    Haptics.textHandleMove(context, view)
                    onForward!!()
                }) {
                    Icon(
                        Icons.Rounded.FastForward,
                        contentDescription = stringResource(R.string.player_ff),
                        tint = MusiqueColors.textPrimary,
                        modifier = Modifier.size(sideIconSize),
                    )
                }
            )
        }
        add(
            TransportButton("next", sideWeight, false, {
                Haptics.textHandleMove(context, view)
                onNext()
            }) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.common_next),
                    tint = MusiqueColors.textPrimary,
                    modifier = Modifier.size(sideIconSize),
                )
            }
        )
    }

    val sources = remember(buttons.size) { List(buttons.size) { MutableInteractionSource() } }
    val pressed = sources.map { it.collectIsPressedAsState().value }
    val pressedIndex = pressed.indexOf(true)

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        buttons.forEachIndexed { index, button ->
            val target = when (pressedIndex) {
                -1 -> button.baseWeight
                index -> button.baseWeight + 0.08f
                else -> button.baseWeight - 0.08f / (buttons.size - 1)
            }
            val weight by animateFloatAsState(target, MusiqueMotion.press(), label = "transportWeight")
            // Cider reads as one segmented control: round outer ends, tight inner corners.
            val shape: Shape = when {
                button.primary -> MusiqueShapes.transportMain
                index == 0 -> MusiqueShapes.transportStart
                index == buttons.lastIndex -> MusiqueShapes.transportEnd
                else -> MusiqueShapes.transportInner
            }
            val enabled = !button.primary || iconState != TransportIcon.Unavailable
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(weight)
                    .fillMaxHeight()
                    .pressScale(sources[index], pressedScale = 0.97f, pressedAlpha = 0.85f)
                    .clip(shape)
                    .background(
                        when {
                            button.primary && enabled -> MusiqueColors.pillSelected
                            button.primary -> MusiqueColors.pillSelected.copy(alpha = 0.6f)
                            pressed[index] -> MusiqueColors.fillOnArtworkPressed
                            else -> MusiqueColors.fillOnArtwork
                        }
                    )
                    .clickable(
                        interactionSource = sources[index],
                        indication = ripple(),
                        enabled = enabled,
                        onClick = button.onClick,
                    ),
            ) {
                button.content()
            }
        }
    }
}

/**
 * Rounded-top drawer pinned to the bottom of Now Playing: "Playing from <context>" plus Lyrics,
 * Devices and Queue. Dragging it up opens the queue.
 */
@Composable
internal fun NowPlayingDrawer(
    contextName: String?,
    showLyricsButton: Boolean,
    lyricsActive: Boolean,
    isRemotePlayback: Boolean,
    onLyricsClick: () -> Unit,
    onDevicesClick: () -> Unit,
    onQueueClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
    shape: Shape = MusiqueShapes.sheetTop,
    showHandle: Boolean = true,
) {
    val palette = LocalNowPlayingPalette.current
    val density = LocalDensity.current
    var dragTotal by remember { mutableFloatStateOf(0f) }
    val openThresholdPx = with(density) { 36.dp.toPx() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.10f))
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { dragTotal += it },
                onDragStarted = { dragTotal = 0f },
                onDragStopped = { velocity ->
                    if (dragTotal < -openThresholdPx || velocity < -900f) onQueueClick()
                    dragTotal = 0f
                },
            )
            .padding(bottom = bottomInset),
    ) {
        if (showHandle) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally)
                    .size(MusiqueDimens.grabberWidth, MusiqueDimens.grabberHeight)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f)),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (showHandle) MusiqueDimens.playerDrawerHeight - 12.dp else MusiqueDimens.playerDrawerHeight)
                .padding(start = 20.dp, end = 12.dp),
        ) {
            Text(
                text = buildAnnotatedString {
                    if (contextName != null) {
                        withStyle(SpanStyle(color = Color.White.copy(alpha = 0.72f))) {
                            append(stringResource(R.string.player_playing_from_prefix))
                        }
                        withStyle(SpanStyle(color = MusiqueColors.textPrimary, fontWeight = FontWeight.Bold)) {
                            append(contextName)
                        }
                    }
                },
                style = TextStyle.Default.merge(MusiqueType.rowSubtitle),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onQueueClick,
                    ),
            )
            Spacer(Modifier.width(8.dp))
            if (showLyricsButton) {
                PlayerIconButton(
                    icon = Icons.Rounded.Lyrics,
                    contentDescription = stringResource(
                        if (lyricsActive) R.string.player_lyrics_hide else R.string.player_lyrics_show
                    ),
                    onClick = onLyricsClick,
                    iconSize = 22.dp,
                    tint = if (lyricsActive) MusiqueColors.onPillSelected else MusiqueColors.textPrimary,
                    background = if (lyricsActive) MusiqueColors.pillSelected else Color.Transparent,
                )
            }
            PlayerIconButton(
                icon = Icons.Rounded.Devices,
                contentDescription = stringResource(R.string.cd_devices),
                onClick = onDevicesClick,
                iconSize = 22.dp,
                tint = if (isRemotePlayback) palette.accent else MusiqueColors.textPrimary,
            )
            PlayerIconButton(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = stringResource(R.string.player_open_queue),
                onClick = onQueueClick,
                iconSize = 22.dp,
            )
        }
    }
}

/** Translucent pill grabber that sits at the top of the sheet. */
@Composable
internal fun SheetGrabber(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(MusiqueDimens.grabberWidth, MusiqueDimens.grabberHeight)
            .clip(RoundedCornerShape(50))
            .background(MusiqueColors.grabber),
    )
}
