package com.ranhroi.esp32hidremote.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ranhroi.esp32hidremote.R


sealed class OnboardingModel(
    @DrawableRes val image: Int,
    @StringRes val titleRes: Int,      // Uses resource ID
    @StringRes val descriptionRes: Int, // Uses resource ID
) {

    // Step 1: Plug ESP32 hardware into the computer
    data object FirstPage : OnboardingModel(
        image = R.drawable.ic_setup_step1,
        titleRes = R.string.onboarding_title_hardware,
        descriptionRes = R.string.onboarding_desc_hardware
    )

    // Step 2: App setup confirmation
    data object SecondPage : OnboardingModel(
        image = R.drawable.ic_setup_step2,
        titleRes = R.string.onboarding_title_welcome,
        descriptionRes = R.string.onboarding_desc_welcome
    )

    // Step 3: Connect Bluetooth pairing with ESP32 HID
    data object ThirdPages : OnboardingModel(
        image = R.drawable.ic_setup_step3,
        titleRes = R.string.onboarding_title_bluetooth,
        descriptionRes = R.string.onboarding_desc_bluetooth
    )

    // Step 4: Ready to control (Mouse, keyboard, shortcuts)
    data object FivePages : OnboardingModel(
        image = R.drawable.ic_setup_step4,
        titleRes = R.string.onboarding_title_ready,
        descriptionRes = R.string.onboarding_desc_ready
    )
}
