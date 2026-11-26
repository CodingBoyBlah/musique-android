package app.musique.ui.components.bottomsheet.account

import app.musique.ui.components.bottomsheet.SheetActionGroup
import app.musique.ui.components.bottomsheet.SheetActionRow
import androidx.compose.material.icons.automirrored.rounded.Logout
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueType
import app.musique.ui.components.bottomsheet.MusiqueModalSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.musique.R
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.components.user.AccountAvatar
import app.musique.ui.components.user.StatusPill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailBottomSheet(
    title: String,
    description: String,
    isLoggedIn: Boolean,
    username: String? = null,
    userImageUrl: String? = null,
    onLogout: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenshotMode = LocalUiSettings.current.screenshotMode
    val displayName = when {
        isLoggedIn && screenshotMode -> stringResource(R.string.screenshot_mode_username)
        isLoggedIn && !username.isNullOrBlank() -> username
        else -> null
    }

    MusiqueModalSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp)
        ) {
            AccountAvatar(
                isLoggedIn = isLoggedIn,
                name = displayName,
                imageUrl = userImageUrl,
                screenshotMode = screenshotMode,
                size = 88.dp
            )

            Text(
                text = displayName ?: title,
                style = MusiqueType.heroTitle,
                color = MusiqueColors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isLoggedIn) {
                StatusPill(text = stringResource(R.string.common_logged_in))
            }

            Text(
                text = description,
                style = MusiqueType.body,
                color = MusiqueColors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoggedIn) {
                SheetActionGroup {
                    SheetActionRow(
                        label = stringResource(R.string.common_logout),
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        destructive = true,
                        onClick = {
                            onLogout()
                            onDismiss()
                        }
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.account_login_in_progress),
                    style = MusiqueType.caption,
                    color = MusiqueColors.textTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
