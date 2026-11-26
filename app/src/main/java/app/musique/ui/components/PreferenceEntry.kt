package app.musique.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.musique.R
import app.musique.ui.components.bottomsheet.MusiqueModalSheet
import app.musique.ui.components.bottomsheet.SheetButtonRow
import app.musique.ui.components.bottomsheet.SheetTitle
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueType

/** The accent every settings control uses: the app's (possibly user chosen) primary. */
@Composable
fun settingsAccent(): Color = MaterialTheme.colorScheme.primary

/** Leading icon in a tinted iOS style squircle. */
@Composable
fun SettingsIconBadge(
    tint: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val glyph = if (tint.luminance() > 0.6f) Color.Black.copy(alpha = 0.86f) else Color.White
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(SettingsDefaults.iconSize)
            .clip(SettingsDefaults.iconShape)
            .background(tint)
    ) {
        CompositionLocalProvider(LocalContentColor provides glyph) {
            Box(Modifier.size(SettingsDefaults.iconGlyphSize), contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

/**
 * Base row of an inset grouped list (lives inside a [SettingsSection]): optional tinted icon
 * squircle, title with an optional muted description, and trailing value / chevron / switch.
 */
@Composable
fun PreferenceEntry(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: String? = null,
    content: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    isEnabled: Boolean = true,
    iconTint: Color? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SettingsDefaults.rowMinHeight)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = isEnabled,
                        onClick = onClick
                    )
                } else Modifier
            )
            .background(if (pressed) Color.White.copy(alpha = 0.07f) else Color.Transparent)
            .alpha(if (isEnabled) 1f else 0.45f)
            .padding(horizontal = SettingsDefaults.rowHorizontalPadding, vertical = 10.dp)
    ) {
        if (icon != null) {
            SettingsIconBadge(tint = iconTint ?: LocalSettingsIconTint.current ?: settingsAccent()) {
                icon()
            }
            Spacer(Modifier.width(SettingsDefaults.iconGap))
        }

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            ProvideTextStyle(MusiqueType.rowTitle) {
                CompositionLocalProvider(LocalContentColor provides MusiqueColors.textPrimary) {
                    title()
                }
            }

            if (description != null) {
                Text(
                    text = description,
                    style = MusiqueType.cardSubtitle,
                    color = MusiqueColors.textTertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            content?.invoke()
        }

        if (trailingContent != null) {
            Spacer(Modifier.width(10.dp))
            CompositionLocalProvider(LocalContentColor provides MusiqueColors.textTertiary) {
                trailingContent()
            }
        }
    }
}

/** Muted chevron at the end of a row that opens another page. */
@Composable
fun SettingsChevron(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = MusiqueColors.textTertiary.copy(alpha = 0.45f),
        modifier = modifier.size(22.dp)
    )
}

/** Muted trailing value text, like "High" or "Off" before a chevron. */
@Composable
fun SettingsValueText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MusiqueType.body,
        color = MusiqueColors.textTertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.End,
        modifier = modifier.widthIn(max = 160.dp)
    )
}

@Composable
fun NavigationPreferenceEntry(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
    value: String? = null,
    iconTint: Color? = null,
) {
    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = description,
        icon = icon,
        onClick = onClick,
        isEnabled = isEnabled,
        iconTint = iconTint,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    SettingsValueText(value)
                    Spacer(Modifier.width(2.dp))
                }
                SettingsChevron()
            }
        }
    )
}

/** Switch in the settings language: accent track when on, iOS gray when off, full size thumb. */
@Composable
fun SettingsSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val accent = settingsAccent()
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        // A thumb content slot keeps the thumb full size in both states, like iOS.
        thumbContent = { Box(Modifier.size(1.dp)) },
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = accent,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFF39393D),
            uncheckedBorderColor = Color.Transparent,
            disabledCheckedThumbColor = Color.White.copy(alpha = 0.8f),
            disabledCheckedTrackColor = accent.copy(alpha = 0.4f),
            disabledCheckedBorderColor = Color.Transparent,
            disabledUncheckedThumbColor = Color.White.copy(alpha = 0.6f),
            disabledUncheckedTrackColor = Color(0xFF39393D).copy(alpha = 0.6f),
            disabledUncheckedBorderColor = Color.Transparent,
        )
    )
}

@Composable
fun SwitchPreferenceEntry(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: String? = null,
    content: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
    isEnabled: Boolean = true,
    isChecked: Boolean = false,
    iconTint: Color? = null,
) {
    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = description,
        content = content,
        icon = icon,
        iconTint = iconTint,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                trailingContent?.invoke()
                SettingsSwitch(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    enabled = isEnabled,
                )
            }
        },
        onClick = { onCheckedChange(!isChecked) },
        isEnabled = isEnabled
    )
}

