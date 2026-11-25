package app.musique.ui.components.musique

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import app.musique.R
import app.musique.ui.components.ScreenshotAvatar
import app.musique.ui.components.SmartImage
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueMotion
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.musiqueTypography
import app.musique.ui.theme.pressScale

/**
 * Computes a smooth 0f..1f collapse fraction for [listState] over [thresholdDp].
 */
@Composable
fun rememberCollapseFraction(
    listState: LazyListState,
    thresholdDp: Dp = 44.dp,
): Float {
    val thresholdPx = with(LocalDensity.current) { thresholdDp.toPx() }.coerceAtLeast(1f)
    val raw by remember(listState, thresholdPx) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / thresholdPx).coerceIn(0f, 1f)
        }
    }
    return raw
}

/**
 * Computes a smooth 0f..1f collapse fraction for [gridState] over [thresholdDp].
 */
@Composable
fun rememberCollapseFraction(
    gridState: LazyGridState,
    thresholdDp: Dp = 44.dp,
): Float {
    val thresholdPx = with(LocalDensity.current) { thresholdDp.toPx() }.coerceAtLeast(1f)
    val raw by remember(gridState, thresholdPx) {
        derivedStateOf {
            if (gridState.firstVisibleItemIndex > 0) 1f
            else (gridState.firstVisibleItemScrollOffset / thresholdPx).coerceIn(0f, 1f)
        }
    }
    return raw
}

/**
 * Cider / Apple Music top bar: 32sp [MusiqueType.largeTitle] left-aligned at rest that smoothly
 * collapses as [collapseFraction] advances into a 56dp translucent sticky bar with a hairline,
 * compact 18sp centered title, and pinned trailing actions.
 */
@Composable
fun LargeTitleTopBar(
    title: String,
    collapseFraction: Float,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = stringResource(R.string.common_back),
    barColor: Color = MusiqueColors.barTranslucent,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val progress = collapseFraction.coerceIn(0f, 1f)
    val barHeight = lerp(MusiqueDimens.topBarHeight, MusiqueDimens.collapsedBarHeight, progress)
    val largeAlpha = (1f - progress * 1.8f).coerceIn(0f, 1f)
    val compactAlpha = ((progress - 0.35f) / 0.55f).coerceIn(0f, 1f)
    val bgAlpha = (progress * 1.1f).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(barColor.copy(alpha = barColor.alpha * bgAlpha))
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .padding(horizontal = MusiqueDimens.screenPadding)
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(MusiqueDimens.minTouchTarget),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = backContentDescription,
                        tint = MusiqueColors.textPrimary,
                    )
                }
            }

            // Large left-aligned title when unscrolled
            Text(
                text = title,
                style = MusiqueType.largeTitle,
                color = MusiqueColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(
                        start = if (onBack != null) 44.dp else 0.dp,
                        end = 104.dp,
                    )
                    .graphicsLayer {
                        alpha = largeAlpha
                        translationY = -progress * 12f
                    },
            )

            // Compact centered title when scrolled
            Text(
                text = title,
                style = MusiqueType.barTitle,
                color = MusiqueColors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 96.dp)
                    .graphicsLayer {
                        alpha = compactAlpha
                    },
            )

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = trailingContent,
            )
        }

        if (progress > 0.02f) {
            HorizontalDivider(
                thickness = MusiqueDimens.hairlineWidth,
                color = MusiqueColors.hairline.copy(alpha = MusiqueColors.hairline.alpha * progress),
            )
        }
    }
}

/**
 * Standard top-right actions for primary Cider browse tabs: a 24dp Devices icon inside a
 * 44dp touch target and a 36dp circular profile avatar that opens Settings.
 */
