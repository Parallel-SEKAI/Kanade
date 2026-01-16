# Plan: Redesign Playlist Mode Buttons in Full-screen Player

## Goal
Redesign the shuffle and repeat mode buttons in the playlist view of the full-screen player to be more visually appealing and consistent with Material 3 principles.

## Tasks
- [ ] **Task 1: Locate and Analyze current implementation**
    - [x] Identified `PlaylistContent` in `app/src/main/java/org/parallel_sekai/kanade/ui/screens/player/PlayerComponents.kt`.
- [ ] **Task 2: Design and Implement new Mode Buttons**
    - Replace the existing `IconButton` implementations with a more refined design.
    - Use `Surface` or `Box` with background and click handling for better control over the visual state.
    - Ensure active states (shuffle on, repeat modes) are clearly distinguished.
- [ ] **Task 3: Verification**
    - Build the project to ensure no compilation errors.
    - (Manual) Verify the new UI in the app.

## Proposed Design Changes
- Use a pill-shaped or rounded-rect container for the buttons.
- Increase the contrast for active states.
- Ensure the icons and backgrounds harmonize with the dark, immersive player UI.