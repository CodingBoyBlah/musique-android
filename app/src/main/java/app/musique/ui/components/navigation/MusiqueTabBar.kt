package app.musique.ui.components.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.musique.ui.theme.ArtworkPalette
import app.musique.ui.theme.LocalNowPlayingPalette
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueMotion
import app.musique.ui.theme.MusiqueShapes
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.pressScale

data class NavDestination(
    val id: String,
    val label: String,
    val route: Route,
    val icon: @Composable (selected: Boolean) -> Unit,
)

/**
 * Cider-style floating capsule tab bar. Inactive tabs render icon-only; the selected tab expands
 * with a spring into a palette-tinted inner pill containing a filled icon and label.
 */
@Composable
fun MusiqueTabBar(
    items: List<NavDestination>,
    selectedId: String?,
    onItemSelected: (NavDestination) -> Unit,
    modifier: Modifier = Modifier,
    showSelectedLabel: Boolean = true,
) {
    val activePillTint = LocalNowPlayingPalette.current.surface

    Surface(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = MusiqueDimens.navBarBottomGap)
            .height(MusiqueDimens.navBarHeight),
        shape = MusiqueShapes.navCapsule,
        color = MusiqueColors.chromeFloating,
        border = BorderStroke(1.dp, MusiqueColors.hairline),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val isSelected = item.id == selectedId
                key(item.id) {
                    TabPillItem(
                        destination = item,
                        selected = isSelected,
                        activePillColor = activePillTint,
                        showLabel = showSelectedLabel,
                        onClick = { onItemSelected(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabPillItem(
    destination: NavDestination,
    selected: Boolean,
    activePillColor: Color,
    showLabel: Boolean,
    onClick: () -> Unit,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) MusiqueColors.textPrimary else MusiqueColors.iconInactive,
        animationSpec = MusiqueMotion.layout(),
        label = "tabContentColor",
    )
    val pillBackground by animateColorAsState(
        targetValue = if (selected) activePillColor else Color.Transparent,
        animationSpec = MusiqueMotion.layout(),
        label = "tabPillBackground",
    )
    val iconSize by animateDpAsState(
        targetValue = if (selected) 22.dp else 24.dp,
        animationSpec = MusiqueMotion.layout(),
        label = "tabIconSize",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .pressScale(interactionSource, pressedScale = 0.94f)
            .clip(MusiqueShapes.navActivePill)
            .background(pillBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .animateContentSize(animationSpec = MusiqueMotion.layout())
            .padding(horizontal = 14.dp)
            .semantics {
                this.selected = selected
                contentDescription = destination.label
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(iconSize),
                    contentAlignment = Alignment.Center,
                ) {
                    destination.icon(selected)
                }
                AnimatedVisibility(
                    visible = selected && showLabel,
                    enter = expandHorizontally(animationSpec = MusiqueMotion.layout()) +
                        fadeIn(animationSpec = tween(durationMillis = 180, delayMillis = 50)),
                    exit = shrinkHorizontally(animationSpec = MusiqueMotion.layout()) +
                        fadeOut(animationSpec = tween(durationMillis = 90)),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = destination.label,
                            style = MusiqueType.navLabel,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            color = contentColor,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MusiqueTabBarPreview() {
    CompositionLocalProvider(
        LocalNowPlayingPalette provides ArtworkPalette.Default.copy(surface = Color(0xFF504448))
    ) {
        Box(
            modifier = Modifier
                .background(MusiqueColors.canvas)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            MusiqueTabBar(
                items = listOf(
                    NavDestination("home", "Home", Route.HomeScreen) { selected ->
                        Icon(if (selected) Icons.Filled.Home else Icons.Outlined.Home, null)
                    },
                    NavDestination("library", "Library", Route.LibraryScreen) { selected ->
                        Icon(if (selected) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic, null)
                    },
                    NavDestination("search", "Search", Route.SearchScreen) { selected ->
                        Icon(if (selected) Icons.Filled.Search else Icons.Outlined.Search, null)
                    },
                ),
                selectedId = "home",
                onItemSelected = {},
            )
        }
    }
}
