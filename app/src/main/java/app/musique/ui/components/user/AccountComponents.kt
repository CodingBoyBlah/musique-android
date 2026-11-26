package app.musique.ui.components.user

import app.musique.ui.theme.MusiqueType
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.musique.R
import app.musique.ui.components.ScreenshotAvatar
import app.musique.ui.components.SmartImage

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AccountAvatar(
    isLoggedIn: Boolean,
    name: String?,
    imageUrl: String?,
    screenshotMode: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF4A4A4C))
    ) {
        when {
            isLoggedIn && screenshotMode -> ScreenshotAvatar(Modifier.fillMaxSize())
            isLoggedIn && !imageUrl.isNullOrBlank() -> SmartImage(
                url = imageUrl,
                contentDescription = stringResource(R.string.common_profile_picture),
                modifier = Modifier.fillMaxSize()
            )
            isLoggedIn && !name.isNullOrBlank() -> Text(
                text = name.first().uppercase(),
                style = MusiqueType.largeTitle.copy(fontSize = (size.value * 0.42f).sp),
                color = Color.White
            )
            else -> Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

@Composable
fun StatusPill(text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .background(Color(0xFF30D158).copy(alpha = 0.14f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(Color(0xFF30D158), CircleShape)
        )
        Text(
            text = text,
            style = MusiqueType.caption,
            color = Color(0xFF30D158)
        )
    }
}