package app.musique.ui.components.bottomsheet.playback

import app.musique.ui.components.bottomsheet.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueType
import app.musique.ui.components.bottomsheet.MusiqueModalSheet
import app.musique.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.musique.ui.components.AudioBarsIndicator
import app.musique.ui.viewmodel.bottomsheet.PlaybackDevicesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaybackDevicesBottomSheet(
    viewModel: PlaybackDevicesViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )

    LaunchedEffect(viewModel) {
        viewModel.observeDevices()
    }

    val scope = rememberCoroutineScope()

    val deviceState by viewModel.deviceState.collectAsState()
    val devices = deviceState.devices
    val playback by viewModel.playback.collectAsState()
    val controlError by viewModel.controlError.collectAsState()

    MusiqueModalSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .padding(horizontal = MusiqueSheetDefaults.horizontalPadding)
                .padding(top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                SheetTitle(
                    text = stringResource(R.string.devices_title),
                    subtitle = stringResource(R.string.count_devices, devices.size),
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val canTransfer = deviceState.available && deviceState.localDeviceId != null &&
                        deviceState.transferringDeviceId == null
                    SheetRoundButton(
                        icon = Icons.Rounded.SwapHoriz,
                        contentDescription = stringResource(R.string.cd_transfer_playback),
                        tint = if (canTransfer) MusiqueColors.textPrimary else MusiqueColors.textTertiary,
                        onClick = {
                            if (canTransfer) scope.launch { viewModel.makeActive(deviceState.lease) }
                        }
                    )
                    val canRefresh = deviceState.available && !deviceState.isRefreshing
                    SheetRoundButton(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.cd_refresh_devices),
                        tint = if (canRefresh) MusiqueColors.textPrimary else MusiqueColors.textTertiary,
                        onClick = {
                            if (canRefresh) scope.launch { viewModel.loadDevices() }
                        }
                    )
                }
            }

            if (deviceState.isRefreshing) {
                LinearProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MusiqueSheetDefaults.groupColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                )
            }
            deviceState.error?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            controlError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            playback?.takeIf { it.isRemote && it.volume != null && "volume" in it.allowed }?.let { remote ->
                key(remote.target) {
                    var volume by remember { mutableFloatStateOf(remote.volume!!.toFloat()) }
                    var dragging by remember { androidx.compose.runtime.mutableStateOf(false) }
                    LaunchedEffect(remote.volume, dragging) { if (!dragging) volume = remote.volume!!.toFloat() }
                    Text(stringResource(R.string.devices_remote_volume, remote.deviceName), style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = volume, valueRange = 0f..65535f,
                        onValueChange = { dragging = true; volume = it },
                        onValueChangeFinished = {
                            dragging = false
                            viewModel.setRemoteVolume(remote.target, volume.toInt())
                        },
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (devices.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(Icons.Default.SearchOff, contentDescription = null)

                                Text(
                                    text = stringResource(when {
                                        !deviceState.available -> R.string.devices_playback_not_ready
                                        deviceState.isRefreshing -> R.string.devices_loading
                                        else -> R.string.devices_none
                                    }),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                itemsIndexed(devices, key = { _, d -> d.id ?: d.name }) { index, it ->
                    val icon = when (it.type) {
                        "Computer" -> Icons.Rounded.Laptop
                        "Smartphone" -> Icons.Rounded.Smartphone
                        "Speaker" -> Icons.Rounded.Speaker
                        else -> Icons.Rounded.DeviceUnknown
                    }
                    val isSelected = it.isActive
                    val corner = 14.dp
                    val shape = RoundedCornerShape(
                        topStart = if (index == 0) corner else 0.dp,
                        topEnd = if (index == 0) corner else 0.dp,
                        bottomStart = if (index == devices.lastIndex) corner else 0.dp,
                        bottomEnd = if (index == devices.lastIndex) corner else 0.dp,
                    )
                    val accent = MaterialTheme.colorScheme.primary

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(MusiqueSheetDefaults.groupColor)
                    ) {
                        if (index > 0) SheetDivider(inset = 64.dp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    enabled = deviceState.available && deviceState.transferringDeviceId == null && !it.isRestricted && !it.isActive,
                                ) { viewModel.selectDevice(it, deviceState.lease) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) accent else Color.White.copy(alpha = 0.1f))
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MusiqueColors.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = it.name,
                                    style = MusiqueType.rowTitle,
                                    color = if (isSelected) accent else MusiqueColors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (it.isRestricted || it.isPrivateSession) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (it.isRestricted) DeviceTag(Icons.Rounded.Lock, stringResource(R.string.common_restricted))
                                        if (it.isPrivateSession) DeviceTag(Icons.Rounded.Security, stringResource(R.string.common_private))
                                    }
                                }
                            }

                            if (deviceState.transferringDeviceId == it.id) {
                                CircularProgressIndicator(
                                    color = accent,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (isSelected) {
                                AudioBarsIndicator(
                                    isPlaying = deviceState.isPlaying,
                                    barCount = 4,
                                    barWidth = 3.dp,
                                    barHeight = 12.dp,
                                    spacing = 3.dp,
                                    color = accent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun DeviceTag(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MusiqueColors.textTertiary, modifier = Modifier.size(13.dp))
        Text(text = label, style = MusiqueType.cardSubtitle, color = MusiqueColors.textTertiary)
    }
}
