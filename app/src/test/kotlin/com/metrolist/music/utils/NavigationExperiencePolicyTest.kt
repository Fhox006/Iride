package com.metrolist.music.utils

import com.metrolist.music.ui.component.FolioDeviceProfile
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

    @Test fun android14WarningIsHandledInsideOnboardingAndReturnsForInstalledUpdates() {
        assertEquals(DeviceExperiencePanel.None, panel(sdk = 34, onboarding = false))
        assertEquals(DeviceExperiencePanel.Android14, panel(sdk = 34, mode = SystemNavigationMode.ThreeButtons))
        assertEquals(DeviceExperiencePanel.None, panel(sdk = 34, accepted = "33:100"))
        assertEquals(DeviceExperiencePanel.Android14, panel(sdk = 34, current = "33:101", accepted = "33:100"))
        assertEquals(DeviceExperiencePanel.Android14, panel(sdk = 34, current = "34:101", accepted = "33:100"))
        assertEquals(DeviceExperiencePanel.None, panel(sdk = 33))
        assertEquals(DeviceExperiencePanel.None, panel(sdk = 35))
    }

    @Test fun deviceRecommendationsNeverOverrideSavedChoice() {
        val modern = FolioDeviceProfile(lowRam = false, sdk = 35)
        val legacy = modern.copy(sdk = 34)
        val lowRam = modern.copy(lowRam = true)

        assertFalse(modern.needsIntroduction)
        assertTrue(legacy.needsIntroduction)
        assertTrue(lowRam.needsIntroduction)
        assertFalse(lowRam.copy(sdk = 34).lightweight(preference = false))
        assertFalse(lowRam.lightweight(preference = false))
        assertTrue(lowRam.lightweight(preference = true))
        assertFalse(legacy.lightweight(preference = legacy.lowRam))
        assertFalse(modern.lightweight(preference = modern.lowRam))
        for (sdk in listOf(28, 31, 34, 35, 36)) {
            assertFalse(FolioDeviceProfile(lowRam = false, sdk = sdk).defaultLightweightMode)
            assertFalse(FolioDeviceProfile(lowRam = true, sdk = sdk).defaultLightweightMode)
        }
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

    @Test fun acceptedButtonsSkipSetupAndTutorial() {
        for (mode in listOf(SystemNavigationMode.ThreeButtons, SystemNavigationMode.TwoButtons, SystemNavigationMode.Unknown)) {
            assertEquals(DeviceExperiencePanel.None, panel(mode = mode, tutorial = false, buttonsAccepted = true))
        }
    }

    @Test fun acceptingButtonsPreservesFutureGestureWelcomeAndRepeatedSwitches() {
        assertEquals(DeviceExperiencePanel.None, panel(mode = SystemNavigationMode.ThreeButtons, tutorial = false, buttonsAccepted = true))
        assertEquals(DeviceExperiencePanel.Navigation, panel(tutorial = false, buttonsAccepted = true))
        assertEquals(DeviceExperiencePanel.None, panel(tutorial = true, buttonsAccepted = true))
        // Returning to buttons resets the tutorial, without reopening a declined warning.
        assertEquals(DeviceExperiencePanel.None, panel(mode = SystemNavigationMode.TwoButtons, tutorial = false, buttonsAccepted = true))
        assertEquals(DeviceExperiencePanel.Navigation, panel(tutorial = false, buttonsAccepted = true))
    }

    @Test fun acceptingButtonsDoesNotDismissAndroid14Acknowledgement() {
        assertEquals(DeviceExperiencePanel.Android14, panel(sdk = 34, mode = SystemNavigationMode.ThreeButtons, buttonsAccepted = true))
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
        buttonsAccepted: Boolean = false,
    ) = deviceExperiencePanel(sdk, current, accepted, onboarding, tutorial, mode, buttonsAccepted)
}
