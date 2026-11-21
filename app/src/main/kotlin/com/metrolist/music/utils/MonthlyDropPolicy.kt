package com.metrolist.music.utils

import androidx.annotation.StringRes
import com.metrolist.music.BuildConfig
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

/** Shared local-time gate for user-visible monthly features. */
internal object MonthlyDropPolicy {
    private const val FIRST_STABLE_VERSION = "1.0.0"

    val enabledForBuild by lazy {
        BuildConfig.BUILD_TYPE == "release" &&
            Updater.compareVersions(BuildConfig.VERSION_NAME, FIRST_STABLE_VERSION) >= 0
    }

    fun isActiveFor(activationMonth: YearMonth, now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())): Boolean =
        enabledForBuild && YearMonth.from(now) >= activationMonth
}

internal data class MonthlyDropFeature(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
)

internal data class MonthlyDrop(
    val month: YearMonth,
    val features: List<MonthlyDropFeature>,
)

internal object MonthlyDropSchedule {
    // Add the first drop when its month and release content are confirmed.
    private val drops = listOf<MonthlyDrop>()

    val hasScheduledDrops: Boolean get() = drops.isNotEmpty()

    fun currentDrop(now: ZonedDateTime): MonthlyDrop? {
        if (!MonthlyDropPolicy.enabledForBuild) return null
        val month = YearMonth.from(now)
        return drops.firstOrNull { it.month == month && it.features.isNotEmpty() }
    }

    fun isFeatureActive(
        featureId: String,
        now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault()),
    ): Boolean = drops.any { drop ->
        MonthlyDropPolicy.isActiveFor(drop.month, now) && drop.features.any { it.id == featureId }
    }
}
