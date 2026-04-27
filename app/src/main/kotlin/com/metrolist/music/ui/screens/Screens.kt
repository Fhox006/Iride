/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.metrolist.music.R

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    @DrawableRes val iconIdInactive: Int,
    @DrawableRes val iconIdActive: Int,
    val route: String,
) {
    object Home : Screens(
        titleId = R.string.home,
        iconIdInactive = R.drawable.home_outlined,
        iconIdActive = R.drawable.home_filled,
        route = "home"
    )

    object WhatNew : Screens(
        titleId = R.string.what_new,
        iconIdInactive = R.drawable.newspaper,
        iconIdActive = R.drawable.newspaper,
        route = "what_new"
    )

    object Search : Screens(
        titleId = R.string.search,
        iconIdInactive = R.drawable.search,
        iconIdActive = R.drawable.search,
        route = "search_input"
    )

    object ListenTogether : Screens(
        titleId = R.string.together,
        iconIdInactive = R.drawable.group_outlined,
        iconIdActive = R.drawable.group_filled,
        route = "listen_together"
    )

    object Library : Screens(
        titleId = R.string.filter_library,
        iconIdInactive = R.drawable.bookmark_outlined,
        iconIdActive = R.drawable.bookmark_filled,
        route = "library"
    )

    object Account : Screens(
        titleId = R.string.account,
        iconIdInactive = R.drawable.account,
        iconIdActive = R.drawable.account,
        route = "settings"
    )

    companion object {
        val MainScreens = listOf(Home, WhatNew, Library, Account, Search)
    }
}
