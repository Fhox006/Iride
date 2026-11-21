package com.metrolist.music.ui.component

import java.util.Locale

fun chartCountryName(code: String, locale: Locale, globalLabel: String): String =
    if (code == "global") {
        globalLabel
    } else {
        Locale.Builder().setRegion(code.uppercase(Locale.ROOT)).build().getDisplayCountry(locale)
    }

private val chartMarketCodes = ("global us gb ad ar au at by be bo br bg ca cl co cr cy cz dk do ec eg sv ee fi fr " +
    "de gr gt hn hk hu is in id ie il it jp kz lv lt lu my mt mx ma nl nz ni ng no pk pa py pe ph pl pt ro ru sa " +
    "sg sk za kr es se ch tw th tr ua ae uy ve vn").split(' ').toSet()

fun supportsChartMarket(code: String): Boolean = code in chartMarketCodes
