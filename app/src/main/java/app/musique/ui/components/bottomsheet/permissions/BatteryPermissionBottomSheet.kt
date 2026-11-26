package app.musique.ui.components.bottomsheet.permissions

import app.musique.ui.components.bottomsheet.SheetExplainer
import androidx.compose.material.icons.rounded.*
import app.musique.ui.components.bottomsheet.MusiqueModalSheet
import app.musique.R

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryOptimizationBottomSheet(
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    MusiqueModalSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        BatteryOptimizationSheetContent(
            onAllow = onAllow,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun BatteryOptimizationSheetContent(
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    SheetExplainer(
        icon = Icons.Rounded.BatteryChargingFull,
        iconTint = app.musique.ui.components.SettingsDefaults.tintGreen,
        title = stringResource(R.string.battery_title),
        body = stringResource(R.string.battery_desc),
        primaryText = stringResource(R.string.battery_action),
        onPrimary = onAllow,
        secondaryText = stringResource(R.string.common_not_now),
        onSecondary = onDismiss,
        modifier = Modifier.navigationBarsPadding()
    )
}