@Composable
fun TopBarAccountActions(
    avatarUrl: String?,
    screenshotMode: Boolean,
    onDevicesClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val devicesInteraction = remember { MutableInteractionSource() }
        IconButton(
            onClick = onDevicesClick,
            interactionSource = devicesInteraction,
            modifier = Modifier
                .size(MusiqueDimens.minTouchTarget)
                .pressScale(devicesInteraction, pressedScale = 0.90f),
        ) {
            Icon(
                imageVector = Icons.Outlined.Devices,
                contentDescription = stringResource(R.string.cd_devices),
                tint = MusiqueColors.textPrimary,
                modifier = Modifier.size(24.dp),
            )
        }

        val profileInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .defaultMinSize(MusiqueDimens.minTouchTarget, MusiqueDimens.minTouchTarget)
                .pressScale(profileInteraction, pressedScale = 0.92f)
                .clickable(
                    interactionSource = profileInteraction,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.common_settings),
                    onClick = onProfileClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                screenshotMode -> ScreenshotAvatar(Modifier.size(36.dp))
                !avatarUrl.isNullOrBlank() -> SmartImage(
                    url = avatarUrl,
                    imageSize = 36.dp,
                    shape = CircleShape,
                    contentDescription = stringResource(R.string.common_account),
                )
                else -> Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MusiqueColors.avatarPlaceholder),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = stringResource(R.string.common_account),
                        tint = MusiqueColors.avatarPlaceholderIcon,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * 40dp translucent circular button used over artwork on detail screens (Back, Share, More).
 * Maintains a 44dp minimum hit area with press-scale feedback.
 */
@Composable
fun FloatingRoundButton(
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = MusiqueDimens.roundButton,
    containerColor: Color = MusiqueColors.chromeTranslucent,
    contentColor: Color = MusiqueColors.textPrimary,
    icon: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .defaultMinSize(MusiqueDimens.minTouchTarget, MusiqueDimens.minTouchTarget)
            .pressScale(interactionSource, pressedScale = 0.90f, pressedAlpha = 0.8f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .size(size)
                .border(MusiqueDimens.hairlineWidth, MusiqueColors.glassEdge, CircleShape),
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    icon()
                }
            }
        }
    }
}

/**
 * Overlay top bar for artwork-backed detail pages (Album, Playlist, Artist, Show, Track, Profile).
 * At rest, only floating round buttons appear over the background; once scrolled past the hero
 * block, a translucent bar tinted by [tintColor] with a compact title fades in.
 */
@Composable
fun DetailFloatingTopBar(
    title: String,
    collapseFraction: Float,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    tintColor: Color = MusiqueColors.canvas,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val progress by animateFloatAsState(
        targetValue = collapseFraction.coerceIn(0f, 1f),
        animationSpec = MusiqueMotion.layout(),
        label = "detailTopBar",
    )
    val titleAlpha = ((progress - 0.35f) / 0.65f).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(tintColor.copy(alpha = 0.92f * progress))
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MusiqueDimens.collapsedBarHeight)
                .padding(horizontal = MusiqueDimens.screenPadding),
        ) {
            FloatingRoundButton(
                onClick = onBack,
                contentDescription = stringResource(R.string.common_back),
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }

            if (titleAlpha > 0.01f) {
                Text(
                    text = title,
                    style = MusiqueType.barTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = MusiqueColors.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 72.dp)
                        .graphicsLayer { alpha = titleAlpha },
                )
            }

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = trailingContent,
            )
        }

        if (progress > 0.05f) {
            HorizontalDivider(
                thickness = MusiqueDimens.hairlineWidth,
                color = MusiqueColors.hairline.copy(alpha = MusiqueColors.hairline.alpha * progress),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun LargeTitleTopBarPreview() {
    MaterialTheme(typography = musiqueTypography()) {
        Column(modifier = Modifier.background(MusiqueColors.canvas)) {
            LargeTitleTopBar(
                title = "Home",
                collapseFraction = 0f,
                trailingContent = {
                    TopBarAccountActions(
                        avatarUrl = null,
                        screenshotMode = false,
                        onDevicesClick = {},
                        onProfileClick = {},
                    )
                },
            )
            Spacer(Modifier.height(12.dp))
            LargeTitleTopBar(
                title = "Home",
                collapseFraction = 1f,
                trailingContent = {
                    TopBarAccountActions(
                        avatarUrl = null,
                        screenshotMode = false,
                        onDevicesClick = {},
                        onProfileClick = {},
                    )
                },
            )
        }
    }
}
