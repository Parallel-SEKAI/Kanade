# Kanade UI Design: Apple Music Style

## Lyric Widget Optimization (v2)

```text
+---------------------------------------+
|  [Compact Header (Art + Info)]        |
+---------------------------------------+
|  :::::::::::::::::::::::::::::::::::  |
|                                       |
|  Previous lyric line (Blur 8dp)       | <-- Blurred & Dimmed
|                                       |
|  ACTIVE LINE (Sharp, ExtraBold)       | <-- Crystal Clear Focus
|                                       |
|  Next lyric line (Blur 6dp)           | <-- Blurred & Dimmed
|                                       |
|  :::::::::::::::::::::::::::::::::::  |
+---------------------------------------+
|  [Playback Controls Section]          |
+---------------------------------------+
```

### Advanced Focus System
1. **Dynamic Blurring**:
   - Inactive lines transition from sharp to blurred using `animateDpAsState`.
   - Enhances depth perception and directs eye attention to the current line.
2. **Layering**:
   - Background (Fluid) -> Lyric (Blurred/Sharp) -> Controls (High Contrast).