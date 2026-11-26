package app.musique.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.musique.R
import app.musique.ScreenBottomPadding
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.pressScale

/** Measurements of the Apple style grouped inset lists used across settings. */
object SettingsDefaults {
    val rowMinHeight = 52.dp
    val rowHorizontalPadding = 16.dp
    val iconSize = 30.dp
    val iconGlyphSize = 18.dp
    val iconShape = RoundedCornerShape(8.dp)
    val iconGap = 14.dp
    val groupShape = RoundedCornerShape(14.dp)
    val compactBarHeight = 56.dp

    /** Hairline dividers start past the leading icon, like iOS inset grouped lists. */
    val dividerInset: Dp = rowHorizontalPadding + iconSize + iconGap

    /** iOS system tints for the leading icon squircles. */
    val tintRed = Color(0xFFFF453A)
    val tintOrange = Color(0xFFFF9F0A)
    val tintYellow = Color(0xFFFFD60A)
    val tintGreen = Color(0xFF30D158)
    val tintTeal = Color(0xFF40C8E0)
    val tintBlue = Color(0xFF0A84FF)
    val tintIndigo = Color(0xFF5E5CE6)
    val tintPurple = Color(0xFFBF5AF2)
    val tintPink = Color(0xFFFF375F)
    val tintGray = Color(0xFF8E8E93)
}

/** Tint for leading icon squircles inside a [SettingsSection]; null falls back to the accent. */
val LocalSettingsIconTint = compositionLocalOf<Color?> { null }

/**
 * A settings page: black canvas, a large title that scrolls away and hands over to a compact
 * centered title in a translucent bar, and the grouped sections in [content].
 */
@Composable
fun SettingsScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val collapseDistancePx = with(density) { 44.dp.toPx() }

    // 0 while the large title is fully visible, 1 once it has scrolled under the bar.
    val collapse by remember(listState) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / collapseDistancePx).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MusiqueColors.canvas)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = statusTop + SettingsDefaults.compactBarHeight,
                start = MusiqueDimens.screenPadding,
                end = MusiqueDimens.screenPadding,
                bottom = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item(key = "settings_large_title", contentType = "large_title") {
                Text(
                    text = title,
                    style = MusiqueType.largeTitle,
                    color = MusiqueColors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp)
                        .graphicsLayer { alpha = 1f - collapse * 0.9f }
                )
            }

            content()

            item(key = "settings_bottom_spacer") { Spacer(Modifier.height(ScreenBottomPadding)) }
        }

        SettingsCompactBar(
            title = title,
            collapse = collapse,
            onNavigateBack = onNavigateBack,
            actions = actions,
        )
    }
}

@Composable
private fun SettingsCompactBar(
    title: String,
    collapse: Float,
    onNavigateBack: () -> Unit,
    actions: (@Composable RowScope.() -> Unit)?,
) {
    val hairline = MusiqueColors.hairline
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(MusiqueColors.canvas.copy(alpha = 0.86f * collapse))
                drawLine(
                    color = hairline.copy(alpha = hairline.alpha * collapse),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(SettingsDefaults.compactBarHeight)
    ) {
        SettingsBackButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
        )

        Text(
            text = title,
            style = MusiqueType.barTitle,
            color = MusiqueColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 72.dp)
                .graphicsLayer {
                    val t = ((collapse - 0.55f) / 0.45f).coerceIn(0f, 1f)
                    alpha = t
                    translationY = (1f - t) * 6.dp.toPx()
                }
        )

        if (actions != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                content = actions
            )
        }
    }
}

/** Round translucent back button, the same chrome the detail pages float over their art. */
@Composable
fun SettingsBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(MusiqueDimens.roundButton)
            .pressScale(interaction, pressedScale = 0.9f)
            .clip(CircleShape)
            .background(MusiqueColors.chromeTranslucent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = stringResource(R.string.common_back),
            tint = MusiqueColors.textPrimary,
            modifier = Modifier.size(28.dp)
        )
    }
}

class SettingsSectionScope {
    internal val entries = mutableListOf<@Composable () -> Unit>()

    /** Adds one row to the group. Rows are separated by inset hairlines automatically. */
    fun entry(content: @Composable () -> Unit) {
        entries += content
    }
}

/**
 * An inset grouped list: an optional small header, rows in one rounded card separated by
 * hairlines inset past the leading icon, and an optional footer note.
 *
 * [icon] is accepted for source compatibility and no longer drawn; [iconTint] colors the
 * leading squircles of every row in the group.
 */
@Composable
fun SettingsSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    footer: String? = null,
    iconTint: Color? = null,
    content: SettingsSectionScope.() -> Unit,
) {
    val entries = SettingsSectionScope().apply(content).entries

    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            SettingsSectionHeader(title)
        }

        CompositionLocalProvider(LocalSettingsIconTint provides (iconTint ?: LocalSettingsIconTint.current)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SettingsDefaults.groupShape)
                    .background(MusiqueColors.card)
                    .animateContentSize()
            ) {
                entries.forEachIndexed { index, entry ->
                    if (index > 0) SettingsDivider()
                    entry()
                }
            }
        }

        if (footer != null) {
            SettingsSectionFooter(footer)
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MusiqueType.caption.copy(letterSpacing = MusiqueType.eyebrow.letterSpacing),
        color = MusiqueColors.textTertiary,
        modifier = modifier.padding(start = SettingsDefaults.rowHorizontalPadding, bottom = 7.dp)
    )
}

@Composable
fun SettingsSectionFooter(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MusiqueType.caption,
        color = MusiqueColors.textTertiary,
        modifier = modifier.padding(
            start = SettingsDefaults.rowHorizontalPadding,
            end = SettingsDefaults.rowHorizontalPadding,
            top = 7.dp
        )
    )
}

@Composable
fun SettingsDivider(inset: Dp = SettingsDefaults.dividerInset) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = inset)
            .height(0.5.dp)
            .background(Color.White.copy(alpha = 0.1f))
    )
}

/** Card used for one off content inside settings pages that is not a list of rows. */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SettingsDefaults.groupShape)
            .background(MusiqueColors.card)
    ) {
        content()
    }
}

/** Round translucent icon button for the trailing side of the settings bar. */
@Composable
fun SettingsRoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(MusiqueDimens.roundButton)
            .pressScale(interaction, pressedScale = 0.9f)
            .clip(CircleShape)
            .background(MusiqueColors.chromeTranslucent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MusiqueColors.textPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}
