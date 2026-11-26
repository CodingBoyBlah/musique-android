package app.musique.ui.components.bottomsheet.library

import app.musique.ui.components.bottomsheet.*
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.musique.core.model.PlaylistFolder
import app.musique.ui.components.ColorPicker
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreateFolderBottomSheet(
    onDismiss: () -> Unit,
    onCreated: (PlaylistFolder) -> Unit,
    initialName: String = "",
    initialColor: Color = Color(0xFF1DB954),
    folderId: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val coroutineScope = rememberCoroutineScope()
    val isEditMode = folderId != null

    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    MusiqueModalSheet(
        onDismissRequest = {
            coroutineScope.launch {
                sheetState.hide()
                onDismiss()
            }
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(selectedColor)
                ) {
                    Icon(
                        Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                SheetTitle(text = if (isEditMode) "Edit Folder" else "Create Folder")
            }

            SheetActionGroup {
                SheetTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = stringResource(R.string.folder_name),
                )
            }

            SheetSectionLabel(stringResource(R.string.folder_color))

            ColorPicker(
                initial = selectedColor,
                onColorChanged = { selectedColor = it },
                preview = false,

                )

            SheetButtonRow(
                primaryText = if (isEditMode) stringResource(R.string.common_save) else stringResource(R.string.common_create),
                primaryEnabled = name.isNotBlank(),
                onPrimary = {
                    val id = folderId ?: UUID.randomUUID().toString()
                    val folder = PlaylistFolder(
                        id = id,
                        name = name.trim(),
                        color = selectedColor.toArgb().toLong(),
                    )
                    onCreated(folder)
                    coroutineScope.launch {
                        sheetState.hide()
                        onDismiss()
                    }
                },
                secondaryText = stringResource(R.string.common_cancel),
                onSecondary = {
                    coroutineScope.launch {
                        sheetState.hide()
                        onDismiss()
                    }
                },
            )

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

private fun Color.toArgb(): Int {
    return (alpha * 255).toInt() shl 24 or
            (red * 255).toInt() shl 16 or
            (green * 255).toInt() shl 8 or
            (blue * 255).toInt()
}
