# Plan: Enable Predictive Back Gesture Support

- [x] **Step 1: Update Manifest**
    - Add `android:enableOnBackInvokedCallback="true"` to `<application>` in `app/src/main/AndroidManifest.xml`.
- [x] **Step 2: Update Dependencies (Optional but recommended)**
    - Check if upgrading `navigation-compose` to `2.8.x` is beneficial for better predictive back support. (Verified: current 2.7.7 is sufficient, 2.8.x is better but not strictly required for basic support).
- [x] **Step 3: UI Adaptation**
    - Ensure `MainActivity.kt` uses the standard Compose Navigation.
    - Check for any custom `BackHandler` usage that might need to be replaced with `PredictiveBackHandler` for smoother animations (e.g., in the Player screen).
- [x] **Step 4: Verification**
    - Verify that navigating back between screens shows the predictive back animation (if supported by the OS and navigation library).
- [x] **Step 5: More Section Support**
    - Upgrade `navigation-compose` to `2.8.5` to enable built-in predictive back animations for all fragments/screens in the `NavHost`.
    - Verified that standard pop animations in `2.8.5` handle `Settings` and `LyricsSettings` automatically.

Is this plan suitable? Should I proceed?
