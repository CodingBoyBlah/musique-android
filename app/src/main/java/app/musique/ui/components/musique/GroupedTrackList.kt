package app.musique.ui.components.musique

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.musique.ui.theme.MusiqueColors
import app.musique.ui.theme.MusiqueDimens
import app.musique.ui.theme.MusiqueShapes
import app.musique.ui.theme.MusiqueType
import app.musique.ui.theme.musiqueTypography

/**
 * Rounded 20dp tinted container holding a group of rows separated by inset hairline dividers.
 * Use directly for bounded lists; for long virtualized LazyColumn lists use
 * [GroupedTrackItemContainer] with `itemsIndexed` so recycling stays intact.
 */
@Composable
fun GroupedTrackList(
    modifier: Modifier = Modifier,
    containerColor: Color = MusiqueColors.card,
    shape: Shape = MusiqueShapes.groupedList,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content,
        )
    }
}

/**
 * Hairline 0.5dp divider inset from the start edge to align with row text rather than leading art/numbers.
 */
@Composable
fun GroupedListDivider(
    modifier: Modifier = Modifier,
    startInset: Dp = 48.dp,
    endInset: Dp = MusiqueDimens.screenPadding,
    color: Color = MusiqueColors.hairline,
) {
    HorizontalDivider(
        modifier = modifier.padding(start = startInset, end = endInset),
        thickness = MusiqueDimens.hairlineWidth,
        color = color,
    )
}

/**
 * Corner shape for row [index] of [count] in a Cider tile group: large corners on the outside of
 * the group, small ones where two tiles meet.
 */
fun groupedItemShape(
    index: Int,
    count: Int,
    cornerRadius: Dp = 18.dp,
    innerRadius: Dp = 6.dp,
): Shape = when {
    count <= 1 -> RoundedCornerShape(cornerRadius)
    index == 0 -> RoundedCornerShape(
        topStart = cornerRadius, topEnd = cornerRadius, bottomStart = innerRadius, bottomEnd = innerRadius,
    )
    index == count - 1 -> RoundedCornerShape(
        topStart = innerRadius, topEnd = innerRadius, bottomStart = cornerRadius, bottomEnd = cornerRadius,
    )
    else -> RoundedCornerShape(innerRadius)
}

/** Gap between two tiles of a group, as in Cider's song lists. */
val GroupedTileGap = 3.dp

/**
 * One row of a Cider style song list in a LazyColumn: each row is its own tile, separated by a
 * small gap, with the group's outer corners rounded. Keeps LazyColumn item recycling intact.
 */
@Composable
fun GroupedTrackItemContainer(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MusiqueColors.chrome,
    @Suppress("UNUSED_PARAMETER") dividerStartInset: Dp = 48.dp,
    horizontalMargin: Dp = MusiqueDimens.screenPadding,
    cornerRadius: Dp = 18.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalMargin)
            .padding(bottom = if (index < count - 1) GroupedTileGap else 0.dp),
        shape = groupedItemShape(index = index, count = count, cornerRadius = cornerRadius),
        color = containerColor,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun GroupedTrackListPreview() {
    MaterialTheme(typography = musiqueTypography()) {
        GroupedTrackList(
            modifier = Modifier
                .background(MusiqueColors.canvas)
                .padding(16.dp),
            containerColor = Color(0xFF5B2B10),
        ) {
            Text(
                text = "Track One",
                style = MusiqueType.rowTitle,
                color = MusiqueColors.textPrimary,
                modifier = Modifier.padding(16.dp),
            )
            GroupedListDivider()
            Text(
                text = "Track Two",
                style = MusiqueType.rowTitle,
                color = MusiqueColors.textPrimary,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
