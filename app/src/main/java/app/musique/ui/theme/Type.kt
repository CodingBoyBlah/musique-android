package app.musique.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.musique.R

/**
 * Google Sans Flex, the face Cider for Android is set in (OFL, one variable file). Each weight is
 * an instance of the variable font; rounding stays off so it matches Cider's crisp terminals.
 */
private fun sansWeight(weight: Int) = Font(
    R.font.google_sans_flex,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("ROND", 0f),
    ),
)

val SansFamily = FontFamily(
    sansWeight(400),
    sansWeight(500),
    sansWeight(600),
    sansWeight(700),
    sansWeight(800),
)

/** Tabular figures keep timers and durations from jittering. */
private const val Tabular = "tnum"

private fun sans(
    size: Int,
    weight: FontWeight,
    lineHeight: Int,
    tracking: Double = 0.0,
    features: String? = null,
) = TextStyle(
    fontFamily = SansFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.em,
    fontFeatureSettings = features,
)

/**
 * The Musique type scale, one style per role. Use these directly in new UI;
 * [musiqueTypography] maps them onto Material roles so existing screens pick up Google Sans too.
 */
object MusiqueType {
    /** Tab page titles: Home, Library, Search. */
    val largeTitle = sans(34, FontWeight.Bold, 40, -0.01)
    /** Title shown in a collapsed top bar. */
    val barTitle = sans(18, FontWeight.SemiBold, 22)
    /** Section headers such as "Recently Played". */
    val sectionTitle = sans(20, FontWeight.Medium, 25)
    /** Album, playlist and Now Playing titles. */
    val heroTitle = sans(24, FontWeight.SemiBold, 30)
    /** Artist page name over the portrait. */
    val artistName = sans(34, FontWeight.Bold, 40, -0.01)
    /** Now Playing artist and album subtitles. */
    val heroSubtitle = sans(18, FontWeight.Normal, 23)
    /** Titles on cards and grid tiles. */
    val cardTitle = sans(15, FontWeight.Medium, 20)
    val cardSubtitle = sans(13, FontWeight.Normal, 17)
    /** Small uppercase label above a card title. */
    val eyebrow = sans(12, FontWeight.Medium, 15, 0.01)
    val rowTitle = sans(16, FontWeight.Normal, 21)
    val rowSubtitle = sans(14, FontWeight.Normal, 18)
    val body = sans(15, FontWeight.Normal, 21)
    val button = sans(16, FontWeight.Medium, 20)
    val navLabel = sans(14, FontWeight.Medium, 17)
    val caption = sans(12, FontWeight.Medium, 16, 0.01)
    /** Durations, scrubber times, counts. */
    val timestamp = sans(12, FontWeight.Medium, 16, 0.01, Tabular)
    val rowDuration = sans(13, FontWeight.Normal, 17, 0.0, Tabular)
    /** Synced lyric line. Measure every line at this size so lines never re-wrap when lit. */
    val lyricLine = sans(30, FontWeight.Bold, 37, -0.01)
    val lyricBackground = sans(21, FontWeight.Bold, 27)
    val lyricTranslation = sans(14, FontWeight.Medium, 19)
    /** Mini player text. */
    val miniTitle = sans(15, FontWeight.Medium, 19)
    val miniSubtitle = sans(13, FontWeight.Normal, 17)
}

fun musiqueTypography(): Typography = Typography(
    displayLarge = sans(52, FontWeight.Bold, 58, -0.02),
    displayMedium = sans(42, FontWeight.Bold, 48, -0.02),
    displaySmall = sans(36, FontWeight.Bold, 42, -0.015),
    headlineLarge = MusiqueType.largeTitle,
    headlineMedium = sans(28, FontWeight.Bold, 34, -0.01),
    headlineSmall = MusiqueType.heroTitle,
    titleLarge = MusiqueType.sectionTitle,
    titleMedium = sans(17, FontWeight.Medium, 22),
    titleSmall = MusiqueType.cardTitle,
    bodyLarge = MusiqueType.rowTitle.copy(fontWeight = FontWeight.Normal),
    bodyMedium = MusiqueType.rowSubtitle,
    bodySmall = MusiqueType.cardSubtitle,
    labelLarge = MusiqueType.button,
    labelMedium = MusiqueType.caption,
    labelSmall = sans(11, FontWeight.Medium, 14, 0.02),
)
