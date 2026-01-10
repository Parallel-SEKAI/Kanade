# Kanade Redesign Plan: Apple Music Style Full Screen Player

## 1. UI/UX Redesign (Apple Music Style)
- [ ] **Dynamic Background**: Always use a multi-layered blurred album art background instead of a solid color.
- [ ] **Album Art**: Implement large rounded corners (approx 24.dp) and a "breathing" scale animation when playing/paused.
- [ ] **Layout Reconstruction**:
    - [ ] Move Title and Artist to the left above the progress bar.
    - [ ] Add a "More Options" button next to the title.
    - [ ] Redesign the progress bar and time labels to be more minimalist.
    - [ ] Redesign Playback Controls: Larger icons, consistent spacing.
- [ ] **Bottom Action Bar**: Add icons for Lyrics, AirPlay (placeholder), and Queue (placeholder) at the very bottom.
- [ ] **Lyrics UI Improvements**:
    - [ ] **Typography**: Use much larger, bolder fonts for the active line.
    - [ ] **Alignment**: Change to left-aligned with a significant left margin (standard Apple Music style).
    - [ ] **Visual Effects**: Add a subtle "glow" or increased brightness to the active line.
    - [ ] **Compact Header**: Create a minimal header for the lyric view (mini-art + title).
    - [ ] **Scrolling**: Implement smoother, more centered auto-scrolling logic.

## 2. Technical Tasks
- [ ] **Palette Integration**: Use Android Palette API or Compose-based color extraction for better background blending.
- [ ] **Animation Polishing**: Refine `SharedTransition` and add subtle scale animations for the album art.
- [ ] **Component Modularization**: Refactor `FullScreenContent` into smaller, manageable pieces (Header, Content, Controls, Footer).

## 3. Verification
- [ ] **Build and Install**: `./gradlew installDebug`
- [ ] **UI Review**: Verify transparency, blur performance, and layout alignment.