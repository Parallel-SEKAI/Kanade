# Kanade Redesign Plan: Apple Music Style Full Screen Player

## 1. UI/UX Redesign (Apple Music Style)
- [x] **Dynamic Background**: Added basic blurred background.
- [/] **Optimized Dynamic Background (Apple Music Style)**:
    - [ ] **Dependency**: Add `androidx.palette:palette-ktx`.
    - [ ] **Logic**: Implement Palette color extraction from album art.
    - [ ] **Component**: Create `FluidBackground` with moving gradient blobs.
    - [ ] **Polishing**: Smooth color cross-fade and GPU optimization.
- [x] **Album Art**: Implement large rounded corners and scale animation.
- [x] **Layout Reconstruction**:
    - [x] Title and Artist left-aligned.
    - [x] Redesign progress bar and controls.
- [x] **Lyrics UI Improvements**: Large font, left-aligned, smooth centering.
- [x] **Smooth Move Transitions**: Persistent elements for seamless animation.

## 2. Technical Tasks
- [ ] **Palette Integration**: Use Android Palette API for color extraction.
- [x] **Animation Polishing**: Refined `SharedTransition` and persistent element offsets.
- [ ] **Performance**: Ensure 60fps for fluid background.

## 3. Verification
- [ ] **Build and Install**: `./gradlew installDebug`
- [ ] **UI Review**: Verify transparency, blur performance, and layout alignment.
