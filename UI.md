# UI Design - Fullscreen Player Playlist

## Fullscreen Player Overlay (Playlist Mode)

```text
+---------------------------------------+
|  [v] Indicator                        |
|                                       |
|  [Art]  Song Title           (More)   |
|         Artist Name                   |
|                                       |
|  +---------------------------------+  |
|  | Playing Next      [Shuf] [Rep]  |  |
|  |                                 |  |
|  | [Art] Song 2           [::]     |  |
|  |       Artist 2                  |  |
|  |                                 |  |
|  | [Art] Song 3           [::]     |  |
|  |       Artist 3                  |  |
|  |                                 |  |
|  | ...                             |  |
|  +---------------------------------+  |
|                                       |
|  (Slider)---------------------------- |
|  0:45                          -3:20  |
|                                       |
|  [Prev]      [Play/Pause]      [Next] |
|                                       |
|  [Lyrics]     [Airplay]     [Playlist*|
+---------------------------------------+
```

### Components:
- **Header**: Mini art, Title, and Artist moved to top left (similar to lyric mode).
- **Playlist Area**: A `LazyColumn` showing the remaining songs in the queue.
- **List Item**: 
    - Small album art.
    - Title and Artist.
    - Drag handle (placeholder icon).
    - Highlighting for the current song.
- **Controls**: Same as cover/lyric mode, but with the Playlist button highlighted.

### Transitions:
- Clicking the Playlist button fades out the current center content (Cover or Lyrics) and fades in the Playlist.
- The top header (Art/Title/Artist) follows the same interpolation logic as Lyric mode.
