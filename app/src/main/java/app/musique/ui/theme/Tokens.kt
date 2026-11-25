package app.musique.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Fixed colors of the Cider / Apple Music look (dark). Artwork driven colors come from
 * [ArtworkPalette]; Material's colorScheme is still used for accents inside legacy screens.
 */
object MusiqueColors {
    /** Canvas behind the tab pages. */
    val canvas = Color(0xFF000000)
    /** Cards, grouped lists, search field. */
    val card = Color(0xFF161616)
    val cardRaised = Color(0xFF1C1C1E)
    /** Floating chrome: tab bar capsule, round buttons over artwork. */
    val chrome = Color(0xFF232323)
    val chromeFloating = Color(0xF0232323)
    val chromeTranslucent = Color(0xD9232323)
    val hairline = Color.White.copy(alpha = 0.08f)
    val glassEdge = Color.White.copy(alpha = 0.08f)

    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.72f)
    val textTertiary = Color.White.copy(alpha = 0.55f)
    val iconInactive = Color.White.copy(alpha = 0.62f)

    /** Translucent fill for secondary buttons over artwork (transport prev/next, shuffle). */
    val fillOnArtwork = Color.White.copy(alpha = 0.14f)
    val fillOnArtworkPressed = Color.White.copy(alpha = 0.22f)
    val trackInactive = Color.White.copy(alpha = 0.25f)
    val grabber = Color.White.copy(alpha = 0.42f)
    val pillSelected = Color.White
    val onPillSelected = Color(0xFF1C1714)

    /** Apple Music / Cider rose, used when there is no artwork to take color from. */
    val accent = Color(0xFFFA2F48)
}

object MusiqueShapes {
    val miniPlayer = RoundedCornerShape(22.dp)
    val miniPlayerPlayButton = RoundedCornerShape(14.dp)
    val navCapsule = RoundedCornerShape(30.dp)
    val navActivePill = RoundedCornerShape(22.dp)
    val card = RoundedCornerShape(14.dp)
    val cardLarge = RoundedCornerShape(18.dp)
    val tile = RoundedCornerShape(12.dp)
    val groupedList = RoundedCornerShape(20.dp)
    val artworkSmall = RoundedCornerShape(8.dp)
    val artworkMini = RoundedCornerShape(10.dp)
    val artworkHero = RoundedCornerShape(10.dp)
    val transportSide = RoundedCornerShape(24.dp)
    val transportMain = RoundedCornerShape(26.dp)
    val actionButton = RoundedCornerShape(16.dp)
    val searchField = RoundedCornerShape(12.dp)
    val sheetTop = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}

object MusiqueDimens {
    val screenPadding = 16.dp
    val navBarHeight = 58.dp
    /** Gap between the tab bar capsule and the system gesture inset. */
    val navBarBottomGap = 12.dp
    val miniPlayerHeight = 62.dp
    /** Gap between the mini player and the tab bar. */
    val miniPlayerGap = 8.dp
    val miniPlayerSideMargin = 16.dp
    val topBarHeight = 64.dp
    val roundButton = 40.dp
    val minTouchTarget = 44.dp
    val rowHeight = 56.dp
    val playerHorizontalPadding = 24.dp
    val transportHeight = 82.dp
    val playerDrawerHeight = 64.dp
    val lyricsCompactArtSize = 48.dp
    val queueRowHeight = 64.dp
    val queueArtSize = 48.dp
    val grabberWidth = 36.dp
    val grabberHeight = 4.dp

    /** Height occupied by the floating tab bar plus its bottom gap above the gesture inset. */
    val tabBarTotalHeight = navBarBottomGap + navBarHeight
    /** Bottom inset of the collapsed mini player above the gesture inset. */
    val miniPlayerBottomInset = tabBarTotalHeight + miniPlayerGap
    /** Total bottom footprint when the mini player is visible above the tab bar. */
    val miniPlayerTotalHeight = miniPlayerBottomInset + miniPlayerHeight
    val fabBottomWithMiniPlayer = miniPlayerTotalHeight + 12.dp
    val fabBottomWithoutMiniPlayer = tabBarTotalHeight + 12.dp
    val sheetCornerDragRadius = 28.dp
    val sheetDismissThreshold = 120.dp

    /**
     * Bottom content padding a scrolling tab page needs so its last item clears the floating
     * mini player and tab bar (add the navigation bar inset on top of this).
     */
    val floatingChromeHeight = miniPlayerTotalHeight + 16.dp

    /**
     * Stand-in for the gesture navigation inset where a fixed value is needed (screen bottom
     * padding constants). The shell itself measures the real inset.
     */
    val navInsetAllowance = 24.dp

    /** Gap between a toast and the floating chrome below it. */
    val notificationGap = 8.dp
}
