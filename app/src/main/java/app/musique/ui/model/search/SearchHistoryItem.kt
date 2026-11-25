package app.musique.ui.model.search

import kotlinx.serialization.Serializable

@Serializable
data class SearchHistoryItem(
    val uri: String,
    val type: SearchResultType,
)
