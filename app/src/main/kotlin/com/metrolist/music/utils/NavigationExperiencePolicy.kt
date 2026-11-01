package com.metrolist.music.utils

internal enum class SystemNavigationMode { Gestures, ThreeButtons, TwoButtons, Unknown }

internal fun navigationModeFromSignals(
    edgeGestures: Boolean,
    platformMode: Int?,
    xiaomiGestures: Int? = null,
): SystemNavigationMode = when {
    edgeGestures -> SystemNavigationMode.Gestures
    xiaomiGestures == 1 -> SystemNavigationMode.Gestures
    xiaomiGestures == 0 -> SystemNavigationMode.ThreeButtons
    platformMode == 2 -> SystemNavigationMode.Gestures
    platformMode == 0 -> SystemNavigationMode.ThreeButtons
    platformMode == 1 -> SystemNavigationMode.TwoButtons
    else -> SystemNavigationMode.Unknown
}

internal enum class NavigationSettingsLabel {
    Settings, System, Gestures, NavigationMode, GestureNavigation, Display, NavigationBar,
    SwipeGestures, HomeScreen, SystemNavigation, SystemUpdates, AdditionalSettings,
}

internal data class NavigationSettingsGuide(
    val steps: List<NavigationSettingsLabel>,
    val selection: NavigationSettingsLabel,
    val verifiedFamily: Boolean = true,
)

internal fun navigationSettingsGuide(manufacturer: String, brand: String, sdk: Int): NavigationSettingsGuide {
    val names = setOf(manufacturer.lowercase(java.util.Locale.ROOT), brand.lowercase(java.util.Locale.ROOT))
    fun guide(vararg steps: NavigationSettingsLabel, selection: NavigationSettingsLabel) =
        NavigationSettingsGuide(listOf(NavigationSettingsLabel.Settings) + steps, selection)
    return when {
        "samsung" in names -> guide(
            NavigationSettingsLabel.Display, NavigationSettingsLabel.NavigationBar,
            selection = NavigationSettingsLabel.SwipeGestures,
        )
        names.any { it in setOf("xiaomi", "redmi", "poco") } && sdk >= 31 -> guide(
            NavigationSettingsLabel.HomeScreen, NavigationSettingsLabel.SystemNavigation,
            selection = NavigationSettingsLabel.Gestures,
        )
        "google" in names -> if (sdk >= 34) guide(
            NavigationSettingsLabel.System, NavigationSettingsLabel.NavigationMode,
            selection = NavigationSettingsLabel.GestureNavigation,
        ) else guide(
            NavigationSettingsLabel.System, NavigationSettingsLabel.Gestures, NavigationSettingsLabel.SystemNavigation,
            selection = NavigationSettingsLabel.GestureNavigation,
        )
        names.any { it in setOf("honor", "huawei") } -> guide(
            NavigationSettingsLabel.SystemUpdates, NavigationSettingsLabel.SystemNavigation,
            selection = NavigationSettingsLabel.Gestures,
        )
        "motorola" in names -> guide(
            *if (sdk >= 34) arrayOf(NavigationSettingsLabel.Gestures, NavigationSettingsLabel.SystemNavigation)
            else arrayOf(NavigationSettingsLabel.System, NavigationSettingsLabel.Gestures, NavigationSettingsLabel.SystemNavigation),
            selection = NavigationSettingsLabel.GestureNavigation,
        )
        "oneplus" in names && sdk >= 35 -> guide(
            NavigationSettingsLabel.SystemUpdates, NavigationSettingsLabel.SystemNavigation,
            selection = NavigationSettingsLabel.Gestures,
        )
        "oneplus" in names && sdk == 34 -> guide(
            NavigationSettingsLabel.AdditionalSettings, NavigationSettingsLabel.SystemNavigation,
            selection = NavigationSettingsLabel.Gestures,
        )
        else -> NavigationSettingsGuide(
            listOf(NavigationSettingsLabel.Settings), NavigationSettingsLabel.GestureNavigation,
            verifiedFamily = false,
        )
    }
}

internal fun needsAndroid14Acknowledgement(sdk: Int, currentBuild: String, acceptedBuild: String): Boolean =
    sdk == 34 && currentBuild != acceptedBuild

internal fun needsNavigationSetup(mode: SystemNavigationMode): Boolean =
    mode == SystemNavigationMode.ThreeButtons || mode == SystemNavigationMode.TwoButtons

internal enum class DeviceExperiencePanel { None, Android14, Navigation }

internal fun deviceExperiencePanel(
    sdk: Int,
    currentBuild: String,
    acceptedBuild: String,
    onboardingCompleted: Boolean,
    tutorialCompleted: Boolean,
    mode: SystemNavigationMode,
): DeviceExperiencePanel = when {
    needsAndroid14Acknowledgement(sdk, currentBuild, acceptedBuild) -> DeviceExperiencePanel.Android14
    onboardingCompleted && (needsNavigationSetup(mode) || !tutorialCompleted) -> DeviceExperiencePanel.Navigation
    else -> DeviceExperiencePanel.None
}
