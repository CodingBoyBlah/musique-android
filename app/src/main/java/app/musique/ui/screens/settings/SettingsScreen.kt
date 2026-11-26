package app.musique.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Interests
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musique.R
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.components.NavigationPreferenceEntry
import app.musique.ui.components.SettingsChevron
import app.musique.ui.components.SettingsDefaults
import app.musique.ui.components.SettingsScaffold
import app.musique.ui.components.SettingsSection
import app.musique.ui.components.bottomsheet.account.AccountDetailBottomSheet
import app.musique.ui.components.user.AccountAvatar
import app.musique.ui.components.user.StatusPill
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueType
import app.musique.ui.viewmodel.settings.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    openInterfaceSettings: () -> Unit,
    openPlaybackSettings: () -> Unit,
    openMiscSettings: () -> Unit,
    openAboutSettings: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.checkAuthState()
    }

    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val profilePicture by viewModel.profilePicture.collectAsStateWithLifecycle()
    var showPlaybackSheet by remember { mutableStateOf(false) }

    val screenshotMode = LocalUiSettings.current.screenshotMode

    if (showPlaybackSheet) {
        AccountDetailBottomSheet(
            title = stringResource(R.string.common_playback_login),
            description = stringResource(R.string.settings_playback_login_desc),
            isLoggedIn = isLoggedIn,
            username = username,
            userImageUrl = profilePicture,
            onLogout = { viewModel.logoutPlayback() },
            onDismiss = { showPlaybackSheet = false }
        )
    }

    SettingsScaffold(
        title = stringResource(R.string.common_settings),
        onNavigateBack = onNavigateBack,
        modifier = modifier
    ) {
        item {
            SettingsSection {
                entry {
                    AccountEntry(
                        isLoggedIn = isLoggedIn,
                        username = if (isLoggedIn && screenshotMode) "Developer" else username,
                        profilePicture = profilePicture,
                        screenshotMode = screenshotMode,
                        onClick = {
                            if (isLoggedIn) showPlaybackSheet = true
                            else viewModel.startSpircAuth(context)
                        }
                    )
                }
            }
        }

        item {
            SettingsSection {
                entry {
                    NavigationPreferenceEntry(
                        title = { Text(stringResource(R.string.common_interface)) },
                        description = stringResource(R.string.settings_interface_desc),
                        icon = { Icon(Icons.Rounded.Interests, contentDescription = null) },
                        iconTint = SettingsDefaults.tintBlue,
                        onClick = openInterfaceSettings,
                    )
                }
                entry {
                    NavigationPreferenceEntry(
                        title = { Text(stringResource(R.string.common_playback)) },
                        description = stringResource(R.string.settings_playback_desc),
                        icon = { Icon(Icons.Rounded.Headphones, contentDescription = null) },
                        iconTint = SettingsDefaults.tintPink,
                        onClick = openPlaybackSettings,
                    )
                }
            }
        }

        item {
            SettingsSection {
                entry {
                    NavigationPreferenceEntry(
                        title = { Text(stringResource(R.string.common_misc)) },
                        description = stringResource(R.string.settings_misc_desc),
                        icon = { Icon(Icons.Rounded.DeveloperMode, contentDescription = null) },
                        iconTint = SettingsDefaults.tintGray,
                        onClick = openMiscSettings,
                    )
                }
                entry {
                    NavigationPreferenceEntry(
                        title = { Text(stringResource(R.string.common_about)) },
                        description = stringResource(R.string.settings_about_desc),
                        icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                        iconTint = SettingsDefaults.tintIndigo,
                        onClick = openAboutSettings,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountEntry(
    isLoggedIn: Boolean,
    username: String?,
    screenshotMode: Boolean,
    profilePicture: String?,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .background(if (pressed) Color.White.copy(alpha = 0.07f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        AccountAvatar(
            isLoggedIn = isLoggedIn,
            name = username,
            imageUrl = profilePicture,
            screenshotMode = screenshotMode,
            size = 60.dp
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (isLoggedIn && !username.isNullOrBlank()) username
                else stringResource(R.string.common_playback_login),
                style = MusiqueType.sectionTitle,
                color = MusiqueColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isLoggedIn) {
                StatusPill(text = stringResource(R.string.common_logged_in))
            } else {
                Text(
                    text = stringResource(R.string.settings_librespot_creds),
                    style = MusiqueType.cardSubtitle,
                    color = MusiqueColors.textTertiary
                )
            }
        }

        SettingsChevron()
    }
}
