# Implementation Plan - Karaoke Word-by-Word Progress Filling

Implement a horizontal linear gradient fill effect for lyrics, where the "current word" fills smoothly based on playback progress instead of changing color instantly.

## User Review Required

> [!IMPORTANT]
> This change will replace the current alpha-based word animation with a continuous color-filling effect. Is this transition style what you're looking for?

## Proposed Changes

### UI Components (`app/src/main/java/org/parallel_sekai/kanade/ui/screens/player/PlayerComponents.kt`)

- [x] **Create `KaraokeWord` Composable**: 
    - A specialized text component that renders a single word.
    - Uses `drawWithContent` and `BlendMode.SrcIn` to achieve partial filling.
    - Calculates horizontal fill percentage based on `(currentProgress - startTime) / duration`.
- [x] **Update `WordByWordLine`**:
    - Replace the standard `Text` within the `FlowRow` with `KaraokeWord`.
    - Retain existing scale animations for the active word to enhance the "singing" feel.
- [ ] **Optimize Performance**:
    - Ensure `KaraokeWord` uses `CompositingStrategy.Offscreen` to handle blend modes correctly without affecting other UI elements.

## Verification Plan

### Automated Tests
- N/A (Visual effect verification)

### Manual Verification
- Play a track with enhanced LRC/TTML lyrics (word-by-word data).
- Observe the active word as it's being sung.
- Verify the filling is smooth and matches the singing pace.
- Check that previous words remain fully filled and upcoming words remain dimmed.
