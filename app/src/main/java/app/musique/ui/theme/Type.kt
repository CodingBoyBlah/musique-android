package app.musique.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.musique.R

/** Inter stands in for SF Pro, which Apple Music and Cider are drawn in. */
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
)

/** Inter's alternate 1 and open digits read closer to SF; tabular figures keep timers from jittering. */
private const val InterFeatures = "cv11, ss01"
private const val InterTabular = "cv11, ss01, tnum"

private fun inter(
    size: Int,
    weight: FontWeight,
    lineHeight: Int,
    tracking: Double = 0.0,
    features: String = InterFeatures,
) = TextStyle(
    fontFamily = InterFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.em,
    fontFeatureSettings = features,
)

/**
 * The Musique type scale, one style per role. Use these directly in new UI;
 * [musiqueTypography] maps them onto Material roles so existing screens pick up Inter too.
 */
object MusiqueType {
    /** Tab page titles: Home, Library, Search. */
    val largeTitle = inter(32, FontWeight.ExtraBold, 38, -0.025)
    /** Title shown in a collapsed top bar. */
    val barTitle = inter(18, FontWeight.SemiBold, 22, -0.01)
    /** Section headers such as "Recently Played". */
    val sectionTitle = inter(20, FontWeight.Bold, 25, -0.015)
    /** Album, playlist and Now Playing titles. */
    val heroTitle = inter(24, FontWeight.Bold, 29, -0.02)
    /** Artist page name over the portrait. */
    val artistName = inter(32, FontWeight.ExtraBold, 38, -0.025)
    /** Now Playing artist and album subtitles. */
    val heroSubtitle = inter(17, FontWeight.Medium, 22, -0.01)
    /** Titles on cards and grid tiles. */
    val cardTitle = inter(15, FontWeight.SemiBold, 20, -0.01)
    val cardSubtitle = inter(13, FontWeight.Normal, 17)
    /** Small uppercase label above a card title. */
    val eyebrow = inter(11, FontWeight.SemiBold, 14, 0.04)
    val rowTitle = inter(16, FontWeight.Medium, 21, -0.005)
    val rowSubtitle = inter(14, FontWeight.Normal, 18)
    val body = inter(15, FontWeight.Normal, 21)
    val button = inter(15, FontWeight.SemiBold, 20)
    val navLabel = inter(13, FontWeight.SemiBold, 16)
    val caption = inter(12, FontWeight.Medium, 16, 0.01)
    /** Durations, scrubber times, counts. */
    val timestamp = inter(12, FontWeight.Medium, 16, 0.01, InterTabular)
    val rowDuration = inter(13, FontWeight.Normal, 17, 0.0, InterTabular)
    /** Synced lyric line. Measure every line at this size so lines never re-wrap when lit. */
    val lyricLine = inter(30, FontWeight.ExtraBold, 37, -0.022)
    val lyricBackground = inter(21, FontWeight.Bold, 27, -0.01)
    val lyricTranslation = inter(14, FontWeight.Medium, 19)
    /** Mini player text. */
    val miniTitle = inter(15, FontWeight.SemiBold, 19, -0.005)
    val miniSubtitle = inter(13, FontWeight.Normal, 17)
}

fun musiqueTypography(): Typography = Typography(
    displayLarge = inter(52, FontWeight.ExtraBold, 58, -0.03),
    displayMedium = inter(42, FontWeight.ExtraBold, 48, -0.03),
    displaySmall = inter(36, FontWeight.ExtraBold, 42, -0.025),
    headlineLarge = MusiqueType.largeTitle,
    headlineMedium = inter(28, FontWeight.Bold, 34, -0.02),
    headlineSmall = MusiqueType.heroTitle,
    titleLarge = MusiqueType.sectionTitle,
    titleMedium = inter(17, FontWeight.SemiBold, 22, -0.01),
    titleSmall = MusiqueType.cardTitle,
    bodyLarge = MusiqueType.rowTitle.copy(fontWeight = FontWeight.Normal),
    bodyMedium = MusiqueType.rowSubtitle,
    bodySmall = MusiqueType.cardSubtitle,
    labelLarge = MusiqueType.button,
    labelMedium = MusiqueType.caption,
    labelSmall = inter(11, FontWeight.Medium, 14, 0.02),
)
