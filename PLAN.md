# Plan: Show full-screen player from media notification

## Goal
Show the full-screen player when the user clicks on the media notification.

## Tasks
- [ ] **Task 1: Update AndroidManifest.xml**
    - Set `android:launchMode="singleTop"` for `MainActivity` to ensure we can handle new intents efficiently.
- [ ] **Task 2: Update KanadePlaybackService.kt**
    - Create a `PendingIntent` targeting `MainActivity` with an extra `EXTRA_EXPAND_PLAYER = true`.
    - Set this `PendingIntent` as the `sessionActivity` for the `MediaSession`.
- [ ] **Task 3: Update MainActivity.kt**
    - Define a constant for `EXTRA_EXPAND_PLAYER`.
    - Create a function to check the intent and trigger `PlayerIntent.Expand` if needed.
    - Call this function in `onCreate` and override `onNewIntent` to call it as well.

## Verification
- Start playback.
- Go to home screen or another app.
- Click the media notification.
- The app should open and automatically expand the player to full-screen.
