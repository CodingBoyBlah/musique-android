package app.musique.core.update

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musique.R
import app.musique.ui.components.bottomsheet.MusiqueModalSheet
import app.musique.ui.components.bottomsheet.SheetButtonRow
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueType

/** Sheet shown once per version when the launch check finds a newer Musique release. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatePrompt(updater: AppUpdater) {
    val visible by updater.promptVisible.collectAsStateWithLifecycle()
    val state by updater.state.collectAsStateWithLifecycle()
    if (!visible) return

    val release = when (val s = state) {
        is UpdateState.Available -> s.release
        is UpdateState.Downloading -> s.release
        is UpdateState.Installing -> s.release
        is UpdateState.Error -> s.release
        else -> null
    } ?: return

    MusiqueModalSheet(
        onDismissRequest = updater::dismissPrompt,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF111C3B), Color(0xFF0A122A))))
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(76.dp)
                )
            }
            Text(
                text = stringResource(R.string.update_prompt_title, release.version),
                style = MusiqueType.heroTitle,
                color = MusiqueColors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = release.notes.ifBlank { stringResource(R.string.update_prompt_body) },
                style = MusiqueType.body,
                color = MusiqueColors.textSecondary,
                textAlign = if (release.notes.isBlank()) TextAlign.Center else TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState())
            )

            when (val s = state) {
                is UpdateState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { s.progress },
                        color = MusiqueColors.textPrimary,
                        trackColor = MusiqueColors.trackInactive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.update_downloading, (s.progress * 100).toInt()),
                        style = MusiqueType.caption,
                        color = MusiqueColors.textTertiary,
                    )
                }
                is UpdateState.Installing -> Text(
                    text = stringResource(R.string.update_installing),
                    style = MusiqueType.caption,
                    color = MusiqueColors.textTertiary,
                )
                else -> {
                    if (s is UpdateState.Error) {
                        Text(
                            text = s.message ?: stringResource(R.string.update_failed),
                            style = MusiqueType.caption,
                            color = MusiqueColors.accent,
                            textAlign = TextAlign.Center,
                        )
                    }
                    SheetButtonRow(
                        primaryText = stringResource(R.string.update_now),
                        onPrimary = { updater.install(release) },
                        secondaryText = stringResource(R.string.update_later),
                        onSecondary = updater::dismissPrompt,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
