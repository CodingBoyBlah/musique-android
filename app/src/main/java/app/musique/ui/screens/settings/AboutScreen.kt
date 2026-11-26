package app.musique.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import app.musique.ui.components.SettingsDefaults
import app.musique.ui.components.SettingsDivider
import app.musique.ui.components.SettingsScaffold
import app.musique.ui.components.SettingsSectionHeader
import app.musique.ui.components.bottomsheet.PillButton
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueType
import app.musique.BuildConfig
import app.musique.MyIcons
import app.musique.R
import app.musique.ScreenBottomPadding
import app.musique.ui.viewmodel.AboutViewModel
import app.musique.ui.viewmodel.UpdateState

private const val DEVELOPER = "TomKo"
private const val GITHUB_URL = "https://github.com/CodingBoyBlah/musique-android"
private const val GITHUB_DISPLAY = "github.com/CodingBoyBlah/musique-android"

private sealed interface AboutItem {
    data class Info(val label: String, val value: String) : AboutItem
    data class Body(val text: String) : AboutItem
    data class Developer(val label: String, val name: String) : AboutItem
    data class Link(
        val icon: ImageVector,
        val label: String,
        val sub: String,
        val onClick: () -> Unit,
    ) : AboutItem
    data class Update(val state: UpdateState) : AboutItem
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenUrl: (String) -> Unit = {},
    viewModel: AboutViewModel = viewModel(),
) {
    val updateState by viewModel.updateState.collectAsState()
    SettingsScaffold(
        title = stringResource(R.string.common_about),
        onNavigateBack = onNavigateBack,
        modifier = modifier
    ) {
            item { Hero() }

            item {
                AboutSection(
                    title = stringResource(R.string.about_section_app),
                    icon = Icons.Rounded.Info,
                    items = listOf(
                        AboutItem.Info(stringResource(R.string.about_version), BuildConfig.VERSION_NAME),
                        AboutItem.Info(stringResource(R.string.about_build), "#${BuildConfig.VERSION_CODE}"),
                    )
                )
            }

            item {
                AboutSection(
                    title = stringResource(R.string.about_updates),
                    icon = Icons.Rounded.SystemUpdate,
                    items = if (updateState is UpdateState.Idle) emptyList()
                    else listOf(AboutItem.Update(updateState)),
                    footer = {
                        UpdateButton(
                            state = updateState,
                            onCheck = viewModel::checkForUpdates,
                            onDownload = onOpenUrl,
                        )
                    }
                )
            }

            item {
                AboutSection(
                    title = stringResource(R.string.about_have_idea),
                    icon = Icons.Rounded.Lightbulb,
                    items = listOf(
                        AboutItem.Body(stringResource(R.string.about_report)),
                        AboutItem.Developer(stringResource(R.string.about_made_by), DEVELOPER),
                        AboutItem.Link(
                            icon = Icons.Rounded.Code,
                            label = stringResource(R.string.about_source_code),
                            sub = GITHUB_DISPLAY,
                            onClick = { onOpenUrl(GITHUB_URL) }
                        ),
                    )
                )
            }

    }
}

