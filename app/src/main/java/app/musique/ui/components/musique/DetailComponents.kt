package app.musique.ui.components.musique

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.musique.R
import app.musique.core.model.ReleaseDate
import app.musique.core.model.ReleaseDatePrecision
import app.musique.core.model.precision
import app.musique.ui.Haptics
import app.musique.ui.components.AudioBarsIndicator
import app.musique.ui.components.SkeletonBox
import app.musique.ui.components.SmartImage
import app.musique.ui.theme.ArtworkPalette
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueShapes
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.pressScale
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Full-bleed vertical artwork-palette gradient with a soft top color glow, matching Cider's
 * album and playlist detail backdrop.
 */
@Composable
fun DetailPaletteGradientBackdrop(
    palette: ArtworkPalette,
    modifier: Modifier = Modifier,
) {
    val glowColor = palette.swatches.firstOrNull()?.copy(alpha = 0.24f) ?: Color.Transparent
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to palette.backgroundTop,
                        0.68f to palette.backgroundTop,
                        1.0f to palette.backgroundBottom,
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to glowColor,
                            0.45f to Color.Transparent,
                        )
                    )
                )
        )
    }
}

/**
 * Centered hero block for Album / Playlist / Show detail screens:
 * ~284dp artwork with elevation shadow, title, creator pill with avatar(s), and metadata line.
 */
