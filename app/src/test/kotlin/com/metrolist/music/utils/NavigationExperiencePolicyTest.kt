package com.metrolist.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationExperiencePolicyTest {
    @Test fun distinguishesButtonsGesturesAndUnknownWithoutUsingBarHeight() {
        assertEquals(SystemNavigationMode.ThreeButtons, navigationModeFromSignals(false, 0))
        assertEquals(SystemNavigationMode.TwoButtons, navigationModeFromSignals(false, 1))
        assertEquals(SystemNavigationMode.Gestures, navigationModeFromSignals(false, 2))
        assertEquals(SystemNavigationMode.Unknown, navigationModeFromSignals(false, null))
        assertEquals(SystemNavigationMode.Unknown, navigationModeFromSignals(false, 99))
        assertEquals(SystemNavigationMode.Gestures, navigationModeFromSignals(true, null))
    }

    @Test fun recognizesXiaomiGesturesWithHiddenIndicator() {
        assertEquals(SystemNavigationMode.Gestures, navigationModeFromSignals(false, 0, 1))
        assertEquals(SystemNavigationMode.ThreeButtons, navigationModeFromSignals(false, 2, 0))
    }

    @Test fun android14WarningPrecedesOnboardingAndReturnsForEachInstalledUpdate() {
        assertEquals(DeviceExperiencePanel.Android14, panel(sdk = 34, onboarding = false))
        assertEquals(DeviceExperiencePanel.Android14, panel(sdk = 34, mode = SystemNavigationMode.ThreeButtons))
        assertEquals(DeviceExperiencePanel.None, panel(sdk = 34, accepted = "33:100"))
        assertEquals(DeviceExperiencePanel.Android14, panel(sdk = 34, current = "33:101", accepted = "33:100"))
        assertEquals(DeviceExperiencePanel.Android14, panel(sdk = 34, current = "34:101", accepted = "33:100"))
        assertEquals(DeviceExperiencePanel.None, panel(sdk = 33))
        assertEquals(DeviceExperiencePanel.None, panel(sdk = 35))
    }

    @Test fun buttonsAlwaysReopenTheGateEvenAfterTutorialCompletion() {
        assertEquals(DeviceExperiencePanel.Navigation, panel(mode = SystemNavigationMode.ThreeButtons))
        assertEquals(DeviceExperiencePanel.Navigation, panel(mode = SystemNavigationMode.TwoButtons))
        assertEquals(DeviceExperiencePanel.None, panel(mode = SystemNavigationMode.Gestures))
        assertEquals(DeviceExperiencePanel.Navigation, panel(tutorial = false))
        assertEquals(DeviceExperiencePanel.None, panel(onboarding = false, tutorial = false))
    }

    @Test fun unknownModeDoesNotInventAThreeButtonDetection() {
        assertFalse(needsNavigationSetup(SystemNavigationMode.Unknown))
        assertEquals(DeviceExperiencePanel.None, panel(mode = SystemNavigationMode.Unknown))
        assertEquals(DeviceExperiencePanel.Navigation, panel(mode = SystemNavigationMode.Unknown, tutorial = false))
    }

    @Test fun googlePathTracksTheNavigationMenuMove() {
        assertEquals(
            listOf(NavigationSettingsLabel.Settings, NavigationSettingsLabel.System, NavigationSettingsLabel.NavigationMode),
            navigationSettingsGuide("Google", "google", 35).steps,
        )
        assertEquals(
            listOf(NavigationSettingsLabel.Settings, NavigationSettingsLabel.System, NavigationSettingsLabel.Gestures, NavigationSettingsLabel.SystemNavigation),
            navigationSettingsGuide("Google", "google", 33).steps,
        )
    }

    @Test fun manufacturerPathsUseTheirOwnEnglishLabels() {
        val samsung = navigationSettingsGuide("SAMSUNG", "Samsung", 35)
        assertEquals(listOf(NavigationSettingsLabel.Settings, NavigationSettingsLabel.Display, NavigationSettingsLabel.NavigationBar), samsung.steps)
        assertEquals(NavigationSettingsLabel.SwipeGestures, samsung.selection)
        for (brand in listOf("Xiaomi", "Redmi", "POCO")) {
            assertEquals(NavigationSettingsLabel.HomeScreen, navigationSettingsGuide("Xiaomi", brand, 35).steps[1])
        }
        assertEquals(NavigationSettingsLabel.SystemUpdates, navigationSettingsGuide("HONOR", "HONOR", 35).steps[1])
        assertEquals(NavigationSettingsLabel.AdditionalSettings, navigationSettingsGuide("OnePlus", "OnePlus", 34).steps[1])
        assertEquals(NavigationSettingsLabel.SystemUpdates, navigationSettingsGuide("OnePlus", "OnePlus", 35).steps[1])
        assertEquals(NavigationSettingsLabel.Gestures, navigationSettingsGuide("motorola", "motorola", 35).steps[1])
    }

    @Test fun unknownFirmwareGetsSearchInstructionsInsteadOfMadeUpPath() {
        assertFalse(navigationSettingsGuide("Unknown", "Unknown", 36).verifiedFamily)
        assertFalse(navigationSettingsGuide("Xiaomi", "Xiaomi", 29).verifiedFamily)
        assertTrue(navigationSettingsGuide("Samsung", "Samsung", 34).verifiedFamily)
    }

    private fun panel(
        sdk: Int = 35,
        current: String = "33:100",
        accepted: String = "",
        onboarding: Boolean = true,
        tutorial: Boolean = true,
        mode: SystemNavigationMode = SystemNavigationMode.Gestures,
    ) = deviceExperiencePanel(sdk, current, accepted, onboarding, tutorial, mode)
}
