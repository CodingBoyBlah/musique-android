package app.musique.ui.screens.library.show

import androidx.compose.runtime.Immutable
import app.musique.core.model.ConsumptionOrder
import app.musique.core.model.Episode
import app.musique.core.model.Show
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class ShowUiState(
    val isLoading: Boolean = true,
    val show: Show? = null,
    val episodes: List<Episode> = emptyList(),
    val isLoadingMore: Boolean = false,
    val consumptionOrder: ConsumptionOrder? = null,
    val hasMore: Boolean = false,
    val error: String? = null,
)