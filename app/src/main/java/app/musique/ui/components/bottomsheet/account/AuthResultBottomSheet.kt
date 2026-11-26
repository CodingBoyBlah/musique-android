package app.musique.ui.components.bottomsheet.account

import app.musique.ui.components.bottomsheet.SheetExplainer
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.res.stringResource
import app.musique.R
import app.musique.ui.components.bottomsheet.MusiqueModalSheet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthResultBottomSheet(
    isSuccess: Boolean,
    message: String,
    errorDetails: String? = null,
    onDismiss: () -> Unit,
) {
    MusiqueModalSheet(
        onDismissRequest = onDismiss,
    ) {
        SheetExplainer(
            icon = if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
            iconTint = if (isSuccess) app.musique.ui.components.SettingsDefaults.tintGreen
            else app.musique.ui.components.bottomsheet.MusiqueSheetDefaults.destructive,
            title = message,
            body = null,
            primaryText = stringResource(R.string.common_done),
            onPrimary = onDismiss,
            extra = if (!isSuccess && errorDetails != null) {
                {
                    Text(
                        text = errorDetails,
                        style = app.musique.ui.theme.MusiqueType.rowDuration,
                        color = app.musique.ui.theme.MusiqueColors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = app.musique.ui.components.bottomsheet.MusiqueSheetDefaults.groupColor,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .padding(12.dp),
                    )
                }
            } else null,
        )
    }
}
