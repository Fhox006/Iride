package com.metrolist.music.discovery

import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.pages.HomePage
import java.util.Locale

enum class MixMood(
    val label: String,
    val color: Int,
    val petals: Int,
    val sharpness: Float,
    vararg val aliases: String,
) {
    CHILL("Chill", 0xFF00C2CB.toInt(), 8, 0.15f, "Relax", "Relaxed", "Rilassante", "Relaxante"),
    FOCUS("Focus", 0xFF2D6CDF.toInt(), 6, 0.90f, "Concentrazione"),
    ENERGY("Energy", 0xFFFF5A36.toInt(), 10, 0.95f, "Energize", "Energise", "Energia", "Energico"),
    FEEL_GOOD("Feel Good", 0xFFFFC94A.toInt(), 7, 0.25f, "Feel-Good", "Buonumore"),
    PARTY("Party", 0xFFC724B1.toInt(), 9, 0.85f, "Festive", "Festa"),
    WORKOUT("Workout", 0xFFB6FF3C.toInt(), 5, 1f, "Gym", "Allenamento"),
    ROMANTIC("Romantic", 0xFFFF4D8D.toInt(), 8, 0.20f, "Romance", "Love", "Romantico", "Amore"),
    SAD("Sad", 0xFF5A6B9C.toInt(), 6, 0.10f, "Melancholic", "Malinconico", "Triste", "Tristezza"),
    SLEEP("Sleep", 0xFFDDE65A.toInt(), 12, 0.05f, "Sonno", "Dormire");

    private val mixTitlePatterns = (listOf(label) + aliases).map { title ->
        val name = Regex.escape(title.lowercase(Locale.ROOT).replace('-', ' '))
        Regex("(?<!\\p{L})$name(?!\\p{L})")
    }

    companion object {
        fun fromChip(title: String?): MixMood? = entries.firstOrNull { mood ->
            title != null && (mood.label.equals(title.trim(), true) ||
                mood.aliases.any { it.equals(title.trim(), true) })
        }

        fun fromMixTitle(title: String): MixMood? {
            if (!MoodMixes.hasMixTitle(title)) return null
            val normalized = title.lowercase(Locale.ROOT).replace('-', ' ')
            return entries.firstOrNull { mood ->
                mood.mixTitlePatterns.any { it.containsMatchIn(normalized) }
            }
        }
    }
}

object MoodMixes {
    private val mixTitle = Regex("(?<!\\p{L})(?:super\\s*)?mix(?:es)?(?!\\p{L})", RegexOption.IGNORE_CASE)
    private val mixNumber = Regex("\\p{Nd}+")
    private val excludedCategories = setOf(
        "home", "podcast", "podcasts", "travel", "commute", "wellness", "viaggio", "viaggi", "benessere",
    )

    fun hasMixTitle(title: String): Boolean = mixTitle.containsMatchIn(title)

    fun isAllowedCategory(title: String?): Boolean = !title.isNullOrBlank() &&
        title.trim().lowercase(Locale.ROOT) !in excludedCategories

    fun chips(chips: List<HomePage.Chip>?): List<HomePage.Chip> = chips.orEmpty()
        .filter { isAllowedCategory(it.title) && !it.endpoint?.params.isNullOrBlank() }
        .map { chip ->
            chip.copy(title = MixMood.fromChip(chip.title)?.label ?: chip.title)
        }
        .distinctBy { it.endpoint?.params }

    fun select(items: List<YTItem>): List<PlaylistItem> = items.filterIsInstance<PlaylistItem>()
            // RDCLAK also identifies curated playlists and RD artist radio: neither alone proves a mix.
            .filter { !it.isPodcast && (it.id.startsWith("RDTMA") || hasMixTitle(it.title)) }
            .distinctBy { it.id }
            .sortedBy { mixOrder(it.title) }
            .take(10)
    private fun mixOrder(title: String): Int {
        val digits = mixNumber.findAll(title).lastOrNull()?.value ?: return Int.MAX_VALUE
        return digits.fold(0L) { value, digit ->
            (value * 10 + Character.digit(digit, 10)).coerceAtMost(Int.MAX_VALUE.toLong())
        }.toInt()
    }
}
