package app.musique.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.musique.R
import app.musique.data.repository.OutifyBackup
import app.musique.data.setting.LocalUiSettings
import app.musique.ui.components.NavigationPreferenceEntry
import app.musique.ui.components.PreferenceEntry
import app.musique.ui.components.SettingsScaffold
import app.musique.ui.components.SettingsActionRow
import app.musique.ui.components.SettingsDefaults
import app.musique.ui.components.SettingsDivider
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import app.musique.ui.components.SettingsSection
import app.musique.ui.components.SwitchPreferenceEntry
import app.musique.ui.viewmodel.settings.BackupStatus
import app.musique.ui.viewmodel.settings.MiscSettingsViewModel
import app.musique.ui.viewmodel.settings.SyncStatus

@Composable
fun MiscSettingsScreen(
    viewModel: MiscSettingsViewModel,
    onNavigateBack: () -> Unit,
    openDebugScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val likedCount by viewModel.likedCount.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()
    val screenshotMode = LocalUiSettings.current.screenshotMode

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-outify-backup")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportBackup(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importBackup(it) }
    }

    SettingsScaffold(
        title = stringResource(R.string.common_misc),
        onNavigateBack = onNavigateBack,
        modifier = modifier
    ) {
        item {
            SettingsSection(iconTint = SettingsDefaults.tintGray) {
                entry {
                    SwitchPreferenceEntry(
                        title = { Text(stringResource(R.string.settings_screenshot_mode)) },
                        description = stringResource(R.string.settings_screenshot_mode_desc),
                        isChecked = screenshotMode,
                        onCheckedChange = viewModel::setScreenshotMode,
                        icon = { Icon(Icons.Default.Screenshot, contentDescription = null) }
                    )
                }
            }
        }

        item {
            SettingsSection(
                iconTint = SettingsDefaults.tintGreen,
                title = stringResource(R.string.section_sync),
                icon = Icons.Default.Sync,
                footer = stringResource(R.string.sync_fetches_desc),
            ) {
                entry {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.settings_liked_tracks)) },
                        description = stringResource(R.string.sync_stored_count, likedCount),
                        icon = { Icon(Icons.Default.Storage, contentDescription = null) },
                        content = {
                            if (!isAuthenticated) {
                                Text(
                                    text = stringResource(R.string.tile_no_creds),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }
                entry {
                    SyncAction(
                        status = syncStatus,
                        isAuthenticated = isAuthenticated,
                        onSync = viewModel::syncLikedTracks
                    )
                }
            }
        }

        item {
            SettingsSection(
                iconTint = SettingsDefaults.tintTeal,
                title = stringResource(R.string.section_backup_restore),
                icon = Icons.Default.Backup
            ) {
                entry {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.settings_app_settings)) },
                        description = stringResource(
                            R.string.settings_backup_desc,
                            OutifyBackup.FILE_EXTENSION
                        ),
                        icon = { Icon(Icons.Default.Save, contentDescription = null) }
                    )
                }
                entry {
                    BackupAction(
                        status = backupStatus,
                        onExport = { exportLauncher.launch("outify-backup.${OutifyBackup.FILE_EXTENSION}") },
                        onImport = { importLauncher.launch(arrayOf("*/*")) }
                    )
                }
            }
        }

        item {
            SettingsSection(iconTint = SettingsDefaults.tintRed) {
                entry {
                    PreferenceEntry(
                        title = {
                            Text(
                                stringResource(R.string.common_reset_to_defaults),
                                color = SettingsDefaults.tintRed
                            )
                        },
                        description = stringResource(R.string.settings_reset_prefs_desc),
                        icon = { Icon(Icons.Default.RestartAlt, contentDescription = null) },
                        onClick = { viewModel.resetPreferences() }
                    )
                }
                entry {
                    NavigationPreferenceEntry(
                        title = { Text(stringResource(R.string.common_debug)) },
                        description = stringResource(R.string.settings_show_debug_desc),
                        icon = { Icon(Icons.Default.BugReport, contentDescription = null) },
                        iconTint = SettingsDefaults.tintGray,
                        onClick = openDebugScreen
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SyncAction(
    status: SyncStatus,
    isAuthenticated: Boolean,
    onSync: () -> Unit,
) {
    if (status is SyncStatus.Idle) {
        SettingsActionRow(
            text = stringResource(R.string.sync_liked_tracks),
            icon = Icons.Rounded.Refresh,
            enabled = isAuthenticated,
            onClick = onSync,
        )
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        when (status) {
            is SyncStatus.Idle -> Unit

            is SyncStatus.Syncing -> {
                Text(
                    text = stringResource(R.string.sync_in_progress),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            is SyncStatus.Progress -> {
                Text(
                    text = stringResource(
                        R.string.notif_sync_progress_short,
                        status.current,
                        status.total
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LinearWavyProgressIndicator(
                    progress = {
                        (status.current.toFloat() / status.total.coerceAtLeast(1)).coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            is SyncStatus.Success -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.notif_complete_excl),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            is SyncStatus.Error -> {
                Text(
                    text = stringResource(R.string.error_format, status.message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                SettingsActionRow(
                    text = stringResource(R.string.common_retry),
                    icon = Icons.Rounded.Refresh,
                    onClick = onSync,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BackupAction(
    status: BackupStatus,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    if (status is BackupStatus.Idle) {
        Column {
            SettingsActionRow(
                text = stringResource(R.string.settings_export),
                icon = Icons.Rounded.Save,
                onClick = onExport,
            )
            SettingsDivider(inset = SettingsDefaults.rowHorizontalPadding)
            SettingsActionRow(
                text = stringResource(R.string.settings_import),
                icon = Icons.Rounded.Restore,
                onClick = onImport,
            )
        }
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        when (status) {
            is BackupStatus.Idle -> Unit

            is BackupStatus.Exporting -> BusyRow(stringResource(R.string.settings_exporting))
            is BackupStatus.Importing -> BusyRow(stringResource(R.string.settings_importing))

            is BackupStatus.Success -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = status.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            is BackupStatus.Error -> {
                Text(
                    text = status.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BusyRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LoadingIndicator(Modifier.size(32.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