@Composable
fun DetailHeroHeader(
    title: String,
    artworkUrl: String?,
    creatorName: String,
    metadataLine: String,
    modifier: Modifier = Modifier,
    creatorAvatarUrls: List<String?> = emptyList(),
    onCreatorClick: (() -> Unit)? = null,
    description: String? = null,
    monochrome: Boolean? = null,
    artworkModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = artworkModifier
                .size(MusiqueDimens.detailArtworkSize)
                .shadow(
                    elevation = 20.dp,
                    shape = MusiqueShapes.artworkHero,
                    ambientColor = Color.Black,
                    spotColor = Color.Black,
                )
                .clip(MusiqueShapes.artworkHero)
                .background(MusiqueColors.cardRaised),
            contentAlignment = Alignment.Center,
        ) {
            if (!artworkUrl.isNullOrBlank()) {
                SmartImage(
                    url = artworkUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    shape = MusiqueShapes.artworkHero,
                    monochrome = monochrome,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Album,
                    contentDescription = null,
                    tint = MusiqueColors.textTertiary,
                    modifier = Modifier.size(64.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MusiqueType.heroTitle.copy(fontSize = 22.sp, lineHeight = 27.sp),
            color = MusiqueColors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (creatorName.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            CreatorPill(
                name = creatorName,
                avatarUrls = creatorAvatarUrls,
                onClick = onCreatorClick,
            )
        }

        if (metadataLine.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = metadataLine,
                style = MusiqueType.rowSubtitle.copy(fontSize = 13.sp),
                color = MusiqueColors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MusiqueType.cardSubtitle,
                color = MusiqueColors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CreatorPill(
    name: String,
    avatarUrls: List<String?>,
    onClick: (() -> Unit)?,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val rowModifier = if (onClick != null) {
        Modifier
            .clip(CircleShape)
            .pressScale(interactionSource, pressedScale = 0.96f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    } else {
        Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val visibleAvatars = avatarUrls.filterNotNull().filter { it.isNotBlank() }.take(3)
        if (visibleAvatars.isNotEmpty()) {
            val stackWidth = 22.dp + (12.dp * (visibleAvatars.size - 1))
            Box(modifier = Modifier.width(stackWidth).height(22.dp)) {
                visibleAvatars.forEachIndexed { index, url ->
                    SmartImage(
                        url = url,
                        imageSize = 22.dp,
                        shape = CircleShape,
                        modifier = Modifier
                            .offset(x = (index * 12).dp)
                            .zIndex((visibleAvatars.size - index).toFloat()),
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MusiqueColors.fillOnArtwork),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MusiqueColors.textSecondary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = name,
            style = MusiqueType.heroSubtitle.copy(fontSize = 16.sp),
            color = MusiqueColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Track row styled for Cider's grouped album/playlist container:
 * leading track number or playing bars, optional 44dp thumbnail (for playlists),
 * title + explicit badge + optional subtitle, tabular duration, and `···` overflow action.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CiderDetailTrackRow(
    trackIndex: Int,
    title: String,
    durationMs: Long,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    artworkUrl: String? = null,
    showArtwork: Boolean = false,
    isExplicit: Boolean = false,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    accentColor: Color = MusiqueColors.accent,
    monochrome: Boolean? = null,
    onLongClick: (() -> Unit)? = null,
    onArtworkClick: (() -> Unit)? = null,
    trailingAccessory: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .combinedClickable(
                onClick = {
                    Haptics.confirm(context, view)
                    onClick()
                },
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isCurrent) {
                AudioBarsIndicator(
                    isPlaying = isPlaying,
                    barCount = 3,
                    barWidth = 3.dp,
                    barHeight = 14.dp,
                    spacing = 2.dp,
                    color = accentColor,
                )
            } else {
                Text(
                    text = "${trackIndex + 1}",
                    style = MusiqueType.timestamp.copy(fontSize = 14.sp),
                    color = MusiqueColors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (showArtwork) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MusiqueColors.fillOnArtwork)
                    .then(
                        if (onArtworkClick != null) Modifier.clickable(onClick = onArtworkClick)
                        else Modifier
                    ),
            ) {
                SmartImage(
                    url = artworkUrl,
                    imageSize = 44.dp,
                    shape = RoundedCornerShape(6.dp),
                    monochrome = monochrome,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MusiqueType.rowTitle.copy(
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = if (isCurrent) accentColor else MusiqueColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isExplicit) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Icon(
                        imageVector = Icons.Rounded.Explicit,
                        contentDescription = null,
                        tint = MusiqueColors.textTertiary,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MusiqueType.rowSubtitle.copy(fontSize = 13.sp),
                    color = MusiqueColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        trailingAccessory?.let {
            it()
            Spacer(modifier = Modifier.width(6.dp))
        }

        if (durationMs > 0L) {
            Text(
                text = formatTrackDuration(durationMs),
                style = MusiqueType.rowDuration,
                color = MusiqueColors.textTertiary,
            )
        }

        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = stringResource(R.string.cd_more_options),
                tint = MusiqueColors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun CiderDetailTrackSkeletonRow(
    showArtwork: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBox(
            modifier = Modifier
                .width(20.dp)
                .height(14.dp),
            shape = RoundedCornerShape(4.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        if (showArtwork) {
            SkeletonBox(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(6.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(15.dp),
                shape = RoundedCornerShape(4.dp),
            )
            if (showArtwork) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp),
                    shape = RoundedCornerShape(4.dp),
                )
            }
        }
    }
}

fun formatTrackDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

@Composable
fun formatSongCountLabel(count: Int): String = if (count == 1) {
    stringResource(R.string.detail_song_count_singular, count)
} else {
    stringResource(R.string.detail_song_count_plural, count)
}

@Composable
fun formatTotalRuntimeLabel(totalMs: Long): String {
    val totalMinutes = ((totalMs + 30_000L) / 60_000L).coerceAtLeast(1L).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        stringResource(R.string.detail_runtime_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.detail_runtime_minutes, totalMinutes)
    }
}

fun formatReleaseDateLong(date: ReleaseDate): String {
    val year = date.year ?: return ""
    return runCatching {
        when (date.precision()) {
            ReleaseDatePrecision.Day -> {
                val localDate = LocalDate.of(year, date.month!!, date.day!!)
                localDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault()))
            }
            ReleaseDatePrecision.Month -> {
                val ym = YearMonth.of(year, date.month!!)
                ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
            }
            ReleaseDatePrecision.Year -> year.toString()
        }
    }.getOrDefault(year.toString())
}
