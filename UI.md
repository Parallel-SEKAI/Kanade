# Artist Parsing Settings UI Layout

## ArtistParsingSettingsScreen

### Header
- App bar with title "Artist Parsing Settings" and back button.

### Content
- Scrollable Column

    #### Separators Section
    - Title: "Artist Separators (Priority Order)"
    - Description: "Artists will be split using the first matching separator from this list. Drag to reorder."
    - `LazyColumn` for separators:
        - Each item:
            - `Row` with `DragHandle` (for reordering).
            - `TextField` for the separator string.
            - `IconButton` (Delete icon) to remove.
        - Button: "Add New Separator"

    #### Whitelist Section
    - Title: "Artist Whitelist"
    - Description: "Artist names in this list will not be split, even if they contain separators."
    - `LazyColumn` for whitelist items:
        - Each item:
            - `TextField` for the whitelisted artist name.
            - `IconButton` (Delete icon) to remove.
        - Button: "Add New Whitelist Entry"

    #### Join String Section
    - Title: "Display Join String"
    - Description: "Used to combine multiple artists for display (e.g., 'Artist A & Artist B')."
    - `TextField` for `joinString` (e.g., default: ", ").

## Album Detail Page (Optimized)

### Header
- Full width album art (if available).
- Album Title.
- **Album Artist** (Intersection of all song artists).

### Song List
- List of songs in the album.
- **Item Layout**:
    - [Hidden] Individual Song Art (Cover).
    - Title.
    - [Conditional] Artist Name (Hidden if all songs have same artist).

## Lyrics Image Sharing UI

### Trigger
- **Action**: Long press on any lyric line in the full-screen player's lyric view.

### Lyric Selection Page (ModalBottomSheet)
- **Header**: Title "Share Lyrics", "Share" and "Save" buttons.
- **Content**: 
    - A list of all lyrics with checkboxes for multiple selection.
    - Pre-select the long-pressed line.
    - Real-time preview toggle (optional) or dynamic image generation.

### Card Design (Generated Image)
- **Background**: 
    - Material 3 surface color or a blurred version of the current album art.
    - Dark mode support (defaulting to a dark, elegant aesthetic).
- **Metadata Section (Top)**:
    - **Left Side**: 
        - Song Title (Large, Bold, White).
        - Artist Name (Medium, White with 70% opacity).
        - Album Name (Small, White with 50% opacity).
    - **Right Side**:
        - Album Cover (Small square with rounded corners).
- **Lyrics Section (Middle)**:
    - Divider line.
    - Selected lyric lines (Left aligned).
    - Translation lines (Small, directly below the original text).
    - Divider line.
- **Branding Section (Bottom)**:
    - **Right Side**: Text `Parallel-SEKAI/Kanade`.
- **Layout Reference (ASCII)**:
```text
+---------------------------------------------------+
|                                                   |
|   Song Title                       +----------+   |
|   Artist Name                      |          |   |
|   Album Name                       |  Album   |   |
|                                    |  Cover   |   |
|                                    |          |   |
|                                    +----------+   |
|                                                   |
|   ---------------------------------------------   |
|                                                   |
|   Lyric Line 1 (Original)                         |
|   Lyric Line 1 (Translation)                      |
|                                                   |
|   Lyric Line 2 (Original)                         |
|   Lyric Line 2 (Translation)                      |
|                                                   |
|   ---------------------------------------------   |
|                                                   |
|                             Parallel-SEKAI/Kanade |
|                                                   |
+---------------------------------------------------+
```
