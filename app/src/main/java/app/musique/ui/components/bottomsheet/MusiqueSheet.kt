package app.musique.ui.components.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.pressScale

/** The one look every Musique bottom sheet shares. */
object MusiqueSheetDefaults {
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val containerColor: Color = MusiqueColors.cardRaised
    val scrimColor: Color = Color.Black.copy(alpha = 0.52f)
    val horizontalPadding = 16.dp

    /** Grouped action cards inside a sheet sit one step above the sheet surface. */
    val groupColor = Color(0xFF2A2A2D)
    val groupShape = RoundedCornerShape(14.dp)
    val actionRowHeight = 52.dp
    val destructive = Color(0xFFFF453A)

    @Composable
    fun DragHandle(modifier: Modifier = Modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 10.dp)
        ) {
            Box(
                Modifier
                    .size(MusiqueDimens.grabberWidth, 5.dp)
                    .clip(CircleShape)
                    .background(MusiqueColors.grabber)
            )
        }
    }
}

/**
 * Dark Material scheme used inside sheets so legacy Material content (text fields, chips,
 * list items) lands on the Cider surfaces instead of tonal Material You containers.
 */
@Composable
private fun sheetColorScheme() = MaterialTheme.colorScheme.copy(
    surface = MusiqueSheetDefaults.containerColor,
    onSurface = MusiqueColors.textPrimary,
    onSurfaceVariant = MusiqueColors.textTertiary,
    surfaceVariant = MusiqueSheetDefaults.groupColor,
    surfaceContainerLowest = MusiqueColors.card,
    surfaceContainerLow = MusiqueSheetDefaults.containerColor,
    surfaceContainer = MusiqueSheetDefaults.groupColor,
    surfaceContainerHigh = MusiqueSheetDefaults.groupColor,
    surfaceContainerHighest = Color(0xFF333336),
    surfaceBright = Color(0xFF333336),
    outline = Color.White.copy(alpha = 0.18f),
    outlineVariant = Color.White.copy(alpha = 0.1f),
)

/**
 * [ModalBottomSheet] in the Cider style: raised dark surface, 24dp top corners, translucent
 * scrim and a slim grabber.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusiqueModalSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    showGrabber: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = MusiqueSheetDefaults.shape,
        containerColor = MusiqueSheetDefaults.containerColor,
        contentColor = MusiqueColors.textPrimary,
        tonalElevation = 0.dp,
        scrimColor = MusiqueSheetDefaults.scrimColor,
        dragHandle = if (showGrabber) {
            { MusiqueSheetDefaults.DragHandle() }
        } else null,
    ) {
        MaterialTheme(
            colorScheme = sheetColorScheme(),
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
        ) {
            content()
        }
    }
}

/** Sheet title in the section title style, with an optional muted subtitle. */
@Composable
fun SheetTitle(
    text: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    textAlign: TextAlign = TextAlign.Start,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = MusiqueType.sectionTitle,
            color = MusiqueColors.textPrimary,
            textAlign = textAlign,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MusiqueType.rowSubtitle,
                color = MusiqueColors.textTertiary,
                textAlign = textAlign,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

/** Small muted label above a group of rows inside a sheet. */
@Composable
fun SheetSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MusiqueType.caption.copy(letterSpacing = MusiqueType.eyebrow.letterSpacing),
        color = MusiqueColors.textTertiary,
        modifier = modifier.padding(start = 16.dp, bottom = 7.dp, top = 4.dp)
    )
}

/** A rounded card of rows separated by hairlines, like an Apple context menu. */
@Composable
fun SheetActionGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MusiqueSheetDefaults.groupShape)
            .background(MusiqueSheetDefaults.groupColor),
        content = content
    )
}

@Composable
fun SheetDivider(modifier: Modifier = Modifier, inset: Dp = 16.dp) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = inset)
            .height(0.5.dp)
            .background(Color.White.copy(alpha = 0.1f))
    )
}

/**
 * Apple context menu row: label on the left, glyph on the right. [destructive] rows are red.
 * Put rows in a [SheetActionGroup] and separate them with [SheetDivider].
 */
@Composable
fun SheetActionRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    supportingText: String? = null,
    destructive: Boolean = false,
    enabled: Boolean = true,
    tint: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val color = when {
        destructive -> MusiqueSheetDefaults.destructive
        tint != null -> tint
        else -> MusiqueColors.textPrimary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MusiqueSheetDefaults.actionRowHeight)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .background(if (pressed) Color.White.copy(alpha = 0.08f) else Color.Transparent)
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MusiqueType.rowTitle.copy(fontWeight = MusiqueType.body.fontWeight),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MusiqueType.cardSubtitle,
                    color = MusiqueColors.textTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            CompositionLocalProvider(LocalContentColor provides color) { trailing() }
        } else if (icon != null) {
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Large white pill, the primary action of Cider screens and sheets. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    containerColor: Color = MusiqueColors.pillSelected,
    contentColor: Color = MusiqueColors.onPillSelected,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .pressScale(interaction, pressedScale = 0.96f)
            .clip(CircleShape)
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.35f))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !loading,
                onClick = onClick
            )
            .padding(horizontal = 24.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text = text, style = MusiqueType.button.copy(fontSize = MusiqueType.rowTitle.fontSize), color = contentColor)
        }
    }
}

