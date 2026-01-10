# Kanade UI Design: Apple Music Style

## Dynamic Background Optimization

```text
+---------------------------------------+
|          [ Player Controls ]          | <-- High contrast text/icons
+---------------------------------------+
|        [ Semi-transparent Scrim ]     | <-- Ensures readability
+---------------------------------------+
|   ( Blob A )         ( Blob B )       | <-- Moving radial gradients
|           ( Blob C )                  | <-- 3-5 extracted Palette colors
+---------------------------------------+
|      [ Base Blurred Art ]             | <-- 60dp+ blur for core tone
+---------------------------------------+
```

### Background Visual Strategy
1. **Layer 1: Base Tone**
   - Highly blurred album art image.
2. **Layer 2: Fluid Gradients**
   - 3 to 4 moving blobs using `Brush.radialGradient`.
   - Positions and radii animated with `rememberInfiniteTransition`.
   - Colors dynamically extracted using Palette API (Vibrant, Muted, Dominant).
3. **Layer 3: Scrim**
   - Semi-transparent overlay to ensure accessibility and text contrast.

---

## Full Screen Player Layout

```text
+---------------------------------------+
|                [Grab]                 |
|  (Art) Title - Artist           ( ...)|  <-- Compact header (Lyric Mode)
+---------------------------------------+
|                                       |
|          +-----------------+          |
|          |                 |          |
|          |    ALBUM ART    |          |
|          |                 |          |
|          +-----------------+          |
|                                       |
+---------------------------------------+
|                                       |
|  Song Title                           |
|  Artist Name                          |
|                                       |
|  +---------------------------------+  |
|  |-----------O                     |  |
|  0:45                          -3:15  |
|                                       |
|      [Prev]     [PLAY]     [Next]     |
|                                       |
|   [Lyrics]    [Cast]    [Queue]       |
+---------------------------------------+
```