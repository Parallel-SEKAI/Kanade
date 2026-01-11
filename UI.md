# UI Design - Karaoke Progress Filling

## Visual Representation (ASCII)

Legend:
- `[####]` : Active/Filled text (White)
- `[....]` : Inactive/Empty text (White 35% Alpha)
- `[##..]` : Partially filled word (Transitioning)

### Lyric Line State:

Current Line (Active):
```text
  [HELL] [O ] [DA..] [RKNE] [SS ]
    ^      ^     ^      ^      ^
  Done   Done Filling  Next   Next
```

### Detailed Word Transition (Zoomed):
Word: "DARKNESS" (Duration: 1000ms, Progress: 500ms)

```text
+-----------------------+
| D  A  R  K | N  E  S  S|
| [  Active  | Inactive ]|
+-----------------------+
             ^
       Linear Gradient 
       Split Point (50%)
```

## Behavior
1. **Unstarted Word**: Rendered with `inactiveColor` (35% alpha white).
2. **Current Word**: Horizontal fill from left to right.
   - The fill boundary moves linearly according to the word's internal progress.
   - The edge is sharp (hard stop) to mimic classic karaoke.
3. **Completed Word**: Fully rendered with `activeColor` (100% white).
4. **Translation**: Remains unchanged (standard fade/highlight).