package app.musique.ui.components.musique

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.musique.R
import app.musique.ui.Haptics
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueShapes
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.musiqueTypography
import app.musique.ui.theme.pressScale

/**
 * Cider / Apple Music action row for detail screens.
 * When [onSaveClick] is supplied, renders the 3-button M3E layout from spec §7:
 * [Shuffle (0.24f)] [Play (0.52f)] [Add/Saved (0.24f)].
 * When [onSaveClick] is null, renders a balanced 2-button [Play] [Shuffle] row.
 */
@Composable
fun PlayShuffleRow(
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSaveClick: (() -> Unit)? = null,
    isSaved: Boolean = false,
    saveIcon: ImageVector = if (isSaved) Icons.Rounded.Check else Icons.Rounded.Add,
    saveContentDescription: String = stringResource(
        if (isSaved) R.string.detail_saved else R.string.common_add
    ),
    playLabel: String = stringResource(R.string.detail_play),
    shuffleLabel: String = stringResource(R.string.detail_shuffle),
    primaryColor: Color = Color.White,
    onPrimaryColor: Color = MusiqueColors.onDarkPlay,
    secondaryColor: Color = MusiqueColors.fillSecondary,
    onSecondaryColor: Color = MusiqueColors.textPrimary,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val view = LocalView.current

    if (onSaveClick != null) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionSquircleButton(
                onClick = {
                    Haptics.toggle(context, view)
                    onShuffleClick()
                },
                containerColor = secondaryColor,
                contentColor = onSecondaryColor,
                enabled = enabled,
                modifier = Modifier
                    .weight(0.24f)
                    .height(MusiqueDimens.actionButtonHeight),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = shuffleLabel,
                    modifier = Modifier.size(22.dp),
                )
            }

            ActionSquircleButton(
                onClick = {
                    Haptics.confirm(context, view)
                    onPlayClick()
                },
                containerColor = primaryColor,
                contentColor = onPrimaryColor,
                enabled = enabled,
                modifier = Modifier
                    .weight(0.52f)
                    .height(MusiqueDimens.actionButtonHeight),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = playLabel,
                        style = MusiqueType.button,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            ActionSquircleButton(
                onClick = {
                    Haptics.confirm(context, view)
                    onSaveClick()
                },
                containerColor = secondaryColor,
                contentColor = onSecondaryColor,
                enabled = enabled,
                modifier = Modifier
                    .weight(0.24f)
                    .height(MusiqueDimens.actionButtonHeight),
            ) {
                Icon(
                    imageVector = saveIcon,
                    contentDescription = saveContentDescription,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionSquircleButton(
                onClick = {
                    Haptics.confirm(context, view)
                    onPlayClick()
                },
                containerColor = primaryColor,
                contentColor = onPrimaryColor,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(MusiqueDimens.actionButtonHeight),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = playLabel,
                        style = MusiqueType.button,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            ActionSquircleButton(
                onClick = {
                    Haptics.toggle(context, view)
                    onShuffleClick()
                },
                containerColor = secondaryColor,
                contentColor = onSecondaryColor,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(MusiqueDimens.actionButtonHeight),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = shuffleLabel,
                        style = MusiqueType.button,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Tonal squircle button with Apple-style spring press scale.
 */
@Composable
fun ActionSquircleButton(
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape = MusiqueShapes.actionButton,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier.pressScale(interactionSource, pressedScale = 0.94f, pressedAlpha = 0.85f),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF542205)
@Composable
private fun PlayShuffleRowPreview() {
    MaterialTheme(typography = musiqueTypography()) {
        PlayShuffleRow(
            onPlayClick = {},
            onShuffleClick = {},
            onSaveClick = {},
            isSaved = false,
            secondaryColor = Color(0xFF6E3A1C),
            modifier = Modifier.padding(24.dp),
        )
    }
}
