package com.metrolist.music.repository

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.models.coveredReverieMonths
import com.metrolist.music.models.firstFullReverieMonth
import com.metrolist.music.models.homeReverieMonth
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate
import java.time.YearMonth
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonthlyReverieRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    database: MusicDatabase,
) {
    private val seenMonthsKey = stringSetPreferencesKey("reverie_seen_home_months")
    private val today = MutableStateFlow(LocalDate.now())
    private val sessionMonths = MutableStateFlow<Set<String>>(emptySet())

    val listeningMonths = combine(database.listeningMonths(), today) { months, date ->
        months.map(YearMonth::parse).filter { it <= YearMonth.from(date) }
    }.distinctUntilChanged()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val eligibleMonths = context.dataStore.data.map {
        (it[HideVideoSongsKey] ?: false) to (it[HideExplicitKey] ?: false)
    }.distinctUntilChanged().flatMapLatest { (hideVideo, hideExplicit) ->
        database.reverieMonthActivity(hideVideo, hideExplicit)
    }.map { rows -> rows.filter { it.eligible }.map { YearMonth.parse(it.month) } }.distinctUntilChanged()

    private val installationDate = Instant.ofEpochMilli(
        context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime,
    ).atZone(ZoneId.systemDefault()).toLocalDate()

    private val trackingStarted = database.firstListeningTimestamp().map { timestamp ->
        val firstListening = timestamp?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
        minOf(installationDate, firstListening ?: installationDate)
    }.distinctUntilChanged()

    val firstFullMonth = trackingStarted.map(::firstFullReverieMonth).distinctUntilChanged()

    val completedMonths = combine(eligibleMonths, today, trackingStarted, ::coveredReverieMonths)
        .distinctUntilChanged()

    val availableMonths = completedMonths

    val homeMonth = combine(
        completedMonths,
        today,
        context.dataStore.data.map { it[seenMonthsKey].orEmpty() }.distinctUntilChanged(),
        sessionMonths,
        ::homeReverieMonth,
    ).distinctUntilChanged()

    init {
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                private val startedActivities = mutableSetOf<Activity>()

                override fun onActivityStarted(activity: Activity) { startedActivities.add(activity) }
                override fun onActivityResumed(activity: Activity) {
                    startedActivities.add(activity)
                    today.value = LocalDate.now()
                }
                override fun onActivityStopped(activity: Activity) {
                    startedActivities.remove(activity)
                    // Keep the card during navigation and rotation; retire it when the app closes.
                    if (startedActivities.isEmpty() && !activity.isChangingConfigurations) {
                        sessionMonths.value = emptySet()
                    }
                }
                override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }

    suspend fun markHomeSeen(month: YearMonth) {
        val key = month.toString()
        if (key in sessionMonths.value) return
        sessionMonths.value = sessionMonths.value + key
        context.dataStore.edit { it[seenMonthsKey] = it[seenMonthsKey].orEmpty() + key }
    }
}