@Composable
private fun Hero(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF111C3B), Color(0xFF0A122A))))
        ) {
            Icon(
                imageVector = MyIcons.Logo,
                contentDescription = "Musique",
                tint = Color.Unspecified,
                modifier = Modifier.size(96.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Musique",
                style = MusiqueType.heroTitle,
                color = MusiqueColors.textPrimary
            )
            Text(
                text = BuildConfig.VERSION_NAME,
                style = MusiqueType.cardSubtitle,
                color = MusiqueColors.textTertiary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateButton(
    state: UpdateState,
    onCheck: () -> Unit,
    onDownload: (String) -> Unit,
) {
    val available = state as? UpdateState.Available
    val checking = state is UpdateState.Checking

    PillButton(
        text = stringResource(
            if (available != null) R.string.about_update_download
            else R.string.about_check_updates
        ),
        icon = if (available != null) Icons.Rounded.Download else Icons.Rounded.Refresh,
        onClick = { if (available != null) onDownload(available.url) else onCheck() },
        enabled = !checking,
        loading = checking,
        modifier = Modifier.padding(top = 12.dp)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AboutSection(
    title: String,
    icon: ImageVector,
    items: List<AboutItem>,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionHeader(title)

        if (items.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SettingsDefaults.groupShape)
                    .background(MusiqueColors.card)
            ) {
                items.forEachIndexed { index, item ->
                    if (index > 0) SettingsDivider(inset = SettingsDefaults.rowHorizontalPadding)
                    val onClick = (item as? AboutItem.Link)?.onClick
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(item.containerColor())
                            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                    ) {
                        when (item) {
                            is AboutItem.Info -> InfoRow(item.label, item.value)
                            is AboutItem.Body -> BodyRow(item.text)
                            is AboutItem.Developer -> DeveloperRow(item.label, item.name)
                            is AboutItem.Link -> LinkRow(item.icon, item.label, item.sub)
                            is AboutItem.Update -> UpdateRow(item.state)
                        }
                    }
                }
            }
        }

        footer?.invoke()
    }
}

@Composable
private fun AboutItem.containerColor(): Color = when (this) {
    is AboutItem.Update -> when (state) {
        is UpdateState.Available -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        is UpdateState.Error -> SettingsDefaults.tintRed.copy(alpha = 0.14f)
        else -> Color.Transparent
    }
    else -> Color.Transparent
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = label,
            style = MusiqueType.rowTitle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MusiqueType.body,
            color = MusiqueColors.textTertiary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BodyRow(text: String) {
    Text(
        text = text,
        style = MusiqueType.rowTitle,
        color = MusiqueColors.textTertiary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DeveloperRow(label: String, name: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(SettingsDefaults.iconSize)
                .background(SettingsDefaults.tintPink, CircleShape)
        ) {
            Text(
                text = name.first().uppercase(),
                style = MusiqueType.button,
                color = Color.White
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MusiqueType.caption,
                color = MusiqueColors.textTertiary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = name,
                    style = MusiqueType.rowTitle,
                    color = MusiqueColors.textPrimary
                )
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = SettingsDefaults.tintPink,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun LinkRow(icon: ImageVector, label: String, sub: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MusiqueColors.textTertiary,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MusiqueType.rowTitle,
                color = MusiqueColors.textPrimary
            )
            Text(
                text = sub,
                style = MusiqueType.cardSubtitle,
                color = MusiqueColors.textTertiary
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = stringResource(R.string.common_open),
            tint = MusiqueColors.textTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateRow(state: UpdateState) {
    val contentColor = when (state) {
        is UpdateState.Available -> MaterialTheme.colorScheme.primary
        is UpdateState.Error -> SettingsDefaults.tintRed
        else -> MusiqueColors.textPrimary
    }

    val title: String
    val supporting: String?
    val icon: ImageVector

    when (state) {
        is UpdateState.Available -> {
            title = stringResource(R.string.about_update_available, state.version)
            supporting = stringResource(R.string.about_update_available_desc)
            icon = Icons.Rounded.SystemUpdate
        }
        is UpdateState.Error -> {
            title = stringResource(R.string.about_update_error)
            supporting = state.message
            icon = Icons.Rounded.Error
        }
        is UpdateState.UpToDate -> {
            title = stringResource(R.string.about_up_to_date)
            supporting = null
            icon = Icons.Rounded.CheckCircle
        }
        else -> { // Checking (Idle shows no row)
            title = stringResource(R.string.about_checking_updates)
            supporting = null
            icon = Icons.Rounded.Refresh
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            if (state is UpdateState.Checking) {
                LoadingIndicator(Modifier.size(32.dp))
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MusiqueType.rowTitle,
                color = contentColor
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MusiqueType.cardSubtitle,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}