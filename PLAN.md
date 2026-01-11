# Kanade Redesign Plan: Apple Music Style Full Screen Player

## 1. UI/UX Redesign (Apple Music Style)
- [x] **Dynamic Background**: Added multi-layered fluid background.
- [ ] **Optimized Lyric Widget (Apple Music Style)**:
    - [x] **Visuals**: Implement fading edges.
    - [x] **Typography**: Extra bold fonts, improved spacing.
    - [/] **Focus Effects**: Added alpha/scale focus.
    - [ ] **Lyric Blur**: Implement dynamic blur for inactive lines.
    - [x] **Scrolling**: Target-based centered scrolling.
    - [x] **Interaction**: Click-to-seek with haptic feedback.
    - [x] **Auto-hide Controls**: Hide header/footer with physical expansion.
- [x] **Album Art**: Large rounded corners and scale animation.
- [x] **Layout Reconstruction**: Left-aligned info, redesigned progress/controls.
- [x] **Smooth Move Transitions**: Persistent elements for seamless animation.

## 2. Technical Tasks
- [x] **Palette Integration**: Extracted colors for fluid background.
- [x] **Animation Polishing**: Persistent element offsets.
- [ ] **Performance**: Ensure 60fps with high-radius blurs.

## 3. Verification
- [ ] **Build and Install**: `./gradlew installDebug`
- [ ] **UI Review**: Verify blur aesthetics and scrolling smoothness.
