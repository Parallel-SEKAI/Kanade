# UI Layout - MiniPlayer Fix

## Current Layout (Problematic)
```text
+---------------------------+
|        Top App Bar        |
+---------------------------+
|                           |
|      Page Content         | (Library, Search, etc.)
|      (NavHost)            |
|                           |
+---------------------------+
| [ MiniPlayer (Opaque)   ] | (In bottomBar)
+---------------------------+
| [ NavigationBar         ] | (In bottomBar)
+---------------------------+
```
*Problem: MiniPlayer is in bottomBar, pushing Page Content up. Rounded corners show bottomBar background.*

## Proposed Layout
```text
+---------------------------+
|        Top App Bar        |
+---------------------------+
|                           |
|      Page Content         | (NavHost)
|      (Expands behind MP)  |
|                           |
|  +---------------------+  |
|  | [ MiniPlayer ]      |  | (Floating over NavHost)
+--+---------------------+--+
| [ NavigationBar         ] | (In bottomBar)
+---------------------------+

## FullScreen Player Controls
```text
+---------------------------+
| [Shuffle] [Prev] [Play] [Next] [Repeat] |
+---------------------------+
```
*Shuffle and Repeat buttons will be added to the main control row in FullScreenContent.*

## Lyric Page Layout (Redesigned)
```text
+---------------------------+
|      [ Blurred Album Art ]|
|      [   Background      ]|
|                           |
|       Line of Lyric       | (Headline Small, Centered)
|    [ Line Translation ]   | (Body Large, Opacity 0.7)
|                           |
|    >> Active Lyric <<     | (Primary Color/Bold)
|    [ Active Trans. ]      |
|                           |
+---------------------------+
```
*Visual improvements:*
- *Background: Blurred version of the current album art.*
- *Overlay: Semi-transparent dark scrim for readability.*
- *Bilingual: Support showing both original text and translation if available.*
- *Typography: Larger font size with generous line height.*
- *Interaction: Immersive full-screen feel.*
```
*Solution: MiniPlayer floats over NavHost content. NavigationBar remains in bottomBar.*