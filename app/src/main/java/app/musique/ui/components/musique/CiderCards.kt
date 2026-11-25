package app.musique.ui.components.musique

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.musique.R
import app.musique.ui.components.SmartImage
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueShapes
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.musiqueTypography
import app.musique.ui.theme.pressScale

/**
 * 3:4 portrait overlay card matching Cider Android's "Top Picks for You" shelf:
 * 18dp corner radius, full-bleed cover, bottom scrim, eyebrow, bold title, and subtitle.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PortraitCard(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    onPlayClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    width: Dp = MusiqueDimens.portraitCardWidth,
    height: Dp = MusiqueDimens.portraitCardHeight,
    monochrome: Boolean? = null,
    fallbackContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .width(width)
            .height(height)
            .pressScale(interactionSource, pressedScale = 0.97f)
            .clip(MusiqueShapes.cardLarge)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = MusiqueShapes.cardLarge,
        color = MusiqueColors.cardRaised,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CardArtwork(
                imageUrl = imageUrl,
                shape = MusiqueShapes.cardLarge,
                monochrome = monochrome,
                fallbackContent = fallbackContent,
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.36f to Color.Transparent,
                                0.68f to Color.Black.copy(alpha = 0.58f),
                                1.0f to Color.Black.copy(alpha = 0.90f),
                            )
                        )
                    )
            )

            if (onPlayClick != null) {
                CardPlayBadge(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                if (!eyebrow.isNullOrBlank()) {
                    Text(
                        text = eyebrow,
                        style = MusiqueType.caption,
                        color = MusiqueColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = title,
                    style = MusiqueType.cardTitle.copy(
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MusiqueColors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MusiqueType.cardSubtitle,
                        color = MusiqueColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Standard square (or circular for artists) media card with title and subtitle underneath.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SquareCard(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onPlayClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    size: Dp? = MusiqueDimens.squareCardSize,
    isCircular: Boolean = false,
    isExplicit: Boolean = false,
    monochrome: Boolean? = null,
    artworkModifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape: Shape = if (isCircular) CircleShape else MusiqueShapes.tile
    val rootModifier = (if (size != null) modifier.width(size) else modifier)
        .pressScale(interactionSource, pressedScale = 0.97f)
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick,
        )

    Column(modifier = rootModifier) {
        Box(
            modifier = artworkModifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(MusiqueColors.cardRaised),
        ) {
            CardArtwork(
                imageUrl = imageUrl,
                shape = shape,
                monochrome = monochrome,
            )

            if (onPlayClick != null) {
                CardPlayBadge(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isCircular) Arrangement.Center else Arrangement.Start,
        ) {
            Text(
                text = title,
                style = MusiqueType.cardTitle,
                color = MusiqueColors.textPrimary,
                textAlign = if (isCircular) TextAlign.Center else TextAlign.Start,
                maxLines = if (subtitle.isNullOrBlank()) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = !isCircular),
            )
            if (isExplicit && !isCircular) {
                Spacer(modifier = Modifier.width(4.dp))
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
                style = MusiqueType.cardSubtitle,
                color = MusiqueColors.textSecondary,
                textAlign = if (isCircular) TextAlign.Center else TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Wide or featured overlay card used for Cider's "Recently Played" shelf (184x168) or wider
 * hero banners with full-bleed artwork, bottom scrim, optional eyebrow, and title.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCard(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    onPlayClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    width: Dp = MusiqueDimens.heroCardWidth,
    height: Dp = MusiqueDimens.heroCardHeight,
    monochrome: Boolean? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .width(width)
            .height(height)
            .pressScale(interactionSource, pressedScale = 0.97f)
            .clip(MusiqueShapes.card)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = MusiqueShapes.card,
        color = MusiqueColors.cardRaised,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CardArtwork(
                imageUrl = imageUrl,
                shape = MusiqueShapes.card,
                monochrome = monochrome,
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.40f to Color.Transparent,
                                0.76f to Color.Black.copy(alpha = 0.56f),
                                1.0f to Color.Black.copy(alpha = 0.88f),
                            )
                        )
                    )
            )

            if (onPlayClick != null) {
                CardPlayBadge(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (!eyebrow.isNullOrBlank()) {
                    Text(
                        text = eyebrow.uppercase(),
                        style = MusiqueType.eyebrow,
                        color = MusiqueColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = title,
                    style = MusiqueType.cardTitle,
                    color = MusiqueColors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MusiqueType.cardSubtitle,
                        color = MusiqueColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * 2-column browse category tile matching Apple Music / Cider Search browse tiles:
 * 16:10 landscape card (~102dp tall), 12dp corner radius, saturated gradient or artwork fill,
 * subtle bottom scrim, and bold white label pinned bottom-left.
 */
@Composable
fun CategoryTile(
    title: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    icon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val resolvedColors = if (gradientColors.size >= 2) gradientColors
    else listOf(MusiqueColors.cardRaised, MusiqueColors.chrome)

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 102.dp)
            .aspectRatio(1.6f)
            .pressScale(interactionSource, pressedScale = 0.97f),
        shape = MusiqueShapes.tile,
        color = resolvedColors.first(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(resolvedColors))
        ) {
            if (!imageUrl.isNullOrBlank()) {
                SmartImage(
                    url = imageUrl,
                    shape = MusiqueShapes.tile,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = title,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.25f to Color.Black.copy(alpha = 0.10f),
                                    1.0f to Color.Black.copy(alpha = 0.72f),
                                )
                            )
                        )
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(40.dp),
                )
            }

            Text(
                text = title,
                style = MusiqueType.cardTitle.copy(fontWeight = FontWeight.Bold),
                color = MusiqueColors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun CardArtwork(
    imageUrl: String?,
    shape: Shape,
    monochrome: Boolean?,
    fallbackContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    if (!imageUrl.isNullOrBlank()) {
        SmartImage(
            url = imageUrl,
            modifier = Modifier.fillMaxSize(),
            shape = shape,
            monochrome = monochrome,
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MusiqueColors.cardRaised, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (fallbackContent != null) {
                fallbackContent()
            } else {
                Icon(
                    imageVector = Icons.Rounded.Album,
                    contentDescription = null,
                    tint = MusiqueColors.textTertiary,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}

@Composable
private fun CardPlayBadge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .size(34.dp)
            .pressScale(interactionSource, pressedScale = 0.88f)
            .border(MusiqueDimens.hairlineWidth, MusiqueColors.glassEdge, CircleShape),
        shape = CircleShape,
        color = MusiqueColors.chromeTranslucent,
        contentColor = MusiqueColors.textPrimary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.cd_play_item),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CiderCardsPreview() {
    MaterialTheme(typography = musiqueTypography()) {
        Row(
            modifier = Modifier
                .background(MusiqueColors.canvas)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PortraitCard(
                title = "Discovery Station",
                eyebrow = "Made for You",
                subtitle = "Updated daily",
                imageUrl = null,
                onClick = {},
                onPlayClick = {},
            )
            SquareCard(
                title = "Restless Bones",
                subtitle = "The Band",
                imageUrl = null,
                onClick = {},
            )
        }
    }
}
