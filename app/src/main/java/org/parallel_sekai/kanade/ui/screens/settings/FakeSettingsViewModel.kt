package org.parallel_sekai.kanade.ui.screens.settings

import android.content.Context
import org.parallel_sekai.kanade.data.repository.FakeSettingsRepository

// This is a fake implementation for preview purposes only
class FakeSettingsViewModel(context: Context) : SettingsViewModel(FakeSettingsRepository(context)) {
    // No need to override anything here if SettingsViewModel's properties are public and open (which they are)
    // We are passing a FakeSettingsRepository to the base SettingsViewModel constructor.
}
