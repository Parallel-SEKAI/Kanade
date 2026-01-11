# UI Design - Search Page

## Search Screen Layout

```text
+---------------------------------------+
|  [ <- ]  [ Search music...         X ]|  <-- Search Bar
+---------------------------------------+
|                                       |
|  RECENT SEARCHES            [ CLEAR ] |  <-- History Header (Visible if query empty)
|                                       |
|  * Song title 1                       |  <-- History Item
|  * Artist Name                        |
|  * Another search                     |
|                                       |
+---------------------------------------+
|                                       |
|  SEARCH RESULTS                       |  <-- Results Header (Visible if query NOT empty)
|                                       |
|  +-------+  Song Title                |  <-- Result Item
|  | COVER |  Artist - Album            |
|  +-------+                            |
|                                       |
|  +-------+  Another Song              |
|  | COVER |  Artist - Album            |
|  +-------+                            |
|                                       |
+---------------------------------------+
|                                       |
|          [ No results found ]         |  <-- Empty State
|                                       |
+---------------------------------------+
| [ Home ] [ Search ] [ Library ] [ More]| <-- Bottom Navigation (Existing)
+---------------------------------------+
```

## Components

- **Search Bar**: Material 3 `SearchFullWidth` or `DockedSearchBar`.
- **Music Item**: Same style as `LibraryScreen` items for consistency.
- **History Item**: Simple text with a delete icon or just clickable text.
- **Empty State**: Centered text with an icon.