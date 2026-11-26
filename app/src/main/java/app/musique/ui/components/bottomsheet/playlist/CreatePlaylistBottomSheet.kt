package app.musique.ui.components.bottomsheet.playlist

import app.musique.ui.components.bottomsheet.*
import app.musique.ui.components.bottomsheet.MusiqueModalSheet
import app.musique.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.musique.ui.notifications.InAppNotificationController
import app.musique.ui.viewmodel.bottomsheet.CreatePlaylistViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreatePlaylistBottomSheet(
    viewModel: CreatePlaylistViewModel,
    onDismiss: () -> Unit,
    onCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    playlistId: String? = null,
    initialName: String = "",
    initialDescription: String = "",
    initialPublic: Boolean = true,
    initialCollaborative: Boolean = false,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val coroutineScope = rememberCoroutineScope()
    val isEditMode = playlistId != null

    var name by remember(playlistId) { mutableStateOf(initialName) }
    var description by remember(playlistId) { mutableStateOf(initialDescription) }
    var isPublic by remember(playlistId) { mutableStateOf(initialPublic) }
    var isCollaborative by remember(playlistId) { mutableStateOf(initialCollaborative) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.result.collect { result ->
            isSaving = false
            result.fold(
                onSuccess = { id ->
                    if (isEditMode) {
                        InAppNotificationController.show(context.getString(R.string.toast_playlist_updated), durationMillis = 2000L)
                    }
                    onDismiss()
                    onCreated(id)
                },
                onFailure = { error ->
                    val message = error.message ?: context.getString(R.string.error_unknown)
                    InAppNotificationController.show(message, durationMillis = 3000L)
                }
            )
        }
    }

    MusiqueModalSheet(
        onDismissRequest = {
            if (!isSaving) {
                coroutineScope.launch {
                    sheetState.hide()
                    onDismiss()
                }
            }
        },
        sheetState = sheetState,
        modifier = modifier,
    ) {
        SheetContent(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            spacing = 16.dp
        ) {
            SheetTitle(
                text = if (isEditMode) stringResource(R.string.createplaylist_edit_title)
                else stringResource(R.string.createplaylist_create_title)
            )

            SheetActionGroup {
                SheetTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = stringResource(R.string.playlist_name),
                )
                SheetDivider()
                SheetTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = stringResource(R.string.playlist_desc_optional),
                    singleLine = false,
                    maxLines = 3,
                )
            }

            SheetActionGroup {
                SheetSwitchRow(
                    label = stringResource(R.string.playlist_public),
                    supportingText = stringResource(
                        if (isPublic) R.string.sheet_playlist_public_desc
                        else R.string.sheet_playlist_private_desc
                    ),
                    checked = isPublic,
                    onCheckedChange = { isPublic = it },
                    enabled = !isCollaborative,
                )
                SheetDivider()
                SheetSwitchRow(
                    label = stringResource(R.string.playlist_collaborative),
                    supportingText = stringResource(R.string.playlist_others),
                    checked = isCollaborative,
                    onCheckedChange = { collaborative ->
                        isCollaborative = collaborative
                        if (collaborative) isPublic = false
                    },
                )
            }

            SheetButtonRow(
                primaryText = when {
                    isSaving && isEditMode -> stringResource(R.string.createplaylist_saving)
                    isSaving -> stringResource(R.string.createplaylist_creating)
                    isEditMode -> stringResource(R.string.common_save)
                    else -> stringResource(R.string.common_create)
                },
                primaryEnabled = name.isNotBlank() && !isSaving,
                onPrimary = {
                    isSaving = true
                    if (isEditMode) {
                        viewModel.modifyPlaylist(
                            playlistId = playlistId,
                            name = name.trim(),
                            description = description.trim().ifEmpty { "" },
                            isPublic = isPublic,
                            isCollaborative = isCollaborative,
                        )
                    } else {
                        viewModel.createPlaylist(
                            name = name.trim(),
                            description = description.trim().ifEmpty { "" },
                            isPublic = isPublic,
                            isCollaborative = isCollaborative,
                        )
                    }
                },
                secondaryText = stringResource(R.string.common_cancel),
                onSecondary = {
                    if (!isSaving) {
                        coroutineScope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
