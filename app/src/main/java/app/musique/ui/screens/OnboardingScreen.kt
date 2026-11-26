package app.musique.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musique.R
import app.musique.ui.components.bottomsheet.PillButton
import app.musique.ui.components.bottomsheet.SheetTextButton
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueType
import app.musique.ui.viewmodel.OnboardingViewModel

/**
 * Apple style welcome: a drifting collage of artwork tiles over the black canvas, a big
 * title, short copy, a white pill to connect playback and a text button for the details.
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isPlaybackLoggedIn by viewModel.isPlaybackLoggedIn.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MusiqueColors.canvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ArtworkCollage(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            )

            WelcomeContent(
                isLoggedIn = isPlaybackLoggedIn,
                onConnect = { viewModel.startPlaybackAuth(context) },
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

private data class CollageTile(val colors: List<Color>, val icon: ImageVector)

private val collageRows: List<List<CollageTile>> = listOf(
    listOf(
        CollageTile(listOf(Color(0xFFFA2F48), Color(0xFF8E1230)), Icons.Rounded.MusicNote),
        CollageTile(listOf(Color(0xFFFF9F0A), Color(0xFFB34700)), Icons.Rounded.Radio),
        CollageTile(listOf(Color(0xFF5E5CE6), Color(0xFF26246B)), Icons.Rounded.Headphones),
        CollageTile(listOf(Color(0xFF30D158), Color(0xFF0B5E2A)), Icons.Rounded.Album),
        CollageTile(listOf(Color(0xFFBF5AF2), Color(0xFF4F1670)), Icons.Rounded.Lyrics),
        CollageTile(listOf(Color(0xFF0A84FF), Color(0xFF07386E)), Icons.AutoMirrored.Rounded.QueueMusic),
    ),
    listOf(
        CollageTile(listOf(Color(0xFF40C8E0), Color(0xFF0D5968)), Icons.Rounded.GraphicEq),
        CollageTile(listOf(Color(0xFFFF375F), Color(0xFF6E0D24)), Icons.Rounded.Mic),
        CollageTile(listOf(Color(0xFFFFD60A), Color(0xFF8A6A00)), Icons.Rounded.LibraryMusic),
        CollageTile(listOf(Color(0xFF8E8E93), Color(0xFF2C2C2E)), Icons.Rounded.Album),
        CollageTile(listOf(Color(0xFFFA2F48), Color(0xFF5B0A1C)), Icons.Rounded.Headphones),
        CollageTile(listOf(Color(0xFF64D2FF), Color(0xFF0B4A66)), Icons.Rounded.MusicNote),
    ),
    listOf(
        CollageTile(listOf(Color(0xFFFF9F0A), Color(0xFF7A2E00)), Icons.Rounded.Lyrics),
        CollageTile(listOf(Color(0xFF0A84FF), Color(0xFF1C1C6E)), Icons.Rounded.Radio),
        CollageTile(listOf(Color(0xFFBF5AF2), Color(0xFF2F0F52)), Icons.AutoMirrored.Rounded.QueueMusic),
        CollageTile(listOf(Color(0xFF30D158), Color(0xFF14432A)), Icons.Rounded.Mic),
        CollageTile(listOf(Color(0xFFFF375F), Color(0xFF8A1B3A)), Icons.Rounded.GraphicEq),
        CollageTile(listOf(Color(0xFF5E5CE6), Color(0xFF1B1A4F)), Icons.Rounded.LibraryMusic),
    ),
)

@Composable
private fun ArtworkCollage(modifier: Modifier = Modifier) {
    val tile = 112.dp
    val gap = 12.dp
    val transition = rememberInfiniteTransition(label = "collage")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )

    BoxWithConstraints(
        modifier = modifier
            .graphicsLayer { clip = true }
            .drawWithContent {
                drawContent()
                // Feather the collage into the canvas so the title sits on pure black.
                drawRect(
                    Brush.verticalGradient(
                        0f to MusiqueColors.canvas.copy(alpha = 0.55f),
                        0.22f to Color.Transparent,
                        0.62f to Color.Transparent,
                        1f to MusiqueColors.canvas,
                    )
                )
            }
    ) {
        val rowWidth = (tile + gap) * collageRows.first().size
        Column(
            verticalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { rotationZ = -8f }
        ) {
            collageRows.forEachIndexed { rowIndex, row ->
                val direction = if (rowIndex % 2 == 0) -1f else 1f
                CollageRow(
                    tiles = row,
                    tile = tile,
                    gap = gap,
                    width = rowWidth,
                    modifier = Modifier.graphicsLayer {
                        translationX = direction * (drift - 0.5f) * 160.dp.toPx() +
                            (rowIndex - 1) * 40.dp.toPx()
                    }
                )
            }
        }
    }
}

@Composable
private fun CollageRow(
    tiles: List<CollageTile>,
    tile: Dp,
    gap: Dp,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(gap),
        modifier = modifier.requiredWidth(width)
    ) {
        tiles.forEach { item ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(tile)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(item.colors))
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.32f),
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}

@Composable
private fun WelcomeContent(
    isLoggedIn: Boolean,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDetails by rememberSaveable { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.onb_welcome_title),
            style = MusiqueType.largeTitle.copy(fontSize = MusiqueType.largeTitle.fontSize * 1.1f),
            color = MusiqueColors.textPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.onb_welcome_body),
            style = MusiqueType.body.copy(fontSize = MusiqueType.rowTitle.fontSize),
            color = MusiqueColors.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            WelcomeFeature(Icons.Rounded.GraphicEq, stringResource(R.string.onb_stream_tracks))
            WelcomeFeature(Icons.Rounded.LibraryMusic, stringResource(R.string.onb_view_content))
            WelcomeFeature(Icons.Rounded.Sync, stringResource(R.string.onb_keep_synced))
        }

        AnimatedVisibility(
            visible = showDetails,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Text(
                text = stringResource(R.string.onb_playback_login_desc),
                style = MusiqueType.cardSubtitle,
                color = MusiqueColors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        AnimatedVisibility(visible = isLoggedIn) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.onb_playback_connected),
                    style = MusiqueType.button,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        AnimatedContent(
            targetState = isLoggedIn,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "connect"
        ) { connected ->
            PillButton(
                text = stringResource(if (connected) R.string.onb_connected else R.string.onb_connect_playback),
                onClick = onConnect,
                enabled = !connected,
            )
        }

        Spacer(Modifier.height(6.dp))

        SheetTextButton(
            text = stringResource(if (showDetails) R.string.onb_hide_details else R.string.onb_how_it_works),
            onClick = { showDetails = !showDetails },
        )
    }
}

@Composable
private fun WelcomeFeature(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MusiqueColors.accent.copy(alpha = 0.16f))
        ) {
            Icon(icon, contentDescription = null, tint = MusiqueColors.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = text,
            style = MusiqueType.rowTitle,
            color = MusiqueColors.textPrimary,
        )
    }
}
