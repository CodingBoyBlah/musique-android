package app.musique.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import app.musique.ui.components.bottomsheet.TonalPillButton
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.musique.R

@Composable
fun ErrorScreen(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MusiqueColors.card)
        ) {
            Icon(
                imageVector = Icons.Rounded.WifiOff,
                contentDescription = stringResource(R.string.common_error),
                modifier = Modifier.size(32.dp),
                tint = MusiqueColors.textTertiary,
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.error_something_went_wrong),
            style = MusiqueType.sectionTitle,
            color = MusiqueColors.textPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = message,
            style = MusiqueType.body,
            color = MusiqueColors.textTertiary,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )

        if (onRetry != null) {
            Spacer(Modifier.height(24.dp))

            TonalPillButton(
                text = stringResource(R.string.common_retry),
                icon = Icons.Rounded.Refresh,
                onClick = onRetry,
                modifier = Modifier.widthIn(max = 180.dp)
            )
        }
    }
}