data class DropdownOption<T>(
    val value: T,
    val label: String,
)

/** Row with the current choice as trailing value text; tapping opens an Apple style menu. */
@Composable
fun <T> DropdownPreferenceEntry(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
    options: List<DropdownOption<T>>,
    selectedValue: T,
    onValueChange: (T) -> Unit,
    isEnabled: Boolean = true,
    iconTint: Color? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == selectedValue }?.label.orEmpty()

    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = description,
        icon = icon,
        iconTint = iconTint,
        isEnabled = isEnabled,
        onClick = { expanded = true },
        trailingContent = {
            Box {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SettingsValueText(selectedLabel)
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Rounded.UnfoldMore,
                        contentDescription = null,
                        tint = MusiqueColors.textTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                SettingsMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEach { option ->
                        SettingsMenuItem(
                            text = option.label,
                            selected = option.value == selectedValue,
                            onClick = {
                                onValueChange(option.value)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    )
}

/** Dark rounded popup menu used by settings pickers. */
@Composable
fun SettingsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(0.dp, 6.dp),
        shape = RoundedCornerShape(14.dp),
        containerColor = MusiqueColors.cardRaised,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        modifier = Modifier.widthIn(min = 200.dp)
    ) {
        content()
    }
}

@Composable
fun SettingsMenuItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MusiqueType.body,
                color = MusiqueColors.textPrimary
            )
        },
        trailingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = settingsAccent(),
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        onClick = onClick,
    )
}

/** Row with an inline right aligned text field, like iOS "Name    Musique" rows. */
@Composable
fun TextInputPreferenceEntry(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: String? = null,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
    icon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    isEnabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onClick: (() -> Unit)? = null,
    iconTint: Color? = null,
) {
    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = description,
        icon = icon,
        iconTint = iconTint,
        isEnabled = isEnabled,
        onClick = onClick,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val fieldStyle = MusiqueType.body.copy(
                    color = MusiqueColors.textPrimary,
                    textAlign = TextAlign.End
                )
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = isEnabled,
                    singleLine = singleLine,
                    keyboardOptions = keyboardOptions,
                    textStyle = fieldStyle,
                    cursorBrush = SolidColor(settingsAccent()),
                    modifier = Modifier
                        .widthIn(min = 72.dp, max = 160.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (value.isEmpty() && placeholder != null) {
                                Text(
                                    text = placeholder,
                                    style = fieldStyle.copy(color = MusiqueColors.textTertiary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            inner()
                        }
                    }
                )

                if (trailingContent != null) {
                    Spacer(Modifier.width(10.dp))
                    trailingContent()
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPreferenceEntry(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: String? = null,
    value: Color,
    onValueChange: (Color) -> Unit,
    icon: (@Composable () -> Unit)? = null,
    isEnabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    iconTint: Color? = null,
) {
    var showPicker by remember { mutableStateOf(false) }
    var draftColor by remember(value) { mutableStateOf(value) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun openPicker() {
        draftColor = value
        showPicker = true
    }

    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = description,
        icon = icon,
        iconTint = iconTint,
        isEnabled = isEnabled,
        onClick = { onClick?.invoke() ?: openPicker() },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(value)
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                )
                Spacer(Modifier.width(4.dp))
                SettingsChevron()
            }
        }
    )

    if (showPicker) {
        MusiqueModalSheet(
            onDismissRequest = { showPicker = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SheetTitle(stringResource(R.string.common_choose_color))

                ColorPicker(
                    initial = draftColor,
                    onColorChanged = { draftColor = it }
                )

                SheetButtonRow(
                    primaryText = stringResource(R.string.common_done),
                    onPrimary = {
                        onValueChange(draftColor)
                        showPicker = false
                    },
                    secondaryText = stringResource(R.string.common_cancel),
                    onSecondary = { showPicker = false },
                )
            }
        }
    }
}

/**
 * A row that performs an action, drawn like an iOS button cell: the label in the accent
 * (or red when [destructive]) with an optional glyph on the right.
 */
@Composable
fun SettingsActionRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    destructive: Boolean = false,
    enabled: Boolean = true,
) {
    val color = if (destructive) SettingsDefaults.tintRed else settingsAccent()
    PreferenceEntry(
        modifier = modifier,
        title = { Text(text, color = color) },
        onClick = onClick,
        isEnabled = enabled,
        trailingContent = icon?.let {
            {
                Icon(it, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
        }
    )
}
