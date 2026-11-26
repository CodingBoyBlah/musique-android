package app.musique.ui.components.bottomsheet.library

import app.musique.ui.components.bottomsheet.*
import app.musique.ui.components.bottomsheet.MusiqueModalSheet
import app.musique.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.musique.ui.viewmodel.library.ExplicitFilter
import app.musique.ui.viewmodel.library.SortBy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSortBottomSheet(
    onDismissRequest: () -> Unit,
    explicitFilter: ExplicitFilter,
    onExplicitFilterChange: (ExplicitFilter) -> Unit,
    artistNameFilter: String,
    onArtistNameFilterChange: (String) -> Unit,
    trackNameFilter: String,
    onTrackNameFilterChange: (String) -> Unit,
    sortBy: SortBy,
    onSortByChange: (SortBy) -> Unit,
    sortAscending: Boolean,
    onSortAscendingChange: (Boolean) -> Unit,
) {
    MusiqueModalSheet(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SheetContent(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            spacing = 8.dp
        ) {
            SheetTitle(
                text = stringResource(R.string.filter_sort),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SheetSectionLabel(stringResource(R.string.filter_explicit))
            val explicitOptions = listOf(
                ExplicitFilter.BOTH to stringResource(R.string.filter_show_all),
                ExplicitFilter.EXPLICIT_ONLY to stringResource(R.string.filter_explicit_only),
                ExplicitFilter.NON_EXPLICIT_ONLY to stringResource(R.string.filter_non_explicit_only)
            )
            SheetActionGroup {
                explicitOptions.forEachIndexed { index, (option, label) ->
                    if (index > 0) SheetDivider()
                    SheetCheckRow(
                        label = label,
                        selected = explicitFilter == option,
                        onClick = { onExplicitFilterChange(option) }
                    )
                }
            }

            SheetSectionLabel(
                stringResource(R.string.filter_filters),
                modifier = Modifier.padding(top = 8.dp)
            )
            SheetActionGroup {
                SheetTextField(
                    value = artistNameFilter,
                    onValueChange = onArtistNameFilterChange,
                    placeholder = stringResource(R.string.filter_artist_name),
                )
                SheetDivider()
                SheetTextField(
                    value = trackNameFilter,
                    onValueChange = onTrackNameFilterChange,
                    placeholder = stringResource(R.string.filter_track_name),
                )
            }

            SheetSectionLabel(
                stringResource(R.string.filter_sort_by),
                modifier = Modifier.padding(top = 8.dp)
            )
            val sortOptions = listOf(
                SortBy.POSITION to stringResource(R.string.sort_added_default),
                SortBy.ARTIST_NAME to stringResource(R.string.sort_artist_name),
                SortBy.TRACK_NAME to stringResource(R.string.sort_track_name),
                SortBy.DURATION to stringResource(R.string.sort_duration)
            )
            SheetActionGroup {
                sortOptions.forEachIndexed { index, (option, label) ->
                    if (index > 0) SheetDivider()
                    SheetCheckRow(
                        label = label,
                        selected = sortBy == option,
                        onClick = { onSortByChange(option) }
                    )
                }
            }

            SheetActionGroup(modifier = Modifier.padding(top = 8.dp)) {
                SheetSwitchRow(
                    label = if (sortAscending) stringResource(R.string.sort_ascending)
                    else stringResource(R.string.sort_descending),
                    checked = sortAscending,
                    onCheckedChange = onSortAscendingChange
                )
            }
        }
    }
}
