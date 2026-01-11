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
