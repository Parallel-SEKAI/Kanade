# Kanade UI Design: Apple Music Style

## Full Screen Player Redesign

```text
+---------------------------------------+
|                [Grab]                 |  <-- Swipe down to collapse
|   ( )          Now Playing        ( ) |  <-- AirPlay/Context, Title, More
+---------------------------------------+
|                                       |
|          +-----------------+          |
|          |                 |          |
|          |    ALBUM ART    |          |
|          |   (28.dp Round) |          |
|          |                 |          |
|          +-----------------+          |
|                                       |
+---------------------------------------+
|                                       |
|  Song Title                     ( ...)|
|  Artist Name                          |
|                                       |
|  +---------------------------------+  |
|  |-----------O                     |  |  <-- Progress Slider
|  0:45                          -3:15  |
|                                       |
|      [Prev]     [PLAY]     [Next]     |  <-- Large centered controls
|                                       |
|  - [Volume Slider] +                  |  <-- Subtle volume control
|                                       |
|   [Lyrics]    [Cast]    [Queue]       |  <-- Bottom action bar
+---------------------------------------+
```

## Lyric View Redesign (Apple Music Style)

```text
+---------------------------------------+
|  [Grab]                               |
|  (Art) Title - Artist           ( ...)|  <-- Compact header
+---------------------------------------+
|                                       |
|  This is a lyric line                 |  <-- Previous line (Dimmed)
|                                       |
|  THIS IS THE ACTIVE LINE              |  <-- Bold, Large, High Contrast
|  (with glow or highlight)             |
|                                       |
|  Next lyric line                      |  <-- Next line (Dimmed)
|                                       |
|  Another upcoming line                |  <-- (Dimmed)
|                                       |
|                                       |
|                                       |
+---------------------------------------+
|  ( ) [Prev]   [PLAY]   [Next]    ( )  |  <-- Smaller footer controls
+---------------------------------------+
```

### Lyric Visual Elements
1. **Layout**:
   - Left-aligned text for a modern "sing-along" feel (or centered with heavy weight).
   - Massive font size for the active line (`HeadlineMedium` to `HeadlineLarge`).
   - Significant vertical spacing between lines to ensure clarity.

2. **Animations**:
   - **Smooth Scrolling**: The active line should always stay near the upper-middle of the screen.
   - **Transition**: Smooth cross-fade and vertical slide when lyrics change.
   - **Word-by-word (Optional/Future)**: If timing data is available, highlight words as they are sung.

3. **Background**:
   - Same dynamic blurred background as the main player, but potentially slightly darker to make white text pop.

4. **Header/Footer**:
   - Minimalist header with a tiny square album art and song info.
   - Footer retains essential playback controls but is more compact to give more space to lyrics.

1. **Background**:
   - A base layer of blurred album art (high blur radius, e.g., 40dp-60dp).
   - A semi-transparent dark/light overlay depending on the system theme to ensure text readability.
   - Animated gradients that subtly shift based on the album art's dominant colors.

2. **Album Art Animation**:
   - **Playing State**: Scale 1.0f, subtle shadow.
   - **Paused State**: Scale 0.85f, no shadow, more rounded.
   - Transition using `animateFloatAsState` for smooth scaling.

3. **Lyrics Mode**:
   - Album art shrinks to a tiny icon at the top left (beside the title).
   - Interactive, high-contrast lyrics take over the center.
   - Background remains the same dynamic blurred art.

4. **Typography**:
   - Title: `HeadlineSmall` or `TitleLarge`, Bold.
   - Artist: `BodyLarge` or `TitleMedium`, semi-transparent.
   - Time: `LabelMedium`, Monospace for stable width.

5. **Controls**:
   - Use `FilledIconButton` or custom shapes with glassmorphism effects for the Play/Pause button.
   - Skip buttons are plain icons but larger than current implementation.