/** Translucent secondary pill, for the second choice next to a [PillButton]. */
@Composable
fun TonalPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    PillButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        containerColor = Color.White.copy(alpha = 0.12f),
        contentColor = MusiqueColors.textPrimary,
    )
}

/** Plain text button in the accent, for "Not now" style secondary actions. */
@Composable
fun SheetTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MusiqueColors.textSecondary,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 44.dp)
            .pressScale(interaction, pressedScale = 0.96f, pressedAlpha = 0.6f)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = text,
            style = MusiqueType.button,
            color = if (enabled) color else color.copy(alpha = 0.4f),
        )
    }
}

/** Primary white pill with an optional text button under it. */
@Composable
fun SheetButtonRow(
    primaryText: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    primaryLoading: Boolean = false,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        PillButton(
            text = primaryText,
            onClick = onPrimary,
            enabled = primaryEnabled,
            loading = primaryLoading,
        )
        if (secondaryText != null && onSecondary != null) {
            SheetTextButton(text = secondaryText, onClick = onSecondary)
        }
    }
}

/** Big round glyph at the top of an explanatory sheet (permissions, auth result). */
@Composable
fun SheetHeroIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(72.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(tint.copy(alpha = 0.16f))
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(36.dp))
    }
}

/**
 * Header of a context sheet: artwork, title and subtitle, with optional trailing buttons
 * (like a heart). [subtitle] is a slot so callers can make artist names tappable.
 */
@Composable
fun SheetMediaHeader(
    artworkUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    artworkShape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp),
    monochrome: Boolean? = null,
    onArtworkClick: (() -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(artworkShape)
                .background(MusiqueSheetDefaults.groupColor)
                .then(if (onArtworkClick != null) Modifier.clickable(onClick = onArtworkClick) else Modifier)
        ) {
            app.musique.ui.components.SmartImage(
                url = artworkUrl,
                shape = artworkShape,
                monochrome = monochrome,
                modifier = Modifier.matchParentSize()
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MusiqueType.cardTitle.copy(fontSize = MusiqueType.rowTitle.fontSize),
                color = MusiqueColors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                CompositionLocalProvider(LocalContentColor provides MusiqueColors.textTertiary) {
                    subtitle()
                }
            }
        }

        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) { trailing() }
        }
    }
}

/** Round 36dp translucent button used in sheet headers (like, share). */
@Composable
fun SheetRoundButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MusiqueColors.textPrimary,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(36.dp)
            .pressScale(interaction, pressedScale = 0.88f)
            .clip(CircleShape)
            .background(MusiqueSheetDefaults.groupColor)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** One of the square tiles in the quick action strip at the top of iOS context menus. */
@Composable
fun SheetQuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        modifier = modifier
            .height(72.dp)
            .pressScale(interaction, pressedScale = 0.96f)
            .clip(MusiqueSheetDefaults.groupShape)
            .background(
                if (pressed) MusiqueSheetDefaults.groupColor.copy(alpha = 0.7f)
                else MusiqueSheetDefaults.groupColor
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MusiqueColors.textPrimary, modifier = Modifier.size(22.dp))
        Text(
            text = label,
            style = MusiqueType.caption,
            color = MusiqueColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/** Standard padding wrapper for sheet content. */
@Composable
fun SheetContent(
    modifier: Modifier = Modifier,
    spacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MusiqueSheetDefaults.horizontalPadding)
            .padding(bottom = 20.dp),
        content = content
    )
}

/**
 * Explanatory sheet body (permissions, sign in results): hero glyph, title, copy, a white
 * pill and an optional text button.
 */
@Composable
fun SheetExplainer(
    icon: ImageVector,
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    primaryText: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        SheetHeroIcon(icon = icon, tint = iconTint)
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MusiqueType.heroTitle,
            color = MusiqueColors.textPrimary,
            textAlign = TextAlign.Center
        )
        if (!body.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                style = MusiqueType.body,
                color = MusiqueColors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
        if (extra != null) {
            Spacer(Modifier.height(16.dp))
            extra()
        }
        if (primaryText != null && onPrimary != null) {
            Spacer(Modifier.height(28.dp))
            SheetButtonRow(
                primaryText = primaryText,
                onPrimary = onPrimary,
                secondaryText = secondaryText,
                onSecondary = onSecondary,
            )
        }
    }
}

/** Borderless form field for use inside a [SheetActionGroup], like an iOS form cell. */
@Composable
fun SheetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 4,
    enabled: Boolean = true,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions =
        androidx.compose.foundation.text.KeyboardOptions.Default,
) {
    val style = MusiqueType.rowTitle.copy(
        color = MusiqueColors.textPrimary,
        fontWeight = MusiqueType.body.fontWeight
    )
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        textStyle = style,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MusiqueSheetDefaults.actionRowHeight)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(placeholder, style = style.copy(color = MusiqueColors.textTertiary))
                }
                inner()
            }
        }
    )
}

/** Row with a label, optional supporting text and an accent switch, inside a sheet group. */
@Composable
fun SheetSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MusiqueSheetDefaults.actionRowHeight)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .alpha(if (enabled) 1f else 0.45f)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MusiqueType.rowTitle, color = MusiqueColors.textPrimary)
            if (supportingText != null) {
                Text(supportingText, style = MusiqueType.cardSubtitle, color = MusiqueColors.textTertiary)
            }
        }
        Spacer(Modifier.width(12.dp))
        app.musique.ui.components.SettingsSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}
