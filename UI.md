# UI Design - Library Page Enhancements

## Library Main Screen

The top of the Library screen will feature 5 primary entry points.

```text
+---------------------------------------+
|              Your Library             |
+---------------------------------------+
|                                       |
|  [ All Music ]      [ Artists   ]     |  <-- 2x3 Grid or Vertical List
|  [ Albums    ]      [ Playlists ]     |
|  [ Folders   ]                        |
|                                       |
+---------------------------------------+
|           Recently Played             |
|                                       |
|  [Song A]  [Song B]  [Song C]         |
|                                       |
+---------------------------------------+
|             All Songs                 |
|                                       |
|  ( ) Song 1                           |
|  ( ) Song 2                           |
|  ...                                  |
+---------------------------------------+
```

### Entry Button Style (Material 3 Card)
Each button will be a `Surface` or `ElevatedCard` with an icon and text.

```text
+-------------------+
|  (Icon)           |
|  All Music        |
+-------------------+
```

## Artist List Screen
```text
+---------------------------------------+
| < Artists                             |
+---------------------------------------+
| Search Artists...                     |
+---------------------------------------+
| Artist A                              |
| 5 Albums, 20 Songs                    |
|                                       |
| Artist B                              |
| 2 Albums, 10 Songs                    |
+---------------------------------------+
```

## Album List Screen
```text
+---------------------------------------+
| < Albums                              |
+---------------------------------------+
| Search Albums...                      |
+---------------------------------------+
| [Cover] Album Title                   |
|         Artist Name                   |
|                                       |
| [Cover] Album Title 2                 |
|         Artist Name                   |
+---------------------------------------+
```

## Folder List Screen
```text
+---------------------------------------+
| < Folders                             |
+---------------------------------------+
| /storage/emulated/0/Music             |
| 50 Songs                              |
|                                       |
| /storage/emulated/0/Download          |
| 10 Songs                              |
+---------------------------------------+
```
